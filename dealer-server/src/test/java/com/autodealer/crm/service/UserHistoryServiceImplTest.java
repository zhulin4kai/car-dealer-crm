package com.autodealer.crm.service;

import com.autodealer.crm.dto.user.UserHistoryDtos.Collection;
import com.autodealer.crm.dto.user.UserHistoryDtos.Query;
import com.autodealer.crm.dto.user.UserHistoryRows.ActionFacet;
import com.autodealer.crm.dto.user.UserHistoryRows.ProjectionQuery;
import com.autodealer.crm.dto.user.UserHistoryRows.ProjectionRow;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.mapper.UserHistoryProjectionMapper;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.UserAuthorizationPolicy;
import com.autodealer.crm.service.impl.UserHistoryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserHistoryServiceImplTest {
    @Mock TUserMapper users;
    @Mock UserHistoryProjectionMapper history;
    @Mock UserAuthorizationPolicy policy;

    private UserHistoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserHistoryServiceImpl(users, history, policy, new ObjectMapper());
    }

    @Test
    void pushesActionCountOffsetAndLimitToDatabaseWhileKeepingUnfilteredActionOptions() {
        TUser target = target();
        when(users.selectByPrimaryKey(2)).thenReturn(target);
        when(history.count(any())).thenReturn(5L);
        when(history.selectPage(any())).thenReturn(List.of(operationRow()));
        when(history.selectActionFacets(any())).thenReturn(List.of(
                operationFacet("USER_PROFILE_UPDATE"), operationFacet("USER_STATUS_CHANGE")));

        Query query = new Query();
        query.setPage(3);
        query.setSize(2);
        query.setActionCode("USER_STATUS_CHANGE");
        query.setStartTime(OffsetDateTime.parse("2026-07-01T00:00:00+08:00"));
        query.setEndTime(OffsetDateTime.parse("2026-07-31T23:59:59+08:00"));

        Collection result = service.getUserHistory(2, query);

        assertEquals(5, result.getTotal());
        assertEquals(3, result.getPages());
        assertEquals(1, result.getSize());
        assertEquals("operation:91", result.getList().get(0).eventId());
        assertEquals(List.of("USER_PROFILE_UPDATE", "USER_STATUS_CHANGE"),
                result.getActionOptions().stream().map(option -> option.code()).toList());

        ArgumentCaptor<ProjectionQuery> criteria = ArgumentCaptor.forClass(ProjectionQuery.class);
        verify(history).selectPage(criteria.capture());
        ProjectionQuery actual = criteria.getValue();
        assertEquals(2, actual.getUserId());
        assertEquals("2", actual.getResourceId());
        assertEquals(4L, actual.getOffset());
        assertEquals(2, actual.getLimit());
        assertEquals("OPERATION_LOG", actual.getFilterSource());
        assertEquals("USER_STATUS_CHANGE", actual.getFilterActionCode());
    }

    @Test
    void authorizationFailureClosesBeforeAnyHistoryQuery() {
        TUser target = target();
        when(users.selectByPrimaryKey(2)).thenReturn(target);
        doThrow(new BusinessException(CodeEnum.ACCESS_DENIED)).when(policy).requireManage(target);

        BusinessException failure = assertThrows(BusinessException.class,
                () -> service.getUserHistory(2, new Query()));

        assertEquals(CodeEnum.ACCESS_DENIED, failure.getCodeEnum());
        verifyNoInteractions(history);
    }

    private TUser target() {
        TUser target = new TUser();
        target.setId(2);
        target.setLoginAct("managed_user");
        target.setName("受管用户");
        return target;
    }

    private ProjectionRow operationRow() {
        ProjectionRow row = new ProjectionRow();
        row.setId(91L);
        row.setSourceKey("OPERATION_LOG");
        row.setActionCode("USER_STATUS_CHANGE");
        row.setResult("SUCCESS");
        row.setDetail("{\"summary\":{\"reason\":\"状态调整\"}}");
        row.setOperatorId(1);
        row.setOperatorName("管理员");
        row.setOccurredTime(LocalDateTime.parse("2026-07-20T10:00:00"));
        return row;
    }

    private ActionFacet operationFacet(String actionCode) {
        ActionFacet facet = new ActionFacet();
        facet.setSourceKey("OPERATION_LOG");
        facet.setActionCode(actionCode);
        return facet;
    }
}
