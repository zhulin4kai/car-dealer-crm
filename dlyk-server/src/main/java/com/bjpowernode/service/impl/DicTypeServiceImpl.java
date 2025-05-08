package com.bjpowernode.service.impl;

import com.bjpowernode.mapper.TDicTypeMapper;
import com.bjpowernode.model.TDicType;
import com.bjpowernode.service.DicTypeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DicTypeServiceImpl implements DicTypeService {

    @Resource
    private TDicTypeMapper tDicTypeMapper;

    @Override
    public List<TDicType> loadAllDicData() {
        return tDicTypeMapper.selectByAll();
    }
}
