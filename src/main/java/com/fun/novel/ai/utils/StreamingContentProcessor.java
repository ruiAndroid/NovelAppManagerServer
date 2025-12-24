package com.fun.novel.ai.utils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * 流式内容处理器，用于将AI流式输出的片段合并为完整的语义单元
 * 支持配置句子结束标点符号，确保只输出完整的句子或段落
 */
public class StreamingContentProcessor {

    // 默认的中文句子结束标点
    private static final Set<Character> DEFAULT_CHINESE_END_PUNCTUATION = new HashSet<>();
    
    static {
        DEFAULT_CHINESE_END_PUNCTUATION.add('。');
        DEFAULT_CHINESE_END_PUNCTUATION.add('！');
        DEFAULT_CHINESE_END_PUNCTUATION.add('？');
        DEFAULT_CHINESE_END_PUNCTUATION.add('\n');
    }
    
    // 自定义的结束标点符号集合
    private final Set<Character> endPunctuation;
    
    // 缓冲区，用于存储不完整的句子片段
    private final AtomicReference<StringBuilder> buffer;
    
    // 特殊标记对的配置
    private static final Map<String, String> TAG_PAIRS = new HashMap<>();
    
    // 静态初始化标记对
    static {
        // 基础配置JSON标记对
        TAG_PAIRS.put("jsonStart-createNovelApp-baseConfig", "jsonEnd-createNovelApp-baseConfig");
        // 可以在这里添加更多标记对
    }
    
    // 所有标记的正则表达式
    private static Pattern ALL_TAGS_PATTERN;
    
    // 静态初始化正则表达式
    static {
        StringBuilder regexBuilder = new StringBuilder();
        for (String tag : TAG_PAIRS.keySet()) {
            if (regexBuilder.length() > 0) {
                regexBuilder.append("|");
            }
            regexBuilder.append(Pattern.quote(tag));
            regexBuilder.append("|");
            regexBuilder.append(Pattern.quote(TAG_PAIRS.get(tag)));
        }
        ALL_TAGS_PATTERN = Pattern.compile(regexBuilder.toString());
    }
    
    /**
     * 使用默认中文标点符号的构造函数
     */
    public StreamingContentProcessor() {
        this(DEFAULT_CHINESE_END_PUNCTUATION);
    }
    
    /**
     * 使用自定义标点符号集合的构造函数
     * 
     * @param endPunctuation 句子结束标点符号集合
     */
    public StreamingContentProcessor(Set<Character> endPunctuation) {
        this.endPunctuation = endPunctuation != null ? endPunctuation : DEFAULT_CHINESE_END_PUNCTUATION;
        this.buffer = new AtomicReference<>(new StringBuilder());
    }
    
    /**
     * 添加新的标记对到处理列表中
     * 
     * @param startTag 开始标记
     * @param endTag 结束标记
     * @return 当前StreamingContentProcessor实例，支持链式调用
     */
    public static void addTagPair(String startTag, String endTag) {
        if (startTag != null && endTag != null && !startTag.isEmpty() && !endTag.isEmpty()) {
            TAG_PAIRS.put(startTag, endTag);
            // 更新正则表达式
            rebuildTagsPattern();
        }
    }
    
    /**
     * 重新构建标记正则表达式
     */
    private static synchronized void rebuildTagsPattern() {
        StringBuilder regexBuilder = new StringBuilder();
        for (String tag : TAG_PAIRS.keySet()) {
            if (regexBuilder.length() > 0) {
                regexBuilder.append("|");
            }
            regexBuilder.append(Pattern.quote(tag));
            regexBuilder.append("|");
            regexBuilder.append(Pattern.quote(TAG_PAIRS.get(tag)));
        }
        ALL_TAGS_PATTERN = Pattern.compile(regexBuilder.toString());
    }
    
    /**
     * 处理单个流式内容片段
     * 
     * @param content 流式内容片段
     * @return 完整的句子字符串，如果没有完整句子则返回空字符串
     */
    public String process(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        // 将新内容添加到缓冲区
        buffer.get().append(content);
        
        // 获取缓冲区内容
        String bufferContent = buffer.get().toString();
        
        // 检查是否包含任何完整的标记对
        boolean hasCompleteTagPair = false;
        int lastTagEndPos = -1;
        
        // 遍历所有标记对
        for (Map.Entry<String, String> tagPair : TAG_PAIRS.entrySet()) {
            String startTag = tagPair.getKey();
            String endTag = tagPair.getValue();
            
            int startTagIndex = bufferContent.indexOf(startTag);
            int endTagIndex = bufferContent.indexOf(endTag);
            
            // 如果找到完整的标记对
            if (startTagIndex != -1 && endTagIndex != -1) {
                // 更新最后一个标记结束位置
                int currentEndPos = endTagIndex + endTag.length();
                if (currentEndPos > lastTagEndPos) {
                    lastTagEndPos = currentEndPos;
                }
                hasCompleteTagPair = true;
            }
            // 如果只包含开始标记，暂时不输出
            else if (startTagIndex != -1) {
                return "";
            }
            // 如果只包含结束标记（不完整的标记对），输出直到结束标记
            else if (endTagIndex != -1) {
                int endTagPos = endTagIndex + endTag.length();
                String completeContent = bufferContent.substring(0, endTagPos);
                buffer.set(new StringBuilder(bufferContent.substring(endTagPos)));
                return completeContent;
            }
        }
        
        // 如果找到完整的标记对，输出到最后一个标记结束位置
        if (hasCompleteTagPair && lastTagEndPos != -1) {
            String completeContent = bufferContent.substring(0, lastTagEndPos);
            buffer.set(new StringBuilder(bufferContent.substring(lastTagEndPos)));
            return completeContent;
        }
        
        // 普通文本处理，查找最后一个句子结束标点
        int lastSentenceEnd = -1;
        for (int i = bufferContent.length() - 1; i >= 0; i--) {
            char c = bufferContent.charAt(i);
            if (endPunctuation.contains(c)) {
                lastSentenceEnd = i;
                break;
            }
        }
        
        // 如果找到完整句子
        if (lastSentenceEnd != -1) {
            String completeSentences = bufferContent.substring(0, lastSentenceEnd + 1);
            // 更新缓冲区，只保留剩余的不完整句子
            buffer.set(new StringBuilder(bufferContent.substring(lastSentenceEnd + 1)));
            return completeSentences;
        }
        
        // 没有找到完整的输出单元，返回空字符串
        return "";
    }
    
    /**
     * 获取缓冲区中剩余的不完整内容
     * 
     * @return 剩余的不完整内容
     */
    public String getRemainingContent() {
        return buffer.get().toString();
    }
    
    /**
     * 清空缓冲区
     */
    public void clear() {
        buffer.set(new StringBuilder());
    }
    
    /**
     * 处理Flux流中的所有内容片段
     * 
     * @param contentFlux 内容片段的Flux流
     * @return 包含完整句子的Flux流
     */
    public static Flux<String> processFlux(Flux<String> contentFlux) {
        return processFlux(contentFlux, DEFAULT_CHINESE_END_PUNCTUATION);
    }
    
    /**
     * 处理Flux流中的所有内容片段（自定义标点符号）
     * 
     * @param contentFlux 内容片段的Flux流
     * @param endPunctuation 句子结束标点符号集合
     * @return 包含完整句子的Flux流
     */
    public static Flux<String> processFlux(Flux<String> contentFlux, Set<Character> endPunctuation) {
        StreamingContentProcessor processor = new StreamingContentProcessor(endPunctuation);
        
        return contentFlux
                .map(processor::process)
                .filter(chunk -> !chunk.trim().isEmpty())
                // 添加最后一个不完整的句子
                .concatWith(Mono.defer(() -> {
                    String remaining = processor.getRemainingContent();
                    return remaining.isEmpty() ? Mono.empty() : Mono.just(remaining);
                }));
    }
}
