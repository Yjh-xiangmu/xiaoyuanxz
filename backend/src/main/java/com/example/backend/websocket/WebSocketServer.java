package com.example.backend.websocket;

import com.alibaba.fastjson.JSON;
import com.example.backend.entity.ChatMessage;
import com.example.backend.mapper.ChatMessageMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/{userId}")
@Component
public class WebSocketServer {

    // 记录所有在线的客户端，Key 是 userId，Value 是对应的 Session 建立的连接
    private static final ConcurrentHashMap<Long, Session> onlineSessions = new ConcurrentHashMap<>();

    // 解决 WebSocket 多例模式下无法直接 @Autowired 注入 Mapper 的问题
    private static ChatMessageMapper chatMessageMapper;

    @Autowired
    public void setChatMessageMapper(ChatMessageMapper chatMessageMapper) {
        WebSocketServer.chatMessageMapper = chatMessageMapper;
    }

    // 建立连接成功调用
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        onlineSessions.put(userId, session);
        System.out.println("用户 " + userId + " 上线了，当前在线人数：" + onlineSessions.size());
    }

    // 关闭连接时调用
    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        onlineSessions.remove(userId);
        System.out.println("用户 " + userId + " 下线了，当前在线人数：" + onlineSessions.size());
    }

    // 收到前端发送的消息时调用
    @OnMessage
    public void onMessage(String message, Session session, @PathParam("userId") Long userId) {
        System.out.println("收到用户 " + userId + " 的消息：" + message);

        // 1. 将前端传来的 JSON 字符串解析成对象
        ChatMessage chatMsg = JSON.parseObject(message, ChatMessage.class);
        chatMsg.setSenderId(userId);
        chatMsg.setCreateTime(LocalDateTime.now());
        chatMsg.setIsRead(0); // 默认未读

        // 2. 判断接收方是否在线
        Session receiverSession = onlineSessions.get(chatMsg.getReceiverId());
        if (receiverSession != null && receiverSession.isOpen()) {
            // 接收方在线，直接通过 WebSocket 把消息发过去
            try {
                // 转成 JSON 字符串发给对方
                receiverSession.getBasicRemote().sendText(JSON.toJSONString(chatMsg));
                // 如果能实时发过去，可以视作已送达（这里根据业务可设为1已读，或仍为0等点开才变1）
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("用户 " + chatMsg.getReceiverId() + " 不在线，消息存入离线");
        }

        // 3. 无论对方在不在线，聊天记录都必须存入数据库
        chatMessageMapper.insert(chatMsg);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("WebSocket 发生错误！");
        error.printStackTrace();
    }
}