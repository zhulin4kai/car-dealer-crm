package com.autodealer.crm.modules.identity.persistence.mapper;

import com.autodealer.crm.modules.identity.persistence.model.TUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TUserRoleMapper {
    int insert(TUserRole userRole);
    List<TUserRole> selectCurrentAndFutureByUserId(@Param("userId") Integer userId,
                                                   @Param("effectiveAt") LocalDateTime effectiveAt);
    int expireElapsedMarkers(@Param("userId") Integer userId,
                             @Param("effectiveAt") LocalDateTime effectiveAt);
    int closeByIdAndVersion(@Param("id") Long id, @Param("expectedVersion") Integer expectedVersion,
                            @Param("effectiveTo") LocalDateTime effectiveTo);

    List<TUserRole> selectEffectiveByUserId(@Param("userId") Integer userId,
                                            @Param("effectiveAt") LocalDateTime effectiveAt);
    List<Integer> selectEffectiveUserIdsByRoleId(@Param("roleId") Integer roleId,
                                                  @Param("effectiveAt") LocalDateTime effectiveAt);
    List<Integer> selectCurrentAndFutureUserIdsByRoleId(@Param("roleId") Integer roleId,
                                                        @Param("effectiveAt") LocalDateTime effectiveAt);
}
