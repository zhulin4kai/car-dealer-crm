package com.autodealer.crm.config.security;

/** 仅存在于服务端 SecurityContext，不进入响应或业务请求体。 */
public record SessionAuthenticationDetails(String sessionId, boolean legacy) {}
