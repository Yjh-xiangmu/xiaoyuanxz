package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.entity.Goods;
import com.example.backend.entity.Notification;
import com.example.backend.entity.User;
import com.example.backend.mapper.GoodsMapper;
import com.example.backend.mapper.NotificationMapper;
import com.example.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/goods")
@CrossOrigin
public class GoodsController {

    @Autowired
    private GoodsMapper goodsMapper;

    // 注入 User 和 Notification 的 Mapper，用于扣分和发通知
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private NotificationMapper notificationMapper;

    @PostMapping("/publish")
    public Result<?> publish(@RequestBody Goods goods) {
        if (goods.getSellerId() == null) return Result.error("未获取到卖家信息，请重新登录！");
        goods.setStatus(0);
        goods.setCreateTime(LocalDateTime.now());
        goodsMapper.insert(goods);
        return Result.success("发布成功，已提交至管理员审核！");
    }

    // 增强版：获取商品列表接口（支持状态、关键字、分类多条件查询）
    @GetMapping("/list")
    public Result<java.util.List<Goods>> getList(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Goods> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();

        // 状态筛选
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        // 关键字模糊搜索（标题包含该关键字）
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.like("title", keyword);
        }
        // 分类精准匹配
        if (category != null && !category.trim().isEmpty()) {
            queryWrapper.eq("category", category);
        }

        // 按发布时间倒序排列，最新发布的在最前面
        queryWrapper.orderByDesc("create_time");

        return Result.success(goodsMapper.selectList(queryWrapper));
    }

    // 🌟 全新升级的超级审核接口
    @PostMapping("/audit")
    public Result<?> audit(@RequestBody Map<String, Object> params) {
        Long goodsId = Long.valueOf(params.get("id").toString());
        Integer status = (Integer) params.get("status");
        String rejectReason = (String) params.get("rejectReason");
        // 获取可能传过来的扣除信誉分值
        BigDecimal creditDeduct = params.get("creditDeduct") != null ? new BigDecimal(params.get("creditDeduct").toString()) : BigDecimal.ZERO;

        // 1. 获取商品原信息（为了知道卖家是谁）
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) return Result.error("商品不存在");

        // 2. 更新商品状态
        Goods updateGoods = new Goods();
        updateGoods.setId(goodsId);
        updateGoods.setStatus(status);
        if (status == 2 || status == 3) {
            updateGoods.setRejectReason(rejectReason);
        } else {
            updateGoods.setRejectReason("");
        }
        goodsMapper.updateById(updateGoods);

        // 3. 准备发送系统通知
        Notification msg = new Notification();
        msg.setUserId(goods.getSellerId());
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());

        if (status == 1) {
            msg.setTitle("✅ 商品审核通过");
            msg.setContent("您发布的商品《" + goods.getTitle() + "》已通过审核，正式上架展示！");
        } else if (status == 2) {
            msg.setTitle("⚠️ 商品需补全信息");
            msg.setContent("您发布的商品《" + goods.getTitle() + "》被驳回。管理员备注：" + rejectReason + "。请修改后重新提交。");
        } else if (status == 3) {
            // 4. 如果是违规下架，处理扣分逻辑
            if (creditDeduct.compareTo(BigDecimal.ZERO) > 0) {
                User user = userMapper.selectById(goods.getSellerId());
                // 计算扣分后的新分数（最低为0）
                BigDecimal newScore = user.getCreditScore().subtract(creditDeduct);
                if (newScore.compareTo(BigDecimal.ZERO) < 0) newScore = BigDecimal.ZERO;

                User updateUser = new User();
                updateUser.setId(user.getId());
                updateUser.setCreditScore(newScore);
                userMapper.updateById(updateUser);

                msg.setTitle("🚨 商品违规下架及扣分通知");
                msg.setContent("您发布的商品《" + goods.getTitle() + "》因严重违规被强制下架！管理员提醒：" + rejectReason + "。系统已扣除您的信誉分：" + creditDeduct + " 分。");
            } else {
                msg.setTitle("🚨 商品违规下架通知");
                msg.setContent("您发布的商品《" + goods.getTitle() + "》因违规被强制下架！管理员提醒：" + rejectReason + "。");
            }
        }

        // 保存通知记录
        notificationMapper.insert(msg);

        return Result.success("审核操作完成！");
    }
    // 获取单个商品详情接口
    @GetMapping("/detail/{id}")
    public Result<Goods> getDetail(@PathVariable Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            return Result.error("商品不存在或已被删除");
        }
        return Result.success(goods);
    }
    // 🌟 获取当前用户发布的商品列表（卖家中心用）
    @GetMapping("/myList")
    public Result<java.util.List<Goods>> getMyList(@RequestParam Long sellerId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Goods> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("seller_id", sellerId).orderByDesc("create_time");
        return Result.success(goodsMapper.selectList(queryWrapper));
    }

    // 🌟 1. 修改商品信息（核心风控：只要修改了，就打回待审核状态）
    @PostMapping("/update")
    public Result<?> updateGoods(@RequestBody Goods goods) {
        if (goods.getId() == null) {
            return Result.error("商品ID不能为空");
        }
        // 强制把状态改回 0（待审核），并清空之前的驳回原因
        goods.setStatus(0);
        goods.setRejectReason("");

        goodsMapper.updateById(goods);
        return Result.success("商品信息修改成功，已重新提交系统审核！");
    }
    @PostMapping("/changeStatus")
    public Result<?> changeStatus(@RequestBody java.util.Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Integer status = (Integer) params.get("status");

        Goods updateData = new Goods();
        updateData.setId(id);
        updateData.setStatus(status);
        goodsMapper.updateById(updateData);

        return Result.success(status == 1 ? "重新上架成功！" : "下架成功！");
    }

    // 🌟 3. 删除商品接口
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteGoods(@PathVariable Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods != null && goods.getStatus() == 4) {
            return Result.error("已售出的商品不能删除，需保留交易记录！");
        }
        goodsMapper.deleteById(id);
        return Result.success("商品删除成功！");
    }
}