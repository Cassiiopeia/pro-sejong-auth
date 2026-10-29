package kr.suhsaechan.sejongpeia.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 의존 방향 규칙: 공개 데이터 소스는 인증 흐름(service, parser, 인증 client)을 가져가지 않고, 인증은 공개 데이터를 가져가지 않음
 * 공유 허용: SejongHttpSupport(레거시 TLS), config, exception
 */
class DependencyRulesTest {

  private static final Path MAIN_SOURCE = Path.of("src/main/java/kr/suhsaechan/sejongpeia");
  private static final Pattern INTERNAL_IMPORT = Pattern.compile("^import kr\\.suhsaechan\\.sejongpeia\\.([\\w.]+?)(?:\\.\\*|\\.[A-Z]\\w*);", Pattern.MULTILINE);
  // 공개 데이터가 가져가도 되는 공통 계층 (패키지 이름 기준, client 는 클래스 단위로 따로 확인)
  private static final List<String> SHARED_PACKAGES = List.of("config", "exception", "publicdata");
  private static final String SHARED_CLIENT_CLASS = "import kr.suhsaechan.sejongpeia.client.SejongHttpSupport;";

  @Test
  void publicDataImportsOnlyTheSharedLayer() throws IOException {
    for (Path file : javaFilesUnder("publicdata")) {
      String text = Files.readString(file);
      for (String importedPackage : importedPackages(text)) {
        boolean isShared = SHARED_PACKAGES.stream().anyMatch(shared -> importedPackage.equals(shared) || importedPackage.startsWith(shared + "."));
        boolean isSharedClient = importedPackage.equals("client") && text.contains(SHARED_CLIENT_CLASS);
        assertThat(isShared || isSharedClient)
            .as("%s imports '%s': share only config, exception and SejongHttpSupport", file.getFileName(), importedPackage)
            .isTrue();
      }
    }
  }

  @Test
  void authenticationNeverImportsPublicData() throws IOException {
    for (String authenticationPackage : List.of("service", "parser", "client")) {
      for (Path file : javaFilesUnder(authenticationPackage)) {
        assertThat(Files.readString(file))
            .as("%s must not depend on public data sources", file.getFileName())
            .doesNotContain("kr.suhsaechan.sejongpeia.publicdata");
      }
    }
  }

  private static List<Path> javaFilesUnder(String packageName) throws IOException {
    try (Stream<Path> files = Files.walk(MAIN_SOURCE.resolve(packageName))) {
      return files.filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList());
    }
  }

  private static List<String> importedPackages(String text) {
    Matcher matcher = INTERNAL_IMPORT.matcher(text);
    List<String> packages = new java.util.ArrayList<>();
    while (matcher.find()) {
      packages.add(matcher.group(1));
    }
    return packages;
  }
}
