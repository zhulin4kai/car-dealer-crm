package com.autodealer.crm.ai.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TAiConversation implements Serializable {
    private Long id;
    private String conversationNo;
    private Integer userId;
    private String title;
    private String status;
    private String entryPoint;
    private String contextObjectType;
    private String contextObjectId;
    private String summaryText;
    private String lastRunNo;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime editTime;
    private Integer editBy;

    private static final long serialVersionUID = 1L;
}
