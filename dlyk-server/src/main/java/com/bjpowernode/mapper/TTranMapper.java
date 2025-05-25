package com.bjpowernode.mapper;

import com.bjpowernode.model.TTran;
import com.bjpowernode.query.TranQuery;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TTranMapper {

    int insert(TTran record);

    int insertSelective(TTran record);

    TTran selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTran record);

    int updateByPrimaryKey(TTran record);

    BigDecimal selectBySuccessTranAmount();

    BigDecimal selectByTotalTranAmount();

    int selectByTotalTranCount();

    int selectBySuccessTranCount();
    
    /**
     * 根据查询条件查询交易列表
     */
    List<TTran> selectByQuery(TranQuery query);
}