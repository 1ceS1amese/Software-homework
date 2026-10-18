package com.csms.audit.service;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class AuditWriterTest {
    @Test void removesNestedCredentialsButKeepsBusinessChanges() {
        var input=Map.of("name","课程","password","sensitive","rows",List.of(Map.of("accessToken","secret","score",85)),"nested",Map.of("Authorization","Bearer secret","id",1));
        assertEquals(Map.of("name","课程","rows",List.of(Map.of("score",85)),"nested",Map.of("id",1)),AuditWriter.redact(input));
    }
}
