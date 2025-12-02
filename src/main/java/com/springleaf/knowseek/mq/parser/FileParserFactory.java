package com.springleaf.knowseek.mq.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文件解析工厂
 */
@Component
@RequiredArgsConstructor
public class FileParserFactory {

    private final Map<String, FileParserStrategy> parserMap;

    /**
     * 根据文件类型获取对应的文件解析器
     * @param type 文件类型（对应@Component的值）
     * @return 文件解析器
     */
    public FileParserStrategy getParser(String type) {
        FileParserStrategy parser = parserMap.get(type);
        if (parser == null) {
            return parserMap.get("default");
        }
        return parser;
    }

    /**
     * 根据文件后缀获取解析器
     */
    public FileParserStrategy getParserByExtension(String extension) {
        // 将后缀转为小写，统一处理
        String type = extension.toLowerCase();
        return getParser(type);
    }
}
