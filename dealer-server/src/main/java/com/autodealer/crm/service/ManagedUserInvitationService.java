package com.autodealer.crm.service;
import com.autodealer.crm.dto.user.ManagedUserDtos.CreateRequest;
import com.autodealer.crm.dto.user.ManagedUserDtos.CreateResult;
public interface ManagedUserInvitationService { CreateResult create(CreateRequest request); }
