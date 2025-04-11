package com.fun.novel.service;

import com.fun.novel.entity.NovelWeijuDeliver;
import java.util.List;

public interface NovelWeijuDeliverService {
    NovelWeijuDeliver addDeliver(NovelWeijuDeliver deliver);
    NovelWeijuDeliver updateDeliver(NovelWeijuDeliver deliver);
    void deleteDeliver(Integer id);
    NovelWeijuDeliver getDeliverByDeliverId(String deliverId);
    boolean deleteByDeliverId(String deliverId);
} 