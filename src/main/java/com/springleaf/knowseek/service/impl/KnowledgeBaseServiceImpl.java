package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.knowseek.enums.UploadStatusEnum;
import com.springleaf.knowseek.exception.BusinessException;
import com.springleaf.knowseek.mapper.mysql.FileUploadMapper;
import com.springleaf.knowseek.mapper.mysql.KnowledgeBaseMapper;
import com.springleaf.knowseek.mapper.mysql.UserMapper;
import com.springleaf.knowseek.model.dto.KnowledgeBaseCreateDTO;
import com.springleaf.knowseek.model.dto.KnowledgeBaseUpdateDTO;
import com.springleaf.knowseek.model.entity.FileUpload;
import com.springleaf.knowseek.model.entity.KnowledgeBase;
import com.springleaf.knowseek.model.vo.FileItemVO;
import com.springleaf.knowseek.model.vo.KnowledgeBaseVO;
import com.springleaf.knowseek.service.KnowledgeBaseService;
import com.springleaf.knowseek.utils.FileUtil;
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
    private final UserMapper userMapper;

    @Override
    public List<FileItemVO> getFileList(Long id) {
        List<FileUpload> fileUploads = fileUploadMapper.selectByKnowledgeBaseId(id);
        if (fileUploads.isEmpty()) {
            return Collections.emptyList();
        }

        return fileUploads.stream()
                .map(fileUpload -> {
                    FileItemVO fileItemVO = new FileItemVO();
                    BeanUtils.copyProperties(fileUpload, fileItemVO);

                    // 根据文件名设置文件类型
                    fileItemVO.setType(FileUtil.extractFileExtension(fileUpload.getFileName()));

                    // 格式化文件大小
                    fileItemVO.setTotalSize(FileUtil.formatFileSize(fileUpload.getTotalSize()));

                    // 转换状态
                    UploadStatusEnum statusEnum = UploadStatusEnum.getByStatus(fileUpload.getStatus());
                    fileItemVO.setStatus(statusEnum != null ? statusEnum.getDescription() : "未知状态");

                    // 设置知识库名称
                    fileItemVO.setKnowledgeBaseName(knowledgeBaseMapper.getNameById(fileUpload.getKnowledgeBaseId()));

                    return fileItemVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void createKnowledgeBase(KnowledgeBaseCreateDTO createDTO) {
        long currentUserId = StpUtil.getLoginIdAsLong();
        log.info("用户 [{}] 创建知识库，知识库名称: {}", currentUserId, createDTO.getName());
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(createDTO.getName());
        knowledgeBase.setUserId(currentUserId);
        knowledgeBase.setDescription(createDTO.getDescription());
        knowledgeBaseMapper.insertKnowledgeBase(knowledgeBase);
    }

    @Override
    public void deleteKnowledgeBase(Long id) {
        // 确保只有知识库所有者能删除
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectKnowledgeBaseById(id);
        if (knowledgeBase == null) {
            throw new BusinessException("该知识库不存在");
        }
        if (knowledgeBase.getUserId() != StpUtil.getLoginIdAsLong()) {
            throw new BusinessException("只有知识库所有者能删除该知识库");
        }
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
        knowledgeBaseMapper.updateKnowledgeBaseNameById(updateDTO.getId(), updateDTO.getName(), updateDTO.getDescription());
    }

    @Override
    public KnowledgeBaseVO getKnowledgeBaseById(Long id) {
        KnowledgeBase entity = knowledgeBaseMapper.selectKnowledgeBaseById(id);

        if (entity == null) {
            return null;
        }
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(entity, vo);

        return vo;
    }

    @Override
    public void setPrimaryKnowledgeBase(Long id) {
        if (knowledgeBaseMapper.selectKnowledgeBaseById(id) == null) {
            throw new BusinessException("该知识库不存在");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        userMapper.setPrimaryKnowledgeBaseId(id, userId);
    }


}
