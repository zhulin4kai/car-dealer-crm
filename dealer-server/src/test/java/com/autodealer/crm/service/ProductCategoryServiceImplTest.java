package com.autodealer.crm.service;

import com.autodealer.crm.mapper.ProductCategoryMapper;
import com.autodealer.crm.model.ProductCategory;
import com.autodealer.crm.service.impl.ProductCategoryServiceImpl;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCategoryServiceImplTest {

    @InjectMocks
    private ProductCategoryServiceImpl categoryService;

    @Mock
    private ProductCategoryMapper categoryMapper;

    @Test
    void testGetCategoryList() {
        List<ProductCategory> categories = Arrays.asList(
                createCategory(1L, "Sedan", "SEDAN"),
                createCategory(2L, "SUV", "SUV")
        );
        when(categoryMapper.selectList(anyInt(), anyInt())).thenReturn(categories);

        PageInfo<ProductCategory> result = categoryService.getCategoryList(1, 10);

        assertNotNull(result);
        assertEquals(2, result.getList().size());
        verify(categoryMapper).selectList(0, 10);
    }

    @Test
    void testGetCategoryListEmpty() {
        when(categoryMapper.selectList(anyInt(), anyInt())).thenReturn(Collections.emptyList());

        PageInfo<ProductCategory> result = categoryService.getCategoryList(1, 10);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void testGetCategoryById() {
        ProductCategory category = createCategory(1L, "Sedan", "SEDAN");
        when(categoryMapper.selectById(1L)).thenReturn(category);

        ProductCategory result = categoryService.getCategoryById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Sedan", result.getName());
        assertEquals("SEDAN", result.getCode());
    }

    @Test
    void testGetCategoryByIdNotFound() {
        when(categoryMapper.selectById(999L)).thenReturn(null);

        ProductCategory result = categoryService.getCategoryById(999L);

        assertNull(result);
    }

    @Test
    void testGetCategoryByCode() {
        ProductCategory category = createCategory(1L, "Sedan", "SEDAN");
        when(categoryMapper.selectByCode("SEDAN")).thenReturn(category);

        ProductCategory result = categoryService.getCategoryByCode("SEDAN");

        assertNotNull(result);
        assertEquals("SEDAN", result.getCode());
    }

    @Test
    void testAddCategory() {
        ProductCategory category = new ProductCategory();
        category.setName("New Category");
        category.setCode("NEW");
        when(categoryMapper.insert(any(ProductCategory.class))).thenReturn(1);

        categoryService.addCategory(category);

        assertNotNull(category.getCreateTime());
        assertNotNull(category.getUpdateTime());
        verify(categoryMapper).insert(category);
    }

    @Test
    void testUpdateCategory() {
        ProductCategory category = new ProductCategory();
        category.setId(1L);
        category.setName("Updated Category");
        when(categoryMapper.update(any(ProductCategory.class))).thenReturn(1);

        categoryService.updateCategory(category);

        assertNotNull(category.getUpdateTime());
        verify(categoryMapper).update(category);
    }

    @Test
    void testDeleteCategory() {
        when(categoryMapper.deleteById(1L)).thenReturn(1);

        categoryService.deleteCategory(1L);

        verify(categoryMapper).deleteById(1L);
    }

    private ProductCategory createCategory(Long id, String name, String code) {
        ProductCategory category = new ProductCategory();
        category.setId(id);
        category.setName(name);
        category.setCode(code);
        return category;
    }
}
