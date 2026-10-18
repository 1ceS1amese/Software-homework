package com.csms.enroll.domain;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class TimetableBuilderTest {
    private Map<String,Object> course(long id,long term,int start,int end,int firstWeek,int lastWeek) {
        var row=new HashMap<String,Object>();row.put("id",id);row.put("enrollmentId",id+100);row.put("termId",term);row.put("credit",3);row.put("startWeek",firstWeek);row.put("endWeek",lastWeek);
        row.put("schedules",List.of(Map.of("id",id+200,"dayOfWeek",1,"startSection",1,"endSection",2,"startMinute",start,"endMinute",end,"startWeek",firstWeek,"endWeek",lastWeek)));return row;
    }
    @Test void marksBothEnrollmentsAndDoesNotConfuseAdjacentTimesWeeksOrTerms() {
        var result=TimetableBuilder.build(List.of(course(1,1,480,600,1,8),course(2,1,550,660,8,16),course(3,1,600,700,1,7),course(4,2,480,600,1,16)),null);
        assertEquals(List.of(101L,102L),result.get("conflictedEnrollmentIds"));
        assertEquals(7,((List<?>)result.get("grid")).size());
        assertEquals(List.of(),TimetableBuilder.build(List.of(course(1,1,480,600,1,8),course(2,1,550,660,8,16)),9).get("conflictedEnrollmentIds"));
    }
}
