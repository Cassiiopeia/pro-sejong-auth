package kr.suhsaechan.sejongpeia.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 의존 방향 규칙 3계층: 공통(common)은 소스를 모르고, 소스(auth, publicdata)는 공통만 쓰며 서로 임포트하지 않음
 * config(Spring 자동 설정)는 조립 지점이라 검사 대상이 아님
 */
class DependencyRulesTest {

  private static final Path MAIN_SOURCE = Path.of("src/main/java/kr/suhsaechan/sejongpeia");
  // 내부 임포트의 최상위 패키지 (kr.suhsaechan.sejongpeia.<여기>.…)
  private static final Pattern INTERNAL_IMPORT = Pattern.compile("^import kr\\.suhsaechan\\.sejongpeia\\.(\\w+)\\.", Pattern.MULTILINE);
  // 각 계층이 임포트해도 되는 계층 (자기 자신 제외)
  private static final Map<String, Set<String>> ALLOWED_LAYERS_BY_LAYER = Map.of(
      "common", Set.of(),
      "auth", Set.of("common"),
      "publicdata", Set.of("common"));

  // 공개 데이터 안쪽 규칙: 출처 파일(*Source.java)은 다른 출처와 진입점(SejongPublicData)을 참조하지 않고 공유 클래스만 쓴다
  // 같은 패키지라 import 문이 없어도 참조할 수 있으므로 클래스 이름 사용 자체를 검사
  private static final Pattern PUBLIC_DATA_ENTRY_OR_SOURCE_REFERENCE = Pattern.compile("\\b(\\w+Source|SejongPublicData)\\b");

  @Test
  void everyLayerHasFiles() throws IOException {
    for (String layer : ALLOWED_LAYERS_BY_LAYER.keySet()) {
      assertThat(javaFilesUnder(layer)).as("%s has no java files: rule test would pass without checking anything", layer).isNotEmpty();
    }
  }

  @Test
  void layersImportOnlyAllowedLayers() throws IOException {
    for (Map.Entry<String, Set<String>> entry : ALLOWED_LAYERS_BY_LAYER.entrySet()) {
      String layer = entry.getKey();
      for (Path file : javaFilesUnder(layer)) {
        List<String> forbiddenLayers = importedLayers(Files.readString(file)).stream()
            .filter(importedLayer -> !importedLayer.equals(layer) && !entry.getValue().contains(importedLayer))
            .collect(Collectors.toList());
        assertThat(forbiddenLayers)
            .as("%s/%s imports %s: sources share only common, common knows no source", layer, file.getFileName(), forbiddenLayers)
            .isEmpty();
      }
    }
  }

  @Test
  void publicDataSourcesReferenceOnlySharedHelpers() throws IOException {
    List<Path> sourceFiles = publicDataSourceFiles();
    assertThat(sourceFiles).as("rule test would pass without checking anything").isNotEmpty();
    for (Path file : sourceFiles) {
      String ownClassName = file.getFileName().toString().replace(".java", "");
      List<String> forbiddenClasses = referencedEntryOrSourceClasses(Files.readString(file)).stream()
          .filter(referencedClass -> !referencedClass.equals(ownClassName))
          .distinct()
          .collect(Collectors.toList());
      assertThat(forbiddenClasses)
          .as("publicdata/%s references %s: sources use only the shared helpers and common", file.getFileName(), forbiddenClasses)
          .isEmpty();
    }
  }

  private static List<Path> publicDataSourceFiles() throws IOException {
    try (Stream<Path> files = Files.list(MAIN_SOURCE.resolve("publicdata"))) {
      return files.filter(path -> path.getFileName().toString().endsWith("Source.java")).collect(Collectors.toList());
    }
  }

  private static List<String> referencedEntryOrSourceClasses(String text) {
    Matcher matcher = PUBLIC_DATA_ENTRY_OR_SOURCE_REFERENCE.matcher(text);
    List<String> referencedClasses = new ArrayList<>();
    while (matcher.find()) {
      referencedClasses.add(matcher.group(1));
    }
    return referencedClasses;
  }

  private static List<Path> javaFilesUnder(String layer) throws IOException {
    try (Stream<Path> files = Files.walk(MAIN_SOURCE.resolve(layer))) {
      return files.filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList());
    }
  }

  private static List<String> importedLayers(String text) {
    Matcher matcher = INTERNAL_IMPORT.matcher(text);
    List<String> layers = new ArrayList<>();
    while (matcher.find()) {
      layers.add(matcher.group(1));
    }
    return layers;
  }
}
