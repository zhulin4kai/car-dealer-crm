package com.autodealer.crm.modules.identity.persistence.mapper;

import com.autodealer.crm.modules.identity.application.api.port.UserDirectoryDataPort;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TUserMapper extends UserDirectoryDataPort {
}
