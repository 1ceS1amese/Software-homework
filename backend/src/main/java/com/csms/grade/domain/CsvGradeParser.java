package com.csms.grade.domain;

import com.csms.common.error.*;
import java.util.*;

/** RFC 4180 引号/转义与 CRLF，兼容 UTF-8 BOM 和 LF；限制用于成绩文件。 */
public final class CsvGradeParser {
    private CsvGradeParser() {}
    public static List<List<String>> parse(String text) {
        if(text.startsWith("\uFEFF")) text=text.substring(1);
        List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder field=new StringBuilder();
        boolean quoted=false,closed=false;
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(quoted) {
                if(c=='"') { if(i+1<text.length() && text.charAt(i+1)=='"') { field.append('"');i++; } else { quoted=false;closed=true; } }
                else field.append(c);
            } else if(c==',' || c=='\r' || c=='\n') {
                row.add(field.toString());field.setLength(0);closed=false;
                if(c!=',') { append(rows,row);row=new ArrayList<>();if(c=='\r' && i+1<text.length() && text.charAt(i+1)=='\n') i++; }
            } else if(closed) throw invalid();
            else if(c=='"') { if(!field.isEmpty()) throw invalid();quoted=true; }
            else field.append(c);
            if(field.length()>200 || row.size()>4) throw invalid();
        }
        if(quoted) throw invalid();
        if(!field.isEmpty() || !row.isEmpty() || closed) { row.add(field.toString());append(rows,row); }
        return rows;
    }
    private static void append(List<List<String>> rows,List<String> row) {
        if(row.size()==1 && row.get(0).isBlank()) return;
        rows.add(row);if(rows.size()>501) throw invalid();
    }
    private static BizException invalid() { return new BizException(ErrorCode.GRADE_IMPORT_INVALID,"CSV格式错误、字段过长或超过500条；请使用模板和UTF-8编码"); }
}
