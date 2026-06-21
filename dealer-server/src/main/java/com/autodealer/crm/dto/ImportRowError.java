package com.autodealer.crm.dto;

/**
 * Excel 导入单行错误信息。
 *
 * <p>不包含原始手机号、姓名等敏感数据，仅包含行号、列名和安全错误原因。
 */
public class ImportRowError {

    /**
     * Excel 行号（从 1 开始，不含表头）
     */
    private int row;

    /**
     * 出错的列名
     */
    private String column;

    /**
     * 安全错误原因（不包含用户录入的敏感值）
     */
    private String reason;

    public ImportRowError() {
    }

    public ImportRowError(int row, String column, String reason) {
        this.row = row;
        this.column = column;
        this.reason = reason;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
