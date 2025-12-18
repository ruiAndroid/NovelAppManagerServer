package com.fun.novel.ai.utils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.HashSet;

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
        int lastSentenceEnd = -1;
        
        // 查找最后一个句子结束标点
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
        
        // 没有找到完整句子，返回空字符串
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
