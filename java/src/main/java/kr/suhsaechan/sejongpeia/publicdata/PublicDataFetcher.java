package kr.suhsaechan.sejongpeia.publicdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 * 공개 데이터 요청 공통: 캐시 확인, 요청, 변환, 실패 격리
 * 같은 키 동시 호출은 요청 1회로 합치고, 실패 시 마지막 캐시(만료 포함) 또는 빈 값(잠시 기억), 캐시 항목 수 상한
 */
@Slf4j
final class PublicDataFetcher {

  // 학교 SIS 가 오류를 200 응답의 JSON 으로 돌려줄 때 쓰는 키
  private static final String SUBMIT_ERROR_KEY = "_SUBMIT_ERROR_";

  private final PublicDataHttpCaller httpCaller;
  private final Clock clock;
  private final Duration cacheTimeToLive;
  private final Duration failureMemoryDuration;
  private final int cacheMaxEntries;
  private final ObjectMapper objectMapper = new ObjectMapper();
  // 삽입 순서 유지: 맨 앞이 가장 오래된 항목 (접근은 모두 cacheByKey 모니터 안에서)
  private final LinkedHashMap<String, CachedValue> cacheByKey = new LinkedHashMap<>();
  private final ConcurrentHashMap<String, CompletableFuture<Object>> inFlightByKey = new ConcurrentHashMap<>();

  PublicDataFetcher(PublicDataHttpCaller httpCaller, Clock clock, Duration cacheTimeToLive,
      Duration failureMemoryDuration, int cacheMaxEntries) {
    this.httpCaller = httpCaller;
    this.clock = clock;
    this.cacheTimeToLive = cacheTimeToLive;
    this.failureMemoryDuration = failureMemoryDuration;
    this.cacheMaxEntries = Math.max(1, cacheMaxEntries);
  }

  /** 요청 1건 > 변환 */
  <T> T fetch(String cacheKey, PublicDataRequest request, Function<JsonNode, T> mapper, T emptyValue) {
    return fetchProduced(cacheKey, () -> mapper.apply(requestJson(request)), emptyValue);
  }

  /** 요청 여러 건을 순서대로 한 건씩 > 응답 목록 변환 (하나라도 실패하면 전체 실패) */
  <T> T fetchInSequence(String cacheKey, List<PublicDataRequest> requests, Function<List<JsonNode>, T> mapper, T emptyValue) {
    return fetchProduced(cacheKey, () -> {
      List<JsonNode> responses = new ArrayList<>();
      for (PublicDataRequest request : requests) {
        responses.add(requestJson(request));
      }
      return mapper.apply(responses);
    }, emptyValue);
  }

  @SuppressWarnings("unchecked")
  private <T> T fetchProduced(String cacheKey, Callable<T> producer, T emptyValue) {
    // 유효 캐시(값 또는 최근 실패 기억)면 요청 없음
    CachedValue cached = findCached(cacheKey);
    if (cached != null && isFresh(cached)) {
      return cached.isFailure ? emptyValue : (T) cached.value;
    }
    // 같은 키 진행 중이면 결과만 공유
    CompletableFuture<Object> myFuture = new CompletableFuture<>();
    CompletableFuture<Object> runningFuture = inFlightByKey.putIfAbsent(cacheKey, myFuture);
    if (runningFuture != null) {
      return (T) runningFuture.join();
    }
    // 예외 외의 Throwable(Error 등)로 죽어도 대기자는 만료 캐시 또는 빈 값을 받음
    Object outcome = staleOrEmpty(cacheKey, emptyValue);
    try {
      T result = load(cacheKey, producer, emptyValue);
      outcome = result;
      return result;
    } finally {
      inFlightByKey.remove(cacheKey);
      myFuture.complete(outcome);
    }
  }

  private <T> T load(String cacheKey, Callable<T> producer, T emptyValue) {
    try {
      T value = producer.call();
      putCached(cacheKey, new CachedValue(value, false, clock.instant()));
      return value;
    } catch (Exception e) {
      // 상류 실패 전부 격리 (서버 내부 정보 로그 금지, 예외 종류만)
      log.warn("[sejong public data] upstream failed : {} ({})", cacheKey.substring(0, cacheKey.indexOf('|')),
          e.getClass().getSimpleName());
      CachedValue stale = findCached(cacheKey);
      if (stale != null && !stale.isFailure) {
        return castValue(stale);
      }
      // 캐시 값이 없을 때만 실패 기억 (같은 키 연속 호출이 학교 서버를 두드리지 않게)
      putCached(cacheKey, new CachedValue(null, true, clock.instant()));
      return emptyValue;
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T castValue(CachedValue cached) {
    return (T) cached.value;
  }

  private Object staleOrEmpty(String cacheKey, Object emptyValue) {
    CachedValue stale = findCached(cacheKey);
    return stale != null && !stale.isFailure ? stale.value : emptyValue;
  }

  private CachedValue findCached(String cacheKey) {
    synchronized (cacheByKey) {
      return cacheByKey.get(cacheKey);
    }
  }

  /** 캐시 저장 (가장 최근 순서로 갱신, 가득 차면 가장 오래된 항목 제거) */
  private void putCached(String cacheKey, CachedValue cached) {
    synchronized (cacheByKey) {
      cacheByKey.remove(cacheKey);
      cacheByKey.put(cacheKey, cached);
      Iterator<String> oldestFirst = cacheByKey.keySet().iterator();
      while (cacheByKey.size() > cacheMaxEntries) {
        oldestFirst.next();
        oldestFirst.remove();
      }
    }
  }

  /** 요청 1건 > JSON (2xx 아님, JSON 아님, _SUBMIT_ERROR_ 는 예외) */
  private JsonNode requestJson(PublicDataRequest request) throws Exception {
    PublicDataHttpResponse response = httpCaller.call(request);
    if (!response.isSuccessful()) {
      throw new IllegalStateException("http status " + response.getStatusCode());
    }
    JsonNode root = objectMapper.readTree(response.getBody());
    if (root == null || !root.isContainerNode()) {
      throw new IllegalStateException("body is not json");
    }
    if (root.has(SUBMIT_ERROR_KEY)) {
      throw new IllegalStateException("school reported a submit error");
    }
    return root;
  }

  private boolean isFresh(CachedValue cached) {
    Duration validFor = cached.isFailure ? failureMemoryDuration : cacheTimeToLive;
    return Duration.between(cached.fetchedAt, clock.instant()).compareTo(validFor) < 0;
  }

  private static final class CachedValue {
    private final Object value;
    // true: 값 없이 실패만 기억한 항목
    private final boolean isFailure;
    private final Instant fetchedAt;

    private CachedValue(Object value, boolean isFailure, Instant fetchedAt) {
      this.value = value;
      this.isFailure = isFailure;
      this.fetchedAt = fetchedAt;
    }
  }
}
