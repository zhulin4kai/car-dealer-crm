package com.autodealer.crm.shared.infrastructure.cache;

import com.autodealer.crm.bootstrap.security.MyAuthenticationSuccessHandler;
import com.autodealer.crm.bootstrap.security.MyLogoutSuccessHandler;
import com.autodealer.crm.bootstrap.security.TokenVerifyFilter;
import com.autodealer.crm.modules.fulfillment.transaction.application.internal.TranServiceImpl;
import com.autodealer.crm.modules.fulfillment.transaction.application.internal.TransactionCompletionServiceImpl;
import com.autodealer.crm.shared.infrastructure.constants.Constants;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RedisKeyGovernanceTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java");

    @Test
    void transactionCacheKeys_shouldOnlyKeepActiveProductsAndInvoicesCaches() throws Exception {
        String constantsSource = Files.readString(
                SOURCE_ROOT.resolve("com/autodealer/crm/shared/infrastructure/constants/Constants.java"));
        String redisKeysSource = Files.readString(
                SOURCE_ROOT.resolve("com/autodealer/crm/shared/infrastructure/cache/RedisKeys.java"));
        String tranServiceSource = Files.readString(SOURCE_ROOT.resolve(
                "com/autodealer/crm/modules/fulfillment/transaction/application/internal/TranServiceImpl.java"));
        String completionServiceSource = Files.readString(
                SOURCE_ROOT.resolve(
                        "com/autodealer/crm/modules/fulfillment/transaction/application/internal/TransactionCompletionServiceImpl.java"));

        assertFalse(constantsSource.contains("CACHE_KEY_TRAN"), "交易缓存 key 不能继续散落在 Constants");
        assertFalse(redisKeysSource.contains("TRAN_DETAIL_PREFIX"), "交易详情缓存无生产者/消费者，应删除");
        assertFalse(redisKeysSource.contains("TRAN_LIST_PREFIX"), "交易列表缓存无生产者/消费者，应删除");
        assertFalse(redisKeysSource.contains("TRAN_PAYMENTS_PREFIX"), "交易支付缓存无生产者/消费者，应删除");
        assertFalse(tranServiceSource.contains("\"cdrm:tran:"), "交易服务禁止手写 Redis key");
        assertFalse(tranServiceSource.contains("Constants.CACHE_KEY_TRAN"), "交易服务必须使用 RedisKeys");
        assertFalse(completionServiceSource.contains("transactionDetail"), "完成聚合不应失效死缓存");
        assertFalse(completionServiceSource.contains("transactionListPattern"), "完成聚合不应扫描死缓存");
        assertFalse(completionServiceSource.contains("transactionPayments"), "完成聚合不应失效无读写的支付缓存");
    }

    @Test
    void redisKeys_shouldNotHaveCompatibilityConstantsOrListCacheEntrypoints() throws Exception {
        String constantsSource = Files.readString(
                SOURCE_ROOT.resolve("com/autodealer/crm/shared/infrastructure/constants/Constants.java"));
        String redisKeysSource = Files.readString(
                SOURCE_ROOT.resolve("com/autodealer/crm/shared/infrastructure/cache/RedisKeys.java"));
        String redisManagerSource = Files.readString(
                SOURCE_ROOT.resolve("com/autodealer/crm/shared/infrastructure/cache/RedisManager.java"));
        String tokenFilterSource = Files.readString(
                SOURCE_ROOT.resolve("com/autodealer/crm/bootstrap/security/TokenVerifyFilter.java"));
        String loginHandlerSource = Files.readString(
                SOURCE_ROOT.resolve("com/autodealer/crm/bootstrap/security/MyAuthenticationSuccessHandler.java"));
        String logoutHandlerSource = Files.readString(
                SOURCE_ROOT.resolve("com/autodealer/crm/bootstrap/security/MyLogoutSuccessHandler.java"));

        assertFalse(constantsSource.contains("REDIS_JWT_KEY"), "会话 Redis key 必须通过 RedisKeys.userLogin 构造");
        assertFalse(constantsSource.contains("REDIS_OWNER_KEY"), "负责人 Redis key 必须通过 RedisKeys.ownerList 构造");
        assertFalse(redisKeysSource.contains("dictTypeList"), "字典分页列表缓存没有生产者，不能保留死 key");
        assertFalse(redisKeysSource.contains("dictValueList"), "字典分页列表缓存没有生产者，不能保留死 key");
        assertFalse(redisKeysSource.contains("dictCache("), "旧字典兼容 key 没有生产者，不能保留");
        assertFalse(redisKeysSource.contains("dictCachePattern"), "旧字典兼容 pattern 没有生产者，不能保留");
        assertFalse(redisManagerSource.contains("opsForList"), "普通缓存使用单 value 序列化，禁止 Redis List 回填入口");
        assertFalse(tokenFilterSource.contains("Constants.REDIS_"), "认证过滤器不得手写 Redis key 常量");
        assertFalse(loginHandlerSource.contains("Constants.REDIS_"), "登录处理器不得手写 Redis key 常量");
        assertFalse(logoutHandlerSource.contains("Constants.REDIS_"), "登出处理器不得手写 Redis key 常量");
    }
}
