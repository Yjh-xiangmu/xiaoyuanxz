package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.ChatMessage;
import com.example.backend.entity.User;
import com.example.backend.mapper.ChatMessageMapper;
import com.example.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private UserMapper userMapper;

    // 1. 获取两个用户之间的历史聊天记录
    @GetMapping("/history")
    public Result<List<ChatMessage>> getHistory(@RequestParam Long user1, @RequestParam Long user2) {
        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper ->
                wrapper.eq("sender_id", user1).eq("receiver_id", user2)
                        .or()
                        .eq("sender_id", user2).eq("receiver_id", user1)
        );
        queryWrapper.orderByAsc("create_time");
        return Result.success(chatMessageMapper.selectList(queryWrapper));
    }

    // 🌟 2. 新增：获取当前用户的“私信会话列表”
    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> getSessions(@RequestParam Long userId) {
        // 查出所有与我相关的聊天记录（按时间倒序）
        QueryWrapper<ChatMessage> query = new QueryWrapper<>();
        query.eq("sender_id", userId).or().eq("receiver_id", userId);
        query.orderByDesc("create_time");
        List<ChatMessage> allMessages = chatMessageMapper.selectList(query);

        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> processedUserIds = new HashSet<>();

        // 遍历消息，提取出每一个不同的聊天对象（只取他们之间的最后一条消息）
        for (ChatMessage msg : allMessages) {
            Long partnerId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();

            // 如果这个聊天对象还没被处理过，就加入列表（因为是倒序查的，所以第一次遇到的一定是最新消息）
            if (!processedUserIds.contains(partnerId)) {
                processedUserIds.add(partnerId);
                User partner = userMapper.selectById(partnerId);

                Map<String, Object> sessionInfo = new HashMap<>();
                sessionInfo.put("partnerId", partnerId);
                sessionInfo.put("partnerName", partner != null ? partner.getUsername() : "未知用户");
                sessionInfo.put("latestMessage", msg.getContent());
                sessionInfo.put("updateTime", msg.getCreateTime());

                // 计算对方发给我、且我还没读的消息数量
                QueryWrapper<ChatMessage> unreadQ = new QueryWrapper<>();
                unreadQ.eq("sender_id", partnerId).eq("receiver_id", userId).eq("is_read", 0);
                sessionInfo.put("unreadCount", chatMessageMapper.selectCount(unreadQ));

                result.add(sessionInfo);
            }
        }
        return Result.success(result);
    }

    // 🌟 3. 新增：将与某个人的聊天记录全部标记为已读
    @PostMapping("/readAll")
    public Result<?> readAll(@RequestParam Long senderId, @RequestParam Long receiverId) {
        UpdateWrapper<ChatMessage> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("sender_id", senderId).eq("receiver_id", receiverId).set("is_read", 1);
        chatMessageMapper.update(null, updateWrapper);
        return Result.success();
    }
}