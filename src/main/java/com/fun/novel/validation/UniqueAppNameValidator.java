package com.fun.novel.validation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fun.novel.entity.NovelApp;
import com.fun.novel.mapper.NovelAppMapper;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UniqueAppNameValidator implements ConstraintValidator<UniqueAppName, String> {

    @Autowired
    private NovelAppMapper novelAppMapper;

    @Override
    public boolean isValid(String appName, ConstraintValidatorContext context) {
        if (appName == null) {
            return true; // 让 @NotBlank 处理空值验证
        }
        LambdaQueryWrapper<NovelApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelApp::getAppName, appName);
        return novelAppMapper.selectCount(wrapper) == 0;
    }
}