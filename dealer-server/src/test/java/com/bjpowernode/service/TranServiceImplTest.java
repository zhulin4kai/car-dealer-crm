package com.bjpowernode.service;

import com.bjpowernode.manager.RedisManager;
import com.bjpowernode.mapper.*;
import com.bjpowernode.model.TTran;
import com.bjpowernode.model.TTranApprove;
import com.bjpowernode.model.TTranProduct;
import com.bjpowernode.service.impl.TranServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranServiceImplTest {

    @InjectMocks
    private TranServiceImpl tranService;

    @Mock
    private TTranMapper tranMapper;

    @Mock
    private TTranRemarkMapper tranRemarkMapper;

    @Mock
    private TTranProductMapper tranProductMapper;

    @Mock
    private TTranInvoiceMapper tranInvoiceMapper;

    @Mock
    private TTranApproveMapper tranApproveMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private RedisManager redisManager;

    @Test
    void testDeleteTransactionProductsRollbackOnError() {
        Integer tranId = 1;
        TTranProduct product1 = new TTranProduct();
        product1.setProductId(10);
        product1.setQuantity(3);
        product1.setTranId(tranId);

        TTranProduct product2 = new TTranProduct();
        product2.setProductId(20);
        product2.setQuantity(5);
        product2.setTranId(tranId);

        List<TTranProduct> products = Arrays.asList(product1, product2);

        when(tranProductMapper.selectByTranId(tranId)).thenReturn(products);
        doNothing().when(productMapper).updateStock(eq(10L), eq(3));
        doThrow(new RuntimeException("DB connection lost"))
                .when(productMapper).updateStock(eq(20L), eq(5));

        // Bug: exception is caught and swallowed, returns false but doesn't re-throw,
        // so @Transactional never sees the exception and never rolls back.
        // After fix: the method should re-throw so the transaction rolls back.
        boolean result = tranService.deleteTransactionProducts(tranId);

        // Current buggy behavior: returns false (exception swallowed)
        assertFalse(result, "Method returns false when exception occurs (exception swallowed)");

        // Verify that productMapper.updateStock was called for the first product
        // (restoring stock before delete). The second call threw, so deleteByTranId was never called.
        verify(tranProductMapper, never()).deleteByTranId(tranId);
    }

    @Test
    void testAddTransactionProductsRollbackOnError() {
        Integer tranId = 1;
        TTranProduct product1 = new TTranProduct();
        product1.setProductId(10);
        product1.setQuantity(2);
        product1.setPrice(BigDecimal.valueOf(100));

        TTranProduct product2 = new TTranProduct();
        product2.setProductId(20);
        product2.setQuantity(1);
        product2.setPrice(BigDecimal.valueOf(200));

        List<TTranProduct> products = Arrays.asList(product1, product2);

        when(tranProductMapper.insertSelective(any(TTranProduct.class))).thenReturn(1);
        doNothing().when(productMapper).updateStock(eq(10L), eq(-2));
        doThrow(new RuntimeException("DB error"))
                .when(productMapper).updateStock(eq(20L), eq(-1));

        // Bug: exception is caught and swallowed, returns false but doesn't re-throw,
        // so @Transactional never sees the exception and never rolls back.
        // After fix: the method should re-throw so the transaction rolls back.
        boolean result = tranService.addTransactionProducts(tranId, products);

        // Current buggy behavior: returns false (exception swallowed)
        assertFalse(result, "Method returns false when exception occurs (exception swallowed)");

        // Verify that the second product's stock was attempted but failed
        verify(productMapper).updateStock(eq(10L), eq(-2));
        verify(productMapper).updateStock(eq(20L), eq(-1));
    }

    @Test
    void testApproveTranRollbackOnError() {
        Integer tranId = 1;
        Boolean approved = true;
        String comment = "Approved";
        Integer approveBy = 1;

        TTranApprove approve = new TTranApprove();
        approve.setTranId(tranId);
        approve.setApproveResult(approved);
        approve.setApproveComment(comment);
        approve.setApproveBy(approveBy);

        // First insert succeeds (approve record)
        when(tranApproveMapper.insertSelective(any(TTranApprove.class))).thenReturn(1);
        // Second update fails (transaction stage update)
        when(tranMapper.updateByPrimaryKeySelective(any(TTran.class)))
                .thenThrow(new RuntimeException("DB error during update"));

        // Bug: exception is caught and swallowed, returns false but doesn't re-throw,
        // so @Transactional never sees the exception and never rolls back.
        // After fix: the method should re-throw so the transaction rolls back.
        boolean result = tranService.approveTran(tranId, approved, comment, approveBy);

        // Current buggy behavior: returns false (exception swallowed)
        assertFalse(result, "Method returns false when exception occurs (exception swallowed)");

        // Verify approve record was inserted but transaction was not updated
        verify(tranApproveMapper).insertSelective(any(TTranApprove.class));
        verify(tranMapper).updateByPrimaryKeySelective(any(TTran.class));
    }
}
