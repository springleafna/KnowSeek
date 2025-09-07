package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.knowseek.mapper.FileUploadMapper;
import com.springleaf.knowseek.mapper.KnowledgeBaseMapper;
import com.springleaf.knowseek.model.dto.KnowledgeBaseCreateDTO;
import com.springleaf.knowseek.model.dto.KnowledgeBaseUpdateDTO;
import com.springleaf.knowseek.model.entity.FileUpload;
import com.springleaf.knowseek.model.entity.KnowledgeBase;
import com.springleaf.knowseek.model.vo.FileItemVO;
import com.springleaf.knowseek.model.vo.KnowledgeBaseVO;
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
    private final KnowledgeBaseMapper knowledgeBaseMapper;

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

    @Override
    public void createKnowledgeBase(KnowledgeBaseCreateDTO createDTO) {
        long currentUserId = StpUtil.getLoginIdAsLong();
        log.info("用户 [{}] 创建知识库，知识库名称: {}", currentUserId, createDTO.getName());
        knowledgeBaseMapper.insertKnowledgeBase(createDTO.getName(), currentUserId, createDTO.getDescription());
    }

    @Override
    public void deleteKnowledgeBase(Long id) {
        // 这里可以加一步权限校验，确保只有知识库所有者能删除
        knowledgeBaseMapper.deleteKnowledgeBaseById(id);
    }

    @Override
    public List<KnowledgeBaseVO> listKnowledgeBasesByCurrentUser() {
        long currentUserId = StpUtil.getLoginIdAsLong();

        List<KnowledgeBase> entityList = knowledgeBaseMapper.selectAllKnowledgeBase(currentUserId);
        if (entityList.isEmpty()) {
            return Collections.emptyList();
        }

        return entityList.stream().map(entity -> {
            KnowledgeBaseVO vo = new KnowledgeBaseVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateKnowledgeBaseName(KnowledgeBaseUpdateDTO updateDTO) {
        // 这里也可以加权限校验
        knowledgeBaseMapper.updateKnowledgeBaseNameById(updateDTO.getId(), updateDTO.getName(), updateDTO.getDescription());
    }

    @Override
    public KnowledgeBaseVO getKnowledgeBaseById(Long id) {
        KnowledgeBase entity = knowledgeBaseMapper.selectKnowledgeBaseById(id);

        if (entity == null) {
            return null;
        }

        // 创建 VO 实例并使用 BeanUtils 复制属性
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(entity, vo);

        return vo;
    }


}
