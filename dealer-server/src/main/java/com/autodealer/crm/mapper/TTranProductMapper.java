package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TTranProduct;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface TTranProductMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTranProduct record);

    int insertSelective(TTranProduct record);

    TTranProduct selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTranProduct record);

    int updateByPrimaryKey(TTranProduct record);
    
    /**
     * 根据交易ID查询产品列表
     */
    List<TTranProduct> selectByTranId(Integer tranId);
    
    /**
     * 根据交易ID删除产品列表
     */
    int deleteByTranId(Integer tranId);

    int selectClueNameByTranId(Integer tranId);
} 