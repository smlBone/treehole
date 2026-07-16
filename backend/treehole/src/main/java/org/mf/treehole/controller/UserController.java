package org.mf.treehole.controller;

import jakarta.annotation.Resource;
import org.mf.treehole.common.Result;
import org.mf.treehole.dto.ChangePasswordRequest;
import org.mf.treehole.dto.UpdateProfileRequest;
import org.mf.treehole.entity.User;
import org.mf.treehole.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping("/me")
    public Result<User> getMyProfile() {
        return userService.getMyProfile();
    }

    @GetMapping("/{id}")
    public Result<User> getUserProfile(@PathVariable Long id) {
        return userService.getUserProfile(id);
    }

    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody UpdateProfileRequest req) {
        return userService.updateProfile(req);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest req) {
        return userService.changePassword(req);
    }

    @PostMapping("/{id}/follow")
    public Result<Void> follow(@PathVariable Long id) {
        return userService.follow(id);
    }

    @DeleteMapping("/{id}/follow")
    public Result<Void> unfollow(@PathVariable Long id) {
        return userService.unfollow(id);
    }

    @PostMapping("/{id}/block")
    public Result<Void> block(@PathVariable Long id) {
        return userService.block(id);
    }

    @DeleteMapping("/{id}/block")
    public Result<Void> unblock(@PathVariable Long id) {
        return userService.unblock(id);
    }

    @GetMapping("/{id}/following")
    public Result<?> getFollowing(@PathVariable Long id,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return userService.getFollowingList(id, page, size);
    }

    @GetMapping("/{id}/followers")
    public Result<?> getFollowers(@PathVariable Long id,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return userService.getFollowerList(id, page, size);
    }
}
