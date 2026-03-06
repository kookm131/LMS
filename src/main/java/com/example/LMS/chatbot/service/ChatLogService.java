package com.example.LMS.chatbot.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class ChatLogService {
    private static final int MAX_HISTORY = 20;

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Map<String, String>>> storage = new ConcurrentHashMap<>();

    public void save(String mode, String sessionId, String question, String answer) {
        String safeSessionId = (sessionId == null || sessionId.isBlank()) ? "anonymous" : sessionId;
        ConcurrentLinkedDeque<Map<String, String>> deque = storage.computeIfAbsent(safeSessionId, ignored -> new ConcurrentLinkedDeque<>());

        deque.addLast(Map.of(
                "question", question == null ? "" : question,
                "answer", answer == null ? "" : answer
        ));

        while (deque.size() > MAX_HISTORY) {
            deque.pollFirst();
        }
    }

    public List<Map<String, String>> recentHistory(String sessionId) {
        String safeSessionId = (sessionId == null || sessionId.isBlank()) ? "anonymous" : sessionId;
        ConcurrentLinkedDeque<Map<String, String>> deque = storage.get(safeSessionId);
        if (deque == null || deque.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(deque);
    }
}
