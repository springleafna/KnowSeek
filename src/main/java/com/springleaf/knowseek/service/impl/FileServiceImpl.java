package com.springleaf.knowseek.service.impl;

import com.springleaf.knowseek.config.FileConfig;
import com.springleaf.knowseek.service.FileService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileConfig fileConfig;

    public void ensureUploadDir() {
        File dir = new File(fileConfig.getUploadDir());
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void saveChunk(String uploadId, String fileName, int chunkIndex, InputStream inputStream) throws IOException {
        ensureUploadDir();
        String chunkDir = fileConfig.getUploadDir() + "/" + uploadId;
        File dir = new File(chunkDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File chunkFile = new File(chunkDir, "chunk." + chunkIndex);
        try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
            IOUtils.copy(inputStream, fos);
        }
    }

    public Set<Integer> getUploadedChunks(String uploadId) {
        Set<Integer> uploaded = new HashSet<>();
        String chunkDir = fileConfig.getUploadDir() + "/" + uploadId;
        File dir = new File(chunkDir);
        if (!dir.exists()) return uploaded;

        File[] files = dir.listFiles((d, name) -> name.startsWith("chunk."));
        if (files != null) {
            for (File f : files) {
                try {
                    String suffix = f.getName().split("\\.")[1];
                    uploaded.add(Integer.parseInt(suffix));
                } catch (Exception e) {
                    // 忽略非法文件名
                }
            }
        }
        return uploaded;
    }

    public String mergeChunks(String uploadId, String fileName, int totalChunks) throws IOException {
        String chunkDir = fileConfig.getUploadDir() + "/" + uploadId;
        String targetPath = fileConfig.getUploadDir() + "/" + fileName;

        try (FileOutputStream fos = new FileOutputStream(targetPath)) {
            for (int i = 0; i < totalChunks; i++) {
                File chunk = new File(chunkDir, "chunk." + i);
                if (!chunk.exists()) {
                    throw new RuntimeException("分片丢失: " + i);
                }
                try (FileInputStream fis = new FileInputStream(chunk)) {
                    IOUtils.copy(fis, fos);
                }
            }
        }

        deleteChunkDir(uploadId);
        return targetPath;
    }

    public void deleteChunkDir(String uploadId) {
        String chunkDir = fileConfig.getUploadDir() + "/" + uploadId;
        File dir = new File(chunkDir);
        if (dir.exists()) {
            Arrays.stream(dir.listFiles()).forEach(File::delete);
            dir.delete();
        }
    }

    public FileConfig getFileConfig() {
        return fileConfig;
    }
}