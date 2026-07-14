package com.autodealer.crm.modules.identity.persistence.mapper;

import com.autodealer.crm.modules.identity.persistence.model.TPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
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
