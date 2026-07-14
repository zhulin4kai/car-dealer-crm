package com.autodealer.crm.modules.sales.lead.web;

import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.shared.security.PermissionCodes;

import com.autodealer.crm.shared.infrastructure.constants.Constants;
import com.autodealer.crm.modules.sales.lead.application.api.dto.ClueLifecycleRequest;
import com.autodealer.crm.modules.sales.lead.application.api.dto.ImportResult;
import com.autodealer.crm.modules.sales.lead.application.api.dto.TransferClueOwnerRequest;
import com.autodealer.crm.modules.sales.lead.application.api.model.TClue;
import com.autodealer.crm.modules.sales.lead.application.api.query.ClueQuery;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.sales.lead.application.api.ClueService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class ClueController {

    private static final String[] ALLOWED_CONTENT_TYPES = {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/octet-stream"
    };

    @Resource
    private ClueService clueService;

    @Value("${app.import.max-file-size:5242880}")
    private long maxImportFileSize;

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_LIST + "')")
    @GetMapping(value = "/api/clues")
    public Result cluePage(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        PageInfo<TClue> pageInfo = clueService.getClueByPage(page == null ? 1 : page, size);
        return Result.OK(pageInfo);
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_IMPORT + "')")
    @PostMapping(value = "/api/importExcel")
    public ResponseEntity<Result<ImportResult>> importExcel(
            @RequestPart("file") MultipartFile file) throws IOException {
        // 文件为空校验
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.FAIL("上传文件不能为空"));
        }

        // 文件扩展名校验
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".xlsx")) {
            return ResponseEntity.badRequest().body(Result.FAIL("只支持 .xlsx 格式的 Excel 文件"));
        }

        // Content-Type 校验
        String contentType = file.getContentType();
        if (contentType != null && !isAllowedContentType(contentType)) {
            return ResponseEntity.badRequest().body(Result.FAIL("文件类型不合法"));
        }

        // 文件大小校验
        if (file.getSize() > maxImportFileSize) {
            return ResponseEntity.badRequest().body(Result.FAIL("文件大小超过限制（最大 " + (maxImportFileSize / 1024 / 1024) + "MB）"));
        }

        ImportResult result = clueService.importExcel(file.getInputStream());
        if (result.getFailedRows() > 0 || !result.getErrors().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new Result<>(com.autodealer.crm.shared.error.CodeEnum.FAIL.getCode(),
                            "导入存在错误", result));
        }
        return ResponseEntity.ok(Result.OK(result));
    }

    private boolean isAllowedContentType(String contentType) {
        for (String allowed : ALLOWED_CONTENT_TYPES) {
            if (allowed.equals(contentType)) {
                return true;
            }
        }
        return false;
    }

    @GetMapping(value = "/api/clue/{phone}")
    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_ADD + "')")
    public Result checkPhone(@PathVariable(value = "phone") String phone) {
        Boolean check = clueService.checkPhone(phone);
        return check ? Result.OK() : Result.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_ADD + "')")
    @PostMapping(value = "/api/clue")
    public Result addClue(ClueQuery clueQuery) {
        int save = clueService.saveClue(clueQuery);

        return save >= 1 ? Result.OK() : Result.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_VIEW + "')")
    @GetMapping(value = "/api/clue/detail/{id}")
    public Result loadClue(@PathVariable(value = "id") Integer id) {
        TClue tClue = clueService.getClueById(id);
        return Result.OK(tClue);
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_EDIT + "')")
    @PutMapping(value = "/api/clue")
    public Result editClue(ClueQuery clueQuery) {
        int update = clueService.updateClue(clueQuery);

        return update >= 1 ? Result.OK() : Result.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_TRANSFER + "')")
    @PutMapping(value = "/api/clue/{id}/owner")
    public Result transferOwner(@PathVariable(value = "id") Integer id,
                           @Valid @RequestBody TransferClueOwnerRequest request) {
        return clueService.transferOwner(id, request) ? Result.OK() : Result.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_VIEW + "')")
    @GetMapping(value = "/api/clue/{id}/owner-history")
    public Result getOwnerHistory(@PathVariable(value = "id") Integer id) {
        return Result.OK(clueService.getOwnerHistory(id));
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_CLOSE + "')")
    @PutMapping(value = "/api/clue/{id}/close")
    public Result closeClue(@PathVariable(value = "id") Integer id,
                       @Valid @RequestBody ClueLifecycleRequest request) {
        return clueService.closeClue(id, request) ? Result.OK() : Result.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_RESTORE + "')")
    @PutMapping(value = "/api/clue/{id}/restore")
    public Result restoreClue(@PathVariable(value = "id") Integer id,
                         @Valid @RequestBody ClueLifecycleRequest request) {
        return clueService.restoreClue(id, request) ? Result.OK() : Result.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_DELETE + "')")
    @DeleteMapping(value = "/api/clue/{id}")
    public Result delClue(@PathVariable(value = "id") Integer id) {
        int del = clueService.delClueById(id);
        return del >= 1 ? Result.OK() : Result.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_DELETE + "')")
    @PostMapping(value = "/api/clue/batch")
    public Result batchDelClue(@RequestBody List<Integer> ids) {
        if (ids.size() > Constants.MAX_BATCH_SIZE) {
            return Result.FAIL("单次批量删除最多支持 " + Constants.MAX_BATCH_SIZE + " 条记录");
        }
        int del = clueService.batchDelClueByIds(ids);
        return del >= 1 ? Result.OK() : Result.FAIL();
    }
}
