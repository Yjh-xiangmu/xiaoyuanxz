package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.Orders;
import com.example.backend.entity.Review;
import com.example.backend.entity.User;
import com.example.backend.mapper.OrdersMapper;
import com.example.backend.mapper.ReviewMapper;
import com.example.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/review")
@CrossOrigin
public class ReviewController {

    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private UserMapper userMapper;

    // 提交评价 / 追评
    @PostMapping("/add")
    @Transactional // 开启事务，保证评价、改订单状态、加信誉分同时成功或失败
    public Result<?> addReview(@RequestBody Review review) {
        if (review.getIsAppend() == null) review.setIsAppend(0);
        review.setCreateTime(LocalDateTime.now());

        // 1. 保存评价记录
        reviewMapper.insert(review);

        // 2. 如果是【首次评价】，需要更新订单状态，并计算信誉分
        if (review.getIsAppend() == 0) {
            // 标记订单为已首评
            Orders orderUpdate = new Orders();
            orderUpdate.setId(review.getOrderId());
            orderUpdate.setIsRated(1);
            ordersMapper.updateById(orderUpdate);

            // 🌟 核心信用分算法 🌟
            User seller = userMapper.selectById(review.getSellerId());
            boolean shouldAddScore = false;

            if (review.getRating() == 5) {
                // 收到 1 个 5星，直接加分
                shouldAddScore = true;
            } else if (review.getRating() == 4) {
                // 收到 4星，统计当前一共有多少个 4 星
                QueryWrapper<Review> qw = new QueryWrapper<>();
                qw.eq("seller_id", review.getSellerId()).eq("rating", 4);
                long fourStarCount = reviewMapper.selectCount(qw);

                // 如果凑齐了 5 的倍数，加分
                if (fourStarCount > 0 && fourStarCount % 5 == 0) {
                    shouldAddScore = true;
                }
            }

            // 执行加分逻辑（上限100）
            if (shouldAddScore) {
                BigDecimal currentScore = seller.getCreditScore();
                BigDecimal newScore = currentScore.add(new BigDecimal("0.5"));
                if (newScore.compareTo(new BigDecimal("100")) > 0) {
                    newScore = new BigDecimal("100");
                }
                User updateSeller = new User();
                updateSeller.setId(seller.getId());
                updateSeller.setCreditScore(newScore);
                userMapper.updateById(updateSeller);
            }
        }

        return Result.success(review.getIsAppend() == 1 ? "追评成功！" : "评价成功！感谢您的反馈！");
    }
    // 🌟 获取卖家的历史评价列表（连带买家昵称一起返回）
    @GetMapping("/sellerReviews")
    public Result<java.util.List<java.util.Map<String, Object>>> getSellerReviews(@RequestParam Long sellerId) {
        QueryWrapper<Review> qw = new QueryWrapper<>();
        qw.eq("seller_id", sellerId).orderByDesc("create_time");
        java.util.List<Review> list = reviewMapper.selectList(qw);

        java.util.List<java.util.Map<String, Object>> res = new java.util.ArrayList<>();
        for(Review r : list) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("review", r);
            User buyer = userMapper.selectById(r.getBuyerId());
            map.put("buyerName", buyer != null ? buyer.getUsername() : "匿名买家");
            res.add(map);
        }
        return Result.success(res);
    }
}