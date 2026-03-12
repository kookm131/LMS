package com.example.LMS.chatbot.controller;

import com.example.LMS.chatbot.controller.dto.ChatQueryRequest;
import com.example.LMS.chatbot.controller.dto.ChatQueryResponse;
import com.example.LMS.chatbot.controller.dto.RawChatRequest;
import com.example.LMS.chatbot.controller.dto.TokenIssueRequest;
import com.example.LMS.chatbot.service.AuthTokenService;
import com.example.LMS.chatbot.service.ChatLogService;
import com.example.LMS.chatbot.service.ChatQueryService;
import com.example.LMS.chatbot.service.GatewayClient;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Controller
public class ChatbotController {
    private final ChatQueryService chatQueryService;
    private final GatewayClient gatewayClient;
    private final AuthTokenService authTokenService;
    private final ChatLogService chatLogService;

    @Value("${app.auth.dev-auto-token-enabled:false}")
    private boolean devAutoTokenEnabled;

    @Value("${app.auth.dev-user-id:dev-user}")
    private String devUserId;

    @Value("${app.auth.dev-token-ttl-seconds:86400}")
    private long devTokenTtlSeconds;

    @Value("${app.auth.issue-api-key:}")
    private String issueApiKey;

    @Value("${app.auth.default-issue-ttl-seconds:3600}")
    private long defaultIssueTtlSeconds;

    @Value("${app.auth.max-issue-ttl-seconds:2592000}")
    private long maxIssueTtlSeconds;

    @Value("${app.auth.refresh-token-ttl-seconds:1209600}")
    private long refreshTokenTtlSeconds;

    @Value("${app.auth.refresh-cookie-name:chatbot_refresh_token}")
    private String refreshCookieName;

    @Value("${app.auth.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Value("${app.auth.refresh-cookie-same-site:Lax}")
    private String refreshCookieSameSite;

    public ChatbotController(ChatQueryService chatQueryService, GatewayClient gatewayClient, AuthTokenService authTokenService, ChatLogService chatLogService) {
        this.chatQueryService = chatQueryService;
        this.gatewayClient = gatewayClient;
        this.authTokenService = authTokenService;
        this.chatLogService = chatLogService;
    }

    // 사용처: 헬스체크 기능 (GetMapping /health)
    @GetMapping("/health")
    @ResponseBody
    public Map<String, Object> health() {
        return Map.of("status", "ok");
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping /chat)
    @GetMapping("/chat")
    public String chat(
            @RequestParam(value = "sessionTicket", required = false) String sessionTicket,
            HttpServletResponse response,
            Model model,
            Authentication authentication
    ) {
        String effectiveSessionTicket = sessionTicket;
        if ((effectiveSessionTicket == null || effectiveSessionTicket.isBlank()) && devAutoTokenEnabled) {
            long now = Instant.now().getEpochSecond();
            long accessExp = now + devTokenTtlSeconds;
            long refreshExp = now + refreshTokenTtlSeconds;

            String sid = resolveAutoSessionId(authentication);
            effectiveSessionTicket = authTokenService.createAccessToken(sid, accessExp);
            String refreshToken = authTokenService.createRefreshToken(sid, refreshExp);
            response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken).toString());
        }

        String sessionId = null;
        if (effectiveSessionTicket != null && !effectiveSessionTicket.isBlank()) {
            sessionId = authTokenService.extractSessionIdFromAccess(effectiveSessionTicket);
        }

        model.addAttribute("userId", sessionId == null ? "" : sessionId);
        model.addAttribute("sessionTicket", effectiveSessionTicket);
        return "chat";
    }

    // 사용처: 챗봇 질의 기능 (PostMapping /chat/query)
    @PostMapping("/chat/query")
    @ResponseBody
    public ResponseEntity<ChatQueryResponse> query(@Valid @RequestBody ChatQueryRequest request) {
        return ResponseEntity.ok(chatQueryService.query(request));
    }

    // 사용처: 챗봇 스트리밍 질의 기능 (PostMapping /chat/query/stream)
    @PostMapping(value = "/chat/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter queryStream(@Valid @RequestBody ChatQueryRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        CompletableFuture.runAsync(() -> {
            try {
                chatQueryService.streamQuery(request, chunk -> sendEvent(emitter, "chunk", chunk));
                sendEvent(emitter, "done", "[DONE]");
                emitter.complete();
            } catch (Exception e) {
                sendEvent(emitter, "error", e.getMessage() == null ? "응답 생성 중 오류가 발생했습니다." : e.getMessage());
                emitter.complete();
            }
        });

        return emitter;
    }

    // 사용처: RAW 테스트 호출 차단 안내 기능 (PostMapping /chat/raw)
    @PostMapping("/chat/raw")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> raw(@Valid @RequestBody RawChatRequest request) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "RAW 모드는 비활성화되었습니다. /chat/query + ragContext를 사용하세요."
        ));
    }

    // 사용처: 게이트웨이 연결 점검 기능 (GetMapping /chat/ping-gateway)
    @GetMapping("/chat/ping-gateway")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pingGateway() {
        return ResponseEntity.ok(gatewayClient.pingGateway());
    }

    // 사용처: 세션 식별 정보 조회 기능 (GetMapping /chat/whoami)
    @GetMapping("/chat/whoami")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> whoAmI(
            @RequestParam(value = "sessionTicket", required = false) String sessionTicket
    ) {
        String effectiveSessionTicket = sessionTicket;
        String sessionId = null;
        if (effectiveSessionTicket != null && !effectiveSessionTicket.isBlank()) {
            sessionId = authTokenService.extractSessionIdFromAccess(effectiveSessionTicket);
        }
        String resolvedSessionKey = (sessionId == null || sessionId.isBlank())
                ? "agent:chatbot:tutor:session:anonymous"
                : ("agent:chatbot:tutor:session:" + sessionId.toLowerCase().replaceAll("[^a-z0-9-]", "-"));

        return ResponseEntity.ok(Map.of(
                "agentId", gatewayClient.getGatewayAgentId(),
                "sessionId", sessionId == null ? "(token 없음)" : sessionId,
                "sessionKey", resolvedSessionKey
        ));
    }


    // 사용처: 챗봇 대화 이력 조회 기능 (GetMapping /chat/history)
    @GetMapping("/chat/history")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> history(
            @RequestParam(value = "sessionTicket") String sessionTicket
    ) {
        String sessionId = authTokenService.extractSessionIdFromAccess(sessionTicket);
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "items", chatLogService.recentHistory(sessionId)
        ));
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping /session-ticket)
    @GetMapping("/session-ticket")
    public String sessionTicketPage() {
        return "token-issuer";
    }

    // 사용처: 세션 토큰 발급 기능 (PostMapping /session-ticket/issue)
    @PostMapping("/session-ticket/issue")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> issueToken(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @Valid @RequestBody TokenIssueRequest request
    ) {
        validateIssueApiKey(apiKey);

        long accessTtl = (request.ttlSeconds() == null || request.ttlSeconds() <= 0)
                ? defaultIssueTtlSeconds
                : request.ttlSeconds();

        if (accessTtl > maxIssueTtlSeconds) {
            throw new IllegalArgumentException("ttlSeconds가 허용 최대값을 초과했습니다.");
        }

        long now = Instant.now().getEpochSecond();
        long accessExp = now + accessTtl;
        long refreshExp = now + refreshTokenTtlSeconds;

        String accessToken = authTokenService.createAccessToken(request.sessionId(), accessExp);
        String refreshToken = authTokenService.createRefreshToken(request.sessionId(), refreshExp);

        ResponseCookie cookie = buildRefreshCookie(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of(
                        "sessionId", request.sessionId(),
                        "ttlSeconds", accessTtl,
                        "expiresAt", accessExp,
                        "sessionTicket", accessToken
                ));
    }

    // 사용처: 세션 토큰 재발급 기능 (PostMapping /session-ticket/refresh)
    @PostMapping("/session-ticket/refresh")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> refreshToken(HttpServletRequest request) {
        String refreshToken = readCookie(request, refreshCookieName);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("리프레시 토큰이 없습니다.");
        }

        String sessionId = authTokenService.extractSessionIdFromRefresh(refreshToken);
        long now = Instant.now().getEpochSecond();
        long accessExp = now + defaultIssueTtlSeconds;
        long refreshExp = now + refreshTokenTtlSeconds;

        String newAccessToken = authTokenService.createAccessToken(sessionId, accessExp);
        String newRefreshToken = authTokenService.createRefreshToken(sessionId, refreshExp);
        ResponseCookie cookie = buildRefreshCookie(newRefreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of(
                        "sessionId", sessionId,
                        "ttlSeconds", defaultIssueTtlSeconds,
                        "expiresAt", accessExp,
                        "sessionTicket", newAccessToken
                ));
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(Duration.ofSeconds(refreshTokenTtlSeconds))
                .build();
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private void validateIssueApiKey(String apiKey) {
        if (issueApiKey == null || issueApiKey.isBlank()) {
            return;
        }
        if (apiKey == null || !issueApiKey.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 API KEY입니다.");
        }
    }

    private String resolveAutoSessionId(Authentication authentication) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (isAuthenticated) {
            String raw = authentication.getName() == null ? "" : authentication.getName().toLowerCase();
            String normalized = raw.replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
            normalized = normalized.replaceAll("^-|-$", "");
            if (normalized.length() < 3) {
                normalized = "user";
            }
            if (normalized.length() > 50) {
                normalized = normalized.substring(0, 50);
            }
            return "user-" + normalized;
        }

        String random = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
        return "guest-" + random;
    }

    private void sendEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data == null ? "" : data));
        } catch (IOException ignored) {
        }
    }
}
