package com.bjpowernode.mapper;

import com.bjpowernode.model.TTranProduction;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface TTranProductionMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTranProduction record);

    int insertSelective(TTranProduction record);

    TTranProduction selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTranProduction record);

    int updateByPrimaryKey(TTranProduction record);
    
    /**
     * 根据交易产品ID查询生产状态
     */
    TTranProduction selectByTranProductId(Integer tranProductId);
} 