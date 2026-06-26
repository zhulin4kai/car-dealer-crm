package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;

import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.dto.ClueLifecycleRequest;
import com.autodealer.crm.dto.ImportResult;
import com.autodealer.crm.dto.TransferClueOwnerRequest;
import com.autodealer.crm.model.TClue;
import com.autodealer.crm.query.ClueQuery;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ClueService;
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
    public R cluePage(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        PageInfo<TClue> pageInfo = clueService.getClueByPage(page == null ? 1 : page, size);
        return R.OK(pageInfo);
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_IMPORT + "')")
    @PostMapping(value = "/api/importExcel")
    public ResponseEntity<R<ImportResult>> importExcel(
            @RequestPart("file") MultipartFile file) throws IOException {
        // 文件为空校验
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(R.FAIL("上传文件不能为空"));
        }

        // 文件扩展名校验
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".xlsx")) {
            return ResponseEntity.badRequest().body(R.FAIL("只支持 .xlsx 格式的 Excel 文件"));
        }

        // Content-Type 校验
        String contentType = file.getContentType();
        if (contentType != null && !isAllowedContentType(contentType)) {
            return ResponseEntity.badRequest().body(R.FAIL("文件类型不合法"));
        }

        // 文件大小校验
        if (file.getSize() > maxImportFileSize) {
            return ResponseEntity.badRequest().body(R.FAIL("文件大小超过限制（最大 " + (maxImportFileSize / 1024 / 1024) + "MB）"));
        }

        ImportResult result = clueService.importExcel(file.getInputStream());
        if (result.getFailedRows() > 0 || !result.getErrors().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new R<>(com.autodealer.crm.result.CodeEnum.FAIL.getCode(),
                            "导入存在错误", result));
        }
        return ResponseEntity.ok(R.OK(result));
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
    public R checkPhone(@PathVariable(value = "phone") String phone) {
        Boolean check = clueService.checkPhone(phone);
        return check ? R.OK() : R.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_ADD + "')")
    @PostMapping(value = "/api/clue")
    public R addClue(ClueQuery clueQuery) {
        int save = clueService.saveClue(clueQuery);

        return save >= 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_VIEW + "')")
    @GetMapping(value = "/api/clue/detail/{id}")
    public R loadClue(@PathVariable(value = "id") Integer id) {
        TClue tClue = clueService.getClueById(id);
        return R.OK(tClue);
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_EDIT + "')")
    @PutMapping(value = "/api/clue")
    public R editClue(ClueQuery clueQuery) {
        int update = clueService.updateClue(clueQuery);

        return update >= 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_TRANSFER + "')")
    @PutMapping(value = "/api/clue/{id}/owner")
    public R transferOwner(@PathVariable(value = "id") Integer id,
                           @Valid @RequestBody TransferClueOwnerRequest request) {
        return clueService.transferOwner(id, request) ? R.OK() : R.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_VIEW + "')")
    @GetMapping(value = "/api/clue/{id}/owner-history")
    public R getOwnerHistory(@PathVariable(value = "id") Integer id) {
        return R.OK(clueService.getOwnerHistory(id));
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_CLOSE + "')")
    @PutMapping(value = "/api/clue/{id}/close")
    public R closeClue(@PathVariable(value = "id") Integer id,
                       @Valid @RequestBody ClueLifecycleRequest request) {
        return clueService.closeClue(id, request) ? R.OK() : R.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_RESTORE + "')")
    @PutMapping(value = "/api/clue/{id}/restore")
    public R restoreClue(@PathVariable(value = "id") Integer id,
                         @Valid @RequestBody ClueLifecycleRequest request) {
        return clueService.restoreClue(id, request) ? R.OK() : R.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_DELETE + "')")
    @DeleteMapping(value = "/api/clue/{id}")
    public R delClue(@PathVariable(value = "id") Integer id) {
        int del = clueService.delClueById(id);
        return del >= 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.CLUE_DELETE + "')")
    @PostMapping(value = "/api/clue/batch")
    public R batchDelClue(@RequestBody List<Integer> ids) {
        if (ids.size() > Constants.MAX_BATCH_SIZE) {
            return R.FAIL("单次批量删除最多支持 " + Constants.MAX_BATCH_SIZE + " 条记录");
        }
        int del = clueService.batchDelClueByIds(ids);
        return del >= 1 ? R.OK() : R.FAIL();
    }
}
