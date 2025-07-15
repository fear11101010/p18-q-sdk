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


