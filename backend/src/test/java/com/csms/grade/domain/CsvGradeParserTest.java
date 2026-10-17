package com.csms.grade.domain;
import com.csms.common.error.BizException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class CsvGradeParserTest {
    @Test void acceptsBomCrLfAndQuotedFields() {
        assertEquals(List.of(List.of("id","score"),List.of("1","90")),CsvGradeParser.parse("\uFEFFid,score\r\n1,\"90\"\r\n"));
    }
    @Test void keepsEscapedQuotesAndEmbeddedLineBreaks() {
        assertEquals(List.of(List.of("a\"b","a\nb")),CsvGradeParser.parse("\"a\"\"b\",\"a\nb\""));
    }
    @Test void rejectsBrokenQuotesAndOversizedFields() {
        for(String csv:List.of("\"unclosed","a\"b","\"a\"x","a".repeat(201))) assertThrows(BizException.class,()->CsvGradeParser.parse(csv));
    }
    @Test void rejectsMoreThanFiveHundredDataRecords() {
        assertThrows(BizException.class,()->CsvGradeParser.parse("a,b,c,d\n"+"1,2,3,4\n".repeat(501)));
    }
}
