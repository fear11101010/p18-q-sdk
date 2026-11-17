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
 * Data model representing detailed gate access log information from a FeliCa card.
 * <p>
 * This class extends the basic gate access log with additional details about the
 * access event, including more detailed status flags, operator codes, device information,
 * and balance updates. The data follows a multi-byte binary format.
 * </p>
 * 
 * <h3>Key Information:</h3>
 * <ul>
 *   <li>Detailed status flags</li>
 *   <li>Precise timestamp (date and time)</li>
 *   <li>Location (station/stop codes)</li>
 *   <li>Operator and equipment information</li>
 *   <li>Balance before and after transaction</li>
 * </ul>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
public class GateAccessLogInformation {
    /**
     * length 2 byte
     */
    private byte[] statusFlag;
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
    private byte[] currentStationCode;
    /**
     * length 2 byte
     */
    private byte[] currentEquipmentLocationNumber;
    /**
     * length 3 byte
     */
    private byte[] amountOfBasicFare;
    /**
     * length 3 byte
     */
    private byte[] amountOfDistanceFare;
    public static GateAccessLogInformation generateData(byte[] data){
        return GateAccessLogInformation.builder()
                .statusFlag(Arrays.copyOfRange(data,0,2))
                .date(Arrays.copyOfRange(data,2,4))
                .time(Arrays.copyOfRange(data,4,6))
                .currentStationCode(Arrays.copyOfRange(data,6,8))
                .currentEquipmentLocationNumber(Arrays.copyOfRange(data,8,10))
                .amountOfBasicFare(Arrays.copyOfRange(data,10,13))
                .amountOfDistanceFare(Arrays.copyOfRange(data,13,16))
                .build();
    }

    public byte[] getData(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.put(this.statusFlag);
        byteBuffer.put(this.date);
        byteBuffer.put(this.time);
        byteBuffer.put(this.currentStationCode);
        byteBuffer.put(this.currentEquipmentLocationNumber);
        byteBuffer.put(this.amountOfBasicFare);
        byteBuffer.put(this.amountOfDistanceFare);
        return byteBuffer.array();
    }
}
