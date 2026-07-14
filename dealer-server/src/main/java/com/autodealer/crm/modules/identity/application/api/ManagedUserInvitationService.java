package com.autodealer.crm.modules.identity.application.api;

import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.CreateRequest;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.CreateResult;
public interface ManagedUserInvitationService { CreateResult create(CreateRequest request); }
