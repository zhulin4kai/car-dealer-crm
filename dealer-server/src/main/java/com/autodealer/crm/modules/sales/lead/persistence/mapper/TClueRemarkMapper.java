package com.autodealer.crm.modules.sales.lead.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.autodealer.crm.modules.identity.application.api.security.DataScope;
import com.autodealer.crm.modules.sales.lead.application.api.model.TClueRemark;
import com.autodealer.crm.modules.sales.lead.application.api.query.ClueRemarkQuery;

import java.util.List;


@Mapper
public interface TClueRemarkMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(TClueRemark record);

    int insertSelective(TClueRemark record);

    TClueRemark selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TClueRemark record);

    int updateByPrimaryKey(TClueRemark record);

    @DataScope(tableAlias = "tcr", tableField = "create_by")
    List<TClueRemark> selectClueRemarkByPage(ClueRemarkQuery clueRemarkQuery);

    /**
     * 根据线索ID删除关联备注
     */
    int deleteByClueId(Integer clueId);
}
