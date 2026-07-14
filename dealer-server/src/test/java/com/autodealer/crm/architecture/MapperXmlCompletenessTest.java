package com.autodealer.crm.architecture;

import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapperXmlCompletenessTest {

    private static final Pattern NAMESPACE_PATTERN = Pattern.compile(
            "<mapper\\s+namespace=\"([^\"]+)\"");

    @Test
    void mapperXml_whenDiscovered_shouldReferenceAnnotatedMapperInterface() throws Exception {
        Path mapperRoot = resourceRoot().resolve("mapper");
        List<String> violations = new ArrayList<>();
        Map<String, String> namespaceOwners = new HashMap<>();
        int xmlCount = 0;

        try (Stream<Path> paths = Files.walk(mapperRoot)) {
            for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".xml")).toList()) {
                xmlCount++;
                String relativePath = mapperRoot.relativize(path).toString();
                Matcher matcher = NAMESPACE_PATTERN.matcher(Files.readString(path));
                if (!matcher.find()) {
                    violations.add(relativePath + " 缺少 mapper namespace");
                    continue;
                }
                String namespace = matcher.group(1);
                String previousOwner = namespaceOwners.putIfAbsent(namespace, relativePath);
                if (previousOwner != null) {
                    violations.add(namespace + " 被多个 XML 使用: " + previousOwner + ", " + relativePath);
                }
                try {
                    Class<?> mapperType = Class.forName(namespace);
                    if (!mapperType.isInterface()) {
                        violations.add(relativePath + " namespace 不是接口: " + namespace);
                    } else if (!mapperType.isAnnotationPresent(Mapper.class)) {
                        violations.add(relativePath + " namespace 未标注 @Mapper: " + namespace);
                    }
                } catch (ClassNotFoundException exception) {
                    violations.add(relativePath + " namespace 不存在: " + namespace);
                }
            }
        }

        assertTrue(xmlCount >= 75, "Mapper XML 数量异常减少: " + xmlCount);
        assertEquals(List.of(), violations,
                () -> "Mapper XML 完整性检查失败:\n" + String.join("\n", violations));
    }

    @Test
    void mybatisProfiles_whenLoadingMapperXml_shouldUseRecursivePattern() throws IOException {
        List<Path> configurations = List.of(
                resourceRoot().resolve("application.yml"),
                resourceRoot().resolve("application-smoke.yml"),
                testResourceRoot().resolve("application-test.yml")
        );
        List<String> violations = configurations.stream()
                .filter(path -> {
                    try {
                        return !Files.readString(path).contains("mapper-locations: classpath*:mapper/**/*.xml");
                    } catch (IOException exception) {
                        throw new IllegalStateException("读取配置失败: " + path, exception);
                    }
                })
                .map(Path::toString)
                .toList();

        assertEquals(List.of(), violations,
                () -> "MyBatis Profile 未使用递归 Mapper XML 路径:\n" + String.join("\n", violations));
    }

    private Path resourceRoot() {
        return locate("src/main/resources", "dealer-server/src/main/resources");
    }

    private Path testResourceRoot() {
        return locate("src/test/resources", "dealer-server/src/test/resources");
    }

    private Path locate(String modulePath, String repositoryPath) {
        Path moduleRoot = Path.of(modulePath);
        if (Files.isDirectory(moduleRoot)) {
            return moduleRoot;
        }
        Path repositoryRoot = Path.of(repositoryPath);
        if (Files.isDirectory(repositoryRoot)) {
            return repositoryRoot;
        }
        throw new IllegalStateException("无法定位资源目录: " + modulePath);
    }
}
