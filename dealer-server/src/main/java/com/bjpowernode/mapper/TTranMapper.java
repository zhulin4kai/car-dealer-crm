package com.bjpowernode.mapper;

import com.bjpowernode.model.TTran;
import com.bjpowernode.model.TTranProduct;
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
      /**
     * 根据交易ID查询交易产品列表（包含产品名称）
     */
    List<TTranProduct> selectTranProductsByTranId(Integer tranId);
    
    /**
     * 根据ID删除交易
     */
    int deleteByPrimaryKey(Integer id);
    
    /**
     * 批量删除交易
     */
    int deleteByIds(List<Integer> ids);

    /**
     * 根据客户ID查询交易数量
     */
    int selectCountByCustomerId(Integer customerId);
}