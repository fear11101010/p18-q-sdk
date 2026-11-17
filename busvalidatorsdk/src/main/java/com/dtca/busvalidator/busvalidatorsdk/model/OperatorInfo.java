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
 * Data model representing transit operator information from a FeliCa card transaction.
 * <p>
 * This class stores details about the transit operator and location where a
 * transaction occurred. It links transactions to specific operators, equipment,
 * and physical locations within the transit network.
 * </p>
 * 
 * <h3>Operator Data:</h3>
 * <ul>
 *   <li>Operator Code: Identifies the transit company</li>
 *   <li>Equipment Class: Type of validator device</li>
 *   <li>Station Code: Physical location identifier</li>
 *   <li>Service Classification: Type of service (bus, rail, etc.)</li>
 * </ul>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
public class OperatorInfo {
    /**
     * length 2 byte
     */
    byte[] operatorCode;
    /**
     * length 1 byte
     */
    byte equipmentClass;
    /**
     * length 2 byte
     */
    byte[] stationCode;
    /**
     * length 2 byte
     */
    byte[] equipmentLocation;	//3
    /**
     * length 2 byte
     */
    byte[] activationDate;	//4
    /**
     * length 1 byte
     */
    byte statusFlag;
    /**
     * length 1 byte
     */
    byte paymentMethod;
    /**
     * length 2 byte
     */
    byte[] depositAmount;
    /**
     * length 3 byte
     */
    byte[] reserved;

    public static OperatorInfo generateData(byte[] data){
        return OperatorInfo.builder()
                .operatorCode(Arrays.copyOfRange(data,0,2))
                .equipmentClass(data[2])
                .stationCode(Arrays.copyOfRange(data,3,5))
                .equipmentLocation(Arrays.copyOfRange(data,5,7))
                .activationDate(Arrays.copyOfRange(data,7,9))
                .statusFlag(data[9])
                .paymentMethod(data[10])
                .depositAmount(Arrays.copyOfRange(data,11,13))
                .reserved(Arrays.copyOfRange(data,13,16))
                .build();
    }
    public byte[] getData(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.put(this.operatorCode);
        byteBuffer.put(this.equipmentClass);
        byteBuffer.put(this.stationCode);
        byteBuffer.put(this.equipmentLocation);
        byteBuffer.put(this.activationDate);
        byteBuffer.put(this.statusFlag);
        byteBuffer.put(this.paymentMethod);
        byteBuffer.put(this.depositAmount);
        byteBuffer.put(this.reserved);
        return byteBuffer.array();
    }
}
