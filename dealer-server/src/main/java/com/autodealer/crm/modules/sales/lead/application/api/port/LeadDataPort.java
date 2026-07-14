package com.autodealer.crm.modules.sales.lead.application.api.port;

import com.autodealer.crm.modules.identity.application.api.security.DataScope;
import com.autodealer.crm.modules.sales.lead.application.api.model.TClue;
import com.autodealer.crm.shared.pagination.BaseQuery;
import com.autodealer.crm.modules.analytics.application.api.result.NameValue;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LeadDataPort {

    int deleteByPrimaryKey(Integer id);

    int deleteScopedByPrimaryKey(@Param("id") Integer id,
                                 @Param("dataScopeUserId") Integer dataScopeUserId);

    int insert(TClue record);

    int insertSelective(TClue record);

    TClue selectByPrimaryKey(Integer id);

    TClue selectScopedByPrimaryKey(@Param("id") Integer id,
                                   @Param("dataScopeUserId") Integer dataScopeUserId);

    int updateByPrimaryKeySelective(TClue record);

    int updateByPrimaryKey(TClue record);

    @DataScope(tableAlias = "tc", tableField = "owner_id")
    List<TClue> selectClueByPage(BaseQuery build);

    int saveClue(List<TClue> tClueList);

    int selectByCount(String phone);

    int selectClueByCount(@Param("dataScopeUserId") Integer dataScopeUserId);

    TClue selectDetailById(@Param("id") Integer id, @Param("dataScopeUserId") Integer dataScopeUserId);

    List<NameValue> selectBySource(@Param("dataScopeUserId") Integer dataScopeUserId);

    int batchDeleteByIds(List<Integer> ids);

    int batchDeleteScopedByIds(@Param("ids") List<Integer> ids,
                               @Param("dataScopeUserId") Integer dataScopeUserId);

    int updateStateToConverted(@Param("id") Integer id,
                               @Param("editBy") Integer editBy,
                               @Param("dataScopeUserId") Integer dataScopeUserId);

    int updateOwnerAtomic(@Param("id") Integer id,
                          @Param("fromOwnerId") Integer fromOwnerId,
                          @Param("toOwnerId") Integer toOwnerId,
                          @Param("editBy") Integer editBy,
                          @Param("dataScopeUserId") Integer dataScopeUserId);

    int updateStateAtomic(@Param("id") Integer id,
                          @Param("fromState") Integer fromState,
                          @Param("toState") Integer toState,
                          @Param("editBy") Integer editBy,
                          @Param("dataScopeUserId") Integer dataScopeUserId);

    int countActiveByPhoneExcludingId(@Param("phone") String phone,
                                      @Param("excludedId") Integer excludedId,
                                      @Param("closedState") Integer closedState,
                                      @Param("convertedState") Integer convertedState);

    /**
     * 批量查询已存在的手机号，用于 Excel 导入时检测数据库重复。
     */
    List<String> selectExistingPhones(@Param("phones") List<String> phones);

    int countByIntentionProductId(@Param("productId") Long productId);

    int updateRecentFollowFact(@Param("id") Integer id,
                               @Param("lastFollowTime") LocalDateTime lastFollowTime,
                               @Param("lastFollowSummary") String lastFollowSummary,
                               @Param("nextContactTime") LocalDateTime nextContactTime,
                               @Param("editBy") Integer editBy,
                               @Param("dataScopeUserId") Integer dataScopeUserId);
}
