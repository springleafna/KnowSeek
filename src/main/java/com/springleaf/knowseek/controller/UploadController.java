package com.springleaf.knowseek.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class UploadController {

    /**
     * 处理文件上传请求
     * @param fileToUpload 从请求中获取名为 "fileToUpload" 的文件部分。
     * 这个名称必须与HTML表单中 <input type="file" name="fileToUpload"> 的 name 属性完全一致。
     * @return 返回上传结果的字符串信息
     */
    @PostMapping
    public String handleFileUpload(@RequestParam("fileToUpload") MultipartFile fileToUpload) {
        // 检查上传的文件是否为空
        if (fileToUpload.isEmpty()) {
            return "上传失败：文件为空。";
        }

        try {
            // 获取项目根路径，然后拼接上 "download" 目录
            // user.dir 是标准Java系统属性，代表用户的工作目录，在Spring Boot中通常就是项目根目录
            String uploadDir = System.getProperty("user.dir") + File.separator + "download";

            // 创建表示上传目录的Path对象
            Path uploadPath = Paths.get(uploadDir);

            // 如果目录不存在，则创建它
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 获取原始文件名
            String originalFilename = fileToUpload.getOriginalFilename();
            if (originalFilename == null) {
                return "上传失败：无法获取原始文件名。";
            }

            // 为了防止文件名冲突，可以生成一个唯一的文件名
            // 这里使用 UUID + 原始文件名的后缀
            String fileExtension = "";
            int i = originalFilename.lastIndexOf('.');
            if (i > 0) {
                fileExtension = originalFilename.substring(i);
            }
            String newFileName = UUID.randomUUID().toString() + fileExtension;


            // 创建文件的完整目标路径
            Path filePath = uploadPath.resolve(newFileName);

            // 将上传的文件内容传输到目标文件中
            fileToUpload.transferTo(filePath.toFile());

            // 返回成功信息，包含新文件名和保存路径
            return "文件上传成功！保存在: " + filePath.toString();

        } catch (IOException e) {
            // 捕获并处理IO异常
            e.printStackTrace();
            return "上传失败：服务器发生错误。 " + e.getMessage();
        }
    }
}
