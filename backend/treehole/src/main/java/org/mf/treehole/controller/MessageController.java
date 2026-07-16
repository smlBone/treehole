package org.mf.treehole.controller;

import jakarta.annotation.Resource;
import org.mf.treehole.common.Result;
import org.mf.treehole.dto.SendMessageRequest;
import org.mf.treehole.dto.SystemMessageRequest;
import org.mf.treehole.service.MessageService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Resource
    private MessageService messageService;

    @PostMapping
    public Result<Void> sendMessage(@RequestBody SendMessageRequest req) {
        return messageService.sendMessage(req);
    }

    @GetMapping("/conversations")
    public Result<?> getConversations() {
        return messageService.getConversations();
    }

    @GetMapping("/chat/{userId}")
    public Result<?> getChatHistory(@PathVariable Long userId) {
        return messageService.getChatHistory(userId);
    }

    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        return messageService.getUnreadCount();
    }

    @GetMapping("/system")
    public Result<?> getSystemMessages() {
        return messageService.getSystemMessages();
    }

    @PutMapping("/system/{id}/read")
    public Result<Void> readSystemMessage(@PathVariable Long id) {
        return messageService.readSystemMessage(id);
    }

    @PostMapping("/system/broadcast")
    public Result<Void> sendSystemMessage(@RequestBody SystemMessageRequest req) {
        return messageService.sendSystemMessage(req);
    }
}
