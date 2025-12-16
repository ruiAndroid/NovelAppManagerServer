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
                .build()
                ;
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

        return clientRequestSpec.stream().content();
    }



}
