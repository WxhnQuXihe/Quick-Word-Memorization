package com.shorthand.controller.admin;

import com.shorthand.common.Result;
import com.shorthand.domain.bo.WordSaveBo;
import com.shorthand.service.IAdminWordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/words")
@RequiredArgsConstructor
public class AdminWordController {
    private final IAdminWordService adminWordService;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") long pageNo,
                       @RequestParam(defaultValue = "20") long pageSize,
                       @RequestParam(required = false) String keyword) {
        return Result.ok(adminWordService.list(pageNo, pageSize, keyword));
    }

    @PostMapping
    public Result create(@Valid @RequestBody WordSaveBo bo) {
        return Result.ok(adminWordService.create(bo));
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Valid @RequestBody WordSaveBo bo) {
        adminWordService.update(id, bo);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        adminWordService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audio")
    public Result uploadAudio(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return Result.ok(java.util.Map.of("audioUrl", adminWordService.uploadAudio(id, file)));
    }
}
