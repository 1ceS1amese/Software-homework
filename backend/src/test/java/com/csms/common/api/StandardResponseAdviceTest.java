package com.csms.common.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class StandardResponseAdviceTest {
    @Test void preservesLongIdsBeforeJavascriptParsingAndKeepsNumbersAndDates() {
        var advice=new StandardResponseAdvice(new ObjectMapper());
        assertEquals(Map.of("id","9007199254740993","teacherId","9223372036854775807","credit",3,"createdAt","2026-10-18 09:08:07","conflictIds",List.of("9007199254740993")),
            advice.normalize(Map.of("id",9007199254740993L,"teacher_id",Long.MAX_VALUE,"credit",3,"created_at",java.sql.Timestamp.valueOf("2026-10-18 09:08:07"),"conflictIds",List.of(9007199254740993L)),""));
    }
}
