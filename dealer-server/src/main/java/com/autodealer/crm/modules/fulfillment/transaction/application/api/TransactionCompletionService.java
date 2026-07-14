package com.autodealer.crm.modules.fulfillment.transaction.application.api;

public interface TransactionCompletionService {
    boolean tryComplete(Integer tranId, Integer operatorId);
}
