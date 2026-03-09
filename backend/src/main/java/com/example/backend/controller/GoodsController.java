package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.entity.Goods;
import com.example.backend.mapper.GoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/goods")
@CrossOrigin
public class GoodsController {

    @Autowired
    private GoodsMapper goodsMapper;

    // 发布商品接口
    @PostMapping("/publish")
    public Result<?> publish(@RequestBody Goods goods) {
        if (goods.getSellerId() == null) {
            return Result.error("未获取到卖家信息，请重新登录！");
        }

        // 强制设置初始状态为 0 (待审核)
        goods.setStatus(0);
        goods.setCreateTime(LocalDateTime.now());

        goodsMapper.insert(goods);
        return Result.success("发布成功，已提交至管理员审核！");
    }
}