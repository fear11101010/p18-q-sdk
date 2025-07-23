package com.dtca.busvalidator.busvalidatorsdk.model;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DeviceInfo {
    private Long id;

//    @SerializedName("device_serial_number")
    private String deviceSerialNumber;

//    @SerializedName("active_flag")
    private Boolean activeFlag;

//    @SerializedName("operator_code")
    private String operatorCode;

//    @SerializedName("equipment_classification_code")
    private String equipmentClassificationCode;

    private String[] pairedEquipmentLocationNumber;

//    @SerializedName("station_code")
    private String stationCode;

//    @SerializedName("equipment_location_number")
    private String equipmentLocationNumber;

//    @SerializedName("ip_address")
    private String ipAddress;

//    @SerializedName("port")
    private Integer port;

//    @SerializedName("download_path")
    private String downloadPath;

//    @SerializedName("upload_path")
    private String uploadPath;

}
