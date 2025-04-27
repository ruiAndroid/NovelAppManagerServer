package com.fun.novel.service;

import com.fun.novel.entity.AppWeijuDeliver;

public interface AppWeijuDeliverService {
    AppWeijuDeliver addDeliver(AppWeijuDeliver deliver);
    AppWeijuDeliver updateDeliver(AppWeijuDeliver deliver);
    void deleteDeliver(Integer id);
    AppWeijuDeliver getDeliverByDeliverId(String deliverId);
    boolean deleteByDeliverId(String deliverId);
} 