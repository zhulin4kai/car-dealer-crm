package com.autodealer.crm.service.impl;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.mapper.TClueRemarkMapper;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.model.TClueRemark;
import com.autodealer.crm.query.ClueRemarkQuery;
import com.autodealer.crm.service.ClueRemarkService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ClueRemarkServiceImpl implements ClueRemarkService {

    @Resource
    private TClueRemarkMapper tClueRemarkMapper;

    @Resource
    private TClueMapper tClueMapper;

    @Resource
    private CurrentUserProvider currentUserProvider;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int saveClueRemark(ClueRemarkQuery clueRemarkQuery) {
        if (tClueMapper.selectScopedByPrimaryKey(
                clueRemarkQuery.getClueId(), currentUserProvider.getDataScopeUserId()) == null) {
            throw new RuntimeException("线索不存在或无权访问");
        }
        TClueRemark tClueRemark = new TClueRemark();

        //把ClueRemarkQuery对象里面的属性数据复制到TClueRemark对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(clueRemarkQuery, tClueRemark);

        tClueRemark.setCreateTime(new Date()); //创建时间

        tClueRemark.setCreateBy(currentUserProvider.getCurrentUserId()); //创建人

        return tClueRemarkMapper.insertSelective(tClueRemark);
    }

    @Override
    public PageInfo<TClueRemark> getClueRemarkByPage(Integer current, ClueRemarkQuery clueRemarkQuery) {
        // 1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 2.查询
        List<TClueRemark> list = tClueRemarkMapper.selectClueRemarkByPage(clueRemarkQuery);
        // 3.封装分页数据到PageInfo
        PageInfo<TClueRemark> info = new PageInfo<>(list);
        return info;
    }
}
