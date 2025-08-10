package com.springleaf.knowseek.controller;

import com.springleaf.knowseek.model.dto.FileUploadChunkDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @PostMapping("/chunk")
    public ResponseEntity<Map<String, Object>> uploadChunk(@RequestBody FileUploadChunkDTO fileUploadChunkDTO) {


    }



}
