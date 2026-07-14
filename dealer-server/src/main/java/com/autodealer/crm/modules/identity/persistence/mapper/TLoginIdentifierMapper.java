package com.autodealer.crm.modules.identity.persistence.mapper;

import com.autodealer.crm.modules.identity.persistence.model.TLoginIdentifier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface TLoginIdentifierMapper {
    TLoginIdentifier selectByLoginActForUpdate(String loginAct);
    TLoginIdentifier selectActiveByUserIdForUpdate(Integer userId);
    int insert(TLoginIdentifier identifier);
    int retireByExpected(@Param("id") Long id,
                         @Param("expectedVersion") Integer expectedVersion,
                         @Param("retiredAt") LocalDateTime retiredAt,
                         @Param("changedBy") Integer changedBy,
                         @Param("reason") String reason);
    int reactivateByExpected(@Param("id") Long id,
                             @Param("expectedVersion") Integer expectedVersion,
                             @Param("changedBy") Integer changedBy,
                             @Param("reason") String reason);
}
