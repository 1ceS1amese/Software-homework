package com.csms.enroll.domain;

import java.util.*;

/** 按星期、节次组织每格全部课程；冲突额外比较实际分钟、周次与学期。 */
public final class TimetableBuilder {
    private TimetableBuilder() {}
    @SuppressWarnings("unchecked")
    public static Map<String,Object> build(List<Map<String,Object>> classes,Integer week) {
        List<List<List<Map<String,Object>>>> grid=new ArrayList<>();
        for(int d=0;d<7;d++) { var day=new ArrayList<List<Map<String,Object>>>();for(int s=0;s<12;s++) day.add(new ArrayList<>());grid.add(day); }
        List<Map<String,Object>> entries=new ArrayList<>();
        for(var tc:classes) for(var schedule:(List<Map<String,Object>>)tc.getOrDefault("schedules",List.of())) {
            if(week!=null&&(week<n(schedule,"startWeek")||week>n(schedule,"endWeek"))) continue;
            var entry=new LinkedHashMap<>(tc);entry.remove("schedules");entry.putAll(schedule);entry.put("teachingClassId",tc.get("id"));entry.put("termId",tc.get("termId"));
            entries.add(entry);
            for(int s=n(schedule,"startSection");s<=n(schedule,"endSection");s++) grid.get(n(schedule,"dayOfWeek")-1).get(s-1).add(entry);
        }
        Set<Object> conflicts=new LinkedHashSet<>();
        for(int i=0;i<entries.size();i++) for(int j=i+1;j<entries.size();j++) {
            var a=entries.get(i);var b=entries.get(j);
            if(!Objects.equals(a.get("termId"),b.get("termId"))||Objects.equals(a.get("teachingClassId"),b.get("teachingClassId"))) continue;
            if(n(a,"dayOfWeek")==n(b,"dayOfWeek")&&n(a,"startMinute")<n(b,"endMinute")&&n(b,"startMinute")<n(a,"endMinute")
                &&n(a,"startWeek")<=n(b,"endWeek")&&n(b,"startWeek")<=n(a,"endWeek")) { conflicts.add(a.get("teachingClassId"));conflicts.add(b.get("teachingClassId")); }
        }
                var result=new LinkedHashMap<String,Object>();
        result.put("grid",grid);result.put("conflictIds",conflicts);result.put("courses",classes);
        result.put("conflictedEnrollmentIds",classes.stream().filter(c->conflicts.contains(c.get("id"))).map(c->c.get("enrollmentId")).toList());
        result.put("startWeek",classes.stream().mapToInt(c->n(c,"startWeek")).min().orElse(1));
        result.put("endWeek",classes.stream().mapToInt(c->n(c,"endWeek")).max().orElse(16));
        result.put("summary",Map.of("enrolledCount",classes.size(),"totalCredit",classes.stream().map(c->new java.math.BigDecimal(c.get("credit").toString())).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add)));
        return result;
    }
    private static int n(Map<String,Object> row,String key) { return ((Number)row.get(key)).intValue(); }
}
