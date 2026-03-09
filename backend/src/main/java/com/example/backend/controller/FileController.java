package com.example.backend.controller;

import com.example.backend.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
@CrossOrigin
public class FileController {

    // 读取物理保存路径
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 上传接口
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        try {
            // 1. 获取文件的原始名称 (例如：测试.jpg)
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                return Result.error("文件格式不正确");
            }

            // 2. 提取文件后缀名 (例如：.jpg)
            String extName = originalFilename.substring(originalFilename.lastIndexOf("."));

            // 3. 生成一个新的唯一文件名，防止中文乱码和同名文件覆盖 (例如：a1b2c3d4.jpg)
            String newFileName = UUID.randomUUID().toString().replace("-", "") + extName;

            // 4. 检查物理文件夹是否存在，不存在则创建
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 5. 将文件保存到目标物理路径
            File targetFile = new File(uploadDir + newFileName);
            file.transferTo(targetFile);

            // 6. 拼接可以直接在浏览器访问的网络 URL
            String url = "http://localhost:8080/uploads/" + newFileName;

            return Result.success(url);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }
}