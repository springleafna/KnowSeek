package com.springleaf.knowseek.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件类型判断工具类
 */
@Slf4j
public class FileUtil {

    /**
     * 根据文件名获取文件扩展名 .xxx
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        int lastSeparatorIndex = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

        // 确保点号在最后一个路径分隔符之后（处理类似 "path/to.file/name" 的情况）
        if (lastDotIndex > lastSeparatorIndex && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex).toLowerCase();
        }

        return "";
    }

    /**
     * 提取文件扩展名
     *
     * @param fileName 文件名
     * @return 小写的文件扩展名，如果没有扩展名则返回null
     */
    public static String extractFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return null;
        }

        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 从URL中提取文件名
     */
    public static String extractFileNameFromUrl(String url) {
        try {
            String[] parts = url.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            log.warn("无法从URL提取文件名: {}", url);
            return "unknown_file";
        }
    }

    /**
     * 根据文件扩展名获取文件类型描述
     *
     * @param extension 文件扩展名
     * @return 文件类型描述
     */
    private static String getFileTypeDescription(String extension) {
        if (extension == null) {
            return "unknown";
        }

        // 根据文件扩展名返回文件类型
        switch (extension.toLowerCase()) {
            case "pdf":
                return "PDF文档";
            case "doc":
            case "docx":
                return "Word文档";
            case "xls":
            case "xlsx":
                return "Excel表格";
            case "ppt":
            case "pptx":
                return "PowerPoint演示文稿";
            case "txt":
                return "文本文件";
            case "rtf":
                return "富文本文档";
            case "md":
                return "Markdown文档";
            case "odt":
                return "OpenDocument文本";
            case "ods":
                return "OpenDocument表格";
            case "odp":
                return "OpenDocument演示文稿";
            case "html":
            case "htm":
                return "HTML文档";
            case "xml":
                return "XML文档";
            case "json":
                return "JSON文件";
            case "csv":
                return "CSV文件";
            case "epub":
                return "EPUB电子书";
            case "pages":
                return "Apple Pages文档";
            case "numbers":
                return "Apple Numbers表格";
            case "keynote":
                return "Apple Keynote演示文稿";
            case "jpg":
            case "jpeg":
                return "JPEG图片";
            case "png":
                return "PNG图片";
            case "gif":
                return "GIF图片";
            case "bmp":
                return "BMP图片";
            case "svg":
                return "SVG图片";
            case "mp4":
                return "MP4视频";
            case "avi":
                return "AVI视频";
            case "mov":
                return "MOV视频";
            case "mp3":
                return "MP3音频";
            case "wav":
                return "WAV音频";
            case "zip":
                return "ZIP压缩包";
            case "rar":
                return "RAR压缩包";
            case "7z":
                return "7Z压缩包";
            default:
                return extension.toUpperCase() + "文件";
        }
    }

    /**
     * 支持的文档类型扩展名（可以被Apache Tika解析并向量化的文件类型）
     */
    private static final Set<String> SUPPORTED_DOCUMENT_EXTENSIONS = new HashSet<>(Arrays.asList(
            // 文档类型
            "pdf",          // PDF文档
            "doc", "docx",  // Microsoft Word文档
            "xls", "xlsx",  // Microsoft Excel表格
            "ppt", "pptx",  // Microsoft PowerPoint演示文稿
            "txt",          // 纯文本文件
            "rtf",          // 富文本格式
            "md",           // Markdown文档

            // OpenDocument格式
            "odt",          // OpenDocument文本文档
            "ods",          // OpenDocument电子表格
            "odp",          // OpenDocument演示文稿

            // 网页和标记语言
            "html", "htm",  // HTML文档
            "xml",          // XML文档
            "json",         // JSON文件
            "csv",          // CSV文件

            // 电子书格式
            "epub",         // EPUB电子书

            // 其他文档格式
            "pages",        // Apple Pages文档
            "numbers",      // Apple Numbers表格
            "keynote"       // Apple Keynote演示文稿
    ));

    /**
     * 不支持的文件类型扩展名（无法有效解析文本内容的文件类型）
     */
    private static final Set<String> UNSUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
            // 图片文件
            "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "tiff", "ico", "psd",

            // 音频文件
            "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a",

            // 视频文件
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm", "m4v", "3gp",

            // 压缩包
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz",

            // 可执行文件
            "exe", "msi", "dmg", "pkg", "deb", "rpm",

            // 字体文件
            "ttf", "otf", "woff", "woff2", "eot",

            // CAD文件
            "dwg", "dxf", "step", "iges",

            // 数据库文件
            "db", "sqlite", "mdb", "accdb",

            // 其他二进制文件
            "bin", "dat", "iso", "img"
    ));

}
