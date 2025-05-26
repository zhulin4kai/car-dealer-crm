package com.bjpowernode.mapper;

import com.bjpowernode.model.TTranApprove;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TTranApproveMapper {
    
    int insert(TTranApprove record);

    int insertSelective(TTranApprove record);

    TTranApprove selectByPrimaryKey(Integer id);
    
    TTranApprove selectByTranId(Integer tranId);

    int updateByPrimaryKeySelective(TTranApprove record);

    int updateByPrimaryKey(TTranApprove record);
} 