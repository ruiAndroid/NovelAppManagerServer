package com.fun.novel.ai.services;

import com.alibaba.cloud.ai.advisor.DocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.fun.novel.ai.advisor.ReasoningContentAdvisor;
import com.fun.novel.ai.tools.CreateAppTool;
import com.fun.novel.ai.tools.UserLocationTool;
import com.fun.novel.ai.tools.WeatherForLocationTool;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

@Service
public class AiChatService {


    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);


    private final ChatClient chatClient;

    private final DashScopeApi dashscopeApi;

    //阿里巴巴百炼知识库是否禁用
    @Value("${spring.ai.alibaba.bailian.enable:false}")
    private Boolean enable;

    //阿里巴巴百炼知识库索引名称
    @Value("${spring.ai.alibaba.bailian.index-name:default-index}")
    private String indexName;

    private final PromptTemplate deepThinkPromptTemplate;

    //针对deepseek-r1的深度思考顾问
    private final ReasoningContentAdvisor reasoningContentAdvisor;

    //集成文档检索
    private DocumentRetrievalAdvisor retrievalAdvisor;

    private final List<ToolCallback> tools;

    public AiChatService(
            DashScopeApi dashscopeApi,
            SimpleLoggerAdvisor simpleLoggerAdvisor,
            MessageChatMemoryAdvisor messageChatMemoryAdvisor,
            @Qualifier("dashScopeChatModel") ChatModel chatModel,
            @Qualifier("mainPromptTemplate") PromptTemplate systemPromptTemplate,
            @Qualifier("deepThinkPromptTemplate") PromptTemplate deepThinkPromptTemplate,
            CreateAppTool createAppTool
    ) {
        // 初始化工具列表
        this.tools = List.of(createAppTool);
        this.dashscopeApi = dashscopeApi;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(
                        systemPromptTemplate.getTemplate()
                ).defaultAdvisors(
                        simpleLoggerAdvisor,
                        messageChatMemoryAdvisor
                ).defaultToolCallbacks(this.tools)
                .build();
        this.deepThinkPromptTemplate = deepThinkPromptTemplate;
        this.reasoningContentAdvisor = new ReasoningContentAdvisor(1);
        

    }

    @PostConstruct
    public void init(){
        if(enable) {
            log.info("Initializing DocumentRetrievalAdvisor with index: {}", indexName);
            this.retrievalAdvisor = new DocumentRetrievalAdvisor(
                    new DashScopeDocumentRetriever(
                            dashscopeApi,
                            DashScopeDocumentRetrieverOptions.builder()
                                    .withIndexName(indexName)
                                    .build()
                    )
            );
        }else {
            log.info("Bailian RAG is disabled, DocumentRetrievalAdvisor will not be initialized");
        }
    }

    public Flux<String> chat(String chatId, String model, String prompt) {

        log.debug("chat model is: {}", model);

        // check if model == "deepseek-r1", output reasoning content.
        if (Objects.equals("deepseek-r1", model)) {
            // add reasoning content advisor.
            chatClient.prompt().advisors(reasoningContentAdvisor);
        }
        var runtimeOptions = DashScopeChatOptions.builder()
                .withModel(model)
                .withTemperature(0.8)
                .withResponseFormat(DashScopeResponseFormat.builder()
                        .type(DashScopeResponseFormat.Type.TEXT)
                        .build()
                ).build();

        ChatClient.ChatClientRequestSpec clientRequestSpec = chatClient.prompt()
                .options(runtimeOptions)
                .user(prompt)
                .advisors(memoryAdvisor -> memoryAdvisor
                        .param(ChatMemory.CONVERSATION_ID, chatId)
                );

        // Only add if enable is true and retrievalAdvisor is initialized
        if (enable && retrievalAdvisor != null) {
            log.debug("Adding DocumentRetrievalAdvisor to chat");
            clientRequestSpec.advisors(retrievalAdvisor);
        }

        try {
            // 直接调用获取完整响应，因为工具调用不支持真正的流式返回
            log.debug("Making direct call to get complete response...");
            var response = clientRequestSpec.call().chatResponse();
            
            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                var generation = response.getResults().get(0);
                var output = generation.getOutput();
                var content = output.getText();
                
                log.debug("Complete response content: '{}', contains newline: {}", content, content.contains("\n"));
                
                // 检查内容是否包含多步骤结果（包含换行符）
                if (content != null && content.contains("\n")) {
                    // 将内容按换行符拆分成多个数据块
                    String[] chunks = content.split("\n");
                    log.debug("Splitting content into {} chunks", chunks.length);
                    
                    // 为每个数据块创建一个延迟的Flux，模拟流式效果
                    return Flux.fromArray(chunks)
                            .filter(chunk -> !chunk.trim().isEmpty()) // 过滤空行
                            .doOnNext(chunk -> log.debug("Sending chunk: '{}'", chunk))
                            .delayElements(java.time.Duration.ofMillis(500)) // 每个数据块延迟500ms发送
                            .concatWith(Flux.just("[DONE]")); // 添加结束标记
                }
                
                // 如果没有换行符，直接返回内容
                return Flux.just(content != null ? content : "")
                        .concatWith(Flux.just("[DONE]"));
            }
            
            // 如果响应为空，返回空结果
            return Flux.just("")
                    .concatWith(Flux.just("[DONE]"));
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return Flux.just("处理聊天请求时出错: " + e.getMessage())
                    .concatWith(Flux.just("[DONE]"));
        }
    }



}
