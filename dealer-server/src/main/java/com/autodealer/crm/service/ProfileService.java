package com.autodealer.crm.service;
import com.autodealer.crm.dto.profile.ProfileDtos.Profile;
import com.autodealer.crm.dto.profile.ProfileDtos.UpdateRequest;
public interface ProfileService { Profile getOwn(); Profile updateOwn(UpdateRequest request); }
