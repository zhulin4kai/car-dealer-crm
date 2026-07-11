package com.autodealer.crm.ai.mapper;

import com.autodealer.crm.ai.model.TAiAssistantPolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TAiAssistantPolicyMapper {
    TAiAssistantPolicy selectSingleton();

    int insert(TAiAssistantPolicy policy);

    int updateIfVersionMatches(@Param("policy") TAiAssistantPolicy policy,
                               @Param("expectedVersion") Integer expectedVersion);
}
