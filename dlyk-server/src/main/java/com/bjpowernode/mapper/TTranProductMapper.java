package com.bjpowernode.mapper;

import com.bjpowernode.model.TTranProduct;
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
} 