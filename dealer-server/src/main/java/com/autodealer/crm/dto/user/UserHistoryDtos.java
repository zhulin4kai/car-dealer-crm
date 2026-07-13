package com.autodealer.crm.dto.user;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.*;

public final class UserHistoryDtos {
    private UserHistoryDtos() {}

    @Data public static class Query {
        private Integer page = 1;
        private Integer size = 10;
        private String actionCode;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private OffsetDateTime startTime;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private OffsetDateTime endTime;
    }
    public record ActionOption(String code,String label) {}
    public record OperatorSummary(Integer id,String name,String employeeNo) {}
    public record ValueField(String code,String label,String valueCode,String valueName,String displayValue) {}
    public record TargetSummary(String typeCode,String typeName,Integer id,String code,String name) {}
    public record BatchSummary(String batchId,int totalCount,int successCount,int failureCount,
                               String targetResultCode,String targetResultName) {}
    public record Item(String eventId,String sourceKey,String actionCode,String actionName,
                       String categoryCode,String categoryName,TargetSummary target,
                       OperatorSummary operator,List<ValueField> beforeValues,List<ValueField> afterValues,
                       String reason,OffsetDateTime effectiveFrom,OffsetDateTime effectiveTo,
                       String resultCode,String resultName,BatchSummary batchSummary,
                       OffsetDateTime occurredAt) {}
    @Data public static class Collection {
        private int pageNum;private int pageSize;private int size;private long total;private int pages;
        private List<Item> list=new ArrayList<>();
        private List<ActionOption> actionOptions=new ArrayList<>();
        private List<String> allowedActions=new ArrayList<>();
        private Map<String,String> unavailableReasons=new LinkedHashMap<>();
    }
}
