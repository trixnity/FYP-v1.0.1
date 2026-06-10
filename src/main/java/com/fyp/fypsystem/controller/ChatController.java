package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.model.*;
import com.fyp.fypsystem.repository.*;
import com.fyp.fypsystem.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatMessageRepository chatRepo;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;

    public ChatController(ChatMessageRepository chatRepo,
                          UserRepository userRepo,
                          JwtUtil jwtUtil) {
        this.chatRepo = chatRepo;
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestHeader("Authorization") String auth,
                                  @RequestBody Map<String, Object> body) {
        User from = resolve(auth);
        if (from == null) return ResponseEntity.status(401).body(err("Unauthorized"));

        Long toId = toLong(body.get("toId"));
        String message = body.get("message") != null ? body.get("message").toString().trim() : null;
        if (toId == null) return ResponseEntity.badRequest().body(err("toId required"));
        if (message == null || message.isBlank()) return ResponseEntity.badRequest().body(err("message required"));

        User to = userRepo.findById(toId).orElse(null);
        if (to == null) return ResponseEntity.badRequest().body(err("Recipient not found"));

        ChatMessage msg = new ChatMessage();
        msg.setFromId(from.getId());
        msg.setToId(toId);
        msg.setFromName(from.getName());
        msg.setToName(to.getName());
        msg.setMessage(message);
        return ResponseEntity.ok(chatRepo.save(msg));
    }

    @GetMapping("/with/{otherId}")
    public ResponseEntity<?> getConversation(@RequestHeader("Authorization") String auth,
                                             @PathVariable Long otherId) {
        User user = resolve(auth);
        if (user == null) return ResponseEntity.status(401).body(err("Unauthorized"));

        List<ChatMessage> msgs = chatRepo.findConversation(user.getId(), otherId);
        msgs.stream()
            .filter(m -> m.getToId().equals(user.getId()) && !Boolean.TRUE.equals(m.getIsRead()))
            .forEach(m -> { m.setIsRead(true); chatRepo.save(m); });
        return ResponseEntity.ok(msgs);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount(@RequestHeader("Authorization") String auth) {
        User user = resolve(auth);
        if (user == null) return ResponseEntity.status(401).body(err("Unauthorized"));
        return ResponseEntity.ok(Map.of("count", chatRepo.countByToIdAndIsReadFalse(user.getId())));
    }

    private User resolve(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        try { return userRepo.findByEmail(jwtUtil.extractEmail(auth.substring(7))).orElse(null); }
        catch (Exception e) { return null; }
    }
    private Long toLong(Object v) { try { return v != null ? Long.parseLong(v.toString()) : null; } catch (Exception e) { return null; } }
    private Map<String, String> err(String msg) { return Map.of("error", msg); }
}
