package com.bjpowernode.web;

import com.alibaba.excel.EasyExcel;
import com.bjpowernode.constant.Constants;
import com.bjpowernode.model.CustomerOption;
import com.bjpowernode.model.TCustomer;
import com.bjpowernode.query.CustomerQuery;
import com.bjpowernode.result.CustomerExcel;
import com.bjpowernode.result.R;
import com.bjpowernode.service.CustomerService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 客户管理控制器
 */
@RestController
public class CustomerController {

    @Resource
    private CustomerService customerService;

    /**
     * 获取客户列表
     */
    @GetMapping("/api/customer/list")
    public R<PageInfo<TCustomer>> list(
            CustomerQuery query,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.OK(customerService.getCustomerList(query, page, size));
    }

    /**
     * 获取客户选项（用于下拉选择）
     */
    @GetMapping("/api/customer/options")
    public R<List<CustomerOption>> options() {
        return R.OK(customerService.getCustomerOptions());
    }

    /**
     * 获取客户详情
     */
    @GetMapping("/api/customer/{id}")
    public R<TCustomer> detail(@PathVariable Integer id) {
        return R.OK(customerService.getCustomerById(id));
    }

    @PostMapping(value = "/api/clue/customer")
    @PreAuthorize("hasAuthority('customer:transfer')")
    public R convertCustomer(@RequestBody CustomerQuery customerQuery) {
        Boolean convert = customerService.convertCustomer(customerQuery);
        return convert ? R.OK() : R.FAIL();
    }
    

    @GetMapping(value = "/api/customers")
    public R cluePage(@RequestParam(value = "current", required = false) Integer current) {
        if (current == null) {
            current = 1;
        }

        PageInfo<TCustomer> pageInfo = customerService.getCustomerByPage(current);
        return R.OK(pageInfo);
    }


    /**
     * 导出Excel
     *
     * @param response
     * @throws IOException
     */
    @GetMapping(value = "/api/exportExcel")
    @PreAuthorize("hasAuthority('customer:export')")
    public void exportExcel(HttpServletResponse response, @RequestParam(value = "ids", required = false) String ids) throws IOException {

        //要想让浏览器弹出下载框，你后端要设置一下响应头信息
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(Constants.EXCEL_FILE_NAME+System.currentTimeMillis(), StandardCharsets.UTF_8) + ".xlsx");

        //2、后端查询数据库的数据，把数据写入Excel，然后把Excel以IO流的方式输出到前端浏览器（我们来实现）

        List<String> idList = StringUtils.hasText(ids) ? Arrays.asList(ids.split(",")) : new ArrayList<>();
        
        // 限制单次导出的最大数量
        if (idList.isEmpty()) {
            // 如果没有指定ID，限制最多导出10000条
            idList = null; // 传null给Service层，让它处理
        } else if (idList.size() > 10000) {
            throw new RuntimeException("单次导出最多支持10000条记录");
        }
        
        List<CustomerExcel> dataList = customerService.getCustomerByExcel(idList);

        EasyExcel.write(response.getOutputStream(), CustomerExcel.class)
                .sheet()
                .doWrite(dataList);
    }
}
