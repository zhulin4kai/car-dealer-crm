package com.autodealer.crm.dto.user;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;

public final class UserSessionDtos {
    private UserSessionDtos() {}
    @Data public static class Item {
        private String id; private String deviceSummary; private String clientSummary; private String networkSummary;
        private LocalDateTime loginTime; private LocalDateTime lastActivityTime; private LocalDateTime expiresAt;
        private boolean current; private boolean rememberMe;
        private List<String> allowedActions=new ArrayList<>();
        private Map<String,String> unavailableReasons=new LinkedHashMap<>();
    }
    @Data public static class Collection {
        private Integer targetUserId; private Long sessionRevision;
        private List<String> allowedActions=new ArrayList<>();
        private Map<String,String> unavailableReasons=new LinkedHashMap<>();
        private List<Item> sessions=new ArrayList<>();
    }
    @Data public static class RevokeRequest {
        @NotNull @Min(0) private Long sessionRevision;
        @NotBlank @Size(max=500) private String reason;
    }
    public record Issued(String token,String sessionId) {}
}
