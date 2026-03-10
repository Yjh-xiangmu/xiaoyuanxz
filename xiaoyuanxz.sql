/*
 Navicat Premium Data Transfer

 Source Server         : yjh
 Source Server Type    : MySQL
 Source Server Version : 80026
 Source Host           : localhost:3306
 Source Schema         : xiaoyuanxz

 Target Server Type    : MySQL
 Target Server Version : 80026
 File Encoding         : 65001

 Date: 10/03/2026 13:53:07
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for address
-- ----------------------------
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `receiver` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收件人姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系电话',
  `detail_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '详细地址(如：XX校区X区X栋XXX室)',
  `is_default` tinyint NULL DEFAULT 0 COMMENT '是否默认地址：1-是，0-否',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '收货地址表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of address
-- ----------------------------
INSERT INTO `address` VALUES (1, 3, '李四', '15903366978', '翻斗花园', 1, '2026-03-09 23:03:16');
INSERT INTO `address` VALUES (2, 1, '张三', '13900000000', '龙湖校区菜鸟驿站', 1, '2026-03-09 23:27:42');

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `goods_id` bigint NULL DEFAULT NULL COMMENT '关联的商品ID(用于发送商品卡片)',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `is_read` tinyint NULL DEFAULT 0 COMMENT '状态：0-未读, 1-已读',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '聊天消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_message
-- ----------------------------
INSERT INTO `chat_message` VALUES (1, 3, 1, 5, '你好\n', 1, '2026-03-09 23:14:20');
INSERT INTO `chat_message` VALUES (2, 1, 3, NULL, ' 你好\n', 1, '2026-03-09 23:20:06');
INSERT INTO `chat_message` VALUES (3, 3, 1, 5, '你好你哦豁\n', 1, '2026-03-10 00:50:38');
INSERT INTO `chat_message` VALUES (4, 3, 1, NULL, '你好\n', 1, '2026-03-10 00:54:35');

-- ----------------------------
-- Table structure for goods
-- ----------------------------
DROP TABLE IF EXISTS `goods`;
CREATE TABLE `goods`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `seller_id` bigint NOT NULL COMMENT '卖家用户ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '商品详细描述',
  `price` decimal(10, 2) NOT NULL COMMENT '售卖价格',
  `original_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '原价',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类：数码,书籍,生活,服饰,其他',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品图片URL(暂存外链或相对路径)',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态: 0-待审核, 1-审核通过(上架), 2-审核驳回(需补全), 3-违规下架, 4-已售出',
  `reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核驳回原因/修改建议',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '发布时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of goods
-- ----------------------------
INSERT INTO `goods` VALUES (1, 1, '测试2', '测试的', 99.00, 299.00, '生活', 'http://localhost:8080/uploads/bfdc37b9d4ce47dfb4677130fb0a402a.jpg', 4, '', '2026-03-09 19:17:23');
INSERT INTO `goods` VALUES (2, 1, '测试2', '测试的', 1.00, 99.00, '数码', 'http://localhost:8080/uploads/7869e7305fd04c48be86ecee6b6e7c9a.jpg', 2, '补全商品信息', '2026-03-09 21:43:43');
INSERT INTO `goods` VALUES (3, 1, '测试3', '测试3', 99.00, 299.00, '数码', 'http://localhost:8080/uploads/351d2cdbcfd94015acf4f88afdda2dc0.jpg', 1, '商品违禁，涉嫌引流', '2026-03-09 21:44:41');
INSERT INTO `goods` VALUES (4, 3, '测试的', '测试沙盒', 199.00, 2999.00, '数码', 'http://localhost:8080/uploads/ca17b2ed4514482888b8bde1243848e6.jpg,http://localhost:8080/uploads/47488024508b42569656650b1568cd1c.jpg', 4, '', '2026-03-09 22:30:43');
INSERT INTO `goods` VALUES (5, 1, '小米17PRO 99新', '99新，仅激活三天。', 4500.00, 5500.00, '数码', 'http://localhost:8080/uploads/e2b42e7bbf5e481f8ac170b41afda83a.jpg,http://localhost:8080/uploads/aefaf2228b5b4a85a040bfb098cbecc1.jpg,http://localhost:8080/uploads/dc9a31bc63534a409807ac130b2aa611.jpg,http://localhost:8080/uploads/baca9bcb388946ef904f3247fc0c2323.jpg,http://localhost:8080/uploads/32a54f5fb5484f04977bd5629d33a054.jpg', 4, '', '2026-03-09 22:45:40');
INSERT INTO `goods` VALUES (6, 1, '测试4', '测试的', 99.00, 199.00, '其他', 'http://localhost:8080/uploads/bba55a728b3a4698874ba74dd2943457.jpg,http://localhost:8080/uploads/784caf1538f947f8b5e077740c2d3c97.png', 1, '', '2026-03-10 12:47:07');

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '接收通知的用户ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知详细内容',
  `is_read` tinyint NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notification
-- ----------------------------
INSERT INTO `notification` VALUES (1, 1, '⚠️ 商品需补全信息', '您发布的商品《测试2》被驳回。管理员备注：补全商品信息。请修改后重新提交。', 1, '2026-03-09 21:44:16');
INSERT INTO `notification` VALUES (2, 1, '🚨 商品违规下架及扣分通知', '您发布的商品《测试3》因严重违规被强制下架！管理员提醒：商品违禁，涉嫌引流。系统已扣除您的信誉分：1 分。', 1, '2026-03-09 21:45:23');
INSERT INTO `notification` VALUES (3, 3, '✅ 商品审核通过', '您发布的商品《测试的》已通过审核，正式上架展示！', 1, '2026-03-09 22:31:06');
INSERT INTO `notification` VALUES (4, 1, '✅ 商品审核通过', '您发布的商品《小米17PRO 99新》已通过审核，正式上架展示！', 1, '2026-03-09 22:46:04');
INSERT INTO `notification` VALUES (5, 3, '📦 订单发货通知', '您购买的订单 [ORD-BB2DCAA5A8] 卖家已发货，请注意查收！', 1, '2026-03-09 23:45:19');
INSERT INTO `notification` VALUES (6, 1, '💰 交易完成通知', '您的订单 [ORD-BB2DCAA5A8] 买家已确认收货，交易圆满完成！', 1, '2026-03-10 00:14:43');
INSERT INTO `notification` VALUES (7, 3, '📦 订单发货通知', '您购买的订单 [ORD-A313174FAA] 卖家已发货，请注意查收！', 0, '2026-03-10 00:55:26');
INSERT INTO `notification` VALUES (8, 1, '💰 交易完成通知', '您的订单 [ORD-A313174FAA] 买家已确认收货，交易圆满完成！', 0, '2026-03-10 00:55:53');
INSERT INTO `notification` VALUES (9, 1, '🚨 账号违规封禁通知', '您的账号因违反平台规定，已被管理员强制封禁！封禁期间将无法使用平台核心功能。如有异议请联系管理员。', 0, '2026-03-10 01:49:22');
INSERT INTO `notification` VALUES (10, 1, '✅ 账号解封通知', '您的账号已解除限制，恢复正常使用，请自觉遵守平台规范。', 0, '2026-03-10 01:49:41');
INSERT INTO `notification` VALUES (11, 1, '🚨 账号违规封禁通知', '您的账号因违反平台规定，已被管理员强制封禁！封禁期间将无法使用平台核心功能。如有异议请联系管理员。', 0, '2026-03-10 01:51:17');
INSERT INTO `notification` VALUES (12, 1, '✅ 账号解封通知', '您的账号已解除限制，恢复正常使用，请自觉遵守平台规范。', 0, '2026-03-10 01:51:28');
INSERT INTO `notification` VALUES (13, 1, '✅ 商品审核通过', '您发布的商品《测试4》已通过审核，正式上架展示！', 0, '2026-03-10 12:47:44');

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '系统生成的唯一订单号',
  `goods_id` bigint NOT NULL COMMENT '购买的商品ID',
  `buyer_id` bigint NOT NULL COMMENT '买家ID',
  `seller_id` bigint NOT NULL COMMENT '卖家ID',
  `amount` decimal(10, 2) NOT NULL COMMENT '实付金额',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '详细地址(下单时的快照)',
  `status` tinyint NULL DEFAULT 0 COMMENT '订单状态：0-待支付, 1-已支付待发货, 2-已发货, 3-已完成, 4-已取消',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '下单时间',
  `pay_time` datetime(0) NULL DEFAULT NULL COMMENT '支付时间',
  `receiver` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '收件人(下单时的快照)',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系电话(下单时的快照)',
  `is_rated` tinyint NULL DEFAULT 0 COMMENT '评价状态: 0-未评, 1-已评(可追评)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orders
-- ----------------------------
INSERT INTO `orders` VALUES (1, 'ORD-BB2DCAA5A8', 1, 3, 1, 99.00, '翻斗花园', 3, '2026-03-09 22:19:55', '2026-03-09 22:48:12', '李四', '15903366978', 1);
INSERT INTO `orders` VALUES (2, 'ORD-D2179E4CD3', 4, 1, 3, 199.00, '龙湖校区菜鸟驿站', 0, '2026-03-09 22:31:19', NULL, '张三', '13900000000', 0);
INSERT INTO `orders` VALUES (3, 'ORD-A313174FAA', 5, 3, 1, 4500.00, '翻斗花园', 3, '2026-03-10 00:50:49', '2026-03-10 00:53:35', '李四', '15903366978', 1);

-- ----------------------------
-- Table structure for review
-- ----------------------------
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '关联的订单ID',
  `goods_id` bigint NOT NULL COMMENT '关联的商品ID',
  `buyer_id` bigint NOT NULL COMMENT '买家ID',
  `seller_id` bigint NOT NULL COMMENT '卖家ID',
  `rating` tinyint NULL DEFAULT NULL COMMENT '星级1-5，追评为0或null',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评价内容',
  `is_append` tinyint NULL DEFAULT 0 COMMENT '是否追评：0-首次评价，1-追评',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品评价记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of review
-- ----------------------------
INSERT INTO `review` VALUES (1, 1, 1, 3, 1, 5, '非常好！！', 0, '2026-03-10 00:19:37');
INSERT INTO `review` VALUES (2, 3, 5, 3, 1, 5, '好', 0, '2026-03-10 00:56:00');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名/学号(登录用)',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'USER' COMMENT '角色：USER-普通用户, ADMIN-管理员',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名(实名认证)',
  `id_card` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '身份证号(实名认证)',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号(实名认证)',
  `credit_score` decimal(4, 1) NULL DEFAULT 100.0 COMMENT '信誉分，默认100',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-正常, 0-限制登录封禁',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '注册时间',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户头像外链地址',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, '2024001', '123456', 'USER', '张三', '410727200102056649', '15914789658', 100.0, 1, '2026-03-09 18:13:54', 'http://localhost:8080/uploads/89add51575dc49e896b290e88bcc436a.jpg');
INSERT INTO `user` VALUES (2, 'admin', '123456', 'ADMIN', NULL, NULL, NULL, 100.0, 1, '2026-03-09 18:30:51', NULL);
INSERT INTO `user` VALUES (3, '2024002', '123456', 'USER', '李四', '410727199002065109', '13800000000', 100.0, 1, '2026-03-09 18:49:32', NULL);

SET FOREIGN_KEY_CHECKS = 1;
