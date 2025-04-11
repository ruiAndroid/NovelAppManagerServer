package com.fun.novel.dto;

import com.fun.novel.entity.NovelWeijuDeliver;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeliverAppResponse {
    private String ap;
    private List<NovelWeijuDeliver> ad_list = new ArrayList<>();

    public static DeliverAppResponse fromDeliver(NovelWeijuDeliver deliver) {
        DeliverAppResponse response = new DeliverAppResponse();
        response.setAp(deliver.getDeliverId());
        response.getAd_list().add(deliver);
        return response;
    }
} 