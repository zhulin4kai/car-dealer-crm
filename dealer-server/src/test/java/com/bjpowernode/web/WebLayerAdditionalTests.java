package com.bjpowernode.web;

import com.bjpowernode.dto.SystemMonitorDTO;
import com.bjpowernode.model.*;
import com.bjpowernode.query.*;
import com.bjpowernode.result.NameValue;
import com.bjpowernode.result.R;
import com.bjpowernode.result.SummaryData;
import com.bjpowernode.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class WebLayerAdditionalTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityService activityService;

    @MockBean
    private ActivityRemarkService activityRemarkService;

    @MockBean
    private ClueService clueService;

    @MockBean
    private ClueRemarkService clueRemarkService;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private TranService tranService;

    @MockBean
    private DicService dicService;

    @MockBean
    private StatisticService statisticService;

    @MockBean
    private SystemMonitorService systemMonitorService;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductCategoryService categoryService;

    @MockBean
    private ProductPromotionService promotionService;

    @MockBean
    private ProductStockRecordService stockRecordService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== ActivityController ====================

    @Test
    void activityPage_withCurrentParam() throws Exception {
        TActivity activity = new TActivity();
        activity.setId(1);
        activity.setName("春节促销");
        PageInfo<TActivity> pageInfo = new PageInfo<>(Collections.singletonList(activity));

        when(activityService.getActivityByPage(anyInt(), any(ActivityQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/activitys")
                        .param("current", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].name").value("春节促销"));
    }

    @Test
    void activityPage_withoutCurrentParam() throws Exception {
        PageInfo<TActivity> pageInfo = new PageInfo<>(Collections.emptyList());
        when(activityService.getActivityByPage(anyInt(), any(ActivityQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/activitys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addActivity_success() throws Exception {
        when(activityService.saveActivity(any(ActivityQuery.class))).thenReturn(1);

        mockMvc.perform(post("/api/activity")
                        .header("Authorization", "Bearer test-token")
                        .param("name", "新活动"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addActivity_failure() throws Exception {
        when(activityService.saveActivity(any(ActivityQuery.class))).thenReturn(0);

        mockMvc.perform(post("/api/activity")
                        .header("Authorization", "Bearer test-token")
                        .param("name", "新活动"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void loadActivity_found() throws Exception {
        TActivity activity = new TActivity();
        activity.setId(1);
        activity.setName("春节促销");

        when(activityService.getActivityById(1)).thenReturn(activity);

        mockMvc.perform(get("/api/activity/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("春节促销"));
    }

    @Test
    void editActivity_success() throws Exception {
        when(activityService.updateActivity(any(ActivityQuery.class))).thenReturn(1);

        mockMvc.perform(put("/api/activity")
                        .header("Authorization", "Bearer test-token")
                        .param("id", "1")
                        .param("name", "修改后的活动"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void editActivity_failure() throws Exception {
        when(activityService.updateActivity(any(ActivityQuery.class))).thenReturn(0);

        mockMvc.perform(put("/api/activity")
                        .header("Authorization", "Bearer test-token")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void batchDeleteActivities_success() throws Exception {
        when(activityService.batchDeleteActivities(anyList())).thenReturn(3);

        mockMvc.perform(post("/api/activity/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2, 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void batchDeleteActivities_failure() throws Exception {
        when(activityService.batchDeleteActivities(anyList())).thenReturn(0);

        mockMvc.perform(post("/api/activity/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void deleteActivity_success() throws Exception {
        when(activityService.deleteActivity(1)).thenReturn(1);

        mockMvc.perform(delete("/api/activity/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteActivity_failure() throws Exception {
        when(activityService.deleteActivity(1)).thenReturn(0);

        mockMvc.perform(delete("/api/activity/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== ActivityRemarkController ====================

    @Test
    void addActivityRemark_success() throws Exception {
        when(activityRemarkService.saveActivityRemark(any(ActivityRemarkQuery.class))).thenReturn(1);

        mockMvc.perform(post("/api/activity/remark")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityId\":1,\"noteContent\":\"备注\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addActivityRemark_failure() throws Exception {
        when(activityRemarkService.saveActivityRemark(any(ActivityRemarkQuery.class))).thenReturn(0);

        mockMvc.perform(post("/api/activity/remark")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityId\":1,\"noteContent\":\"备注\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void activityRemarkPage_withCurrent() throws Exception {
        TActivityRemark remark = new TActivityRemark();
        remark.setId(1);
        remark.setNoteContent("备注内容");
        PageInfo<TActivityRemark> pageInfo = new PageInfo<>(Collections.singletonList(remark));

        when(activityRemarkService.getActivityRemarkByPage(eq(1), any(ActivityRemarkQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/activity/remark")
                        .param("current", "1")
                        .param("activityId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void activityRemarkPage_withoutCurrent() throws Exception {
        PageInfo<TActivityRemark> pageInfo = new PageInfo<>(Collections.emptyList());
        when(activityRemarkService.getActivityRemarkByPage(eq(1), any(ActivityRemarkQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/activity/remark")
                        .param("activityId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getActivityRemarkById() throws Exception {
        TActivityRemark remark = new TActivityRemark();
        remark.setId(1);
        remark.setNoteContent("备注");

        when(activityRemarkService.getActivityRemarkById(1)).thenReturn(remark);

        mockMvc.perform(get("/api/activity/remark/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.noteContent").value("备注"));
    }

    @Test
    void editActivityRemark_success() throws Exception {
        when(activityRemarkService.updateActivityRemark(any(ActivityRemarkQuery.class))).thenReturn(1);

        mockMvc.perform(put("/api/activity/remark")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"noteContent\":\"修改后的备注\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void editActivityRemark_failure() throws Exception {
        when(activityRemarkService.updateActivityRemark(any(ActivityRemarkQuery.class))).thenReturn(0);

        mockMvc.perform(put("/api/activity/remark")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"noteContent\":\"备注\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void delActivityRemark_success() throws Exception {
        when(activityRemarkService.delActivityRemarkById(1)).thenReturn(1);

        mockMvc.perform(delete("/api/activity/remark/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void delActivityRemark_failure() throws Exception {
        when(activityRemarkService.delActivityRemarkById(1)).thenReturn(0);

        mockMvc.perform(delete("/api/activity/remark/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== ClueController ====================

    @Test
    void checkPhone_exists() throws Exception {
        when(clueService.checkPhone("13800138000")).thenReturn(true);

        mockMvc.perform(get("/api/clue/13800138000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void checkPhone_notExists() throws Exception {
        when(clueService.checkPhone("13900139000")).thenReturn(false);

        mockMvc.perform(get("/api/clue/13900139000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @WithMockUser(authorities = {"clue:add"})
    void addClue_success() throws Exception {
        when(clueService.saveClue(any(ClueQuery.class))).thenReturn(1);

        mockMvc.perform(post("/api/clue")
                        .header("Authorization", "Bearer test-token")
                        .param("fullName", "张三")
                        .param("phone", "13800138000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"clue:add"})
    void addClue_failure() throws Exception {
        when(clueService.saveClue(any(ClueQuery.class))).thenReturn(0);

        mockMvc.perform(post("/api/clue")
                        .header("Authorization", "Bearer test-token")
                        .param("fullName", "张三"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @WithMockUser(authorities = {"clue:view"})
    void loadClue_found() throws Exception {
        TClue clue = new TClue();
        clue.setId(1);
        clue.setFullName("张三");

        when(clueService.getClueById(1)).thenReturn(clue);

        mockMvc.perform(get("/api/clue/detail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fullName").value("张三"));
    }

    @Test
    @WithMockUser(authorities = {"clue:edit"})
    void editClue_success() throws Exception {
        when(clueService.updateClue(any(ClueQuery.class))).thenReturn(1);

        mockMvc.perform(put("/api/clue")
                        .header("Authorization", "Bearer test-token")
                        .param("id", "1")
                        .param("fullName", "修改后的线索"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"clue:edit"})
    void editClue_failure() throws Exception {
        when(clueService.updateClue(any(ClueQuery.class))).thenReturn(0);

        mockMvc.perform(put("/api/clue")
                        .header("Authorization", "Bearer test-token")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @WithMockUser(authorities = {"clue:delete"})
    void delClue_success() throws Exception {
        when(clueService.delClueById(1)).thenReturn(1);

        mockMvc.perform(delete("/api/clue/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"clue:delete"})
    void delClue_failure() throws Exception {
        when(clueService.delClueById(1)).thenReturn(0);

        mockMvc.perform(delete("/api/clue/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @WithMockUser(authorities = {"clue:delete"})
    void batchDelClue_success() throws Exception {
        when(clueService.batchDelClueByIds(anyList())).thenReturn(2);

        mockMvc.perform(post("/api/clue/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"clue:delete"})
    void batchDelClue_failure() throws Exception {
        when(clueService.batchDelClueByIds(anyList())).thenReturn(0);

        mockMvc.perform(post("/api/clue/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== ClueRemarkController ====================

    @Test
    void addClueRemark_success() throws Exception {
        when(clueRemarkService.saveClueRemark(any(ClueRemarkQuery.class))).thenReturn(1);

        mockMvc.perform(post("/api/clue/remark")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clueId\":1,\"noteContent\":\"跟踪内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addClueRemark_failure() throws Exception {
        when(clueRemarkService.saveClueRemark(any(ClueRemarkQuery.class))).thenReturn(0);

        mockMvc.perform(post("/api/clue/remark")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clueId\":1,\"noteContent\":\"跟踪内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void clueRemarkPage_withCurrent() throws Exception {
        TClueRemark remark = new TClueRemark();
        remark.setId(1);
        remark.setNoteContent("跟踪内容");
        PageInfo<TClueRemark> pageInfo = new PageInfo<>(Collections.singletonList(remark));

        when(clueRemarkService.getClueRemarkByPage(eq(1), any(ClueRemarkQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/clue/remark")
                        .param("current", "1")
                        .param("clueId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void clueRemarkPage_withoutCurrent() throws Exception {
        PageInfo<TClueRemark> pageInfo = new PageInfo<>(Collections.emptyList());
        when(clueRemarkService.getClueRemarkByPage(eq(1), any(ClueRemarkQuery.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/clue/remark")
                        .param("clueId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== CustomerController ====================

    @Test
    void customerList() throws Exception {
        TCustomer customer = new TCustomer();
        customer.setId(1);
        PageInfo<TCustomer> pageInfo = new PageInfo<>(Collections.singletonList(customer));

        when(customerService.getCustomerList(any(CustomerQuery.class), eq(1), eq(10))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/customer/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void customerOptions() throws Exception {
        CustomerOption option = new CustomerOption();
        option.setCustomerId(1);
        option.setCustomerName("张三");

        when(customerService.getCustomerOptions()).thenReturn(Collections.singletonList(option));

        mockMvc.perform(get("/api/customer/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].customerName").value("张三"));
    }

    @Test
    void customerDetail() throws Exception {
        TCustomer customer = new TCustomer();
        customer.setId(1);
        customer.setDescription("测试客户");

        when(customerService.getCustomerById(1)).thenReturn(customer);

        mockMvc.perform(get("/api/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.description").value("测试客户"));
    }

    @Test
    void convertCustomer_success() throws Exception {
        when(customerService.convertCustomer(any(CustomerQuery.class))).thenReturn(true);

        mockMvc.perform(post("/api/clue/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clueId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void convertCustomer_failure() throws Exception {
        when(customerService.convertCustomer(any(CustomerQuery.class))).thenReturn(false);

        mockMvc.perform(post("/api/clue/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clueId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void customerPage_withCurrent() throws Exception {
        TCustomer customer = new TCustomer();
        customer.setId(1);
        PageInfo<TCustomer> pageInfo = new PageInfo<>(Collections.singletonList(customer));

        when(customerService.getCustomerByPage(2)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/customers")
                        .param("current", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void customerPage_withoutCurrent() throws Exception {
        PageInfo<TCustomer> pageInfo = new PageInfo<>(Collections.emptyList());
        when(customerService.getCustomerByPage(1)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== StatisticController ====================

    @Test
    void summaryData() throws Exception {
        SummaryData data = new SummaryData();
        data.setTotalClueCount(100);
        data.setTotalCustomerCount(50);

        when(statisticService.loadSummaryData()).thenReturn(data);

        mockMvc.perform(get("/api/summary/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalClueCount").value(100));
    }

    @Test
    void saleFunnelData() throws Exception {
        NameValue nv = new NameValue();
        nv.setName("成交");
        nv.setValue(20);

        when(statisticService.loadSaleFunnelData()).thenReturn(Collections.singletonList(nv));

        mockMvc.perform(get("/api/saleFunnel/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("成交"));
    }

    @Test
    void sourcePieData() throws Exception {
        NameValue nv = new NameValue();
        nv.setName("网络");
        nv.setValue(50);

        when(statisticService.loadSourcePieData()).thenReturn(Collections.singletonList(nv));

        mockMvc.perform(get("/api/sourcePie/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("网络"));
    }

    // ==================== SystemMonitorController ====================

    @Test
    void getSystemInfo() throws Exception {
        when(systemMonitorService.getSystemInfo()).thenReturn(new SystemMonitorDTO.SystemInfo());

        mockMvc.perform(get("/api/monitor/system-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getMemoryInfo() throws Exception {
        when(systemMonitorService.getMemoryInfo()).thenReturn(new SystemMonitorDTO.MemoryInfo());

        mockMvc.perform(get("/api/monitor/memory-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getCpuInfo() throws Exception {
        when(systemMonitorService.getCpuInfo()).thenReturn(new SystemMonitorDTO.CpuInfo());

        mockMvc.perform(get("/api/monitor/cpu-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getDiskInfo() throws Exception {
        when(systemMonitorService.getDiskInfo()).thenReturn(new SystemMonitorDTO.DiskInfo());

        mockMvc.perform(get("/api/monitor/disk-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getNetworkInfo() throws Exception {
        when(systemMonitorService.getNetworkInfo()).thenReturn(new SystemMonitorDTO.NetworkInfo());

        mockMvc.perform(get("/api/monitor/network-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getJvmInfo() throws Exception {
        when(systemMonitorService.getJvmInfo()).thenReturn(new SystemMonitorDTO.JvmInfo());

        mockMvc.perform(get("/api/monitor/jvm-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getAllMonitorData() throws Exception {
        when(systemMonitorService.getAllMonitorData()).thenReturn(new SystemMonitorDTO.AllMonitorData());

        mockMvc.perform(get("/api/monitor/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== ProductController ====================

    @Test
    void getProductList() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("比亚迪e2");
        PageInfo<Product> pageInfo = new PageInfo<>(Collections.singletonList(product));

        when(productService.getProductList(1, 10)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getProductById() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("比亚迪e2");

        when(productService.getProductById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("比亚迪e2"));
    }

    @Test
    void addProduct() throws Exception {
        doNothing().when(productService).addProduct(any(Product.class));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新车型\",\"price\":100000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateProduct() throws Exception {
        doNothing().when(productService).updateProduct(any(Product.class));

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"更新车型\",\"price\":120000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteProduct() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getStockAlerts() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("低库存产品");
        PageInfo<Product> pageInfo = new PageInfo<>(Collections.singletonList(product));

        when(productService.getStockAlerts(1, 10, null, null, null)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/products/stockalerts")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void restock() throws Exception {
        doNothing().when(productService).restock(anyLong(), anyInt(), anyString());

        mockMvc.perform(post("/api/products/stock/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":50,\"remark\":\"补货\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== ProductCategoryController ====================

    @Test
    void getCategoryList() throws Exception {
        ProductCategory cat = new ProductCategory();
        cat.setId(1L);
        cat.setName("新能源");
        PageInfo<ProductCategory> pageInfo = new PageInfo<>(Collections.singletonList(cat));

        when(categoryService.getCategoryList(1, 10)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/product-categories")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getCategoryById() throws Exception {
        ProductCategory cat = new ProductCategory();
        cat.setId(1L);
        cat.setName("新能源");

        when(categoryService.getCategoryById(1L)).thenReturn(cat);

        mockMvc.perform(get("/api/product-categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addCategory() throws Exception {
        doNothing().when(categoryService).addCategory(any(ProductCategory.class));

        mockMvc.perform(post("/api/product-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新分类\",\"code\":\"NC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateCategory() throws Exception {
        doNothing().when(categoryService).updateCategory(any(ProductCategory.class));

        mockMvc.perform(put("/api/product-categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"更新分类\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteCategory() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/product-categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== ProductPromotionController ====================

    @Test
    void getPromotionList() throws Exception {
        ProductPromotion promo = new ProductPromotion();
        promo.setId(1L);
        promo.setName("夏季促销");
        PageInfo<ProductPromotion> pageInfo = new PageInfo<>(Collections.singletonList(promo));

        when(promotionService.getPromotionList(1, 10)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/product-promotions")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getPromotionById() throws Exception {
        ProductPromotion promo = new ProductPromotion();
        promo.setId(1L);
        promo.setName("夏季促销");

        when(promotionService.getPromotionById(1L)).thenReturn(promo);

        mockMvc.perform(get("/api/product-promotions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addPromotion() throws Exception {
        doNothing().when(promotionService).addPromotion(any(ProductPromotion.class));

        mockMvc.perform(post("/api/product-promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新促销\",\"type\":\"discount\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updatePromotion() throws Exception {
        doNothing().when(promotionService).updatePromotion(any(ProductPromotion.class));

        mockMvc.perform(put("/api/product-promotions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"更新促销\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deletePromotion() throws Exception {
        doNothing().when(promotionService).deletePromotion(1L);

        mockMvc.perform(delete("/api/product-promotions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== ProductStockController ====================

    @Test
    void stockRestock() throws Exception {
        doNothing().when(productService).restock(anyLong(), anyInt(), anyString());

        mockMvc.perform(post("/api/productstock/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":100,\"remark\":\"补货\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getStockRecords() throws Exception {
        ProductStockRecord record = new ProductStockRecord();
        record.setId(1L);
        record.setProductId(10L);
        record.setQuantity(50);
        PageInfo<ProductStockRecord> pageInfo = new PageInfo<>(Collections.singletonList(record));

        when(stockRecordService.getStockRecordsByProductId(10L, 1, 10)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/productstock/records/10")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== TranController ====================

    @Test
    void tranList() throws Exception {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setTranNo("20240101000001");
        PageInfo<TTran> pageInfo = new PageInfo<>(Collections.singletonList(tran));

        when(tranService.getTransactionList(any(TranQuery.class), eq(1), eq(10))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/tran/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void tranDetail() throws Exception {
        TTran tran = new TTran();
        tran.setId(1);
        tran.setTranNo("20240101000001");

        when(tranService.getTransactionById(1)).thenReturn(tran);

        mockMvc.perform(get("/api/tran/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tranNo").value("20240101000001"));
    }

    @Test
    void tranDelete_success() throws Exception {
        when(tranService.deleteTransaction(1)).thenReturn(true);

        mockMvc.perform(delete("/api/tran/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void tranDelete_failure() throws Exception {
        when(tranService.deleteTransaction(1)).thenReturn(false);

        mockMvc.perform(delete("/api/tran/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void tranBatchDelete_success() throws Exception {
        when(tranService.batchDeleteTransactions(anyList())).thenReturn(true);

        Map<String, List<Integer>> body = new HashMap<>();
        body.put("ids", Arrays.asList(1, 2, 3));

        mockMvc.perform(post("/api/tran/batch-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void tranBatchDelete_emptyIds() throws Exception {
        Map<String, List<Integer>> body = new HashMap<>();
        body.put("ids", Collections.emptyList());

        mockMvc.perform(post("/api/tran/batch-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void tranBatchDelete_nullIds() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/tran/batch-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void getTranRemarks() throws Exception {
        TTranRemark remark = new TTranRemark();
        remark.setId(1);
        remark.setNoteContent("备注");

        when(tranService.getTransactionRemarks(1)).thenReturn(Collections.singletonList(remark));

        mockMvc.perform(get("/api/tran/remarks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].noteContent").value("备注"));
    }

    @Test
    void getTransactionProducts() throws Exception {
        TTranProduct tp = new TTranProduct();
        tp.setId(1);
        tp.setProductName("比亚迪e2");

        when(tranService.getTransactionProductDetails(1)).thenReturn(Collections.singletonList(tp));

        mockMvc.perform(get("/api/tran/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getApproveInfo() throws Exception {
        TTranApprove approve = new TTranApprove();
        approve.setId(1);
        approve.setApproveResult(true);

        when(tranService.getTranApprove(1)).thenReturn(approve);

        mockMvc.perform(get("/api/tran/approve/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getInvoiceList() throws Exception {
        TTranInvoice invoice = new TTranInvoice();
        invoice.setId(1);
        invoice.setInvoiceNo("INV001");

        when(tranService.getTranInvoices(1)).thenReturn(Collections.singletonList(invoice));

        mockMvc.perform(get("/api/tran/invoice/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== DicController ====================

    @Test
    void getDicTypes_withPageParams() throws Exception {
        mockMvc.perform(get("/api/dict/types")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getDicTypes_withoutPageParams() throws Exception {
        mockMvc.perform(get("/api/dict/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getDicTypeById() throws Exception {
        mockMvc.perform(get("/api/dict/type/get/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addDicType_success() throws Exception {
        when(dicService.addDicType(any(TDicType.class))).thenReturn(true);

        mockMvc.perform(post("/api/dict/type/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeCode\":\"test\",\"typeName\":\"测试\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addDicType_failure() throws Exception {
        when(dicService.addDicType(any(TDicType.class))).thenReturn(false);

        mockMvc.perform(post("/api/dict/type/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeCode\":\"test\",\"typeName\":\"测试\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void updateDicType_success() throws Exception {
        when(dicService.updateDicType(eq(1), any(TDicType.class))).thenReturn(true);

        mockMvc.perform(put("/api/dict/type/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeName\":\"更新\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateDicType_failure() throws Exception {
        when(dicService.updateDicType(eq(1), any(TDicType.class))).thenReturn(false);

        mockMvc.perform(put("/api/dict/type/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeName\":\"更新\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void deleteDicType_success() throws Exception {
        when(dicService.deleteDicType(1)).thenReturn(true);

        mockMvc.perform(delete("/api/dict/type/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteDicType_failure() throws Exception {
        when(dicService.deleteDicType(1)).thenReturn(false);

        mockMvc.perform(delete("/api/dict/type/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void batchDeleteDicTypes_success() throws Exception {
        when(dicService.deleteDicTypesByIds(anyList())).thenReturn(true);

        mockMvc.perform(delete("/api/dict/types/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void batchDeleteDicTypes_failure() throws Exception {
        when(dicService.deleteDicTypesByIds(anyList())).thenReturn(false);

        mockMvc.perform(delete("/api/dict/types/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void getDicValues_withPageParams() throws Exception {
        mockMvc.perform(get("/api/dict/values")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getDicValues_withoutPageParams() throws Exception {
        mockMvc.perform(get("/api/dict/values"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getDicValueById() throws Exception {
        mockMvc.perform(get("/api/dict/value/get/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addDicValue_success() throws Exception {
        when(dicService.addDicValue(any(TDicValue.class))).thenReturn(true);

        mockMvc.perform(post("/api/dict/value/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeCode\":\"appellation\",\"typeValue\":\"先生\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addDicValue_failure() throws Exception {
        when(dicService.addDicValue(any(TDicValue.class))).thenReturn(false);

        mockMvc.perform(post("/api/dict/value/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeCode\":\"appellation\",\"typeValue\":\"先生\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void updateDicValue_success() throws Exception {
        when(dicService.updateDicValue(eq(1), any(TDicValue.class))).thenReturn(true);

        mockMvc.perform(put("/api/dict/value/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeValue\":\"更新值\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateDicValue_failure() throws Exception {
        when(dicService.updateDicValue(eq(1), any(TDicValue.class))).thenReturn(false);

        mockMvc.perform(put("/api/dict/value/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeValue\":\"更新值\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void deleteDicValue_success() throws Exception {
        when(dicService.deleteDicValue(1)).thenReturn(true);

        mockMvc.perform(delete("/api/dict/value/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteDicValue_failure() throws Exception {
        when(dicService.deleteDicValue(1)).thenReturn(false);

        mockMvc.perform(delete("/api/dict/value/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void batchDeleteDicValues_success() throws Exception {
        when(dicService.deleteDicValuesByIds(anyList())).thenReturn(true);

        mockMvc.perform(delete("/api/dict/value/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void batchDeleteDicValues_failure() throws Exception {
        when(dicService.deleteDicValuesByIds(anyList())).thenReturn(false);

        mockMvc.perform(delete("/api/dict/value/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void refreshDictData_type() throws Exception {
        mockMvc.perform(get("/api/dict/refresh")
                        .param("type", "type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void refreshDictData_value() throws Exception {
        mockMvc.perform(get("/api/dict/refresh")
                        .param("type", "value"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void refreshDictData_all() throws Exception {
        mockMvc.perform(get("/api/dict/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
