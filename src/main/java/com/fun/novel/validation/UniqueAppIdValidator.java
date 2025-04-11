package com.fun.novel.validation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.mapper.NovelAppMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class UniqueAppIdValidator implements ConstraintValidator<UniqueAppId, String> {

    @Autowired
    private NovelAppMapper novelAppMapper;

    @Override
    public boolean isValid(String appId, ConstraintValidatorContext context) {
        if (appId == null) {
            return true; // 让 @NotBlank 处理空值验证
        }

        // 获取当前请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            // 判断是否是更新操作
            if (request.getRequestURI().contains("/update")) {
                return true; // 如果是更新操作，跳过验证
            }
        }

        // 新增操作时验证唯一性
        LambdaQueryWrapper<NovelApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelApp::getAppid, appId);
        return novelAppMapper.selectCount(wrapper) == 0;
    }
} 