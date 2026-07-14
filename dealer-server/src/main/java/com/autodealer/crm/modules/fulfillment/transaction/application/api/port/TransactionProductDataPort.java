package com.autodealer.crm.modules.fulfillment.transaction.application.api.port;

import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTranProduct;
import java.util.List;

public interface TransactionProductDataPort {
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

    int countByProductId(Long productId);

    int selectClueNameByTranId(Integer tranId);
}
