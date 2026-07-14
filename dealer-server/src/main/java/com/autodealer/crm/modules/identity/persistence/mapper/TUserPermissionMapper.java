package com.autodealer.crm.modules.identity.persistence.mapper;

import com.autodealer.crm.modules.identity.persistence.model.TUserPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TUserPermissionMapper {
    int insert(TUserPermission userPermission);

    int updateCurrentByVersion(@Param("userPermission") TUserPermission userPermission,
                               @Param("expectedVersion") Integer expectedVersion);

    TUserPermission selectCurrentEffective(@Param("userId") Integer userId,
                                           @Param("permissionId") Integer permissionId,
                                           @Param("effectiveAt") LocalDateTime effectiveAt);

    TUserPermission selectCurrent(@Param("userId") Integer userId,
                                  @Param("permissionId") Integer permissionId);

    int deleteByUserAndPermission(@Param("userId") Integer userId,
                                  @Param("permissionId") Integer permissionId);

    List<TUserPermission> selectEffectiveByUserId(@Param("userId") Integer userId,
                                                  @Param("effectiveAt") LocalDateTime effectiveAt);

    List<TUserPermission> selectCurrentAndFutureByUserId(@Param("userId") Integer userId,
                                                         @Param("effectiveAt") LocalDateTime effectiveAt);

    int closeByIdAndVersion(@Param("id") Long id,@Param("expectedVersion") Integer expectedVersion,
                            @Param("effectiveTo") LocalDateTime effectiveTo);

}
