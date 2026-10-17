package com.csms.grade.controller;
import com.csms.common.api.ApiResponse;
import com.csms.grade.service.GradeImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
@RestController
@RequestMapping("/teaching-classes/{id}/grades/import")
public class GradeImportController {
    private final GradeImportService service;
    public GradeImportController(GradeImportService service) { this.service=service; }
    @PostMapping public ResponseEntity<?> preview(@PathVariable Long id,@RequestParam MultipartFile file) {
        var result=service.preview(id,file);
        return ResponseEntity.ok().header("X-Import-Token",result.token()).header("X-Import-Expires",Long.toString(result.expiresAt())).body(ApiResponse.success(result.items()));
    }
    @PostMapping("/confirm") public ApiResponse<?> confirm(@PathVariable Long id,@RequestBody Map<String,String> body) { return ApiResponse.success(service.confirm(id,body.get("previewToken"))); }
}
