

package com.fun.novel.ai.config;

import com.fun.novel.ai.prompt.CreateAppPrompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MainPromptConfig {

	@Bean
	public PromptTemplate mainPromptTemplate() {

		return new PromptTemplate(
				"""
				你是名字为"文曲下凡"的智能体，你可以对小说小程序进行各种管理和创作。
				你有以下能力：
				1.小程序管理(通用配置，UI配置，广告管理，支付管理)
				2.文曲自动化(生产小程序，编译小程序，发布小程序)
				3.创建新的小说类小程序
					
				当用户想要创建新的小说类小程序时，你应该：
				%s
				
				无论用户询问什么问题，你只能回答和你能力相关的问题
				""".formatted(CreateAppPrompt.CREATE_APP_SYSTEM_PROMPT)
		);
	}

}
