package org.mf.treehole.controller;

import jakarta.annotation.Resource;
import org.mf.treehole.common.Result;
import org.mf.treehole.dto.CreateCommentRequest;
import org.mf.treehole.service.CommentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Resource
    private CommentService commentService;

    @PostMapping
    public Result<?> createComment(@RequestBody CreateCommentRequest req) {
        return commentService.createComment(req);
    }

    @GetMapping("/post/{postId}")
    public Result<?> listComments(@PathVariable Long postId) {
        return commentService.listComments(postId);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        return commentService.deleteComment(id);
    }
}
