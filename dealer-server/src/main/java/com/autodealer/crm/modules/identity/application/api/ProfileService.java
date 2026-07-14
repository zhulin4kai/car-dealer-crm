package com.autodealer.crm.modules.identity.application.api;

import com.autodealer.crm.modules.identity.application.api.dto.profile.ProfileDtos.Profile;
import com.autodealer.crm.modules.identity.application.api.dto.profile.ProfileDtos.UpdateRequest;
public interface ProfileService { Profile getOwn(); Profile updateOwn(UpdateRequest request); }
