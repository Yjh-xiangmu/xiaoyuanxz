package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.entity.Goods;
import com.example.backend.entity.Orders;
import com.example.backend.mapper.GoodsMapper;
import com.example.backend.mapper.OrdersMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.backend.entity.Notification;
import com.example.backend.mapper.NotificationMapper;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin
public class OrdersController {

    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private GoodsMapper goodsMapper;
    // 🌟 新增注入通知 Mapper，用于发货/收货时发消息
    @Autowired
    private NotificationMapper notificationMapper;
    // 创建订单接口
    @PostMapping("/create")
    public Result<Orders> createOrder(@RequestBody Orders orders) {
        Goods goods = goodsMapper.selectById(orders.getGoodsId());
        if (goods == null || goods.getStatus() != 1) {
            return Result.error("该商品已下架或被买走啦！");
        }
        if (goods.getSellerId().equals(orders.getBuyerId())) {
            return Result.error("不能购买自己发布的商品哦！");
        }

        // 🌟 核心校验：下单必须要有收货地址！
        if (orders.getReceiver() == null || orders.getAddress() == null) {
            return Result.error("请选择收货地址！");
        }

        // 2. 初始化订单数据
        String orderNo = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        orders.setOrderNo(orderNo);
        orders.setSellerId(goods.getSellerId());
        orders.setAmount(goods.getPrice());
        orders.setStatus(0);
        orders.setCreateTime(LocalDateTime.now());
        // 注意：前端传过来的 receiver, phone, address 已经被包含在 orders 对象里了

        // 3. 保存订单
        ordersMapper.insert(orders);

        // 4. 锁定商品状态
        Goods updateGoods = new Goods();
        updateGoods.setId(goods.getId());
        updateGoods.setStatus(4);
        goodsMapper.updateById(updateGoods);

        return Result.success(orders);
    }
    // 查询当前用户的订单列表
    @GetMapping("/myList")
    public Result<java.util.List<Orders>> getMyList(@RequestParam Long userId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Orders> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("buyer_id", userId);
        queryWrapper.orderByDesc("create_time");

        return Result.success(ordersMapper.selectList(queryWrapper));
    }
    // 🌟 修改订单收货地址（仅限发货前）
    @PostMapping("/updateAddress")
    public Result<?> updateOrderAddress(@RequestBody Orders updateData) {
        Orders order = ordersMapper.selectById(updateData.getId());
        if (order == null) return Result.error("订单不存在");

        // 只有 0(待支付) 和 1(待发货) 的状态才能改地址
        if (order.getStatus() > 1) {
            return Result.error("订单已发货或已完成，无法修改收货地址！");
        }

        order.setReceiver(updateData.getReceiver());
        order.setPhone(updateData.getPhone());
        order.setAddress(updateData.getAddress());
        ordersMapper.updateById(order);

        return Result.success("收货地址修改成功！");
    }
    // 🌟 1. 获取卖家卖出的订单列表
    @GetMapping("/sellList")
    public Result<java.util.List<Orders>> getSellList(@RequestParam Long sellerId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Orders> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("seller_id", sellerId).orderByDesc("create_time");
        return Result.success(ordersMapper.selectList(queryWrapper));
    }

    // 🌟 2. 卖家发货接口
    @PostMapping("/ship/{id}")
    public Result<?> shipOrder(@PathVariable Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order != null && order.getStatus() == 1) {
            order.setStatus(2); // 2: 已发货
            ordersMapper.updateById(order);

            // 给买家发一条系统通知
            Notification msg = new Notification();
            msg.setUserId(order.getBuyerId());
            msg.setTitle("📦 订单发货通知");
            msg.setContent("您购买的订单 [" + order.getOrderNo() + "] 卖家已发货，请注意查收！");
            msg.setIsRead(0);
            msg.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(msg);

            return Result.success("发货成功！已通知买家。");
        }
        return Result.error("操作失败，订单状态不正确");
    }

    // 🌟 3. 买家确认收货接口
    @PostMapping("/receive/{id}")
    public Result<?> receiveOrder(@PathVariable Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order != null && order.getStatus() == 2) {
            order.setStatus(3); // 3: 已完成
            ordersMapper.updateById(order);

            // 给卖家发一条系统通知
            Notification msg = new Notification();
            msg.setUserId(order.getSellerId());
            msg.setTitle("💰 交易完成通知");
            msg.setContent("您的订单 [" + order.getOrderNo() + "] 买家已确认收货，交易圆满完成！");
            msg.setIsRead(0);
            msg.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(msg);

            return Result.success("确认收货成功！交易完成。");
        }
        return Result.error("操作失败，订单未发货");
    }
}