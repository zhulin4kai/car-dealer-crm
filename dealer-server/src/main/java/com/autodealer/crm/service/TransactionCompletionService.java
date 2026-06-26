package com.autodealer.crm.service;

public interface TransactionCompletionService {
    boolean tryComplete(Integer tranId, Integer operatorId);
}
