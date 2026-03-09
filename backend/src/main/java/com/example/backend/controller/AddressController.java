package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.Address;
import com.example.backend.mapper.AddressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/address")
@CrossOrigin
public class AddressController {

    @Autowired
    private AddressMapper addressMapper;

    // 获取当前用户的所有地址列表 (默认地址排在最前面)
    @GetMapping("/list")
    public Result<List<Address>> getList(@RequestParam Long userId) {
        QueryWrapper<Address> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).orderByDesc("is_default").orderByDesc("create_time");
        return Result.success(addressMapper.selectList(queryWrapper));
    }

    // 添加或修改地址
    @PostMapping("/save")
    public Result<?> save(@RequestBody Address address) {
        if (address.getUserId() == null) return Result.error("未获取到用户信息");

        // 如果用户勾选了"设为默认地址"，需要先把该用户其他的地址都改成非默认(0)
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            UpdateWrapper<Address> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("user_id", address.getUserId()).set("is_default", 0);
            addressMapper.update(null, updateWrapper);
        }

        if (address.getId() == null) {
            address.setCreateTime(LocalDateTime.now());
            addressMapper.insert(address);
        } else {
            addressMapper.updateById(address);
        }
        return Result.success("保存地址成功");
    }

    // 删除地址
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        addressMapper.deleteById(id);
        return Result.success("删除成功");
    }
}