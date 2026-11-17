package com.dtca.busvalidator.busvalidatorsdk.model;

import java.nio.ByteBuffer;
import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * Data model representing card issuer information for a FeliCa transit card.
 * <p>
 * This class contains data about the organization that issued the card,
 * including issuer identification codes, initialization data, and dates.
 * This information is used for card validation and inter-operator settlements.
 * </p>
 * 
 * <h3>Issuer Data:</h3>
 * <ul>
 *   <li>Card Issuer ID: Identifies the issuing organization</li>
 *   <li>Equipment Classification: Type of device used for initialization</li>
 *   <li>Initializer ID: Specific device that initialized the card</li>
 *   <li>Issue Date: When the card was first issued</li>
 * </ul>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
public class IssuerInfo {
    /**
     * length 2 byte
     */
    byte[] cardIssuerID;
    /**
     * length 1 byte
     */
    byte equipmentClassificationCode;
    /**
     * length 1 byte
     */
    byte initializerId;
    /**
     * length 2 byte
     */
    byte[] cardIssueDate; // 2
    /**
     * length 1 byte
     */
    byte cardRevision;
    /**
     * length 1 byte
     */
    byte recycleCounter;
    /**
     * length 8 byte
     */
    byte[] cardIssuerPrivateInformation ;  // 8

    public static IssuerInfo generateData(byte[] data){
        return IssuerInfo.builder()
                .cardIssuerID(Arrays.copyOfRange(data,0,2))
                .equipmentClassificationCode(data[2])
                .equipmentClassificationCode(data[3])
                .cardIssueDate(Arrays.copyOfRange(data,4,6))
                .cardRevision(data[6])
                .recycleCounter(data[7])
                .cardIssuerPrivateInformation(Arrays.copyOfRange(data,8,16))
                .build();
    }
    public byte[] getData(){
//        setRecycleCounter((byte) 0x02);
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.put(this.cardIssuerID);
        byteBuffer.put(this.equipmentClassificationCode);
        byteBuffer.put(this.equipmentClassificationCode);
        byteBuffer.put(this.cardIssueDate);
        byteBuffer.put(this.cardRevision);
        byteBuffer.put(this.recycleCounter);
        byteBuffer.put(this.cardIssuerPrivateInformation);
        return byteBuffer.array();
    }
}


