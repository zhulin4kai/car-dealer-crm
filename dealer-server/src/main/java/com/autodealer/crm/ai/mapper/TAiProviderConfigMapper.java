package com.autodealer.crm.ai.mapper;

import com.autodealer.crm.ai.model.TAiProviderConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TAiProviderConfigMapper {
    int insert(TAiProviderConfig record);

    List<TAiProviderConfig> selectAll();

    TAiProviderConfig selectByConfigNo(@Param("configNo") String configNo);

    TAiProviderConfig selectByConfigNoForUpdate(@Param("configNo") String configNo);

    TAiProviderConfig selectEnabled();

    int updateBaseFields(TAiProviderConfig record);

    int updateApiKey(@Param("id") Long id,
                     @Param("encryptedApiKey") String encryptedApiKey,
                     @Param("apiKeyNonce") String apiKeyNonce,
                     @Param("maskedApiKey") String maskedApiKey,
                     @Param("editBy") Integer editBy);

    int disableAll(@Param("editBy") Integer editBy);

    int updateEnabled(@Param("id") Long id,
                      @Param("enabled") Boolean enabled,
                      @Param("editBy") Integer editBy);

    int updateTestResult(@Param("id") Long id,
                         @Param("testStatus") String testStatus,
                         @Param("lastTestErrorCode") String lastTestErrorCode,
                         @Param("lastTestMessage") String lastTestMessage,
                         @Param("editBy") Integer editBy);
}
