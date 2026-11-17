package com.dtca.busvalidator.busvalidatorsdk.model;

import java.nio.ByteBuffer;
import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
/**
 * Data model representing a single gate access log entry from a FeliCa card.
 * <p>
 * This class stores information about an individual access event at a transit gate,
 * including the timestamp, status, and location information. The data follows a
 * fixed binary format for FeliCa card storage.
 * </p>
 * 
 * <h3>Usage:</h3>
 * <p>
 * Gate access logs are used to track entry and exit events for fare calculation,
 * validation, and audit purposes.
 * </p>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
public class GateAccessLog {
    /**
     * length 2 byte
     */
    private byte[] status;
    /**
     * length 2 byte
     */
    private byte[] date;
    /**
     * length 2 byte
     */
    private byte[] time;
    /**
     * length 2 byte
     */
    private byte[] stationCode;
    /**
     * length 2 byte
     */
    private byte[] equipmentLocation;
    /**
     * length 3 byte
     */
    private byte[] baseFareAmount;
    /**
     * length 3 byte
     */
    private byte[] distanceFareAmount;

    public static GateAccessLog generateData(byte[] data){
        return GateAccessLog.builder()
                .status(Arrays.copyOfRange(data,0,2))
                .date(Arrays.copyOfRange(data,2,4))
                .time(Arrays.copyOfRange(data,4,6))
                .stationCode(Arrays.copyOfRange(data,6,8))
                .equipmentLocation(Arrays.copyOfRange(data,8,10))
                .baseFareAmount(Arrays.copyOfRange(data,10,13))
                .distanceFareAmount(Arrays.copyOfRange(data,13,16))
                .build();
    }

    public byte[] getData(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.put(this.status);
        byteBuffer.put(this.date);
        byteBuffer.put(this.time);
        byteBuffer.put(this.stationCode);
        byteBuffer.put(this.equipmentLocation);
        byteBuffer.put(this.baseFareAmount);
        byteBuffer.put(this.distanceFareAmount);
        return byteBuffer.array();
    }
}
