package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.Notification;
import com.example.backend.mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@CrossOrigin
public class NotificationController {

    @Autowired
    private NotificationMapper notificationMapper;

    // 获取当前用户的通知列表
    @GetMapping("/list")
    public Result<List<Notification>> getList(@RequestParam Long userId) {
        QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
        // 按照创建时间倒序，最新的消息在最上面
        queryWrapper.eq("user_id", userId).orderByDesc("create_time");
        return Result.success(notificationMapper.selectList(queryWrapper));
    }

    // 获取未读消息数量 (用于顶部导航栏的小红点)
    @GetMapping("/unreadCount")
    public Result<Long> getUnreadCount(@RequestParam Long userId) {
        QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("is_read", 0);
        return Result.success(notificationMapper.selectCount(queryWrapper));
    }

    // 将消息标记为已读
    @PostMapping("/read/{id}")
    public Result<?> markAsRead(@PathVariable Long id) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setIsRead(1); // 1 代表已读
        notificationMapper.updateById(notification);
        return Result.success();
    }
}