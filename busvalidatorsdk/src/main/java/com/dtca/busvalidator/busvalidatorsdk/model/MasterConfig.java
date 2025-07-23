package com.dtca.busvalidator.busvalidatorsdk.model;


import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class MasterConfig {

    private Long id;
    private String configName;
    private String value;
    private String remarks;
}