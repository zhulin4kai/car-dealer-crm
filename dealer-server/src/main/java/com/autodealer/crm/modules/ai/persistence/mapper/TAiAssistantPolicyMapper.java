package com.autodealer.crm.modules.ai.persistence.mapper;

import com.autodealer.crm.modules.ai.persistence.model.TAiAssistantPolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TAiAssistantPolicyMapper {
    TAiAssistantPolicy selectSingleton();

    int insert(TAiAssistantPolicy policy);

    int updateIfVersionMatches(@Param("policy") TAiAssistantPolicy policy,
                               @Param("expectedVersion") Integer expectedVersion);
}
