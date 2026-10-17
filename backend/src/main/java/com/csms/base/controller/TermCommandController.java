package com.csms.base.controller;
import com.csms.base.service.TermCommandService;
import com.csms.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class TermCommandController {
    private final TermCommandService service;
    public TermCommandController(TermCommandService service) { this.service=service; }
    @PostMapping("/terms") public ApiResponse<?> create(@RequestBody Map<String,Object> b) { return ApiResponse.success(service.save(null,b)); }
    @PutMapping("/terms/{id}") public ApiResponse<?> update(@PathVariable Long id,@RequestBody Map<String,Object> b) { return ApiResponse.success(service.save(id,b)); }
    @PutMapping("/terms/{id}/status") public ApiResponse<?> status(@PathVariable Long id,@RequestBody Map<String,Object> b) { service.status(id,String.valueOf(b.get("status")));return ApiResponse.success(); }
}
