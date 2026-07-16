package org.mf.treehole.controller;

import jakarta.annotation.Resource;
import org.mf.treehole.common.Result;
import org.mf.treehole.service.LikeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/likes")
public class LikeController {

    @Resource
    private LikeService likeService;

    @PostMapping("/{targetType}/{targetId}")
    public Result<?> toggleLike(@PathVariable String targetType, @PathVariable Long targetId) {
        return likeService.toggleLike(targetType, targetId);
    }
}
