package com.csms.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import javax.sql.DataSource;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.flyway.enabled=false","csms.jwt.secret=contract-test-only-0123456789abcdef0123456789abcdef","spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"})
class ApiContractMappingTest {
    @MockBean DataSource dataSource; // No DB process/connection: only inspect actual Spring registered mappings.
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;
    @Test void allDeliveredContractMethodsAreRegisteredUnderVersionedPaths() throws Exception {
        Set<String> actual=new HashSet<>();
        mappings.getHandlerMethods().keySet().forEach(info->info.getMethodsCondition().getMethods().forEach(method->info.getPatternValues().forEach(path->actual.add(method+" "+normalize(path)))));
        String document=Files.readString(Path.of("../docs/05-接口设计与API清单.md"));
        var matcher=Pattern.compile("(?m)^\\| ([A-Z]-\\d+) \\| (GET|POST|PUT|DELETE) \\| `([^`]+)`").matcher(document);
        Set<String> pending=Set.of();int count=0;
        while(matcher.find()) { count++;if(!pending.contains(matcher.group(1))) assertTrue(actual.contains(matcher.group(2)+" "+normalize("/api/v1"+matcher.group(3))),matcher.group(1)+" "+matcher.group(3)); }
        assertEquals(77,count);
    }
    private String normalize(String path) { return path.replaceAll("\\{[^}]+}","{}"); }
}
