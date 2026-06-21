package com.autodealer.crm.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入结果，包含汇总信息和逐行错误详情。
 *
 * <p>全成功策略：任一行有错误时 importedCount 为 0，数据库无写入。
 */
public class ImportResult {

    /**
     * 总解析行数（不含表头）
     */
    private int totalRows;

    /**
     * 有效行数
     */
    private int validRows;

    /**
     * 失败行数
     */
    private int failedRows;

    /**
     * 成功导入行数（全校验通过后才 > 0）
     */
    private int importedCount;

    /**
     * 逐行错误列表，按行号排序
     */
    private List<ImportRowError> errors = new ArrayList<>();

    public ImportResult() {
    }

    public ImportResult(int totalRows, int validRows, int failedRows, int importedCount) {
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.failedRows = failedRows;
        this.importedCount = importedCount;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public void setValidRows(int validRows) {
        this.validRows = validRows;
    }

    public int getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(int failedRows) {
        this.failedRows = failedRows;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }

    public List<ImportRowError> getErrors() {
        return errors;
    }

    public void setErrors(List<ImportRowError> errors) {
        this.errors = errors;
    }

    public void addError(ImportRowError error) {
        this.errors.add(error);
    }
}
