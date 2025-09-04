package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.knowseek.mapper.FileUploadMapper;
import com.springleaf.knowseek.model.entity.FileUpload;
import com.springleaf.knowseek.model.vo.FileItemVO;
import com.springleaf.knowseek.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final FileUploadMapper fileUploadMapper;

    @Override
    public List<FileItemVO> getFileList() {
        long userId = StpUtil.getLoginIdAsLong();
        List<FileUpload> fileUploads = fileUploadMapper.selectByUserId(userId);
        return List.of();
    }
}
