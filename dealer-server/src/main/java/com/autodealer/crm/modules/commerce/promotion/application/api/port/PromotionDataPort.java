package com.autodealer.crm.modules.commerce.promotion.application.api.port;

import com.autodealer.crm.modules.commerce.promotion.application.api.model.TProductPromotion;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface PromotionDataPort {
    List<TProductPromotion> selectList(@Param("offset") Integer offset, @Param("limit") Integer limit);

    List<TProductPromotion> selectAvailableByProductIds(@Param("productIds") List<Long> productIds,
                                                        @Param("now") LocalDateTime now,
                                                        @Param("applicableStore") String applicableStore,
                                                        @Param("customerType") String customerType,
                                                        @Param("applicableChannel") String applicableChannel);

    Integer selectCount();

    int countByProductId(@Param("productId") Long productId);

    int countPromotionReferences(@Param("id") Long id);

    TProductPromotion selectById(@Param("id") Long id);

    TProductPromotion selectByCode(@Param("code") String code);

    int insert(TProductPromotion promotion);

    int update(TProductPromotion promotion);

    int updateStatusAtomic(@Param("id") Long id,
                           @Param("expectedStatuses") List<String> expectedStatuses,
                           @Param("newStatus") String newStatus,
                           @Param("pauseReason") String pauseReason,
                           @Param("endReason") String endReason,
                           @Param("voidReason") String voidReason,
                           @Param("updateTime") LocalDateTime updateTime);

    int consumeBudgetAtomic(@Param("id") Long id,
                            @Param("discountAmount") java.math.BigDecimal discountAmount,
                            @Param("now") LocalDateTime now);

    int deleteById(@Param("id") Long id);
}
