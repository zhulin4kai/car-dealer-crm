package com.autodealer.crm.service;

import com.autodealer.crm.dto.ImportContext;
import com.autodealer.crm.dto.ImportResult;
import com.autodealer.crm.dto.ImportRowError;
import com.autodealer.crm.model.TClue;
import com.autodealer.crm.result.ClueExcelRaw;
import com.autodealer.crm.result.DicEnum;
import com.autodealer.crm.util.PhoneNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 线索 Excel 导入校验器。
 *
 * <p>在校验上下文中对所有原始行进行逐行校验和 ID 转换。
 * <p>不依赖全局 cacheMap；字典、产品、负责人和活动 ID 由 ImportContext 提供。
 */
@Component
public class ClueImportValidator {

    /**
     * Excel 公式注入前缀。
     */
    private static final String[] FORMULA_PREFIXES = {"=", "+", "-", "@"};

    /**
     * 最大行数（不含表头）。
     */
    private static final int MAX_ROWS = 1000;

    /**
     * 单字段最大字符长度。
     */
    private static final int MAX_CELL_LENGTH = 500;

    /**
     * 描述内容最大长度。
     */
    private static final int MAX_NOTE_CONTENT_LENGTH = 255;

    /**
     * 年龄最小值。
     */
    private static final int MIN_AGE = 1;

    /**
     * 年龄最大值。
     */
    private static final int MAX_AGE = 150;

    /**
     * 校验并转换所有原始行，返回包含 ImportResult 和有效 TClue 列表的结果。
     *
     * @param rawList    原始 Excel 数据行
     * @param context    导入上下文（字典、产品、负责人和活动名称映射）
     * @param operatorId 当前操作者 ID
     * @return 校验和转换结果
     */
    public ValidatedClueImport validateAndTransform(List<ClueExcelRaw> rawList,
                                                    ImportContext context,
                                                    Integer operatorId) {
        ImportResult result = new ImportResult();
        List<TClue> validClues = new ArrayList<>();
        int totalRows = rawList.size();
        result.setTotalRows(totalRows);

        if (totalRows > MAX_ROWS) {
            result.addError(new ImportRowError(0, "", "导入行数超过最大限制 " + MAX_ROWS));
            result.setFailedRows(totalRows);
            return new ValidatedClueImport(result, validClues);
        }

        if (totalRows == 0) {
            result.addError(new ImportRowError(0, "", "导入文件为空，没有数据行"));
            return new ValidatedClueImport(result, validClues);
        }

        int failedRows = 0;
        Set<String> seenPhones = new HashSet<>();

        for (int i = 0; i < rawList.size(); i++) {
            ClueExcelRaw raw = rawList.get(i);
            int rowNum = i + 1;
            List<ImportRowError> rowErrors = new ArrayList<>();

            validateRow(raw, rowNum, context, seenPhones, rowErrors);

            if (!rowErrors.isEmpty()) {
                failedRows++;
                result.getErrors().addAll(rowErrors);
            } else {
                TClue clue = transformRow(raw, context, operatorId);
                validClues.add(clue);
            }
        }

        int validRows = totalRows - failedRows;
        result.setValidRows(validRows);
        result.setFailedRows(failedRows);

        return new ValidatedClueImport(result, validClues);
    }

    /**
     * 校验单行，将错误追加到 rowErrors。
     */
    private void validateRow(ClueExcelRaw raw, int rowNum,
                             ImportContext context, Set<String> seenPhones,
                             List<ImportRowError> rowErrors) {
        // 手机号校验
        String phone = raw.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            rowErrors.add(new ImportRowError(rowNum, "手机号", "不能为空"));
        } else {
            String trimmed = phone.trim();
            if (isFormula(trimmed)) {
                rowErrors.add(new ImportRowError(rowNum, "手机号", "包含Excel公式前缀"));
            }
            String normalized = PhoneNormalizer.normalizeMainlandMobile(trimmed);
            if (!PhoneNormalizer.isMainlandMobile(normalized)) {
                rowErrors.add(new ImportRowError(rowNum, "手机号", "格式不正确"));
            }
            if (!seenPhones.add(normalized)) {
                rowErrors.add(new ImportRowError(rowNum, "手机号", "同一文件中重复"));
            }
            if (normalized != null && normalized.length() > 18) {
                rowErrors.add(new ImportRowError(rowNum, "手机号", "长度超过限制"));
            }
        }

        // 姓名校验
        String fullName = raw.getFullName();
        if (fullName != null && !fullName.trim().isEmpty()) {
            if (isFormula(fullName.trim())) {
                rowErrors.add(new ImportRowError(rowNum, "姓名", "包含Excel公式前缀"));
            }
            if (fullName.trim().length() > MAX_CELL_LENGTH) {
                rowErrors.add(new ImportRowError(rowNum, "姓名", "字段超长"));
            }
        }

        // 文本字段公式和长度校验
        checkFieldFormulaAndLength(raw.getWeixin(), "微信号", rowNum, rowErrors);
        checkFieldFormulaAndLength(raw.getQq(), "QQ号", rowNum, rowErrors);
        checkFieldFormulaAndLength(raw.getEmail(), "邮箱", rowNum, rowErrors);
        checkFieldFormulaAndLength(raw.getJob(), "职业", rowNum, rowErrors);
        checkFieldFormulaAndLength(raw.getAddress(), "地址", rowNum, rowErrors);
        checkFieldFormulaAndLength(raw.getDescription(), "线索描述", rowNum, rowErrors);

        // 描述长度额外校验
        if (raw.getDescription() != null && raw.getDescription().length() > MAX_NOTE_CONTENT_LENGTH) {
            rowErrors.add(new ImportRowError(rowNum, "线索描述",
                    "内容不能超过" + MAX_NOTE_CONTENT_LENGTH + "个字符"));
        }

        // 年龄校验
        if (raw.getAge() != null) {
            if (raw.getAge() < MIN_AGE || raw.getAge() > MAX_AGE) {
                rowErrors.add(new ImportRowError(rowNum, "年龄",
                        "不在有效范围(" + MIN_AGE + "-" + MAX_AGE + ")"));
            }
        }

        // 负责人校验（必填）
        if (raw.getOwnerName() == null || raw.getOwnerName().trim().isEmpty()) {
            rowErrors.add(new ImportRowError(rowNum, "负责人", "不能为空"));
        } else if (isFormula(raw.getOwnerName().trim())) {
            rowErrors.add(new ImportRowError(rowNum, "负责人", "包含Excel公式前缀"));
        } else if (context.findOwnerId(raw.getOwnerName()) == null) {
            rowErrors.add(new ImportRowError(rowNum, "负责人", "未找到该负责人"));
        }

        // 活动校验（可选）
        checkOptionalRef(raw.getActivityName(), "所属活动", context::findActivityId, rowNum, rowErrors);

        // 字典字段校验（可选）
        checkOptionalDic(raw.getAppellation(), "称呼", DicEnum.APPELLATION.getCode(),
                context, rowNum, rowErrors);
        checkOptionalDic(raw.getState(), "线索状态", DicEnum.STATE.getCode(),
                context, rowNum, rowErrors);
        checkOptionalDic(raw.getSource(), "线索来源", DicEnum.SOURCE.getCode(),
                context, rowNum, rowErrors);
        checkOptionalDic(raw.getNeedLoan(), "是否贷款", DicEnum.NEEDLOAN.getCode(),
                context, rowNum, rowErrors);
        checkOptionalDic(raw.getIntentionState(), "意向状态", DicEnum.INTENTIONSTATE.getCode(),
                context, rowNum, rowErrors);

        // 意向商品校验（可选）
        checkOptionalRef(raw.getIntentionProduct(), "意向产品", context::findProductId, rowNum, rowErrors);
    }

    /**
     * 将校验通过的原始行转换为 TClue 实体。
     */
    private TClue transformRow(ClueExcelRaw raw, ImportContext context, Integer operatorId) {
        TClue clue = new TClue();

        clue.setOwnerId(context.findOwnerId(raw.getOwnerName()));
        clue.setActivityId(safeFindId(raw.getActivityName(), context::findActivityId));
        clue.setActivityNameSnapshot(context.findActivityNameSnapshot(raw.getActivityName()));
        clue.setFullName(trimOrNull(raw.getFullName()));
        clue.setAppellation(safeFindDicId(DicEnum.APPELLATION.getCode(), raw.getAppellation(), context));
        clue.setPhone(PhoneNormalizer.normalizeMainlandMobile(raw.getPhone()));
        clue.setWeixin(trimOrNull(raw.getWeixin()));
        clue.setQq(trimOrNull(raw.getQq()));
        clue.setEmail(trimOrNull(raw.getEmail()));
        clue.setAge(raw.getAge());
        clue.setJob(trimOrNull(raw.getJob()));
        clue.setYearIncome(raw.getYearIncome());
        clue.setAddress(trimOrNull(raw.getAddress()));
        clue.setNeedLoan(safeFindDicId(DicEnum.NEEDLOAN.getCode(), raw.getNeedLoan(), context));
        clue.setIntentionState(safeFindDicId(DicEnum.INTENTIONSTATE.getCode(), raw.getIntentionState(), context));
        clue.setIntentionProduct(safeFindId(raw.getIntentionProduct(), context::findProductId));
        clue.setState(safeFindDicId(DicEnum.STATE.getCode(), raw.getState(), context));
        clue.setSource(safeFindDicId(DicEnum.SOURCE.getCode(), raw.getSource(), context));
        clue.setDescription(trimOrNull(raw.getDescription()));
        clue.setNextContactTime(raw.getNextContactTime());
        clue.setCreateTime(new java.util.Date());
        clue.setCreateBy(operatorId);

        return clue;
    }

    private Integer safeFindDicId(String typeCode, String text, ImportContext context) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return context.findDicValueId(typeCode, text);
    }

    private Integer safeFindId(String text, java.util.function.Function<String, Integer> finder) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return finder.apply(text);
    }

    private String trimOrNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }

    /**
     * 校验可选引用字段（负责人、活动、商品），非空时必须能在上下文中找到。
     */
    private void checkOptionalRef(String value, String fieldName,
                                  java.util.function.Function<String, Integer> finder,
                                  int rowNum, List<ImportRowError> rowErrors) {
        if (value != null && !value.trim().isEmpty()) {
            if (isFormula(value.trim())) {
                rowErrors.add(new ImportRowError(rowNum, fieldName, "包含Excel公式前缀"));
            } else if (finder.apply(value) == null) {
                rowErrors.add(new ImportRowError(rowNum, fieldName, "未找到匹配记录"));
            }
        }
    }

    /**
     * 校验可选字典字段，非空时必须能在字典中找到匹配值。
     */
    private void checkOptionalDic(String value, String fieldName, String typeCode,
                                  ImportContext context, int rowNum,
                                  List<ImportRowError> rowErrors) {
        if (value != null && !value.trim().isEmpty()) {
            if (isFormula(value.trim())) {
                rowErrors.add(new ImportRowError(rowNum, fieldName, "包含Excel公式前缀"));
            } else if (context.findDicValueId(typeCode, value) == null) {
                rowErrors.add(new ImportRowError(rowNum, fieldName, "未找到匹配字典值"));
            }
        }
    }

    private void checkFieldFormulaAndLength(String value, String fieldName,
                                            int rowNum, List<ImportRowError> rowErrors) {
        if (value != null && !value.trim().isEmpty()) {
            if (isFormula(value.trim())) {
                rowErrors.add(new ImportRowError(rowNum, fieldName, "包含Excel公式前缀"));
            }
            if (value.trim().length() > MAX_CELL_LENGTH) {
                rowErrors.add(new ImportRowError(rowNum, fieldName, "字段超长"));
            }
        }
    }

    /**
     * 判断文本是否以 Excel 公式注入前缀开头。
     */
    private boolean isFormula(String text) {
        if (text == null) return false;
        for (String prefix : FORMULA_PREFIXES) {
            if (text.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验和转换结果，包含 ImportResult 和有效的 TClue 列表。
     */
    public static class ValidatedClueImport {
        private final ImportResult result;
        private final List<TClue> clues;

        public ValidatedClueImport(ImportResult result, List<TClue> clues) {
            this.result = result;
            this.clues = clues;
        }

        public ImportResult getResult() {
            return result;
        }

        public List<TClue> getClues() {
            return clues;
        }
    }
}
