package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.application.api.*;

import com.autodealer.crm.modules.identity.application.api.dto.profile.ProfileDtos.UpdateRequest;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.ProfileRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProfileContractTest {
 @Test void authorizationFieldsAreRejectedInsteadOfIgnored(){ObjectMapper mapper=new ObjectMapper();String json="{\"profileVersion\":1,\"name\":\"张三\",\"phone\":null,\"email\":null,\"avatarUrl\":null,\"authorizationVersion\":99}";assertThrows(Exception.class,()->mapper.readValue(json,UpdateRequest.class));}
 @Test void requestHasNoClientControlledUserId(){assertThrows(NoSuchFieldException.class,()->UpdateRequest.class.getDeclaredField("userId"));}
 @Test void managedProfileRejectsAuthorizationAndAssignmentFields(){ObjectMapper mapper=new ObjectMapper();String json="{\"profileVersion\":1,\"name\":\"张三\",\"phone\":null,\"email\":null,\"roleIds\":[1],\"organizationUnitId\":2}";assertThrows(Exception.class,()->mapper.readValue(json,ProfileRequest.class));}
 @Test void managedProfileHasNoClientControlledTargetId(){assertThrows(NoSuchFieldException.class,()->ProfileRequest.class.getDeclaredField("userId"));}
}
