package com.autodealer.crm.modules.sales.followup.application.internal;

import com.autodealer.crm.modules.sales.followup.application.api.FollowRelatedObjectAccess;
import com.autodealer.crm.modules.sales.followup.application.api.model.FollowRelatedObjectContext;

import com.autodealer.crm.modules.identity.application.api.port.UserDirectoryDataPort;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.port.TransactionDataPort;
import com.autodealer.crm.modules.sales.testdrive.application.api.port.TestDriveDataPort;
import com.autodealer.crm.modules.sales.opportunity.application.api.port.OpportunityDataPort;
import com.autodealer.crm.modules.sales.customer.application.api.port.CustomerDataPort;
import com.autodealer.crm.modules.sales.lead.application.api.port.LeadDataPort;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.sales.followup.application.api.enums.FollowRelatedObjectType;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.sales.lead.application.api.model.TClue;
import com.autodealer.crm.modules.sales.customer.application.api.model.TCustomer;
import com.autodealer.crm.modules.sales.opportunity.application.api.model.TOpportunity;
import com.autodealer.crm.modules.sales.testdrive.application.api.model.TTestDrive;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTran;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.shared.error.CodeEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Component
public class FollowRelatedObjectResolver implements FollowRelatedObjectAccess {

    private final LeadDataPort clueMapper;
    private final CustomerDataPort customerMapper;
    private final OpportunityDataPort opportunityMapper;
    private final TestDriveDataPort testDriveMapper;
    private final TransactionDataPort tranMapper;
    private final UserDirectoryDataPort userMapper;
    private final CurrentUserProvider currentUserProvider;

    public FollowRelatedObjectResolver(LeadDataPort clueMapper,
                                       CustomerDataPort customerMapper,
                                       OpportunityDataPort opportunityMapper,
                                       TestDriveDataPort testDriveMapper,
                                       TransactionDataPort tranMapper,
                                       UserDirectoryDataPort userMapper,
                                       CurrentUserProvider currentUserProvider) {
        this.clueMapper = clueMapper;
        this.customerMapper = customerMapper;
        this.opportunityMapper = opportunityMapper;
        this.testDriveMapper = testDriveMapper;
        this.tranMapper = tranMapper;
        this.userMapper = userMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public FollowRelatedObjectContext requireAccessible(String rawType, Long objectId) {
        FollowRelatedObjectType type = parseObjectType(rawType);
        if (objectId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "关联对象ID不能为空");
        }
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        return switch (type) {
            case CLUE -> resolveClue(objectId, dataScopeUserId);
            case CUSTOMER -> resolveCustomer(objectId, dataScopeUserId);
            case OPPORTUNITY -> resolveOpportunity(objectId, dataScopeUserId);
            case TEST_DRIVE -> resolveTestDrive(objectId, dataScopeUserId);
            case ORDER -> resolveOrder(objectId, dataScopeUserId);
        };
    }

    @Override
    public void validateAssignableOwner(Integer ownerId) {
        if (ownerId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "负责人不能为空");
        }
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        if (dataScopeUserId != null && !dataScopeUserId.equals(ownerId)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "不能为数据范围外的负责人创建跟进任务");
        }
        TUser owner = userMapper.selectByPrimaryKey(ownerId);
        if (owner == null || !Integer.valueOf(1).equals(owner.getAccountEnabled())
                || !Integer.valueOf(1).equals(owner.getAccountNoLocked())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "负责人不存在或不可用");
        }
    }

    public void updateRecentFollowFact(FollowRelatedObjectType type,
                                       Long objectId,
                                       LocalDateTime followTime,
                                       String summary,
                                       LocalDateTime nextFollowTime,
                                       Integer operatorId) {
        String safeSummary = StringUtils.hasText(summary) ? summary.trim() : null;
        Integer scope = currentUserProvider.getDataScopeUserId();
        int rows = switch (type) {
            case CLUE -> clueMapper.updateRecentFollowFact(objectId.intValue(), followTime, safeSummary,
                    nextFollowTime, operatorId, scope);
            case CUSTOMER -> customerMapper.updateRecentFollowFact(objectId.intValue(), followTime, safeSummary,
                    nextFollowTime, operatorId, scope);
            case OPPORTUNITY -> opportunityMapper.updateRecentFollowFact(objectId, followTime, safeSummary,
                    nextFollowTime == null ? null : nextFollowTime.toLocalDate(), operatorId, scope);
            case TEST_DRIVE, ORDER -> 1;
        };
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "关联对象最近跟进事实写入失败");
        }
    }

    private FollowRelatedObjectContext resolveClue(Long objectId, Integer dataScopeUserId) {
        TClue clue = clueMapper.selectScopedByPrimaryKey(objectId.intValue(), dataScopeUserId);
        if (clue == null) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "线索不存在或无权访问");
        }
        return new FollowRelatedObjectContext(FollowRelatedObjectType.CLUE, objectId, clue.getOwnerId(),
                clue.getFullName());
    }

    private FollowRelatedObjectContext resolveCustomer(Long objectId, Integer dataScopeUserId) {
        TCustomer customer = customerMapper.selectScopedById(objectId.intValue(), dataScopeUserId);
        if (customer == null) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "客户不存在或无权访问");
        }
        return new FollowRelatedObjectContext(FollowRelatedObjectType.CUSTOMER, objectId, customer.getOwnerId(),
                customer.getCustomerName());
    }

    private FollowRelatedObjectContext resolveOpportunity(Long objectId, Integer dataScopeUserId) {
        TOpportunity opportunity = opportunityMapper.selectById(objectId);
        if (opportunity == null || (dataScopeUserId != null && !dataScopeUserId.equals(opportunity.getOwnerId()))) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "商机不存在或无权访问");
        }
        return new FollowRelatedObjectContext(FollowRelatedObjectType.OPPORTUNITY, objectId, opportunity.getOwnerId(),
                opportunity.getOpportunityNo());
    }

    private FollowRelatedObjectContext resolveTestDrive(Long objectId, Integer dataScopeUserId) {
        TTestDrive testDrive = testDriveMapper.selectById(objectId);
        if (testDrive == null || (dataScopeUserId != null && !dataScopeUserId.equals(testDrive.getOwnerId()))) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "试驾不存在或无权访问");
        }
        return new FollowRelatedObjectContext(FollowRelatedObjectType.TEST_DRIVE, objectId, testDrive.getOwnerId(),
                testDrive.getTestDriveNo());
    }

    private FollowRelatedObjectContext resolveOrder(Long objectId, Integer dataScopeUserId) {
        TTran order = tranMapper.selectByPrimaryKey(objectId.intValue());
        if (order == null) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "订单不存在或无权访问");
        }
        TCustomer customer = customerMapper.selectScopedById(order.getCustomerId(), dataScopeUserId);
        if (customer == null) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "订单不存在或无权访问");
        }
        return new FollowRelatedObjectContext(FollowRelatedObjectType.ORDER, objectId, customer.getOwnerId(),
                order.getTranNo());
    }

    private FollowRelatedObjectType parseObjectType(String value) {
        try {
            return FollowRelatedObjectType.parse(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, ex.getMessage());
        }
    }
}
