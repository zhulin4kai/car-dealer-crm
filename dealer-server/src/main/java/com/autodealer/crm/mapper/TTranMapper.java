package com.autodealer.crm.mapper;

import com.autodealer.crm.commons.DataScope;
import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.model.TTran;
import com.autodealer.crm.model.TTranProduct;
import com.autodealer.crm.query.TranQuery;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TTranMapper {

    int insert(TTran record);

    int insertSelective(TTran record);

    TTran selectByPrimaryKey(Integer id);

    TTran selectByPrimaryKeyForUpdate(Integer id);

    TTran selectScopedById(@Param("id") Integer id,
                           @Param("dataScopeUserId") Integer dataScopeUserId,
                           @Param("transactionApprovalScope") boolean transactionApprovalScope,
                           @Param("transactionFinanceStages") List<TranStage> transactionFinanceStages);

    int updateByPrimaryKeySelective(TTran record);

    int updateByPrimaryKey(TTran record);

    BigDecimal selectBySuccessTranAmount(@Param("successStage") TranStage successStage,
                                         @Param("transactionAllScope") boolean transactionAllScope,
                                         @Param("dataScopeUserId") Integer dataScopeUserId);

    BigDecimal selectByTotalTranAmount(@Param("transactionAllScope") boolean transactionAllScope,
                                       @Param("dataScopeUserId") Integer dataScopeUserId);

    int selectByTotalTranCount(@Param("transactionAllScope") boolean transactionAllScope,
                               @Param("dataScopeUserId") Integer dataScopeUserId);

    int selectBySuccessTranCount(@Param("successStage") TranStage successStage,
                                 @Param("transactionAllScope") boolean transactionAllScope,
                                 @Param("dataScopeUserId") Integer dataScopeUserId);

    /**
     * 根据查询条件查询交易列表
     */
    @DataScope(tableAlias = "t", tableField = "create_by")
    List<TTran> selectByQuery(TranQuery query);
      /**
     * 根据交易ID查询交易产品列表（包含产品名称）
     */
    List<TTranProduct> selectTranProductsByTranId(Integer tranId);

    /**
     * 根据ID删除交易
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * 批量删除交易
     */
    int deleteByIds(List<Integer> ids);

    /**
     * 根据客户ID查询交易数量
     */
    int selectCountByCustomerId(Integer customerId);

    /**
     * 查询客户非终态交易数量。
     */
    int selectActiveCountByCustomerId(@Param("customerId") Integer customerId);

    /**
     * 原子更新交易阶段，仅当当前阶段匹配时才更新
     * @return 受影响行数，0表示阶段不匹配
     */
    int updateStageAtomic(@Param("id") Integer id, @Param("newStage") TranStage newStage,
                          @Param("expectedStage") TranStage expectedStage,
                          @Param("editBy") Integer editBy);

    int settleAtomic(@Param("id") Integer id,
                     @Param("amount") BigDecimal amount,
                     @Param("originalAmount") BigDecimal originalAmount,
                     @Param("discountAmount") BigDecimal discountAmount,
                     @Param("promotionId") Long promotionId,
                     @Param("promotionSnapshot") String promotionSnapshot,
                     @Param("expectedVersion") Integer expectedVersion,
                     @Param("editBy") Integer editBy);

    int incrementVersion(@Param("id") Integer id,
                         @Param("expectedVersion") Integer expectedVersion,
                         @Param("editBy") Integer editBy);

}
