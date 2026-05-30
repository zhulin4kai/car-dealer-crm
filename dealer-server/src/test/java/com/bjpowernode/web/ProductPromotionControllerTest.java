package com.bjpowernode.web;

import com.bjpowernode.model.ProductPromotion;
import com.bjpowernode.service.ProductPromotionService;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ProductPromotionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductPromotionService promotionService;

    @Test
    void getPromotionList_returnsPageInfo() throws Exception {
        ProductPromotion promotion = new ProductPromotion();
        promotion.setId(1L);
        promotion.setName("Summer Sale");
        promotion.setDiscount(new BigDecimal("0.8"));
        PageInfo<ProductPromotion> pageInfo = new PageInfo<>(Collections.singletonList(promotion));

        when(promotionService.getPromotionList(1, 10)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/product-promotions")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getPromotionList_defaultParams() throws Exception {
        PageInfo<ProductPromotion> pageInfo = new PageInfo<>(Collections.emptyList());
        when(promotionService.getPromotionList(1, 10)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/product-promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getPromotionById_returnsPromotion() throws Exception {
        ProductPromotion promotion = new ProductPromotion();
        promotion.setId(1L);
        promotion.setName("Summer Sale");
        promotion.setDiscount(new BigDecimal("0.8"));

        when(promotionService.getPromotionById(1L)).thenReturn(promotion);

        mockMvc.perform(get("/api/product-promotions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Summer Sale"));
    }

    @Test
    void addPromotion_success() throws Exception {
        doNothing().when(promotionService).addPromotion(any(ProductPromotion.class));

        mockMvc.perform(post("/api/product-promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Promotion\",\"type\":\"DISCOUNT\",\"discount\":0.9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updatePromotion_success() throws Exception {
        doNothing().when(promotionService).updatePromotion(any(ProductPromotion.class));

        mockMvc.perform(put("/api/product-promotions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Promotion\",\"discount\":0.7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deletePromotion_success() throws Exception {
        doNothing().when(promotionService).deletePromotion(1L);

        mockMvc.perform(delete("/api/product-promotions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
