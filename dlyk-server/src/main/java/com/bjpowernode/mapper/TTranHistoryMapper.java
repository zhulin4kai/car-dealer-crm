package com.bjpowernode.mapper;

import com.bjpowernode.model.TTranHistory;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface TTranHistoryMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTranHistory record);

    int insertSelective(TTranHistory record);

    TTranHistory selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTranHistory record);

    int updateByPrimaryKey(TTranHistory record);
    
    /**
     * 根据交易ID查询历史记录
     */
    List<TTranHistory> selectByTranId(Integer tranId);
}