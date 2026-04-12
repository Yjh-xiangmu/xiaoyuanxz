package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.Announcement;
import com.example.backend.mapper.AnnouncementMapper;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/announcement") // 补充了 /api 前缀，与你的系统规范保持一致
@CrossOrigin // 补充了跨域注解
public class AnnouncementController {

    @Resource
    private AnnouncementMapper announcementMapper;

    // 添加公告
    @PostMapping("/add")
    public Result add(@RequestBody Announcement announcement) {
        announcementMapper.insert(announcement);
        return Result.success();
    }

    // 删除公告
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        announcementMapper.deleteById(id);
        return Result.success();
    }

    // 更新公告
    @PutMapping("/update")
    public Result update(@RequestBody Announcement announcement) {
        announcementMapper.updateById(announcement);
        return Result.success();
    }

    // 查询公告列表（按时间倒序，最新的在前面）
    @GetMapping("/list")
    public Result list() {
        QueryWrapper<Announcement> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        List<Announcement> list = announcementMapper.selectList(queryWrapper);
        return Result.success(list);
    }
}