package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.backend.entity.Review;
import com.example.backend.mapper.ReviewMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/user")
@CrossOrigin // 🌟解决前端Vue的跨域请求问题，必加！
public class UserController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    // 🌟 获取卖家信用主页的头部汇总信息
    @GetMapping("/sellerInfo")
    public Result<Map<String, Object>> getSellerInfo(@RequestParam Long sellerId) {
        User seller = userMapper.selectById(sellerId);
        if (seller == null) return Result.error("用户不存在");

        Map<String, Object> map = new HashMap<>();
        map.put("id", seller.getId());
        map.put("username", seller.getUsername());
        map.put("creditScore", seller.getCreditScore());

        // 计算平均星级和评价总数（只算初评，追评没有星级）
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review> qw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
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
    // 注册接口
    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        // 1. 查询用户名是否已经存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername());
        if (userMapper.selectCount(queryWrapper) > 0) {
            return Result.error("该用户名/学号已被注册！");
        }

        // 2. 初始化新用户信息
        user.setRole("USER"); // 默认普通用户
        user.setCreditScore(new java.math.BigDecimal("100.0")); // 默认信誉分100
        user.setStatus(1); // 状态正常
        user.setCreateTime(LocalDateTime.now());

        // 3. 插入数据库
        userMapper.insert(user);
        return Result.success();
    }

    // 登录接口
    @PostMapping("/login")
    public Result<User> login(@RequestBody User user) {
        // 根据用户名和密码查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername())
                .eq("password", user.getPassword());

        User dbUser = userMapper.selectOne(queryWrapper);

        if (dbUser == null) {
            return Result.error("用户名或密码错误！");
        }
        if (dbUser.getStatus() == 0) {
            return Result.error("账号存在违规，已被限制登录！");
        }

        // 登录成功，返回用户信息
        return Result.success(dbUser);
    }
    // 实名认证接口
    @PostMapping("/auth")
    public Result<User> auth(@RequestBody User user) {
        if (user.getId() == null) {
            return Result.error("用户ID不能为空！");
        }
        if (user.getRealName() == null || user.getIdCard() == null || user.getPhone() == null) {
            return Result.error("真实姓名、身份证号和手机号不能为空！");
        }

        // 1. 创建一个用于更新的对象
        User updateData = new User();
        updateData.setId(user.getId());
        updateData.setRealName(user.getRealName());
        updateData.setIdCard(user.getIdCard());
        updateData.setPhone(user.getPhone());

        // 2. 更新数据库
        userMapper.updateById(updateData);

        // 3. 重新查出最新的用户信息并返回给前端
        User newestUser = userMapper.selectById(user.getId());
        return Result.success(newestUser);
    }
    // 🌟 修改个人信息接口
    @PostMapping("/update")
    public Result<?> updateProfile(@RequestBody User user) {
        if (user.getId() == null) {
            return Result.error("用户ID不能为空");
        }

        // 如果用户修改了密码，可以在这里加加密逻辑（目前咱们是明文，直接存）
        userMapper.updateById(user);

        return Result.success("个人信息修改成功！下次登录生效。");
    }
}