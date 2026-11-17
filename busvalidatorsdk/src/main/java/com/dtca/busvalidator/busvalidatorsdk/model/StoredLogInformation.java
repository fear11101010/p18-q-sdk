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
 * Data model representing stored value transaction log information from a FeliCa card.
 * <p>
 * This class contains detailed information about a single stored value (e-purse)
 * transaction, including the service type, timestamp, location, balance changes,
 * and transaction identifiers. The data follows a fixed binary format for
 * FeliCa card storage.
 * </p>
 * 
 * <h3>Transaction Details:</h3>
 * <ul>
 *   <li>Equipment and service classification codes</li>
 *   <li>Transaction context (entry, exit, reload, etc.)</li>
 *   <li>Payment method used</li>
 *   <li>Date and time of transaction</li>
 *   <li>Location codes (place1, place2)</li>
 *   <li>Card balance after transaction</li>
 *   <li>Unique stored value log ID</li>
 * </ul>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
public class StoredLogInformation {

    private byte equipmentClassificationCode; // BIN 1
    private byte serviceClassificationCode;   // BIN 1
    private byte contextCode;                 // BIN 1
    private byte paymentMethodCode;           // BIN 1

    private byte[] date;                       // BIN 2
    private byte time;                        // BIN 1

    private byte[] place1;                     // BIN 2
    private byte[] place2;                     // BIN 2

    private byte[] cardBalance;                  // BIN 3 (handled as 3 bytes)

    private byte[] storedValueLogId;           // BIN 2

    public static StoredLogInformation generateData(byte[] data){
        return StoredLogInformation.builder()
                .equipmentClassificationCode(data[0])
                .serviceClassificationCode(data[1])
                .contextCode(data[2])
                .paymentMethodCode(data[3])
                .date(Arrays.copyOfRange(data,4,6))
                .time(data[6])
                .place1(Arrays.copyOfRange(data,7,9))
                .place2(Arrays.copyOfRange(data,9,11))
                .cardBalance(Arrays.copyOfRange(data,11,14))
                .storedValueLogId(Arrays.copyOfRange(data,14,16))
                .build();
    }

    public byte[] getData() {
        ByteBuffer buffer = ByteBuffer.allocate(16); // Total bytes = 16 (1+1+1+1+2+1+2+2+3+2)

        buffer.put(equipmentClassificationCode);   // BIN 1
        buffer.put(serviceClassificationCode);     // BIN 1
        buffer.put(contextCode);                   // BIN 1
        buffer.put(paymentMethodCode);             // BIN 1

        buffer.put(date);                     // BIN 2
        buffer.put(time);                          // BIN 1

        buffer.put(place1);                   // BIN 2
        buffer.put(place2);                   // BIN 2

        // Put 3 bytes of cardBalance manually
        buffer.put(cardBalance);            // Least significant byte

        buffer.put(storedValueLogId);         // BIN 2

        return buffer.array();
    }
}
