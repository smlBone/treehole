package org.mf.treehole.controller;

import jakarta.annotation.Resource;
import org.mf.treehole.common.Result;
import org.mf.treehole.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Resource
    private FileService fileService;

    @PostMapping("/upload")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return fileService.uploadImage(file);
    }
}
