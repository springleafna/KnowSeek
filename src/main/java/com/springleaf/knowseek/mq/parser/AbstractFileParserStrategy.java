package com.springleaf.knowseek.mq.parser;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractFileParserStrategy implements FileParserStrategy {

    protected static final int BUFFER_SIZE = 8192; // 8KB 缓冲区
    protected static final int CHUNK_SIZE = 1000; // 文本块大小
    protected static final int CHUNK_OVERLAP = 100; // 重叠大小

    // 定义分片分隔符优先级
    // 1. 双换行（段落）
    // 2. 单换行
    // 3. 句子结束符（支持中英文常见标点）
    private static final List<String> SEPARATORS = Arrays.asList(
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
     * 核心递归切分逻辑
     *
     * @param text       待切分的文本
     * @param separators 分隔符列表（按优先级排序：段落 -> 换行 -> 句子 -> 空格）
     * @return 切分并合并后的文本块列表
     */
    private List<String> splitTextInternal(String text, List<String> separators) {
        // --- 1. 递归终止条件与强制兜底 ---
        // 如果没有分隔符可用了，或者文本已经足够小了
        if (separators == null || separators.isEmpty()) {
            if (text.length() > CHUNK_SIZE) {
                // 极端情况：文本依然过长且无分隔符（如长代码、乱码），强制按字符切断
                return hardSplit(text);
            } else {
                return Collections.singletonList(text);
            }
        }

        // 取出当前层级的分隔符（例如 "\n\n"）
        String separator = separators.get(0);
        // 准备下一层级的分隔符列表（例如 ["\n", "。", " "]）
        List<String> nextSeparators = separators.subList(1, separators.size());

        // --- 2. 执行切分 ---
        List<String> splits = new ArrayList<>();

        // 判断当前分隔符是否包含正则特殊字符（根据 SEPARATORS 列表特征判断）
        // 列表里："\n\n", "\n", " " 是普通字符； "。|！..." 是正则
        boolean isRegex = separator.contains("|") || separator.contains("\\.") || separator.contains("[");

        if (isRegex) {
            // 【正则模式】：使用 Lookbehind ((?<=...)) 保留标点符号
            // 例如 "你好。再见。" -> ["你好。", "再见。"]
            String[] rawSplits = text.split("(?<=" + separator + ")");
            for (String s : rawSplits) {
                if (!s.trim().isEmpty()) {
                    splits.add(s);
                }
            }
        } else {
            // 【普通字符模式】：使用普通 split，但需要把分隔符加回去
            // split 默认会去掉分隔符，且丢弃末尾空串，使用 limit=-1 保留结构
            String[] rawSplits = text.split(separator, -1);
            for (int i = 0; i < rawSplits.length; i++) {
                String s = rawSplits[i];
                // 只要不是最后一段，或者原文末尾本身就有分隔符，就补上分隔符
                // 注意：这种补全逻辑对于 \n 很有效，保证了格式还原
                if (i < rawSplits.length - 1 || text.endsWith(separator)) {
                    s += separator;
                }
                if (!s.trim().isEmpty()) {
                    splits.add(s);
                }
            }
        }

        // --- 3. 递归检查与处理 ---
        List<String> goodSplits = new ArrayList<>();

        for (String s : splits) {
            if (s.length() < CHUNK_SIZE) {
                // A. 完美情况：切分后的大小符合要求
                goodSplits.add(s);
            } else {
                // B. 依然过大：使用备用分隔符（nextSeparators）进行递归
                if (!nextSeparators.isEmpty()) {
                    // 递归调用自己，尝试用更细粒度的分隔符切这个大块
                    goodSplits.addAll(splitTextInternal(s, nextSeparators));
                } else {
                    // C. 无路可退：没有更细的分隔符了，只能强制硬切
                    goodSplits.addAll(hardSplit(s));
                }
            }
        }

        // --- 4. 组装 (Merge) ---
        // 将切得太细碎的片段，重新拼凑成接近 CHUNK_SIZE 的块
        return mergeSplits(goodSplits);
    }

    /**
     * 最后的手段：按固定长度强制切分字符串
     */
    private List<String> hardSplit(String text) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += CHUNK_SIZE) {
            chunks.add(text.substring(i, Math.min(text.length(), i + CHUNK_SIZE)));
        }
        return chunks;
    }

    /**
     * 合并切分好的小片段，使其长度接近 CHUNK_SIZE
     * 严格控制重叠大小，防止因语义片段过长导致重叠冗余
     */
    private List<String> mergeSplits(List<String> splits) {
        List<String> docs = new ArrayList<>();
        StringBuilder currentDoc = new StringBuilder();

        for (String split : splits) {
            // 如果加入当前片段后超过了块大小，则生成一个 Chunk
            if (currentDoc.length() + split.length() > CHUNK_SIZE) {
                String fullText = currentDoc.toString();

                if (!fullText.trim().isEmpty()) {
                    docs.add(fullText);
                }

                currentDoc.setLength(0); // 清空 buffer

                // 如果上一个文档足够长，截取末尾的 overlap 部分
                if (fullText.length() > CHUNK_OVERLAP) {
                    // 1. 粗暴截取最后 CHUNK_OVERLAP 个字符
                    String tail = fullText.substring(fullText.length() - CHUNK_OVERLAP);

                    // 2. 避免从单词中间切断
                    // 尝试找到截取后第一个空格，从空格后开始，这样开头更干净
                    // 只有当空格在前半部分才切，防止丢掉太多内容
                    int firstSpace = tail.indexOf(' ');
                    if (firstSpace >= 0 && firstSpace < tail.length() / 2) {
                        tail = tail.substring(firstSpace + 1);
                    }

                    currentDoc.append(tail);
                } else {
                    // 如果上文很短，全量保留
                    currentDoc.append(fullText);
                }
            }

            currentDoc.append(split);
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

    /**
     * 通用的队列写入方法（带背压控制）
     * 作用：将分片放入队列，如果队列满则阻塞等待，直到超时。
     *
     * @param queue 目标队列
     * @param item  要放入的元素
     */
    protected <T> void putToQueue(BlockingQueue<T> queue, T item) {
        try {
            boolean success = queue.offer(item, 180, TimeUnit.SECONDS);

            if (!success) {
                String errorMsg = String.format("严重故障：下游消费端处理过慢，队列阻塞超过180秒。已终止解析，防止数据丢失。Item片段前20字符: %s",
                        item.toString().substring(0, Math.min(item.toString().length(), 20)));
                log.error(errorMsg);
                // 抛出异常，让整个任务失败，而不是静默丢数据
                throw new RuntimeException("系统过载，文件解析中止");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("队列写入被中断");
            throw new RuntimeException("任务被中断", e);
        }
    }
}
