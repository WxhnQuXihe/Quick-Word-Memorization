package com.shorthand.controller.admin;

import com.shorthand.common.Result;
import com.shorthand.service.IAdminImportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "后台-导入")
@RestController
@RequestMapping("/api/admin/import")
@RequiredArgsConstructor
public class AdminImportController {
    private final IAdminImportService adminImportService;

    @PostMapping("/wordbook")
    public Result importWordbook(@RequestParam Long wordbookId,
                                 @RequestParam("file") MultipartFile file,
                                 @RequestAttribute("adminId") Long adminId) {
        return Result.ok(adminImportService.importExcel(file, wordbookId, adminId));
    }

    @GetMapping("/logs")
    public Result logs(@RequestParam(defaultValue = "1") long pageNo,
                       @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(adminImportService.logs(pageNo, pageSize));
    }
}
