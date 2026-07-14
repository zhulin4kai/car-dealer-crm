package com.autodealer.crm.modules.sales.customer.application.api.port;

import com.autodealer.crm.modules.identity.application.api.security.DataScope;
import com.autodealer.crm.modules.sales.customer.application.api.dto.CustomerDuplicateSummary;
import com.autodealer.crm.modules.sales.customer.application.api.dto.CustomerOption;
import com.autodealer.crm.modules.sales.customer.application.api.model.TCustomer;
import com.autodealer.crm.modules.sales.customer.application.api.query.CustomerListQuery;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerDataPort {
    int deleteByPrimaryKey(Integer id);
    int insert(TCustomer record);
    int insertSelective(TCustomer record);
    TCustomer selectByPrimaryKey(Integer id);
    int deleteScopedByPrimaryKey(@Param("id") Integer id,
                                 @Param("dataScopeUserId") Integer dataScopeUserId);
    int updateByPrimaryKeySelective(TCustomer record);
    int updateByPrimaryKey(TCustomer record);
    List<TCustomer> selectCustomerPage();
    List<TCustomer> selectCustomerByExcel(@Param("idList") List<String> idList,
                                          @Param("dataScopeUserId") Integer dataScopeUserId,
                                          @Param("maxRows") Integer maxRows);
    int countCustomerByExcel(@Param("idList") List<String> idList,
                             @Param("dataScopeUserId") Integer dataScopeUserId);
    Integer selectByCount(@Param("dataScopeUserId") Integer dataScopeUserId);
    @DataScope(tableAlias = "tct", tableField = "owner_id")
    List<TCustomer> selectByQuery(CustomerListQuery query);
    List<CustomerOption> selectCustomerOptions(Integer dataScopeUserId);
    TCustomer selectScopedById(@Param("id") Integer id, @Param("dataScopeUserId") Integer dataScopeUserId);
    int countByClueId(@Param("clueId") Integer clueId);
    int countByProductId(@Param("productId") Long productId);
    int countActiveDuplicateContacts(@Param("phone") String phone,
                                      @Param("weixin") String weixin,
                                      @Param("customerName") String customerName,
                                      @Param("excludeId") Integer excludeId);
    List<CustomerDuplicateSummary> selectVisibleDuplicateSummaries(@Param("phone") String phone,
                                                                    @Param("weixin") String weixin,
                                                                    @Param("customerName") String customerName,
                                                                    @Param("excludeId") Integer excludeId,
                                                                    @Param("dataScopeUserId") Integer dataScopeUserId,
                                                                    @Param("limit") Integer limit);
    int updateOwnerAtomic(@Param("id") Integer id,
                          @Param("fromOwnerId") Integer fromOwnerId,
                          @Param("toOwnerId") Integer toOwnerId,
                          @Param("editBy") Integer editBy,
                          @Param("dataScopeUserId") Integer dataScopeUserId);
    int markMerged(@Param("sourceCustomerId") Integer sourceCustomerId,
                   @Param("targetCustomerId") Integer targetCustomerId,
                   @Param("reason") String reason,
                   @Param("operatorId") Integer operatorId,
                   @Param("dataScopeUserId") Integer dataScopeUserId);
    int reassignCustomerRemarks(@Param("sourceCustomerId") Integer sourceCustomerId,
                                @Param("targetCustomerId") Integer targetCustomerId);
    int reassignTransactions(@Param("sourceCustomerId") Integer sourceCustomerId,
                             @Param("targetCustomerId") Integer targetCustomerId);
    int reassignQuotes(@Param("sourceCustomerId") Integer sourceCustomerId,
                       @Param("targetCustomerId") Integer targetCustomerId);
    int countBusinessReferences(@Param("id") Integer id);

    int updateRecentFollowFact(@Param("id") Integer id,
                               @Param("lastFollowTime") LocalDateTime lastFollowTime,
                               @Param("lastFollowSummary") String lastFollowSummary,
                               @Param("nextContactTime") LocalDateTime nextContactTime,
                               @Param("editBy") Integer editBy,
                               @Param("dataScopeUserId") Integer dataScopeUserId);
}
