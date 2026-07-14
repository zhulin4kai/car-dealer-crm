package com.autodealer.crm.modules.fulfillment.payment.application.api.port;

import com.autodealer.crm.modules.fulfillment.payment.application.api.model.TRefundRequest;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface RefundDataPort {
    int insertSelective(TRefundRequest record);

    TRefundRequest selectByPrimaryKey(Integer id);

    TRefundRequest selectByPrimaryKeyForUpdate(Integer id);

    List<TRefundRequest> selectByTranId(@Param("tranId") Integer tranId);

    BigDecimal sumExecutedAmountByOriginalPaymentId(@Param("originalPaymentId") Integer originalPaymentId);

    BigDecimal sumOpenAmountByOriginalPaymentId(@Param("originalPaymentId") Integer originalPaymentId);

    int updateApprovalIfPending(@Param("id") Integer id,
                                @Param("newStatus") String newStatus,
                                @Param("approvedBy") Integer approvedBy,
                                @Param("approvedTime") Date approvedTime,
                                @Param("approveComment") String approveComment,
                                @Param("editBy") Integer editBy,
                                @Param("editTime") Date editTime);

    int markExecutingIfPendingExecution(@Param("id") Integer id,
                                        @Param("executedBy") Integer executedBy,
                                        @Param("executionStartedTime") Date executionStartedTime,
                                        @Param("executionRef") String executionRef,
                                        @Param("executionRemark") String executionRemark,
                                        @Param("editBy") Integer editBy,
                                        @Param("editTime") Date editTime);

    int markCompletedIfExecuting(@Param("id") Integer id,
                                 @Param("refundPaymentId") Integer refundPaymentId,
                                 @Param("executedTime") Date executedTime,
                                 @Param("editBy") Integer editBy,
                                 @Param("editTime") Date editTime);

    int markFailedIfExecutable(@Param("id") Integer id,
                               @Param("executedBy") Integer executedBy,
                               @Param("executedTime") Date executedTime,
                               @Param("failureReason") String failureReason,
                               @Param("executionRef") String executionRef,
                               @Param("executionRemark") String executionRemark,
                               @Param("editBy") Integer editBy,
                               @Param("editTime") Date editTime);

    int deleteByTranId(@Param("tranId") Integer tranId);
}
