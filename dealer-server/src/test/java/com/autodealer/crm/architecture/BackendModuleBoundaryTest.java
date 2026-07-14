package com.autodealer.crm.architecture;

import com.autodealer.crm.modules.analytics.application.api.StatisticService;
import com.autodealer.crm.modules.analytics.application.api.result.NameValue;
import com.autodealer.crm.modules.analytics.application.api.result.SummaryData;
import com.autodealer.crm.modules.analytics.application.internal.StatisticManager;
import com.autodealer.crm.modules.analytics.application.internal.StatisticServiceImpl;
import com.autodealer.crm.modules.analytics.web.StatisticController;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendModuleBoundaryTest {

    private static final String ROOT_PACKAGE = "com.autodealer.crm";
    private static final String MODULES_PACKAGE = ROOT_PACKAGE + ".modules.";
    private static final String SHARED_PACKAGE = ROOT_PACKAGE + ".shared";
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^package\\s+([a-zA-Z0-9_.]+);$");
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "^import\\s+(?:static\\s+)?(com\\.autodealer\\.crm\\.[a-zA-Z0-9_.*]+);$");
    private static final Set<String> GROUPED_MODULES = Set.of("sales", "commerce", "fulfillment");

    @Test
    void productionSource_shouldOnlyUseBootstrapSharedAndModulesRoots() throws IOException {
        List<String> violations = loadProductionSources().stream()
                .filter(source -> !source.packageName().equals(ROOT_PACKAGE + ".bootstrap"))
                .filter(source -> !source.packageName().startsWith(ROOT_PACKAGE + ".bootstrap."))
                .filter(source -> !source.packageName().equals(SHARED_PACKAGE))
                .filter(source -> !source.packageName().startsWith(SHARED_PACKAGE + "."))
                .filter(source -> !source.packageName().equals(ROOT_PACKAGE + ".modules"))
                .filter(source -> !source.packageName().startsWith(ROOT_PACKAGE + ".modules."))
                .map(JavaSource::relativePath)
                .toList();

        assertEquals(List.of(), violations,
                () -> "生产代码只能位于 bootstrap、shared、modules:\n" + String.join("\n", violations));
    }

    @Test
    void sharedSource_whenImportingBusinessModule_shouldBeRejected() throws IOException {
        List<String> violations = new ArrayList<>();
        for (JavaSource source : loadProductionSources()) {
            if (!source.packageName().equals(SHARED_PACKAGE)
                    && !source.packageName().startsWith(SHARED_PACKAGE + ".")) {
                continue;
            }
            source.imports().stream()
                    .filter(imported -> imported.startsWith(MODULES_PACKAGE))
                    .forEach(imported -> violations.add(source.relativePath() + " -> " + imported));
        }

        assertEquals(List.of(), violations,
                () -> "shared 禁止依赖业务模块:\n" + String.join("\n", violations));
    }

    @Test
    void moduleSource_whenImportingAnotherModuleInternalPackage_shouldBeRejected() throws IOException {
        List<String> violations = new ArrayList<>();
        for (JavaSource source : loadProductionSources()) {
            String sourceModule = moduleName(source.packageName());
            if (sourceModule == null) {
                continue;
            }
            for (String imported : source.imports()) {
                String targetModule = moduleName(imported);
                if (targetModule == null || targetModule.equals(sourceModule)) {
                    continue;
                }
                if (!isPublicApplicationApi(imported)) {
                    violations.add(source.relativePath() + " -> " + imported);
                }
            }
        }

        assertEquals(List.of(), violations,
                () -> "跨模块只能依赖 application.api:\n" + String.join("\n", violations));
    }

    @Test
    void analyticsModule_whenMigrated_shouldHaveNoLegacyClassPath() throws IOException {
        Path sourceRoot = productionSourceRoot();
        List<String> legacyPaths = List.of(
                "com/autodealer/crm/web/StatisticController.java",
                "com/autodealer/crm/service/StatisticService.java",
                "com/autodealer/crm/service/impl/StatisticServiceImpl.java",
                "com/autodealer/crm/manager/StatisticManager.java",
                "com/autodealer/crm/result/NameValue.java",
                "com/autodealer/crm/result/SummaryData.java"
        );
        List<String> targetPaths = List.of(
                "com/autodealer/crm/modules/analytics/web/StatisticController.java",
                "com/autodealer/crm/modules/analytics/application/api/StatisticService.java",
                "com/autodealer/crm/modules/analytics/application/api/result/NameValue.java",
                "com/autodealer/crm/modules/analytics/application/api/result/SummaryData.java",
                "com/autodealer/crm/modules/analytics/application/internal/StatisticServiceImpl.java",
                "com/autodealer/crm/modules/analytics/application/internal/StatisticManager.java"
        );

        for (String legacyPath : legacyPaths) {
            assertFalse(Files.exists(sourceRoot.resolve(legacyPath)), "旧统计路径仍存在: " + legacyPath);
        }
        for (String targetPath : targetPaths) {
            assertTrue(Files.isRegularFile(sourceRoot.resolve(targetPath)), "统计模块目标文件缺失: " + targetPath);
        }
    }

    @Test
    void mapperInterface_whenDiscovered_shouldDeclareMapperAnnotation() throws IOException {
        Path sourceRoot = productionSourceRoot();
        List<String> missingAnnotations = new ArrayList<>();
        int mapperCount = 0;
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            for (Path path : paths.filter(candidate -> candidate.getFileName().toString().endsWith("Mapper.java"))
                    .toList()) {
                mapperCount++;
                if (!Files.readString(path).contains("@Mapper")) {
                    missingAnnotations.add(sourceRoot.relativize(path).toString());
                }
            }
        }

        assertTrue(mapperCount >= 77, "Mapper 数量异常减少: " + mapperCount);
        assertEquals(List.of(), missingAnnotations,
                () -> "Mapper 接口必须显式标注 @Mapper:\n" + String.join("\n", missingAnnotations));
    }

    private List<JavaSource> loadProductionSources() throws IOException {
        Path sourceRoot = productionSourceRoot();
        List<JavaSource> sources = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(path);
                String packageName = lines.stream()
                        .map(String::trim)
                        .map(PACKAGE_PATTERN::matcher)
                        .filter(Matcher::matches)
                        .map(matcher -> matcher.group(1))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Java 文件缺少 package: " + path));
                List<String> imports = lines.stream()
                        .map(String::trim)
                        .map(IMPORT_PATTERN::matcher)
                        .filter(Matcher::matches)
                        .map(matcher -> matcher.group(1))
                        .toList();
                sources.add(new JavaSource(sourceRoot.relativize(path).toString(), packageName, imports));
            }
        }
        return sources;
    }

    private Path productionSourceRoot() {
        Path moduleRoot = Path.of("src/main/java");
        if (Files.isDirectory(moduleRoot)) {
            return moduleRoot;
        }
        Path repositoryRoot = Path.of("dealer-server/src/main/java");
        if (Files.isDirectory(repositoryRoot)) {
            return repositoryRoot;
        }
        throw new IllegalStateException("无法定位 dealer-server/src/main/java");
    }

    private String moduleName(String qualifiedName) {
        if (!qualifiedName.startsWith(MODULES_PACKAGE)) {
            return null;
        }
        String[] segments = qualifiedName.substring(MODULES_PACKAGE.length()).split("\\.");
        if (segments.length == 0 || segments[0].isBlank()) {
            return null;
        }
        if (GROUPED_MODULES.contains(segments[0]) && segments.length >= 2) {
            return segments[0] + "." + segments[1];
        }
        return segments[0];
    }

    private boolean isPublicApplicationApi(String imported) {
        return imported.contains(".application.api.")
                || imported.endsWith(".application.api")
                || imported.endsWith(".application.api.*");
    }

    private record JavaSource(String relativePath, String packageName, List<String> imports) {
    }
}
