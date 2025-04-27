package com.fun.novel.dto;

import com.fun.novel.entity.AppWeijuDeliver;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeliverAppResponse {
    private String ap;
    private List<AppWeijuDeliver> ad_list = new ArrayList<>();

    public static DeliverAppResponse fromDeliver(AppWeijuDeliver deliver) {
        DeliverAppResponse response = new DeliverAppResponse();
        response.setAp(deliver.getDeliverId());
        response.getAd_list().add(deliver);
        return response;
    }
} 