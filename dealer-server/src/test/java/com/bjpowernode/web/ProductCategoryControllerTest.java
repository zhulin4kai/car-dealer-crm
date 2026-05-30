package com.bjpowernode.web;

import com.bjpowernode.model.ProductCategory;
import com.bjpowernode.service.ProductCategoryService;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ProductCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductCategoryService categoryService;

    @Test
    void getCategoryList_returnsPageInfo() throws Exception {
        ProductCategory category = new ProductCategory();
        category.setId(1L);
        category.setName("Engine Parts");
        category.setCode("ENG");
        PageInfo<ProductCategory> pageInfo = new PageInfo<>(Collections.singletonList(category));

        when(categoryService.getCategoryList(1, 10)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/product-categories")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getCategoryList_defaultParams() throws Exception {
        PageInfo<ProductCategory> pageInfo = new PageInfo<>(Collections.emptyList());
        when(categoryService.getCategoryList(1, 10)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/product-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getCategoryById_returnsCategory() throws Exception {
        ProductCategory category = new ProductCategory();
        category.setId(1L);
        category.setName("Engine Parts");
        category.setCode("ENG");

        when(categoryService.getCategoryById(1L)).thenReturn(category);

        mockMvc.perform(get("/api/product-categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Engine Parts"));
    }

    @Test
    void addCategory_success() throws Exception {
        doNothing().when(categoryService).addCategory(any(ProductCategory.class));

        mockMvc.perform(post("/api/product-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Category\",\"code\":\"NEW\",\"description\":\"Test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateCategory_success() throws Exception {
        doNothing().when(categoryService).updateCategory(any(ProductCategory.class));

        mockMvc.perform(put("/api/product-categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Category\",\"code\":\"UPD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteCategory_success() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/product-categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
