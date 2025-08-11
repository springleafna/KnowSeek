package com.springleaf.knowseek.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class FileUploadCompleteDTO {

    String fileName;
    String uploadId;
    List<PartETagInfo> partETagInfos;

    @Data
    @NoArgsConstructor
    public static class PartETagInfo {
        private Integer partNumber;
        @JsonProperty("eTag") // 明确指定 JSON 字段名为 "eTag"
        private String eTag;
    }
}
