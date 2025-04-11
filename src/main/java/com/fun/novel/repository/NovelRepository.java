package com.fun.novel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fun.novel.entity.NovelApp; // 假设实体类为 Novel
import org.springframework.stereotype.Repository;

@Repository
public interface NovelRepository extends BaseMapper<NovelApp> {
    /**
     * 自定义查询方法示例，可以根据实际需求进行调整或删除。
     */

}