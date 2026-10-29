package kr.suhsaechan.sejongpeia.publicdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 * 공개 데이터 요청 공통: 캐시 확인, 요청, 변환, 실패 격리
 * 같은 키 동시 호출은 요청 1회로 합치고, 실패 시 마지막 캐시(만료 포함) 또는 빈 값
 */
@Slf4j
final class PublicDataFetcher {

  // 학교 SIS 가 오류를 200 응답의 JSON 으로 돌려줄 때 쓰는 키
  private static final String SUBMIT_ERROR_KEY = "_SUBMIT_ERROR_";

  private final PublicDataHttpCaller httpCaller;
  private final Clock clock;
  private final Duration cacheTimeToLive;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ConcurrentHashMap<String, CachedValue> cacheByKey = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, CompletableFuture<Object>> inFlightByKey = new ConcurrentHashMap<>();

  PublicDataFetcher(PublicDataHttpCaller httpCaller, Clock clock, Duration cacheTimeToLive) {
    this.httpCaller = httpCaller;
    this.clock = clock;
    this.cacheTimeToLive = cacheTimeToLive;
  }

  @SuppressWarnings("unchecked")
  <T> T fetch(String cacheKey, PublicDataRequest request, Function<JsonNode, T> mapper, T emptyValue) {
    // 유효 캐시면 요청 없음
    CachedValue cached = cacheByKey.get(cacheKey);
    if (cached != null && isFresh(cached)) {
      return (T) cached.value;
    }
    // 같은 키 진행 중이면 결과만 공유
    CompletableFuture<Object> myFuture = new CompletableFuture<>();
    CompletableFuture<Object> runningFuture = inFlightByKey.putIfAbsent(cacheKey, myFuture);
    if (runningFuture != null) {
      return (T) runningFuture.join();
    }
    try {
      T result = load(cacheKey, request, mapper, emptyValue);
      myFuture.complete(result);
      return result;
    } finally {
      inFlightByKey.remove(cacheKey);
      myFuture.complete(emptyValue); // 이미 완료됐으면 무시 (예기치 못한 오류에도 대기 해제)
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T load(String cacheKey, PublicDataRequest request, Function<JsonNode, T> mapper, T emptyValue) {
    try {
      T value = mapper.apply(requestJson(request));
      cacheByKey.put(cacheKey, new CachedValue(value, clock.instant()));
      return value;
    } catch (Exception e) {
      // 상류 실패 전부 격리 (서버 내부 정보 로그 금지, 예외 종류만)
      log.warn("[sejong public data] upstream failed : {} ({})", cacheKey.substring(0, cacheKey.indexOf('|')),
          e.getClass().getSimpleName());
      CachedValue stale = cacheByKey.get(cacheKey);
      return stale != null ? (T) stale.value : emptyValue;
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
    return Duration.between(cached.fetchedAt, clock.instant()).compareTo(cacheTimeToLive) < 0;
  }

  private static final class CachedValue {
    private final Object value;
    private final Instant fetchedAt;

    private CachedValue(Object value, Instant fetchedAt) {
      this.value = value;
      this.fetchedAt = fetchedAt;
    }
  }
}
