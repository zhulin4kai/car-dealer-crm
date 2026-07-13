package com.autodealer.crm.service;

import com.autodealer.crm.dto.user.UserLifecycleDtos.*;

public interface UserLifecycleService {
    Context getContext(Integer userId, Integer targetOrganizationId);
    default Context getContext(Integer userId) { return getContext(userId, null); }
    Context transfer(Integer userId, AssignmentCommand request);
    DeparturePrecheck precheckDeparture(Integer userId, DeparturePrecheckRequest request);
    Context startDeparture(Integer userId, StartDepartureRequest request);
    HandoverResult confirmHandover(Integer userId, ConfirmHandoverRequest request);
    Context completeDeparture(Integer userId, CompleteDepartureRequest request);
    RehireResult rehire(Integer userId, RehireRequest request);
}
