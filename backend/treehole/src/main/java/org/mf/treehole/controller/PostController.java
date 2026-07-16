package org.mf.treehole.controller;

import jakarta.annotation.Resource;
import org.mf.treehole.common.Result;
import org.mf.treehole.dto.CreatePostRequest;
import org.mf.treehole.service.PostService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Resource
    private PostService postService;

    @PostMapping
    public Result<?> createPost(@RequestBody CreatePostRequest req) {
        return postService.createPost(req);
    }

    @GetMapping("/public/list")
    public Result<?> listPosts(@RequestParam String board,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        return postService.listPosts(board, page, size);
    }

    @GetMapping("/public/{id}")
    public Result<?> getPostDetail(@PathVariable Long id) {
        return postService.getPostDetail(id);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        return postService.deletePost(id);
    }

    @PostMapping("/{id}/share")
    public Result<Void> sharePost(@PathVariable Long id) {
        return postService.sharePost(id);
    }

    @GetMapping("/user/{userId}")
    public Result<?> getUserPosts(@PathVariable Long userId,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        return postService.getUserPosts(userId, page, size);
    }

    @GetMapping("/history")
    public Result<?> getBrowsingHistory(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return postService.getBrowsingHistory(page, size);
    }

    @GetMapping("/pending")
    public Result<?> getPendingPosts(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return postService.getPendingPosts(page, size);
    }

    @PostMapping("/{id}/review")
    public Result<Void> reviewPost(@PathVariable Long id, @RequestParam String action) {
        return postService.reviewPost(id, action);
    }
}
