package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.Notification;
import com.example.backend.entity.Orders;
import com.example.backend.entity.Review;
import com.example.backend.entity.User;
import com.example.backend.mapper.NotificationMapper;
import com.example.backend.mapper.OrdersMapper;
import com.example.backend.mapper.ReviewMapper;
import com.example.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Autowired
    private NotificationMapper notificationMapper;

    // 1. 买家：提交评价 / 追评
    @PostMapping("/add")
    @Transactional
    public Result<?> addReview(@RequestBody Review review) {
        if (review.getIsAppend() == null) review.setIsAppend(0);
        review.setCreateTime(LocalDateTime.now());

        reviewMapper.insert(review);

        if (review.getIsAppend() == 0) {
            Orders orderUpdate = new Orders();
            orderUpdate.setId(review.getOrderId());
            orderUpdate.setIsRated(1);
            ordersMapper.updateById(orderUpdate);

            // 核心信用分算法
            User seller = userMapper.selectById(review.getSellerId());
            boolean shouldAddScore = false;

            if (review.getRating() == 5) {
                shouldAddScore = true;
            } else if (review.getRating() == 4) {
                QueryWrapper<Review> qw = new QueryWrapper<>();
                qw.eq("seller_id", review.getSellerId()).eq("rating", 4);
                long fourStarCount = reviewMapper.selectCount(qw);
                if (fourStarCount > 0 && fourStarCount % 5 == 0) {
                    shouldAddScore = true;
                }
            }

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

    // 2. 卖家主页：获取卖家的历史评价列表
    @GetMapping("/sellerReviews")
    public Result<List<Map<String, Object>>> getSellerReviews(@RequestParam Long sellerId) {
        QueryWrapper<Review> qw = new QueryWrapper<>();
        qw.eq("seller_id", sellerId).orderByDesc("create_time");
        List<Review> list = reviewMapper.selectList(qw);

        List<Map<String, Object>> res = new ArrayList<>();
        for(Review r : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("review", r);
            User buyer = userMapper.selectById(r.getBuyerId());
            String bName = "匿名买家";
            if (buyer != null) {
                bName = (buyer.getRealName() != null && !buyer.getRealName().trim().isEmpty())
                        ? buyer.getRealName() + " (" + buyer.getUsername() + ")"
                        : buyer.getUsername();
            }
            map.put("buyerName", bName);
            res.add(map);
        }
        return Result.success(res);
    }

    // 🌟 3. 管理员：获取全平台所有评价
    @GetMapping("/adminList")
    public Result<List<Map<String, Object>>> getAdminList(@RequestParam(required = false) String keyword) {
        QueryWrapper<Review> qw = new QueryWrapper<>();
        qw.orderByDesc("create_time");
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.like("content", keyword);
        }
        List<Review> list = reviewMapper.selectList(qw);

        List<Map<String, Object>> res = new ArrayList<>();
        for(Review r : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("review", r);
            User buyer = userMapper.selectById(r.getBuyerId());
            User seller = userMapper.selectById(r.getSellerId());

            String bName = "未知买家";
            if (buyer != null) {
                bName = (buyer.getRealName() != null && !buyer.getRealName().trim().isEmpty()) ? buyer.getRealName() + " (" + buyer.getUsername() + ")" : buyer.getUsername();
            }
            String sName = "未知卖家";
            if (seller != null) {
                sName = (seller.getRealName() != null && !seller.getRealName().trim().isEmpty()) ? seller.getRealName() + " (" + seller.getUsername() + ")" : seller.getUsername();
            }

            map.put("buyerName", bName);
            map.put("sellerName", sName);
            res.add(map);
        }
        return Result.success(res);
    }

    // 🌟 4. 管理员：一键屏蔽违规评价
    @PostMapping("/shield/{id}")
    public Result<?> shieldReview(@PathVariable Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) return Result.error("评价不存在");

        // 核心逻辑：把恶毒语言替换成官方屏蔽文本
        review.setContent("【此评价因包含违规/不当言论，已被管理员屏蔽】");
        reviewMapper.updateById(review);

        // 给发差评的买家发个系统警告通知
        Notification msg = new Notification();
        msg.setUserId(review.getBuyerId());
        msg.setTitle("🚨 评价违规屏蔽通知");
        msg.setContent("您在订单中的一条评价因包含不当言论或广告，已被管理员强制屏蔽！请遵守社区发言规范，多次违规将面临封号风险！");
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(msg);

        return Result.success("该评价已成功屏蔽并警告该用户！");
    }
}