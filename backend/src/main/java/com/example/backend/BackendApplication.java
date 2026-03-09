package com.example.backend; // ⬅️ 这里匹配了你截图中的包名

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
        System.out.println("后端服务启动成功");
    }

    // 这是一个简单的测试接口，用于验证项目是否通了
    @GetMapping("/hello")
    public String hello() {
        return "Hello, 校园闲置交易平台后端已连通！";
    }
}