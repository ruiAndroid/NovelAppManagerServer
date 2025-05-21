package com.fun.novel.service;

import com.fun.novel.entity.AppWeijuDeliver;
import java.util.List;

public interface AppWeijuDeliverService {
    AppWeijuDeliver addDeliver(AppWeijuDeliver deliver);
    List<AppWeijuDeliver> getDeliverList();
    AppWeijuDeliver updateDeliver(AppWeijuDeliver deliver);
    AppWeijuDeliver getDeliverByDeliverId(String deliverId);
    boolean deleteByDeliverId(String deliverId);
} 