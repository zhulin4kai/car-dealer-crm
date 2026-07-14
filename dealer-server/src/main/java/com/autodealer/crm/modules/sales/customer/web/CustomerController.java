package com.autodealer.crm.modules.sales.customer.web;

import com.autodealer.crm.shared.security.PermissionCodes;

import com.alibaba.excel.EasyExcel;
import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.sales.customer.application.api.dto.ConvertCustomerRequest;
import com.autodealer.crm.modules.sales.customer.application.api.dto.CustomerDetailResponse;
import com.autodealer.crm.modules.sales.customer.application.api.dto.CustomerListResponse;
import com.autodealer.crm.modules.sales.customer.application.api.dto.CustomerMergeResponse;
import com.autodealer.crm.modules.sales.customer.application.api.dto.CustomerOption;
import com.autodealer.crm.modules.sales.customer.application.api.dto.MergeCustomerRequest;
import com.autodealer.crm.modules.sales.customer.application.api.dto.TransferCustomerOwnerRequest;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.sales.customer.application.api.query.CustomerListQuery;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.sales.customer.application.api.result.CustomerExcel;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.sales.customer.application.api.CustomerService;
import com.github.pagehelper.PageInfo;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
public class CustomerController {
    private static final String EXCEL_FILE_NAME_PREFIX = "客户信息数据";
    private static final int EXPORT_ID_LIMIT = 10000;
    private final CustomerService customerService;
    private final CurrentUserProvider currentUserProvider;
    private final OperationAuditRecorder auditRecorder;

    public CustomerController(CustomerService cs, CurrentUserProvider cup, OperationAuditRecorder ar) {
        this.customerService = cs; this.currentUserProvider = cup; this.auditRecorder = ar;
    }

    @GetMapping("/api/customers")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_LIST + "')")
    public Result<PageInfo<CustomerListResponse>> list(CustomerListQuery query,
            @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        return Result.OK(customerService.getCustomerList(query, page, size));
    }

    @GetMapping("/api/customer/options")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_LIST + "')")
    public Result<List<CustomerOption>> options() { return Result.OK(customerService.getCustomerOptions()); }

    @GetMapping("/api/customer/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_VIEW + "')")
    public Result<CustomerDetailResponse> detail(@PathVariable Integer id) {
        CustomerDetailResponse r = customerService.getCustomerById(id);
        if (r == null) throw new BusinessException(CodeEnum.NOT_FOUND, "客户不存在");
        return Result.OK(r);
    }

    @PutMapping("/api/customer/{id}/owner")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_TRANSFER + "')")
    public Result<Void> transferOwner(@PathVariable Integer id,
                                 @Valid @RequestBody TransferCustomerOwnerRequest request) {
        customerService.transferOwner(id, request);
        return Result.OK();
    }

    @PostMapping("/api/customer/{id}/merge")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_MERGE + "')")
    public Result<CustomerMergeResponse> mergeCustomer(@PathVariable Integer id,
                                                  @Valid @RequestBody MergeCustomerRequest request) {
        return Result.OK(customerService.mergeCustomer(id, request));
    }

    @DeleteMapping("/api/customer/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_DELETE + "')")
    public Result<Void> deleteCustomer(@PathVariable Integer id) {
        boolean deleted = customerService.deleteCustomer(id);
        if (!deleted) throw new BusinessException(CodeEnum.NOT_FOUND, "客户不存在");
        return Result.OK();
    }

    @PostMapping("/api/clue/customer")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_TRANSFER + "')")
    public Result<Void> convertCustomer(@Valid @RequestBody ConvertCustomerRequest request) {
        customerService.convertCustomer(request); return Result.OK();
    }

    @GetMapping("/api/exportExcel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_EXPORT + "')")
    public void exportExcel(HttpServletResponse response,
            @RequestParam(value = "ids", required = false) String ids) throws IOException {
        List<String> idList = parseAndValidateIdList(ids);
        List<CustomerExcel> dataList = customerService.getCustomerByExcel(idList);
        setExcelResponseHeaders(response);
        EasyExcel.write(response.getOutputStream(), CustomerExcel.class).sheet().doWrite(dataList);
        auditRecorder.recordQuietly(AuditActionEnum.EXPORT_ALL_CUSTOMER, "export", "SUCCESS",
                "{\"count\":" + dataList.size() + ",\"operatorId\":" + currentUserProvider.getCurrentUserId() + "}");
    }

    private List<String> parseAndValidateIdList(String ids) {
        if (!StringUtils.hasText(ids)) return Collections.emptyList();
        String[] parts = ids.split(",");
        List<String> idList = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (String p : parts) {
            String t = p.trim(); if (t.isEmpty()) continue;
            if (!t.matches("\\d+")) throw new BusinessException(CodeEnum.PARAM_ERROR, "导出ID格式有误: " + t);
            if (seen.add(t)) idList.add(t);
        }
        if (idList.size() > EXPORT_ID_LIMIT)
            throw new BusinessException(CodeEnum.FAIL, "单次导出最多支持 " + EXPORT_ID_LIMIT + " 条记录");
        return idList;
    }

    private void setExcelResponseHeaders(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fn = URLEncoder.encode(EXCEL_FILE_NAME_PREFIX + System.currentTimeMillis(),
                StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + fn + ".xlsx\"; filename*=UTF-8''" + fn + ".xlsx");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    }
}
