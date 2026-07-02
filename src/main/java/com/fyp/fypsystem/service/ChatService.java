package com.fyp.fypsystem.service;

import com.fyp.fypsystem.model.ChatMessage;
import com.fyp.fypsystem.model.Role;
import com.fyp.fypsystem.model.SessionPlan;
import com.fyp.fypsystem.model.User;
import com.fyp.fypsystem.repository.ChatMessageRepository;
import com.fyp.fypsystem.repository.SessionPlanRepository;
import com.fyp.fypsystem.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SessionPlanRepository sessionPlanRepository;

    public ChatService(ChatMessageRepository chatMessageRepository,
                       UserRepository userRepository,
                       SessionPlanRepository sessionPlanRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.sessionPlanRepository = sessionPlanRepository;
    }

    public List<Map<String, Object>> getConversationSummaries(User user) {
        Map<Long, Map<String, Object>> summaries = new LinkedHashMap<>();
        Set<Long> allowedPartnerIds = allowedPartnerIds(user);

        for (ChatMessage message : chatMessageRepository.findUserMessagesByLatest(user.getId())) {
            Long partnerId = message.getFromId().equals(user.getId()) ? message.getToId() : message.getFromId();
            if (!allowedPartnerIds.isEmpty() && !allowedPartnerIds.contains(partnerId)) continue;
            summaries.computeIfAbsent(partnerId, id -> summaryFromMessage(user, id, message));
        }

        if (user.getRole() == Role.COACH) {
            List<User> students = userRepository.findByCoachIdAndRole(user.getId(), Role.STUDENT);
            students.sort(Comparator.comparing(u -> safe(u.getName()), String.CASE_INSENSITIVE_ORDER));
            for (User student : students) {
                summaries.computeIfAbsent(student.getId(), id -> emptySummary(student));
            }
            for (SessionPlan plan : sessionPlanRepository.findByCoachIdOrderByCreatedAtDesc(user.getId())) {
                userRepository.findById(plan.getStudentId()).ifPresent(student ->
                        summaries.computeIfAbsent(student.getId(), id -> emptySummary(student)));
            }
        } else if (user.getCoachId() != null) {
            userRepository.findById(user.getCoachId()).ifPresent(coach ->
                    summaries.computeIfAbsent(coach.getId(), id -> emptySummary(coach)));
        }
        if (user.getRole() == Role.STUDENT) {
            for (SessionPlan plan : sessionPlanRepository.findByStudentIdOrderByCreatedAtDesc(user.getId())) {
                userRepository.findById(plan.getCoachId()).ifPresent(coach ->
                        summaries.computeIfAbsent(coach.getId(), id -> emptySummary(coach)));
            }
        }

        List<Map<String, Object>> result = new ArrayList<>(summaries.values());
        result.sort((a, b) -> {
            String at = (String) a.get("latestSentAt");
            String bt = (String) b.get("latestSentAt");
            if (at == null && bt == null) {
                return safe((String) a.get("name")).compareToIgnoreCase(safe((String) b.get("name")));
            }
            if (at == null) return 1;
            if (bt == null) return -1;
            return bt.compareTo(at);
        });
        return result;
    }

    private Set<Long> allowedPartnerIds(User user) {
        Set<Long> ids = new HashSet<>();
        if (user.getRole() == Role.ADMIN) return ids;
        if (user.getRole() == Role.COACH) {
            userRepository.findByCoachIdAndRole(user.getId(), Role.STUDENT)
                    .forEach(student -> ids.add(student.getId()));
            sessionPlanRepository.findByCoachIdOrderByCreatedAtDesc(user.getId())
                    .forEach(plan -> ids.add(plan.getStudentId()));
        } else if (user.getRole() == Role.STUDENT) {
            if (user.getCoachId() != null) ids.add(user.getCoachId());
            sessionPlanRepository.findByStudentIdOrderByCreatedAtDesc(user.getId())
                    .forEach(plan -> ids.add(plan.getCoachId()));
        }
        ids.remove(null);
        return ids;
    }

    private Map<String, Object> summaryFromMessage(User user, Long partnerId, ChatMessage message) {
        User partner = userRepository.findById(partnerId).orElse(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("partnerId", partnerId);
        map.put("name", partner != null ? partner.getName() : partnerNameFromMessage(partnerId, message));
        map.put("email", partner != null ? partner.getEmail() : "");
        map.put("latestMessage", message.getMessage());
        map.put("latestSentAt", message.getSentAt());
        map.put("unreadCount", chatMessageRepository.countByFromIdAndToIdAndIsReadFalse(partnerId, user.getId()));
        return map;
    }

    private Map<String, Object> emptySummary(User partner) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("partnerId", partner.getId());
        map.put("name", partner.getName());
        map.put("email", partner.getEmail());
        map.put("latestMessage", "");
        map.put("latestSentAt", null);
        map.put("unreadCount", 0L);
        return map;
    }

    private String partnerNameFromMessage(Long partnerId, ChatMessage message) {
        if (message.getFromId().equals(partnerId)) {
            return message.getFromName();
        }
        return message.getToName();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
