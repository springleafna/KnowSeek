package com.springleaf.knowseek.mq.parser;

import java.util.*;

public abstract class AbstractFileParserStrategy implements FileParserStrategy {

    protected static final int BUFFER_SIZE = 8192; // 8KB 缓冲区
    protected static final int CHUNK_SIZE = 1000; // 文本块大小
    protected static final int CHUNK_OVERLAP = 100; // 重叠大小

    // 定义分片分隔符优先级
    // 1. 双换行（段落）
    // 2. 单换行
    // 3. 句子结束符（支持中英文常见标点）
    protected static final List<String> SEPARATORS = Arrays.asList(
            "\n\n",
            "\n",
            "。|！|？|\\.|\\!|\\?", // 正则表达式
            " "
    );

    /**
     * 核心分片方法：递归语义分片
     */
    protected List<String> splitTextRecursively(String text) {
        return splitTextInternal(text, SEPARATORS);
    }

    /**
     * 内部递归方法
     */
    protected List<String> splitTextInternal(String text, List<String> separators) {
        String separator = separators.getFirst(); // 当前使用的分隔符
        List<String> nextSeparators = separators.subList(1, separators.size()); // 剩余的分隔符

        // 1. 按照当前分隔符分割文本
        List<String> splits = new ArrayList<>();

        // 处理正则表达式特殊字符
        boolean isRegex = separator.contains("|") || separator.contains("\\");

        if (isRegex) {
            // 如果是正则（句子分隔符），我们需要保留分隔符本身（比如句号）
            // 使用 lookbehind 技巧或者手动分割来保留标点
            // 简单起见，这里使用 split 但可能会丢失标点，为了严谨建议使用 Matcher
            // 这里为了性能和代码简洁，使用简单的正则分割，若需保留标点可优化
            String[] rawSplits = text.split("(?<=" + separator + ")");
            Collections.addAll(splits, rawSplits);
        } else {
            String[] rawSplits = text.split(separator);
            for (String s : rawSplits) {
                if(!s.trim().isEmpty()) {
                    splits.add(s + (separator.equals("\n\n") || separator.equals("\n") ? separator : ""));
                }
            }
        }

        // 2. 合并碎片，形成最终的 Chunks
        List<String> goodSplits = new ArrayList<>();

        for (String s : splits) {
            if (s.length() < CHUNK_SIZE) {
                goodSplits.add(s);
            } else {
                // 如果当前碎片依然过大，且还有更细粒度的分隔符，则递归处理
                if (!nextSeparators.isEmpty()) {
                    goodSplits.addAll(splitTextInternal(s, nextSeparators));
                } else {
                    // 如果没有分隔符了，只能硬切（原样保留或按字符切）
                    goodSplits.add(s);
                }
            }
        }

        // 3. 组装 Chunks (Sliding Window with Overlap)
        return mergeSplits(goodSplits, separator);
    }

    /**
     * 合并切分好的小片段，使其长度接近 CHUNK_SIZE，并保持重叠
     */
    private List<String> mergeSplits(List<String> splits, String separator) {
        List<String> docs = new ArrayList<>();
        StringBuilder currentDoc = new StringBuilder();

        // 用于构建重叠部分的队列
        Deque<String> overlapWindow = new LinkedList<>();
        int currentOverlapSize = 0;

        for (String split : splits) {
            // 如果加入当前片段后超过了块大小
            if (currentDoc.length() + split.length() > CHUNK_SIZE) {
                if (!currentDoc.isEmpty()) {
                    String doc = currentDoc.toString().trim();
                    if (!doc.isEmpty()) {
                        docs.add(doc);
                    }

                    // 这里的核心优化：重叠的内容是基于之前的“完整语义片段”构建的，而不是硬切字符
                    currentDoc.setLength(0);

                    // 从 overlapWindow 中恢复内容作为新 chunk 的开头
                    // 这里的 overlapWindow 存的是完整的句子或段落
                    for (String s : overlapWindow) {
                        currentDoc.append(s);
                    }
                }
            }

            currentDoc.append(split);

            // --- 维护重叠窗口 ---
            overlapWindow.addLast(split);
            currentOverlapSize += split.length();

            // 如果窗口过大（超过了设定的重叠大小），移除最早的片段
            // 这样保证 overlapWindow 里始终保留着最近的约 CHUNK_OVERLAP 长度的完整语义片段
            while (currentOverlapSize > CHUNK_OVERLAP && overlapWindow.size() > 1) {
                // 保留至少一个，防止死循环
                String removed = overlapWindow.removeFirst();
                currentOverlapSize -= removed.length();
            }
        }

        // 处理最后一个块
        if (!currentDoc.isEmpty()) {
            String doc = currentDoc.toString().trim();
            if (!doc.isEmpty()) {
                docs.add(doc);
            }
        }

        return docs;
    }
}
