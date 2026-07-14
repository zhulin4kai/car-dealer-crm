package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.shared.infrastructure.cache.RedisKeys;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserSessionDtos;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserSessionDtos.RevokeRequest;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.infrastructure.cache.RedisManager;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserSessionMapper;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.persistence.model.TUserSession;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.api.UserSessionService;
import com.autodealer.crm.shared.security.JWTUtils;
import com.autodealer.crm.modules.identity.domain.SessionTokenDigester;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
public class UserSessionServiceImpl implements UserSessionService {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(UserSessionServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ObjectMapper AUDIT_JSON = new ObjectMapper().findAndRegisterModules();
    private final TUserSessionMapper sessions;
    private final TUserMapper users;
    private final RedisManager redis;
    private final SessionTokenDigester digester;
    private final CurrentUserProvider current;
    private final UserAuthorizationPolicy policy;
    private final OperationAuditRecorder audit;
    private final Clock clock;
    private final Duration normalAbsolute;
    private final Duration normalIdle;
    private final Duration rememberAbsolute;
    private final Duration rememberIdle;
    private final Duration touchInterval;
    private final int maxConcurrent;

    public UserSessionServiceImpl(TUserSessionMapper sessions, TUserMapper users, RedisManager redis,
                                  SessionTokenDigester digester, CurrentUserProvider current,
                                  UserAuthorizationPolicy policy, OperationAuditRecorder audit, Clock clock,
                                  @Value("${security.session.normal-absolute-seconds:14400}") long normalAbsoluteSeconds,
                                  @Value("${security.session.normal-idle-seconds:1800}") long normalIdleSeconds,
                                  @Value("${security.session.remember-absolute-seconds:604800}") long rememberAbsoluteSeconds,
                                  @Value("${security.session.remember-idle-seconds:86400}") long rememberIdleSeconds,
                                  @Value("${security.session.touch-interval-seconds:300}") long touchIntervalSeconds,
                                  @Value("${security.session.max-concurrent:5}") int maxConcurrent) {
        this.sessions=sessions; this.users=users; this.redis=redis; this.digester=digester;
        this.current=current; this.policy=policy; this.audit=audit; this.clock=clock;
        this.normalAbsolute=Duration.ofSeconds(normalAbsoluteSeconds);
        this.normalIdle=Duration.ofSeconds(normalIdleSeconds);
        this.rememberAbsolute=Duration.ofSeconds(rememberAbsoluteSeconds);
        this.rememberIdle=Duration.ofSeconds(rememberIdleSeconds);
        this.touchInterval=Duration.ofSeconds(touchIntervalSeconds);
        this.maxConcurrent=Math.max(1,maxConcurrent);
    }

    @Override
    @Transactional
    public UserSessionDtos.Issued create(TUser user, boolean rememberMe, HttpServletRequest request) {
        LocalDateTime now = now();
        TUser lockedUser = users.selectByPrimaryKeyForUpdate(user.getId());
        if (lockedUser == null) throw new BusinessException(CodeEnum.NOT_FOUND, "账号不存在");
        List<TUserSession> active = sessions.selectActiveByUserId(lockedUser.getId(), now);
        List<String> maxConcurrentSessionIds = new ArrayList<>();
        int maxConcurrentRevoked = 0;
        while (active.size() >= maxConcurrent) {
            TUserSession oldest = active.remove(active.size()-1);
            if (sessions.revokeOne(oldest.getSessionId(), lockedUser.getId(), now, lockedUser.getId(),
                    "超过最大并发会话数", "MAX_CONCURRENT") != 1) {
                throw new BusinessException(CodeEnum.SESSION_VERSION_CONFLICT);
            }
            maxConcurrentSessionIds.add(oldest.getSessionId());
            maxConcurrentRevoked++;
        }

        Duration absoluteDuration = rememberMe ? rememberAbsolute : normalAbsolute;
        Duration idleDuration = rememberMe ? rememberIdle : normalIdle;
        LocalDateTime absolute = now.plus(absoluteDuration);
        LocalDateTime idle = min(now.plus(idleDuration), absolute);
        String sessionId = randomSessionId();
        long authVersion = lockedUser.getAuthVersion()==null ? 0L : lockedUser.getAuthVersion();
        String token = JWTUtils.createSessionJWT(lockedUser.getId(), sessionId, authVersion,
                now.atZone(clock.getZone()).toInstant(), absolute.atZone(clock.getZone()).toInstant());

        TUserSession fact = new TUserSession();
        fact.setSessionId(sessionId); fact.setUserId(lockedUser.getId()); fact.setTokenDigest(digester.digest(token));
        fact.setIssuedAuthVersion(authVersion); fact.setRememberMe(rememberMe);
        fact.setDeviceSummary(deviceSummary(request)); fact.setClientSummary(clientSummary(request));
        fact.setNetworkSummary(networkSummary(request)); fact.setLoginTime(now); fact.setLastActivityTime(now);
        fact.setIdleExpiresAt(idle); fact.setAbsoluteExpiresAt(absolute); fact.setVersion(0); fact.setCreateTime(now);
        if (sessions.insert(fact)!=1) throw new BusinessException(CodeEnum.OPERATION_FAILED,"会话事实创建失败");

        long idleSeconds = ttlSeconds(now,idle);
        long absoluteSeconds = ttlSeconds(now,absolute);
        boolean sessionStored = redis.set(RedisKeys.userSession(sessionId), fact.getTokenDigest(), idleSeconds);
        if (!sessionStored) throw new BusinessException(CodeEnum.SESSION_CACHE_FAILED);
        boolean indexed = redis.addToSet(RedisKeys.userSessionIndex(lockedUser.getId()),sessionId,absoluteSeconds);
        if (!indexed) {
            redis.delete(RedisKeys.userSession(sessionId));
            throw new BusinessException(CodeEnum.SESSION_CACHE_FAILED);
        }
        registerRollbackCleanup(lockedUser.getId(), sessionId);
        if (users.incrementSessionRevision(lockedUser.getId())!=1) {
            redis.delete(RedisKeys.userSession(sessionId));
            redis.removeFromSet(RedisKeys.userSessionIndex(lockedUser.getId()),sessionId);
            throw new BusinessException(CodeEnum.OPERATION_FAILED,"会话版本更新失败");
        }
        if(!maxConcurrentSessionIds.isEmpty())scheduleCleanup(lockedUser.getId(),maxConcurrentSessionIds,false);
        if(maxConcurrentRevoked>0)audit.recordAuthenticatedActor(AuditActionEnum.USER_SESSION_SECURITY_REVOKE,
                String.valueOf(lockedUser.getId()),"SUCCESS",jsonSummary("MAX_CONCURRENT","超过最大并发会话数",maxConcurrentRevoked),
                lockedUser.getId(),lockedUser.getName());
        audit.recordAuthenticatedActor(AuditActionEnum.USER_SESSION_CREATE,String.valueOf(lockedUser.getId()),
                "SUCCESS",jsonSummary(rememberMe?"LOGIN_REMEMBER_ME":"LOGIN","",1),
                lockedUser.getId(),lockedUser.getName());
        return new UserSessionDtos.Issued(token,sessionId);
    }

    @Override
    @Transactional
    public boolean validateAndTouch(String rawToken,Integer userId,String sessionId,Long authVersion) {
        if (rawToken==null||userId==null||sessionId==null||authVersion==null) return false;
        Object cached = redis.get(RedisKeys.userSession(sessionId));
        if (!(cached instanceof String cachedDigest) || !digester.matches(rawToken,cachedDigest)) return false;
        TUserSession fact = sessions.selectBySessionId(sessionId);
        LocalDateTime now=now();
        if (fact==null || !Objects.equals(userId,fact.getUserId()) || fact.getRevokedAt()!=null
                || !Objects.equals(authVersion,fact.getIssuedAuthVersion())
                || !digester.matches(rawToken,fact.getTokenDigest())
                || !fact.getIdleExpiresAt().isAfter(now) || !fact.getAbsoluteExpiresAt().isAfter(now)) return false;
        if (!fact.getLastActivityTime().plus(touchInterval).isAfter(now)) {
            Duration idleDuration=Boolean.TRUE.equals(fact.getRememberMe())?rememberIdle:normalIdle;
            LocalDateTime nextIdle=min(now.plus(idleDuration),fact.getAbsoluteExpiresAt());
            if (!nextIdle.isAfter(now)) return false;
            int touched=sessions.touchBySessionIdAndVersion(sessionId,fact.getVersion(),now,nextIdle);
            if (touched==1 && !redis.expire(RedisKeys.userSession(sessionId),ttlSeconds(now,nextIdle))) return false;
            if (touched==0) {
                TUserSession latest=sessions.selectBySessionId(sessionId);
                if (latest==null||latest.getRevokedAt()!=null||!latest.getIdleExpiresAt().isAfter(now)) return false;
            }
        }
        return true;
    }

    @Override public UserSessionDtos.Collection ownSessions(){return collection(current.getCurrentUserId(),current.getCurrentSessionId(),true,true);}

    @Override @Transactional public UserSessionDtos.Collection revokeOwn(String sid,RevokeRequest q){Integer uid=current.getCurrentUserId();revokeOne(uid,sid,q,uid,"SELF_ONE");return collection(uid,current.getCurrentSessionId(),true,true);}

    @Override
    @Transactional
    public UserSessionDtos.Collection revokeOwnOthers(RevokeRequest q) {
        Integer uid=current.getCurrentUserId();
        String currentSid=current.getCurrentSessionId();
        List<String> ids=sessions.selectAllActiveSessionIdsByUserId(uid,now()).stream()
                .filter(id->!Objects.equals(id,currentSid)).toList();
        boolean deleteLegacy=currentSid!=null&&legacyPresentStrict(uid);
        if(ids.isEmpty()&&!deleteLegacy)return collection(uid,currentSid,true,true);
        casRevision(uid,q.getSessionRevision());
        if(!ids.isEmpty()){
            if(currentSid==null)sessions.revokeAll(uid,now(),uid,q.getReason(),"SELF_OTHERS");
            else sessions.revokeOthers(uid,currentSid,now(),uid,q.getReason(),"SELF_OTHERS");
        }
        scheduleCleanup(uid,ids,deleteLegacy);
        audit.record(AuditActionEnum.USER_SESSION_REVOKE,String.valueOf(uid),"SUCCESS",
                jsonSummary("SELF_OTHERS",q.getReason(),ids.size()+(deleteLegacy?1:0)));
        return collection(uid,currentSid,true,true);
    }

    @Override @Transactional public UserSessionDtos.Collection revokeOwnAll(RevokeRequest q){Integer uid=current.getCurrentUserId();revokeAll(uid,q,uid,"SELF_ALL");return collection(uid,current.getCurrentSessionId(),true,true);}

    @Override public UserSessionDtos.Collection managedSessions(Integer userId){TUser target=requireManaged(userId);policy.requireManage(target);boolean mutationAllowed=current.hasAuthority(PermissionCodes.USER_STATUS);return collection(userId,null,false,mutationAllowed);}

    @Override @Transactional public UserSessionDtos.Collection revokeManaged(Integer userId,String sid,RevokeRequest q){requireManagedMutation();TUser target=requireManaged(userId);policy.requireManage(target);revokeOne(userId,sid,q,current.getCurrentUserId(),"MANAGED_ONE");return collection(userId,null,false,true);}

    @Override @Transactional public UserSessionDtos.Collection revokeManagedAll(Integer userId,RevokeRequest q){requireManagedMutation();TUser target=requireManaged(userId);policy.requireManage(target);revokeAll(userId,q,current.getCurrentUserId(),"MANAGED_ALL");return collection(userId,null,false,true);}

    @Override @Transactional public void revokeCurrentForLogout(Integer userId,String sid){if(userId==null||sid==null)throw new BusinessException(CodeEnum.SESSION_NOT_FOUND);TUserSession fact=sessions.selectBySessionId(sid);if(fact==null||!Objects.equals(userId,fact.getUserId()))throw new BusinessException(CodeEnum.SESSION_NOT_FOUND);if(fact.getRevokedAt()==null){if(sessions.revokeOne(sid,userId,now(),userId,"用户主动退出","LOGOUT")!=1)throw new BusinessException(CodeEnum.SESSION_VERSION_CONFLICT);if(users.incrementSessionRevision(userId)!=1)throw new BusinessException(CodeEnum.OPERATION_FAILED);TUser actor=users.selectByPrimaryKey(userId);if(actor==null)throw new BusinessException(CodeEnum.OPERATION_FAILED,"登出审计主体不存在");audit.recordAuthenticatedActor(AuditActionEnum.USER_SESSION_REVOKE,String.valueOf(userId),"SUCCESS","{\"type\":\"LOGOUT\"}",userId,actor.getName());scheduleCleanup(userId,List.of(sid),false);}}

    @Override @Transactional public void revokeAllForSecurityChange(Integer userId,Integer operatorId,String reason){List<String> ids=sessions.selectAllActiveSessionIdsByUserId(userId,now());if(!ids.isEmpty()){sessions.revokeAll(userId,now(),operatorId,reason,"SECURITY_CHANGE");if(users.incrementSessionRevision(userId)!=1)throw new BusinessException(CodeEnum.OPERATION_FAILED,"会话版本更新失败");TUser actor=operatorId==null?null:users.selectByPrimaryKey(operatorId);if(actor==null)audit.recordAnonymous(AuditActionEnum.USER_SESSION_SECURITY_REVOKE,String.valueOf(userId),"SUCCESS",jsonSummary("SECURITY_CHANGE",reason,ids.size()));else audit.recordAuthenticatedActor(AuditActionEnum.USER_SESSION_SECURITY_REVOKE,String.valueOf(userId),"SUCCESS",jsonSummary("SECURITY_CHANGE",reason,ids.size()),actor.getId(),actor.getName());}scheduleSecurityCleanup(userId,ids);}

    private void revokeOne(Integer uid,String sid,RevokeRequest q,Integer operator,String type){TUserSession fact=sessions.selectBySessionId(sid);if(fact==null||!Objects.equals(uid,fact.getUserId()))throw new BusinessException(CodeEnum.SESSION_NOT_FOUND);LocalDateTime commandTime=now();if(fact.getRevokedAt()!=null)throw new BusinessException(CodeEnum.SESSION_REVOKED);if(!fact.getIdleExpiresAt().isAfter(commandTime)||!fact.getAbsoluteExpiresAt().isAfter(commandTime))throw new BusinessException(CodeEnum.SESSION_EXPIRED);casRevision(uid,q.getSessionRevision());if(sessions.revokeOne(sid,uid,commandTime,operator,q.getReason(),type)!=1)throw new BusinessException(CodeEnum.SESSION_REVOKED);scheduleCleanup(uid,List.of(sid),false);audit.record(AuditActionEnum.USER_SESSION_REVOKE,String.valueOf(uid),"SUCCESS",jsonSummary(type,q.getReason(),1));}

    private void revokeAll(Integer uid,RevokeRequest q,Integer operator,String type){List<String> ids=sessions.selectAllActiveSessionIdsByUserId(uid,now());boolean deleteLegacy=legacyPresentStrict(uid);if(ids.isEmpty()&&!deleteLegacy)return;casRevision(uid,q.getSessionRevision());if(!ids.isEmpty())sessions.revokeAll(uid,now(),operator,q.getReason(),type);scheduleCleanup(uid,ids,deleteLegacy);audit.record(AuditActionEnum.USER_SESSION_REVOKE,String.valueOf(uid),"SUCCESS",jsonSummary(type,q.getReason(),ids.size()+(deleteLegacy?1:0)));}

    private void casRevision(Integer uid,Long expected){if(users.incrementSessionRevisionByExpected(uid,expected)!=1)throw new BusinessException(CodeEnum.SESSION_VERSION_CONFLICT);}

    private TUser requireManaged(Integer uid){TUser user=users.selectByPrimaryKey(uid);if(user==null)throw new BusinessException(CodeEnum.NOT_FOUND,"用户不存在");return user;}
    private void requireManagedMutation(){if(!current.hasAuthority(PermissionCodes.USER_STATUS))throw new BusinessException(CodeEnum.ACCESS_DENIED,"缺少用户状态管理权限");}

    private UserSessionDtos.Collection collection(Integer uid,String currentSid,boolean own,boolean mutationAllowed){TUser user=users.selectByPrimaryKey(uid);if(user==null)throw new BusinessException(CodeEnum.NOT_FOUND,"用户不存在");List<TUserSession> active=sessions.selectActiveByUserId(uid,now());boolean legacyPresent=currentSid==null&&own||legacyPresentForDisplay(uid);boolean hasAny=!active.isEmpty()||legacyPresent;boolean hasOther=active.stream().anyMatch(s->!Objects.equals(currentSid,s.getSessionId()))||(currentSid!=null&&legacyPresent);UserSessionDtos.Collection out=new UserSessionDtos.Collection();out.setTargetUserId(uid);out.setSessionRevision(user.getSessionRevision()==null?0L:user.getSessionRevision());if(mutationAllowed&&hasAny)out.getAllowedActions().add("REVOKE_ALL");else if(!mutationAllowed&&hasAny)out.getUnavailableReasons().put("REVOKE_ALL","缺少用户状态管理权限");if(own&&hasOther)out.getAllowedActions().add("REVOKE_OTHERS");for(TUserSession fact:active){UserSessionDtos.Item item=new UserSessionDtos.Item();item.setId(fact.getSessionId());item.setDeviceSummary(fact.getDeviceSummary());item.setClientSummary(fact.getClientSummary());item.setNetworkSummary(fact.getNetworkSummary());item.setLoginTime(fact.getLoginTime());item.setLastActivityTime(fact.getLastActivityTime());item.setExpiresAt(min(fact.getIdleExpiresAt(),fact.getAbsoluteExpiresAt()));item.setCurrent(Objects.equals(currentSid,fact.getSessionId()));item.setRememberMe(Boolean.TRUE.equals(fact.getRememberMe()));if(mutationAllowed)item.getAllowedActions().add("REVOKE");else item.getUnavailableReasons().put("REVOKE","缺少用户状态管理权限");out.getSessions().add(item);}return out;}

    private void scheduleCleanup(Integer uid,List<String> ids,boolean deleteLegacy){Runnable action=()->cleanupStrict(uid,ids,deleteLegacy);if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){action.run();}});else action.run();}

    private void registerRollbackCleanup(Integer uid,String sessionId){if(!TransactionSynchronizationManager.isSynchronizationActive())return;TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status==TransactionSynchronization.STATUS_COMMITTED)return;try{redis.delete(RedisKeys.userSession(sessionId));redis.removeFromSet(RedisKeys.userSessionIndex(uid),sessionId);}catch(RuntimeException exception){log.error("会话事务回滚缓存清理失败 userId={} sessionId={}",uid,sessionId,exception);}}});}

    private void scheduleSecurityCleanup(Integer uid,List<String> ids){Runnable action=()->{for(String sid:ids){boolean done=false;for(int attempt=1;attempt<=2&&!done;attempt++){try{boolean deleted=redis.delete(RedisKeys.userSession(sid));boolean removed=redis.removeFromSet(RedisKeys.userSessionIndex(uid),sid);done=deleted&&removed;}catch(RuntimeException exception){log.warn("安全变化会话缓存清理失败 userId={} attempt={}",uid,attempt,exception);}}if(!done)log.warn("安全变化会话缓存清理重试耗尽 userId={} sessionSuffix={}",uid,sid.length()>6?sid.substring(sid.length()-6):sid);}boolean legacyDone=false;for(int attempt=1;attempt<=2&&!legacyDone;attempt++){try{legacyDone=redis.delete(RedisKeys.userLogin(uid));}catch(RuntimeException exception){log.warn("旧会话缓存清理失败 userId={} attempt={}",uid,attempt,exception);}}if(!legacyDone)log.warn("旧会话缓存清理重试耗尽 userId={}",uid);};if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){action.run();}});else action.run();}

    private void cleanupStrict(Integer uid,List<String> ids,boolean deleteLegacy){boolean ok=true;for(String sid:ids){boolean deleted=redis.delete(RedisKeys.userSession(sid));boolean removed=redis.removeFromSet(RedisKeys.userSessionIndex(uid),sid);ok&=deleted&&removed;}if(deleteLegacy)ok&=redis.delete(RedisKeys.userLogin(uid));if(!ok)throw new BusinessException(CodeEnum.SESSION_CACHE_FAILED);}

    private boolean legacyPresentStrict(Integer uid){RedisManager.KeyPresence presence=redis.keyPresence(RedisKeys.userLogin(uid));if(presence==null||presence==RedisManager.KeyPresence.UNAVAILABLE)throw new BusinessException(CodeEnum.SESSION_CACHE_FAILED);return presence==RedisManager.KeyPresence.PRESENT;}
    private boolean legacyPresentForDisplay(Integer uid){return redis.keyPresence(RedisKeys.userLogin(uid))==RedisManager.KeyPresence.PRESENT;}

    private LocalDateTime now(){return LocalDateTime.ofInstant(clock.instant(),clock.getZone());}
    private LocalDateTime min(LocalDateTime a,LocalDateTime b){return a.isBefore(b)?a:b;}
    private long ttlSeconds(LocalDateTime from,LocalDateTime to){return Math.max(1,Duration.between(from,to).getSeconds());}
    private String randomSessionId(){byte[]bytes=new byte[32];RANDOM.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private static String jsonSummary(String type,String reason,int count){try{return AUDIT_JSON.writeValueAsString(Map.of("type",type,"reason",reason==null?"":reason,"count",count));}catch(JsonProcessingException e){throw new IllegalStateException("会话审计摘要序列化失败",e);}}
    private String deviceSummary(HttpServletRequest r){String ua=safe(r.getHeader("User-Agent"),256);return ua.toLowerCase(Locale.ROOT).contains("mobile")?"移动设备":"桌面设备";}
    private String clientSummary(HttpServletRequest r){String ua=safe(r.getHeader("User-Agent"),256);String lower=ua.toLowerCase(Locale.ROOT);if(lower.contains("edg/"))return "Edge 浏览器";if(lower.contains("chrome/"))return "Chrome 浏览器";if(lower.contains("firefox/"))return "Firefox 浏览器";if(lower.contains("safari/"))return "Safari 浏览器";return "未知客户端";}
    private String networkSummary(HttpServletRequest r){String ip=safe(r.getRemoteAddr(),64);if(ip.matches("\\d{1,3}(\\.\\d{1,3}){3}")){String[]p=ip.split("\\.");return p[0]+"."+p[1]+".*.*";}if(ip.contains(":")){String[]p=ip.split(":");return String.join(":",Arrays.copyOf(p,Math.min(4,p.length)))+"::/64";}return "未知网络";}
    private String safe(String value,int max){if(value==null)return "";String clean=value.replaceAll("[\\r\\n\\t\\p{Cntrl}]"," ").trim();return clean.length()<=max?clean:clean.substring(0,max);}
}
