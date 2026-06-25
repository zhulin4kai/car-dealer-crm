package com.autodealer.crm.mapper;

import com.autodealer.crm.commons.DataScope;
import com.autodealer.crm.model.TClue;
import com.autodealer.crm.query.BaseQuery;
import com.autodealer.crm.result.NameValue;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TClueMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(TClue record);

    int insertSelective(TClue record);

    TClue selectByPrimaryKey(Integer id);

    TClue selectScopedByPrimaryKey(@Param("id") Integer id,
                                   @Param("dataScopeUserId") Integer dataScopeUserId);

    int updateByPrimaryKeySelective(TClue record);

    int updateByPrimaryKey(TClue record);

    @DataScope(tableAlias = "tc", tableField = "owner_id")
    List<TClue> selectClueByPage(BaseQuery build);

    int saveClue(List<TClue> tClueList);

    int selectByCount(String phone);

    int selectClueByCount(@Param("dataScopeUserId") Integer dataScopeUserId);

    TClue selectDetailById(@Param("id") Integer id, @Param("dataScopeUserId") Integer dataScopeUserId);

    List<NameValue> selectBySource(@Param("dataScopeUserId") Integer dataScopeUserId);

    int batchDeleteByIds(List<Integer> ids);

    int updateStateToConverted(@Param("id") Integer id,
                               @Param("editBy") Integer editBy,
                               @Param("dataScopeUserId") Integer dataScopeUserId);

    /**
     * 批量查询已存在的手机号，用于 Excel 导入时检测数据库重复。
     */
    List<String> selectExistingPhones(@Param("phones") List<String> phones);

    int countByIntentionProductId(@Param("productId") Long productId);
}
