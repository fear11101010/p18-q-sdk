package com.dtca.busvalidator.busvalidatorsdk.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data model representing device information for bus validator equipment.
 * <p>
 * This class contains identifying and operational information about physical
 * validator devices deployed in buses. It tracks device registration, activation
 * status, and association with operators and equipment types.
 * </p>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DeviceInfo {
    
    /**
     * Unique database identifier for this device.
     */
    private Long id;

    /**
     * Unique serial number of the validator device.
     * <p>
     * This is typically assigned by the manufacturer and used for
     * device identification and tracking.
     * </p>
     */
    private String deviceSerialNumber;

    /**
     * Flag indicating whether this device is currently active.
     * <p>
     * {@code true} if the device is operational and authorized to process transactions,
     * {@code false} if deactivated or suspended.
     * </p>
     */
    private Boolean activeFlag;

    /**
     * Code identifying the transit operator that owns this device.
     * <p>
     * Links the device to a specific bus operator or transit company.
     * </p>
     */
    private String operatorCode;

    /**
     * Classification code for the type of equipment.
     * <p>
     * Indicates whether this is a bus validator, gate terminal,
     * portable reader, or other device type.
     * </p>
     */
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
