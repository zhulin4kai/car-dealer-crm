package com.autodealer.crm.mapper;

import com.autodealer.crm.commons.DataScope;
import com.autodealer.crm.dto.ActivityRoiResponse;
import com.autodealer.crm.model.TActivity;
import com.autodealer.crm.query.ActivityQuery;
import com.autodealer.crm.result.ActivityExportRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TActivityMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(TActivity record);

    int insertSelective(TActivity record);

    TActivity selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TActivity record);

    int updateByPrimaryKey(TActivity record);

    int updateStatusAtomic(@Param("id") Integer id,
                           @Param("fromStatus") String fromStatus,
                           @Param("toStatus") String toStatus,
                           @Param("operatorId") Integer operatorId,
                           @Param("reason") String reason,
                           @Param("dataScopeUserId") Integer dataScopeUserId);

    int reviewAtomic(@Param("id") Integer id,
                     @Param("fromStatus") String fromStatus,
                     @Param("record") TActivity record,
                     @Param("dataScopeUserId") Integer dataScopeUserId);

    @DataScope(tableAlias = "ta", tableField = "owner_id")
    List<TActivity> selectActivityByPage(ActivityQuery query);

    TActivity selectDetailByPrimaryKey(@Param("id") Integer id,
                                       @Param("dataScopeUserId") Integer dataScopeUserId);

    List<TActivity> selecOngoingActivity(Integer dataScopeUserId);

    Integer selectByCount(@Param("dataScopeUserId") Integer dataScopeUserId);

    Integer countBusinessReferences(@Param("id") Integer id);

    ActivityRoiResponse selectActivityRoi(@Param("id") Integer id,
                                          @Param("dataScopeUserId") Integer dataScopeUserId);

    @DataScope(tableAlias = "ta", tableField = "owner_id")
    List<ActivityExportRow> selectActivityExportRows(ActivityQuery query);

    int batchDeleteByIds(List<Integer> ids);
}
