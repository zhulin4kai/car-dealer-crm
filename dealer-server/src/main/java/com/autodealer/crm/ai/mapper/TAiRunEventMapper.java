package com.autodealer.crm.ai.mapper;

import com.autodealer.crm.ai.model.TAiRunEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TAiRunEventMapper {
    int insert(TAiRunEvent event);

    int countSameEvent(@Param("runId") Long runId,
                       @Param("eventId") String eventId,
                       @Param("sequenceNo") Integer sequenceNo);

    List<TAiRunEvent> selectAfterSequence(@Param("runId") Long runId,
                                         @Param("afterSequence") Integer afterSequence);
}
