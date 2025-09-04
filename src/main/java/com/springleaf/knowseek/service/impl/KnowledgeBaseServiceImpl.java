package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.knowseek.mapper.FileUploadMapper;
import com.springleaf.knowseek.model.entity.FileUpload;
import com.springleaf.knowseek.model.vo.FileItemVO;
import com.springleaf.knowseek.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final FileUploadMapper fileUploadMapper;

    @Override
    public List<FileItemVO> getFileList() {
        long userId = StpUtil.getLoginIdAsLong();
        List<FileUpload> fileUploads = fileUploadMapper.selectByUserId(userId);
        if (fileUploads.isEmpty()) {
            return Collections.emptyList();
        }

        return fileUploads.stream()
                .map(fileUpload -> {
                    FileItemVO fileItemVO = new FileItemVO();
                    BeanUtils.copyProperties(fileUpload, fileItemVO);
                    return fileItemVO;
                })
                .collect(Collectors.toList());
    }
}
