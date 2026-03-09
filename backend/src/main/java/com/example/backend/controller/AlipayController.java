package com.example.backend.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.example.backend.entity.Orders;
import com.example.backend.mapper.OrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import com.alipay.api.internal.util.AlipaySignature;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Map;
@RestController
@RequestMapping("/api/alipay")
@CrossOrigin
public class AlipayController {

    @Value("${alipay.app-id}")
    private String appId;
    @Value("${alipay.private-key}")
    private String privateKey;
    @Value("${alipay.alipay-public-key}")
    private String alipayPublicKey;
    @Value("${alipay.gateway-url}")
    private String gatewayUrl;
    @Value("${alipay.return-url}")
    private String returnUrl;

    @Autowired
    private OrdersMapper ordersMapper;

    // 发起支付的接口
    @GetMapping("/pay")
    public String pay(@RequestParam String orderNo) throws AlipayApiException {
        // 1. 根据订单号查询订单信息
        QueryWrapper<Orders> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);
        Orders order = ordersMapper.selectOne(queryWrapper);

        if (order == null) {
            return "订单不存在！";
        }

        // 2. 初始化支付宝客户端 (套路代码，官方文档规定的)
        AlipayClient alipayClient = new DefaultAlipayClient(
                gatewayUrl, appId, privateKey, "json", "UTF-8", alipayPublicKey, "RSA2");

        // 3. 创建支付请求对象
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        // 设置支付成功后的同步跳转地址
        request.setReturnUrl(returnUrl);
        // 异步通知地址（由于是本地电脑运行，支付宝外网无法直接访问你的 localhost，这里暂不设置，我们等下用同步跳转来修改订单状态）
        request.setNotifyUrl("");

        // 4. 组装订单数据 (必须转成 JSON 字符串)
        String subject = "校园闲置交易平台-订单支付";
        String totalAmount = order.getAmount().toString(); // 订单金额

        request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\","
                + "\"total_amount\":\"" + totalAmount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        // 5. 执行请求，拿到支付宝返回的 HTML 表单代码
        String form = alipayClient.pageExecute(request).getBody();

        // 6. 直接把这段 HTML 返回给前端，前端一接收到就会自动渲染并跳转到支付宝！
        return form;
    }
    // 🌟 新增：支付成功后的同步回调接口
    @GetMapping("/return")
    public void payReturn(@RequestParam Map<String, String> params, HttpServletResponse response) throws Exception {
        // 1. 调用支付宝官方 SDK 进行 RSA2 验签，确保数据安全
        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2");

        if (signVerified) {
            // 验签通过，获取支付宝传回来的我们自己的订单号
            String orderNo = params.get("out_trade_no");

            // 2. 去数据库查这个订单
            QueryWrapper<Orders> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("order_no", orderNo);
            Orders order = ordersMapper.selectOne(queryWrapper);

            // 3. 如果订单还是待支付状态(0)，就改为已支付(1)
            if (order != null && order.getStatus() == 0) {
                order.setStatus(1);
                order.setPayTime(LocalDateTime.now());
                ordersMapper.updateById(order);
            }

            // 4. 状态改完后，后端命令浏览器重定向到前端的“我的订单”页面
            response.sendRedirect("http://localhost:5173/user/orders");
        } else {
            // 验签失败，说明有人伪造支付成功请求！
            response.setContentType("text/html;charset=utf-8");
            response.getWriter().println("<h1>安全拦截：支付宝验签失败！</h1>");
        }
    }
}