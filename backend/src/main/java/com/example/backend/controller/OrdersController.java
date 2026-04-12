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
        if (orders.getReceiver() == null || orders.getAddress() == null) {
            return Result.error("请选择收货地址！");
        }

        String orderNo = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        orders.setOrderNo(orderNo);
        orders.setSellerId(goods.getSellerId());
        orders.setAmount(goods.getPrice());
        orders.setStatus(0);
        orders.setIsDispute(0);
        orders.setCreateTime(LocalDateTime.now());

        ordersMapper.insert(orders);

        Goods updateGoods = new Goods();
        updateGoods.setId(goods.getId());
        updateGoods.setStatus(4);
        goodsMapper.updateById(updateGoods);

        return Result.success(orders);
    }

    // 查询买家订单列表
    @GetMapping("/myList")
    public Result<java.util.List<Orders>> getMyList(@RequestParam Long userId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Orders> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("buyer_id", userId);
        queryWrapper.orderByDesc("create_time");
        return Result.success(ordersMapper.selectList(queryWrapper));
    }

    // 修改订单收货地址
    @PostMapping("/updateAddress")
    public Result<?> updateOrderAddress(@RequestBody Orders updateData) {
        Orders order = ordersMapper.selectById(updateData.getId());
        if (order == null) return Result.error("订单不存在");
        if (order.getStatus() > 1) {
            return Result.error("订单已发货或已完成，无法修改收货地址！");
        }
        order.setReceiver(updateData.getReceiver());
        order.setPhone(updateData.getPhone());
        order.setAddress(updateData.getAddress());
        ordersMapper.updateById(order);
        return Result.success("收货地址修改成功！");
    }

    // 获取卖家卖出的订单列表
    @GetMapping("/sellList")
    public Result<java.util.List<Orders>> getSellList(@RequestParam Long sellerId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Orders> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("seller_id", sellerId).orderByDesc("create_time");
        return Result.success(ordersMapper.selectList(queryWrapper));
    }

    // 卖家发货接口
    @PostMapping("/ship/{id}")
    public Result<?> shipOrder(@PathVariable Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order != null && order.getStatus() == 1) {
            order.setStatus(2);
            ordersMapper.updateById(order);

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

    // 买家确认收货接口
    @PostMapping("/receive/{id}")
    public Result<?> receiveOrder(@PathVariable Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order != null && order.getStatus() == 2) {
            order.setStatus(3);
            ordersMapper.updateById(order);

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

    // ========== 新增：买家申请仲裁 ==========
    @PostMapping("/applyDispute/{id}")
    public Result<?> applyDispute(@PathVariable Long id, @RequestBody java.util.Map<String, String> params) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) return Result.error("订单不存在");

        // 只有已支付(1)和已发货(2)的订单才能申请仲裁
        if (order.getStatus() != 1 && order.getStatus() != 2) {
            return Result.error("当前订单状态不支持申请仲裁！");
        }
        if (order.getIsDispute() != null && order.getIsDispute() == 1) {
            return Result.error("该订单已申请过仲裁，请等待管理员处理！");
        }

        String reason = params.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return Result.error("请填写仲裁原因！");
        }

        // 标记仲裁申请
        order.setIsDispute(1);
        order.setDisputeReason(reason);
        ordersMapper.updateById(order);

        // 给卖家发通知
        Notification sellerMsg = new Notification();
        sellerMsg.setUserId(order.getSellerId());
        sellerMsg.setTitle("⚠️ 买家发起仲裁通知");
        sellerMsg.setContent("买家对订单 [" + order.getOrderNo() + "] 发起了仲裁申请，原因：" + reason + "。请配合管理员调查处理，若违规将承担相应责任。");
        sellerMsg.setIsRead(0);
        sellerMsg.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(sellerMsg);

        return Result.success("仲裁申请已提交！管理员将在24小时内介入处理。");
    }

    // ========== 新增：撤销仲裁申请（买卖双方协商解决后） ==========
    @PostMapping("/cancelDispute/{id}")
    public Result<?> cancelDispute(@PathVariable Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) return Result.error("订单不存在");
        if (order.getIsDispute() != 1) return Result.error("该订单没有进行中的仲裁申请");

        order.setIsDispute(0);
        order.setDisputeReason(null);
        ordersMapper.updateById(order);

        return Result.success("仲裁申请已撤销。");
    }

    // 管理员：获取全平台订单列表（支持按订单号搜索）
    @GetMapping("/adminList")
    public Result<java.util.List<Orders>> getAdminList(@RequestParam(required = false) String keyword) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Orders> qw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.like("order_no", keyword);
        }
        qw.orderByDesc("create_time");
        return Result.success(ordersMapper.selectList(qw));
    }

    // 管理员：平台强制介入退款仲裁
    @PostMapping("/forceRefund/{id}")
    public Result<?> forceRefund(@PathVariable Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) return Result.error("订单不存在");

        if (order.getStatus() == 1 || order.getStatus() == 2) {
            order.setStatus(4);
            order.setIsDispute(0); // 仲裁完成，清除标记
            ordersMapper.updateById(order);

            Notification buyerMsg = new Notification();
            buyerMsg.setUserId(order.getBuyerId());
            buyerMsg.setTitle("💰 平台介入退款通知");
            buyerMsg.setContent("管理员介入仲裁，您的订单 [" + order.getOrderNo() + "] 已被强制取消，实付款项将原路退回！");
            buyerMsg.setIsRead(0);
            buyerMsg.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(buyerMsg);

            Notification sellerMsg = new Notification();
            sellerMsg.setUserId(order.getSellerId());
            sellerMsg.setTitle("🚨 订单仲裁强制退款通知");
            sellerMsg.setContent("因买家维权或违规交易，管理员已将您的订单 [" + order.getOrderNo() + "] 强制取消并执行退款操作！");
            sellerMsg.setIsRead(0);
            sellerMsg.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(sellerMsg);

            Goods goods = new Goods();
            goods.setId(order.getGoodsId());
            goods.setStatus(3);
            goodsMapper.updateById(goods);

            return Result.success("仲裁退款执行成功！已通知买卖双方。");
        }
        return Result.error("操作失败，当前订单状态不支持退款。");
    }

    // 买家：取消未支付的订单
    @PostMapping("/cancel/{id}")
    public Result<?> cancelOrder(@PathVariable Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order != null && order.getStatus() == 0) {
            order.setStatus(4);
            ordersMapper.updateById(order);

            Goods goods = new Goods();
            goods.setId(order.getGoodsId());
            goods.setStatus(1);
            goodsMapper.updateById(goods);

            return Result.success("订单取消成功！商品已重新恢复在售。");
        }
        return Result.error("操作失败，该订单当前状态无法取消。");
    }
}