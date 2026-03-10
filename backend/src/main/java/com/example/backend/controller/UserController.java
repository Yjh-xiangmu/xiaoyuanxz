package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.Notification;
import com.example.backend.entity.Review;
import com.example.backend.entity.User;
import com.example.backend.mapper.NotificationMapper;
import com.example.backend.mapper.ReviewMapper;
import com.example.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private NotificationMapper notificationMapper;

    // 1. 用户登录
    @PostMapping("/login")
    public Result<User> login(@RequestBody User user) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("username", user.getUsername()).eq("password", user.getPassword());
        User dbUser = userMapper.selectOne(qw);

        if (dbUser == null) {
            return Result.error("用户名或密码错误");
        }
        // 根据你的 SQL：0-限制登录封禁
        if (dbUser.getStatus() != null && dbUser.getStatus() == 0) {
            return Result.error("该账号已被管理员封禁，禁止登录！");
        }
        return Result.success(dbUser);
    }

    // 2. 用户注册
    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("username", user.getUsername());
        if (userMapper.selectCount(qw) > 0) {
            return Result.error("该用户名/学号已存在");
        }
        user.setRole("USER");
        user.setCreditScore(new BigDecimal("100.0"));
        user.setStatus(1); // 1-正常
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return Result.success(user);
    }

    // 3. 修改个人信息
    @PostMapping("/update")
    public Result<?> updateProfile(@RequestBody User user) {
        if (user.getId() == null) {
            return Result.error("用户ID不能为空");
        }
        userMapper.updateById(user);
        return Result.success("个人信息修改成功！下次登录生效。");
    }

    // 4. 获取卖家信用主页的头部汇总信息
    @GetMapping("/sellerInfo")
    public Result<Map<String, Object>> getSellerInfo(@RequestParam Long sellerId) {
        User seller = userMapper.selectById(sellerId);
        if (seller == null) return Result.error("用户不存在");

        Map<String, Object> map = new HashMap<>();
        map.put("id", seller.getId());

        // 🌟 核心修改：如果有真实姓名，就拼接成 "张三 (2024001)"
        String displayName = seller.getUsername();
        if (seller.getRealName() != null && !seller.getRealName().trim().isEmpty()) {
            displayName = seller.getRealName() + " (" + seller.getUsername() + ")";
        }
        map.put("username", displayName);

        map.put("creditScore", seller.getCreditScore());
        map.put("avatar", seller.getAvatar());

        QueryWrapper<Review> qw = new QueryWrapper<>();
        qw.eq("seller_id", sellerId).eq("is_append", 0);
        List<Review> reviews = reviewMapper.selectList(qw);

        double avgRating = 0.0;
        if (!reviews.isEmpty()) {
            double sum = reviews.stream().mapToInt(Review::getRating).sum();
            avgRating = sum / reviews.size();
        }
        map.put("reviewCount", reviews.size());
        map.put("avgRating", String.format("%.1f", avgRating));

        return Result.success(map);
    }

    // 5. 管理员：获取所有用户列表（支持按用户名/姓名搜索）
    @GetMapping("/list")
    public Result<List<User>> getUserList(@RequestParam(required = false) String keyword) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.like("username", keyword).or().like("real_name", keyword);
        }
        queryWrapper.orderByDesc("create_time");

        List<User> userList = userMapper.selectList(queryWrapper);
        // 抹除密码后再传给前端，保护隐私
        userList.forEach(u -> u.setPassword(null));
        return Result.success(userList);
    }

    // 6. 管理员：违规账号处置 (封禁/解封)
    @PostMapping("/changeStatus")
    public Result<?> changeUserStatus(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("id").toString());
        Integer status = (Integer) params.get("status");

        User user = new User();
        user.setId(userId);
        user.setStatus(status);
        userMapper.updateById(user);

        // 发送系统通知
        Notification msg = new Notification();
        msg.setUserId(userId);
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());

        if (status == 0) { // 0是封禁
            msg.setTitle("🚨 账号违规封禁通知");
            msg.setContent("您的账号因违反平台规定，已被管理员强制封禁！封禁期间将无法使用平台核心功能。如有异议请联系管理员。");
        } else {
            msg.setTitle("✅ 账号解封通知");
            msg.setContent("您的账号已解除限制，恢复正常使用，请自觉遵守平台规范。");
        }
        notificationMapper.insert(msg);

        return Result.success(status == 0 ? "账号已封禁！" : "账号已恢复正常！");
    }

    // 7. 管理员：手动奖惩信誉分
    @PostMapping("/adjustScore")
    public Result<?> adjustScore(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("id").toString());
        BigDecimal newScore = new BigDecimal(params.get("score").toString());
        String reason = params.get("reason").toString();

        User user = new User();
        user.setId(userId);
        user.setCreditScore(newScore);
        userMapper.updateById(user);

        // 发送系统通知告知原因
        Notification msg = new Notification();
        msg.setUserId(userId);
        msg.setTitle("⚖️ 信誉分调整通知");
        msg.setContent("管理员操作了您的信誉分。当前最新分数为：" + newScore + " 分。操作原因：" + reason);
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(msg);

        return Result.success("信誉分调整成功！已通知该用户。");
    }
}