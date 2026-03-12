package com.example.LMS.chatbot.service;

import com.example.LMS.chatbot.controller.dto.ChatQueryRequest;
import com.example.LMS.chatbot.controller.dto.ChatQueryResponse;
import com.example.LMS.chatbot.exception.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Service
public class ChatQueryService {
    // 상세 프롬프트 정책은 OpenClaw agent(chatbot)에서 관리하고,
    // 애플리케이션 코드는 최소 fallback만 유지합니다.
    private static final String SYSTEM_PROMPT = "제공된 참고데이터(ragContext)를 우선 근거로 답변하세요.";

    private final GatewayClient gatewayClient;
    private final ChatLogService chatLogService;
    private final AuthTokenService authTokenService;
    private final TokenRateLimitService tokenRateLimitService;
    private final Set<String> inFlightSessions = ConcurrentHashMap.newKeySet();

    public ChatQueryService(
            GatewayClient gatewayClient,
            ChatLogService chatLogService,
            AuthTokenService authTokenService,
            TokenRateLimitService tokenRateLimitService
    ) {
        this.gatewayClient = gatewayClient;
        this.chatLogService = chatLogService;
        this.authTokenService = authTokenService;
        this.tokenRateLimitService = tokenRateLimitService;
    }

    public ChatQueryResponse query(ChatQueryRequest request) {
        validate(request.question(), request.userName(), request.ragContext());

        String sessionId = resolveSessionIdFromToken(request.sessionTicket());
        String inflightKey = toInflightKey(sessionId);
        if (!inFlightSessions.add(inflightKey)) {
            throw new IllegalArgumentException("같은 세션에서 이전 요청 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            enforceRateLimit(sessionId);

            String sessionKey = toSessionKeyOrNull(sessionId);
            String answer = sanitizeAnswer(gatewayClient.generateAnswer(
                    SYSTEM_PROMPT,
                    buildUserPrompt(request.question(), request.userName(), request.ragContext()),
                    sessionKey
            ));
            chatLogService.save("query-rag-only", inflightKey, request.question(), answer);
            return new ChatQueryResponse(answer);
        } finally {
            inFlightSessions.remove(inflightKey);
        }
    }

    public void streamQuery(ChatQueryRequest request, Consumer<String> onChunk) {
        validate(request.question(), request.userName(), request.ragContext());

        String sessionId = resolveSessionIdFromToken(request.sessionTicket());
        String inflightKey = toInflightKey(sessionId);
        if (!inFlightSessions.add(inflightKey)) {
            throw new IllegalArgumentException("같은 세션에서 이전 요청 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            enforceRateLimit(sessionId);

            String sessionKey = toSessionKeyOrNull(sessionId);
            AtomicInteger chunkCount = new AtomicInteger(0);
            String raw = gatewayClient.streamAnswer(
                    SYSTEM_PROMPT,
                    buildUserPrompt(request.question(), request.userName(), request.ragContext()),
                    sessionKey,
                    chunk -> {
                        String safe = sanitizeAnswer(chunk);
                        if (onChunk != null && !safe.isEmpty()) {
                            chunkCount.incrementAndGet();
                            onChunk.accept(safe);
                        }
                    }
            );

            String answer = sanitizeAnswer(raw);
            if (chunkCount.get() == 0 && onChunk != null && !answer.isBlank()) {
                onChunk.accept(answer);
            }
            chatLogService.save("query-rag-stream", inflightKey, request.question(), answer);
        } finally {
            inFlightSessions.remove(inflightKey);
        }
    }

    private void validate(String question, String userName, String ragContext) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("질문을 입력해 주세요.");
        }
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("사용자 이름을 입력해 주세요.");
        }
        if (ragContext == null || ragContext.isBlank()) {
            throw new IllegalArgumentException("학습 자료를 먼저 불러와 주세요.");
        }
    }

    private String sanitizeAnswer(String answer) {
        if (answer == null) return "";
        return answer
                .replace("**", "")
                .replace("__", "")
                .replace("`", "");
    }

    private String buildUserPrompt(String question, String userName, String ragContext) {
        return "질문:\n" + question
                + "\n\n참고데이터:\n"
                + "[사용자 정보]\n"
                + "- 이름: " + userName + "\n\n"
                + ragContext;
    }


    private void enforceRateLimit(String sessionId) {
        String rateLimitKey = sessionId == null ? "anonymous" : ("sid:" + sessionId);
        if (!tokenRateLimitService.tryAcquire(rateLimitKey)) {
            throw new RateLimitExceededException(
                    "요청이 너무 많습니다. 토큰당 분당 " + tokenRateLimitService.getRequestsPerMinute() + "회까지 요청할 수 있습니다."
            );
        }
    }

    private String resolveSessionIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return authTokenService.extractSessionId(token);
    }

    private String toInflightKey(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? "anonymous" : sessionId;
    }

    private String toSessionKeyOrNull(String sessionId) {
        // 항상 tutor 네임스페이스 세션키를 강제해서
        // gateway 기본 세션키(환경변수)나 과거 openai 네임스페이스로 새 요청이 섞이지 않게 함
        if (sessionId == null || sessionId.isBlank()) {
            return "agent:chatbot:tutor:session:anonymous";
        }
        return toSessionKey(sessionId);
    }

    private String toSessionKey(String sessionId) {
        String safeSessionId = sessionId.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        return "agent:chatbot:tutor:session:" + safeSessionId;
    }
}
