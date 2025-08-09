package com.springleaf.knowseek.controller;

import com.springleaf.knowseek.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/file")
@CrossOrigin
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initUpload(@RequestBody Map<String, Object> params) {
        String fileName = (String) params.get("fileName");
        String fileMd5 = (String) params.get("fileMd5");

        String redisKey = "upload:" + fileMd5;
        String localPath = (String) redisTemplate.opsForValue().get(redisKey);
        if (localPath != null && new File(localPath).exists()) {
            String fileUrl = "/api/file/download?fileName=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "msg", "秒传成功",
                    "data", Map.of("url", fileUrl)
            ));
        }

        String uploadId = UUID.randomUUID().toString();
        int totalChunks = (int) Math.ceil(((Integer) params.get("fileSize")) / (double) 5_000_000);

        Map<String, Object> info = new HashMap<>();
        info.put("uploadId", uploadId);
        info.put("fileName", fileName);
        redisTemplate.opsForValue().set(redisKey, info, java.time.Duration.ofHours(24));

        Map<String, Object> data = new HashMap<>();
        data.put("uploadId", uploadId);
        data.put("fileName", fileName);
        data.put("totalChunks", totalChunks);
        data.put("fileMd5", fileMd5);

        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }

    @PostMapping("/upload-chunk")
    public ResponseEntity<?> uploadChunk(
            @RequestParam String uploadId,
            @RequestParam String fileName,
            @RequestParam Integer chunkIndex,
            @RequestParam Integer totalChunks,
            @RequestPart("chunk") MultipartFile chunk) {

        try {
            fileService.saveChunk(uploadId, fileName, chunkIndex, chunk.getInputStream());
            return ResponseEntity.ok(Map.of("code", 200, "msg", "分片上传成功"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("code", 500, "msg", "上传失败：" + e.getMessage()));
        }
    }

    @GetMapping("/uploaded-chunks")
    public ResponseEntity<Set<Integer>> getUploadedChunks(@RequestParam String uploadId) {
        Set<Integer> chunks = fileService.getUploadedChunks(uploadId);
        return ResponseEntity.ok(chunks);
    }

    @PostMapping("/complete")
    public ResponseEntity<?> completeUpload(@RequestBody Map<String, Object> params) {
        String uploadId = (String) params.get("uploadId");
        String fileName = (String) params.get("fileName");
        Integer totalChunks = ((Number) params.get("totalChunks")).intValue();

        try {
            String finalPath = fileService.mergeChunks(uploadId, fileName, totalChunks);
            String fileUrl = "/api/file/download?fileName=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);

            String fileMd5 = (String) params.get("fileMd5");
            redisTemplate.opsForValue().set("upload:" + fileMd5, finalPath, java.time.Duration.ofDays(7));

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "data", Map.of("url", fileUrl)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("code", 500, "msg", "合并失败：" + e.getMessage()));
        }
    }

    @GetMapping("/download")
    public void downloadFile(@RequestParam String fileName, HttpServletResponse response) throws IOException {
        File file = new File(fileService.getFileConfig().getUploadDir(), fileName);
        if (!file.exists()) {
            response.setStatus(404);
            return;
        }
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));
        java.nio.file.Files.copy(file.toPath(), response.getOutputStream());
        response.flushBuffer();
    }
}