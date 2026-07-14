package com.autodealer.crm.modules.sales.followup.persistence.mapper;

import com.autodealer.crm.modules.sales.followup.application.api.model.TCommunicationRecord;
import com.autodealer.crm.modules.sales.followup.application.api.query.CommunicationRecordQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TCommunicationRecordMapper {
    int insert(TCommunicationRecord record);

    TCommunicationRecord selectById(@Param("id") Long id);

    TCommunicationRecord selectByIdForUpdate(@Param("id") Long id);

    List<TCommunicationRecord> selectByQuery(@Param("query") CommunicationRecordQuery query);

    int markCorrected(@Param("id") Long id,
                      @Param("expectedStatus") String expectedStatus,
                      @Param("correctionReason") String correctionReason,
                      @Param("updateTime") LocalDateTime updateTime,
                      @Param("updateBy") Integer updateBy);

    int voidIfActive(@Param("id") Long id,
                     @Param("expectedStatus") String expectedStatus,
                     @Param("voidReason") String voidReason,
                     @Param("updateTime") LocalDateTime updateTime,
                     @Param("updateBy") Integer updateBy);
}
