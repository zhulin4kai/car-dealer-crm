package com.autodealer.crm.modules.sales.lead.application.internal;

import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.shared.infrastructure.constants.Constants;
import com.autodealer.crm.modules.sales.lead.persistence.mapper.TClueRemarkMapper;
import com.autodealer.crm.modules.sales.lead.persistence.mapper.TClueMapper;
import com.autodealer.crm.modules.sales.lead.application.api.model.TClueRemark;
import com.autodealer.crm.modules.sales.lead.application.api.query.ClueRemarkQuery;
import com.autodealer.crm.modules.sales.lead.application.api.ClueRemarkService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;

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
            throw new BusinessException(CodeEnum.NOT_FOUND, "线索不存在或无权访问");
        }
        TClueRemark tClueRemark = new TClueRemark();

        //把ClueRemarkQuery对象里面的属性数据复制到TClueRemark对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(clueRemarkQuery, tClueRemark);

        tClueRemark.setCreateTime(new Date()); //创建时间

        tClueRemark.setCreateBy(currentUserProvider.getCurrentUserId()); //创建人

        return tClueRemarkMapper.insertSelective(tClueRemark);
    }

    @Override
    public PageInfo<TClueRemark> getClueRemarkByPage(Integer current, Integer pageSize, ClueRemarkQuery clueRemarkQuery) {
        if (current == null || current < 1) {
            current = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = Constants.PAGE_SIZE;
        }
        if (pageSize > 100) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "分页大小不能超过100");
        }
        // 1.设置PageHelper
        PageHelper.startPage(current, pageSize);
        // 2.查询
        List<TClueRemark> list = tClueRemarkMapper.selectClueRemarkByPage(clueRemarkQuery);
        // 3.封装分页数据到PageInfo
        PageInfo<TClueRemark> info = new PageInfo<>(list);
        return info;
    }
}
