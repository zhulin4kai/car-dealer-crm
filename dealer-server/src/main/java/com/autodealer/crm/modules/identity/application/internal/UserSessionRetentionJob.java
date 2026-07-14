package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.persistence.mapper.TUserSessionMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Component
@Profile("!test")
public class UserSessionRetentionJob {
    private final TUserSessionMapper sessions; private final Clock clock; private final int retentionDays;
    public UserSessionRetentionJob(TUserSessionMapper sessions,Clock clock,
      @Value("${security.session.revoked-retention-days:90}") int retentionDays){this.sessions=sessions;this.clock=clock;this.retentionDays=Math.max(1,retentionDays);}
    @Scheduled(cron="${security.session.retention-cron:0 23 3 * * *}")
    @Transactional public void cleanup(){LocalDateTime now=LocalDateTime.ofInstant(clock.instant(),clock.getZone());sessions.revokeExpired(now);sessions.deleteRetainedBefore(now.minusDays(retentionDays));}
}
