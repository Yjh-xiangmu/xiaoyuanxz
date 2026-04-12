package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.Goods;
import com.example.backend.entity.Orders;
import com.example.backend.mapper.GoodsMapper;
import com.example.backend.mapper.OrdersMapper;
import com.example.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@CrossOrigin
public class DataController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private OrdersMapper ordersMapper;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardData() {
        Map<String, Object> data = new HashMap<>();

        // ========== 四大核心指标 ==========
        data.put("totalUsers", userMapper.selectCount(null));

        QueryWrapper<Goods> goodsQuery = new QueryWrapper<>();
        goodsQuery.eq("status", 1);
        data.put("activeGoods", goodsMapper.selectCount(goodsQuery));

        QueryWrapper<Orders> orderQuery = new QueryWrapper<>();
        orderQuery.eq("status", 3);
        data.put("completedOrders", ordersMapper.selectCount(orderQuery));

        List<Orders> completedOrderList = ordersMapper.selectList(orderQuery);
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Orders order : completedOrderList) {
            totalAmount = totalAmount.add(order.getAmount());
        }
        data.put("totalAmount", totalAmount);

        // ========== 待审核商品 ==========
        QueryWrapper<Goods> pendingQuery = new QueryWrapper<>();
        pendingQuery.eq("status", 0);
        data.put("pendingAuditCount", goodsMapper.selectCount(pendingQuery));

        QueryWrapper<Goods> recentPendingQuery = new QueryWrapper<>();
        recentPendingQuery.eq("status", 0).orderByDesc("create_time").last("LIMIT 5");
        data.put("recentPendingGoods", goodsMapper.selectList(recentPendingQuery));

        // ========== 真正的仲裁订单（is_dispute=1） ==========
        QueryWrapper<Orders> disputeQuery = new QueryWrapper<>();
        disputeQuery.eq("is_dispute", 1);
        data.put("disputeOrderCount", ordersMapper.selectCount(disputeQuery));

        QueryWrapper<Orders> recentDisputeQuery = new QueryWrapper<>();
        recentDisputeQuery.eq("is_dispute", 1).orderByDesc("create_time").last("LIMIT 5");
        data.put("recentDisputeOrders", ordersMapper.selectList(recentDisputeQuery));

        // ========== 饼图数据 ==========
        String[] categories = {"数码", "书籍", "生活", "服饰", "其他"};
        List<Map<String, Object>> pieData = new ArrayList<>();
        for (String cat : categories) {
            QueryWrapper<Goods> catQ = new QueryWrapper<>();
            catQ.eq("category", cat).ne("status", 3);
            long count = goodsMapper.selectCount(catQ);
            if (count > 0) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", cat);
                map.put("value", count);
                pieData.add(map);
            }
        }
        data.put("categoryPieData", pieData);

        return Result.success(data);
    }
}