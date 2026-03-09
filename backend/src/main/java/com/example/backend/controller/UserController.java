package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/user")
@CrossOrigin // 🌟解决前端Vue的跨域请求问题，必加！
public class UserController {

    @Autowired
    private UserMapper userMapper;

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
}