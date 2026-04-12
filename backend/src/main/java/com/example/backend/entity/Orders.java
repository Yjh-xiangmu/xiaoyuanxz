package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Orders {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long goodsId;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private String receiver;
    private String phone;
    private String address;
    private Integer isRated;
    // 新增：仲裁相关字段
    private Integer isDispute;      // 0-正常 1-申请仲裁中
    private String disputeReason;   // 申请仲裁原因
}