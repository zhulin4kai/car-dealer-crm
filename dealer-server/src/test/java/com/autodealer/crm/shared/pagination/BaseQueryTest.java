package com.autodealer.crm.shared.pagination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BaseQueryTest {

    @Test
    void builderDefaultsToCorrectPagination() {
        BaseQuery query = BaseQuery.builder().build();
        assertEquals(1, query.getCurrent());
        assertEquals(10, query.getPageSize());
        assertNull(query.getDataScopeUserId());
    }

    @Test
    void builderAllowsOverrideCurrent() {
        BaseQuery query = BaseQuery.builder().current(5).build();
        assertEquals(5, query.getCurrent());
        assertEquals(10, query.getPageSize());
    }

    @Test
    void builderAllowsOverridePageSize() {
        BaseQuery query = BaseQuery.builder().pageSize(20).build();
        assertEquals(1, query.getCurrent());
        assertEquals(20, query.getPageSize());
    }

    @Test
    void builderAllowsPartialOverride() {
        BaseQuery query = BaseQuery.builder().current(3).pageSize(50).build();
        assertEquals(3, query.getCurrent());
        assertEquals(50, query.getPageSize());
    }

    @Test
    void noArgsConstructorHasDefaults() {
        BaseQuery query = new BaseQuery();
        assertEquals(1, query.getCurrent());
        assertEquals(10, query.getPageSize());
    }

    @Test
    void dataScopeUserIdIsNullByDefault() {
        BaseQuery query = BaseQuery.builder().build();
        assertNull(query.getDataScopeUserId());
    }

    @Test
    void setterWorksForCurrent() {
        BaseQuery query = new BaseQuery();
        query.setCurrent(7);
        assertEquals(7, query.getCurrent());
    }

    @Test
    void setterWorksForPageSize() {
        BaseQuery query = new BaseQuery();
        query.setPageSize(30);
        assertEquals(30, query.getPageSize());
    }
}
