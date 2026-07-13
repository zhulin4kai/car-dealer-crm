package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TPermission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TPermissionMapper {
    int insert(TPermission record);

    int insertSelective(TPermission record);

    TPermission selectByPrimaryKey(Integer id);
    TPermission selectByCode(String code);

    int updateMutableByIdAndVersion(@Param("permission") TPermission permission,
                                    @Param("expectedVersion") Integer expectedVersion);

    List<TPermission> selectMenuPermissionByUserId(Integer userId);

    List<TPermission> selectButtonPermissionByUserId(Integer userId);
    List<TPermission> selectAll();
    List<TPermission> selectByIds(List<Integer> ids);
}
