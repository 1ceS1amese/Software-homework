package com.csms.grade.service;
import com.csms.audit.service.AuditWriter;
import com.csms.common.context.CurrentUserHolder;
import com.csms.common.error.BizException;
import com.csms.common.service.CommandSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class GradeImportExpiryTest {
    static class TestClock extends Clock {
        Instant now=Instant.parse("2030-01-01T00:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now;}
    }
    @AfterEach void cleanContext(){CurrentUserHolder.remove();}
    @Test void expiredPreviewCannotWriteWithoutWaitingForRealTime() {
        var jdbc=mock(JdbcTemplate.class);var audit=mock(AuditWriter.class);var clock=new TestClock();
        var grades=spy(new GradeService(jdbc,audit));
        doNothing().when(grades).assertTeacherOwns(1L,2L,false);
        doReturn(List.of(Map.<String,Object>of("enrollmentId",10L,"status","DRAFT"))).when(grades).classGrades(1L);
        var imports=new GradeImportService(new CommandSupport(jdbc),grades,audit,new ObjectMapper(),clock);
        CurrentUserHolder.set(new CurrentUserHolder.UserContext(2L,"teacher","TEACHER",1L));
        var file=new MockMultipartFile("file","grades.csv","text/csv","enrollmentId,regularScore,midtermScore,finalScore\n10,80,81,82\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var result=imports.preview(1L,file);assertFalse(result.token().isBlank());
        clock.now=clock.now.plusSeconds(601);
        assertThrows(BizException.class,()->imports.confirm(1L,result.token()));
        verify(grades,never()).saveDraft(anyLong(),anyList());
    }
}
