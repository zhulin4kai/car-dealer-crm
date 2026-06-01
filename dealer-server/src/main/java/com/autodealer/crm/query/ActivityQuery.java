package com.autodealer.crm.query;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ActivityQuery extends BaseQuery {

    private Integer id;

    private Integer ownerId;

    private String name;

    /**
     * 前端传过来的是一个格式为：YYYY-MM-DD HH:mm:ss 的字符串日期，后台接收要把字符串的日期转成java.util.Date日期，需要使用@DateTimeFormat注解
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 前端传过来的是一个格式为：YYYY-MM-DD HH:mm:ss 的字符串日期，后台接收要把字符串的日期转成java.util.Date日期，需要使用@DateTimeFormat注解
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    private BigDecimal cost;

    /**
     * 前端传过来的是一个格式为：YYYY-MM-DD HH:mm:ss 的字符串日期，后台接收要把字符串的日期转成java.util.Date日期，需要使用@DateTimeFormat注解
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private String description;
}
