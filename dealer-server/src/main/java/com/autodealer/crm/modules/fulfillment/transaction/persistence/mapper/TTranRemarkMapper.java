package com.autodealer.crm.modules.fulfillment.transaction.persistence.mapper;

import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTranRemark;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface TTranRemarkMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTranRemark record);

    int insertSelective(TTranRemark record);

    TTranRemark selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTranRemark record);

    int updateByPrimaryKey(TTranRemark record);

    /**
     * 根据交易ID查询跟踪记录
     */
    List<TTranRemark> selectByTranId(Integer tranId);

    /**
     * 根据交易ID删除跟踪记录
     */
    int deleteByTranId(Integer tranId);
}
