package com.csms.base.controller;

import com.csms.base.service.OrganizationService;
import com.csms.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class OrganizationController {
    private final OrganizationService service;
    public OrganizationController(OrganizationService service) { this.service=service; }
    @PostMapping("/depts") public ApiResponse<?> createDept(@RequestBody Map<String,Object> b) { return ApiResponse.success(service.save("dept",null,b)); }
    @PutMapping("/depts/{id}") public ApiResponse<?> updateDept(@PathVariable Long id,@RequestBody Map<String,Object> b) { return ApiResponse.success(service.save("dept",id,b)); }
    @DeleteMapping("/depts/{id}") public ApiResponse<?> deleteDept(@PathVariable Long id) { service.delete("dept",id); return ApiResponse.success(); }
    @PostMapping("/majors") public ApiResponse<?> createMajor(@RequestBody Map<String,Object> b) { return ApiResponse.success(service.save("major",null,b)); }
    @PutMapping("/majors/{id}") public ApiResponse<?> updateMajor(@PathVariable Long id,@RequestBody Map<String,Object> b) { return ApiResponse.success(service.save("major",id,b)); }
    @DeleteMapping("/majors/{id}") public ApiResponse<?> deleteMajor(@PathVariable Long id) { service.delete("major",id); return ApiResponse.success(); }
}
