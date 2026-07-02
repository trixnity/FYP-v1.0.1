package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("SELECT m FROM ChatMessage m WHERE (m.fromId = :a AND m.toId = :b) OR (m.fromId = :b AND m.toId = :a) ORDER BY m.sentAt ASC")
    List<ChatMessage> findConversation(@Param("a") Long a, @Param("b") Long b);

    @Query("SELECT m FROM ChatMessage m WHERE m.fromId = :userId OR m.toId = :userId ORDER BY m.sentAt DESC")
    List<ChatMessage> findUserMessagesByLatest(@Param("userId") Long userId);

    long countByToIdAndIsReadFalse(Long toId);

    long countByFromIdAndToIdAndIsReadFalse(Long fromId, Long toId);
}
