package com.bjpowernode.service.impl;

import com.alibaba.excel.EasyExcel;
import com.bjpowernode.config.converter.ClueExcelConverter;
import com.bjpowernode.config.listener.UploadDataListener;
import com.bjpowernode.constant.Constants;
import com.bjpowernode.mapper.TClueMapper;
import com.bjpowernode.mapper.TClueRemarkMapper;
import com.bjpowernode.model.TClue;
import com.bjpowernode.model.TUser;
import com.bjpowernode.query.BaseQuery;
import com.bjpowernode.query.ClueQuery;
import com.bjpowernode.result.ClueExcel;
import com.bjpowernode.service.ClueService;
import com.bjpowernode.util.JWTUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

@Service
public class ClueServiceImpl implements ClueService {

    @Resource
    private TClueMapper tClueMapper;

    @Resource
    private TClueRemarkMapper tClueRemarkMapper;

    @Resource
    private ClueExcelConverter clueExcelConverter;

    @Override
    public PageInfo<TClue> getClueByPage(Integer current, Integer pageSize) {
        // 参数校验
        if (current == null || current < 1) {
            current = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = Constants.PAGE_SIZE;
        }
        // 限制pageSize范围
        if (pageSize > 100) {
            pageSize = 100;
        }
        
        // 1.设置PageHelper
        PageHelper.startPage(current, pageSize);
        // 2.查询
        List<TClue> list = tClueMapper.selectClueByPage(BaseQuery.builder().build());
        // 3.封装分页数据到PageInfo
        PageInfo<TClue> info = new PageInfo<>(list);
        return info;
    }

    @Override
    public void importExcel(InputStream inputStream, String token) {
        //链式编程，3个参数, 第一个参数是要读取的Excel文件，第二个参数是Excel模板类，第三个参数是文件读取的监听器
        EasyExcel.read(inputStream, ClueExcel.class, new UploadDataListener(tClueMapper, token, clueExcelConverter))
                .sheet()
                .doRead();
    }

    @Override
    public Boolean checkPhone(String phone) {
        int count = tClueMapper.selectByCount(phone);
        return count <= 0; //没有查到手机号是true
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int saveClue(ClueQuery clueQuery) {
        int count = tClueMapper.selectByCount(clueQuery.getPhone());
        if (count <= 0) {
            TClue tClue = new TClue();

            //把前端提交过来的参数数据对象ClueQuery复制到TClue对象中
            //Spring框架有个工具类BeanUtils可以进行对象的复制,复制的条件要求是：两个对象的字段名要相同，字段的类型也相同，这样才可以复制
            BeanUtils.copyProperties(clueQuery, tClue);

            //解析jwt得到userId
            Integer loginUserId = JWTUtils.parseUserFromJWT(clueQuery.getToken()).getId();

            tClue.setCreateTime(new Date()); //创建时间
            tClue.setCreateBy(loginUserId); //创建人id

            return tClueMapper.insertSelective(tClue);
        } else {
            throw new RuntimeException("该手机号已经录入过了，不能再录入");
        }
    }

    @Override
    public TClue getClueById(Integer id) {
        return tClueMapper.selectDetailById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateClue(ClueQuery clueQuery) {
        // 先查询原记录，获取原手机号
        TClue originalClue = tClueMapper.selectByPrimaryKey(clueQuery.getId());
        if (originalClue == null) {
            throw new RuntimeException("线索记录不存在");
        }

        TClue tClue = new TClue();

        //把前端提交过来的参数数据对象ClueQuery复制到TClue对象中
        //Spring框架有个工具类BeanUtils可以进行对象的复制,复制的条件要求是：两个对象的字段名要相同，字段的类型也相同，这样才可以复制
        BeanUtils.copyProperties(clueQuery, tClue);

        // 如果传入的手机号与原记录不同，忽略手机号字段
        if (clueQuery.getPhone() != null && !clueQuery.getPhone().equals(originalClue.getPhone())) {
            tClue.setPhone(null); // 设置为null，让MyBatis的updateByPrimaryKeySelective跳过该字段
        }

        //解析jwt得到userId
        Integer loginUserId = JWTUtils.parseUserFromJWT(clueQuery.getToken()).getId();

        tClue.setEditTime(new Date()); //编辑时间
        tClue.setEditBy(loginUserId); //编辑人id

        return tClueMapper.updateByPrimaryKeySelective(tClue);
    }    
    
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int delClueById(Integer id) {
        if (id == null) {
            return 0;
        }
        // 先删除关联的线索备注
        tClueRemarkMapper.deleteByClueId(id);
        // 再删除线索
        return tClueMapper.deleteByPrimaryKey(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int batchDelClueByIds(List<Integer> ids) {
        if (ids == null || ids.size() == 0) {
            return 0;
        }
        // 先删除关联的线索备注
        for (Integer id : ids) {
            tClueRemarkMapper.deleteByClueId(id);
        }
        // 再删除线索
        return tClueMapper.batchDeleteByIds(ids);
    }
}
