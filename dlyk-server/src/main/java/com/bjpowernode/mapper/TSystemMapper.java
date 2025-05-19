package com.bjpowernode.mapper;

import com.bjpowernode.model.TSystem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TSystemMapper {
    List<TSystem> selectAll();
    TSystem selectById(@Param("id") Integer id);
    int insert(TSystem system);
    int update(TSystem system);
    int deleteById(@Param("id") Integer id);
    int batchDelete(@Param("ids") List<Integer> ids);
    int updateStatus(@Param("id") Integer id, @Param("isopen") String isopen);
}
