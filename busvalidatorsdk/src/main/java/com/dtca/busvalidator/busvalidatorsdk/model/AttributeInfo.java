package com.dtca.busvalidator.busvalidatorsdk.model;

import java.nio.ByteBuffer;
import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data model representing card attribute information for FeliCa smart cards.
 * <p>
 * This class encapsulates various attributes associated with a transit card including
 * function codes, control codes, expiry dates, transaction identifiers, and recharge information.
 * The data structure follows a fixed 32-byte format for FeliCa card compatibility.
 * </p>
 * 
 * <h3>Data Structure (32 bytes total):</h3>
 * <ul>
 *   <li>Bytes 0-1: Card Function Code</li>
 *   <li>Byte 2: Card Control Code</li>
 *   <li>Byte 3: Discount Code</li>
 *   <li>Bytes 4-5: Expiry Date</li>
 *   <li>Bytes 6-7: Transaction Data ID</li>
 *   <li>Bytes 8-15: Replaced Card ID</li>
 *   <li>Bytes 16-23: Merchandise Management Code</li>
 *   <li>Bytes 24-25: Negative Value</li>
 *   <li>Byte 26: Recharge Type</li>
 *   <li>Bytes 27-31: Reserved</li>
 * </ul>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttributeInfo {
    
    /**
     * Card function code indicating the card's operational capabilities.
     * <p>Length: 2 bytes (positions 0-1)</p>
     */
    byte[] cardFunctionCode;
    
    /**
     * Card control code for access control and permissions.
     * <p>Length: 1 byte (position 2)</p>
     */
    byte cardControlCode;
    
    /**
     * Discount code associated with the card holder.
     * <p>Length: 1 byte (position 3)</p>
     */
    byte discountCode;
    
    /**
     * Expiry date of the card in binary format.
     * <p>Length: 2 bytes (positions 4-5)</p>
     */
    byte[] expiryDate;
    
    /**
     * Transaction data identifier for tracking purposes.
     * <p>Length: 2 bytes (positions 6-7)</p>
     */
    byte[] txnDataId;
    
    /**
     * Card ID of the previous card if this is a replacement card.
     * <p>Length: 8 bytes (positions 8-15)</p>
     */
    byte[] replacedCardId;
    
    /**
     * Merchandise management code for product tracking and inventory.
     * <p>Length: 8 bytes (positions 16-23)</p>
     */
    byte[] merchendizeManagementCode;
    
    /**
     * Negative value amount (debt or pending charges).
     * <p>Length: 2 bytes (positions 24-25)</p>
     */
    byte[] negativeValue;
    
    /**
     * Recharge type indicator specifying the method of card recharge.
     * <p>Length: 1 byte (position 26)</p>
     */
    byte rechargeType;

    /**
     * Reserved bytes for future use or padding.
     * <p>Length: 5 bytes (positions 27-31)</p>
     */
    byte[] reserved;

    /**
     * Parses a raw byte array and generates an AttributeInfo object.
     * <p>
     * This static factory method deserializes a 32-byte array into an
     * AttributeInfo object by extracting data from specific byte positions
     * according to the FeliCa card format specification.
     * </p>
     * 
     * @param data the raw byte array containing attribute information (must be at least 32 bytes)
     * @return a new AttributeInfo object populated with the parsed data
     * @throws ArrayIndexOutOfBoundsException if the data array is smaller than 32 bytes
     * 
     * @example
     * <pre>
     * byte[] rawData = // ... 32 bytes from card reading
     * AttributeInfo info = AttributeInfo.generateData(rawData);
     * </pre>
     */
    public static AttributeInfo generateData(byte[] data){
        return AttributeInfo.builder()
                .cardFunctionCode(Arrays.copyOfRange(data, 0, 2))
                .cardControlCode(data[2])
                .discountCode(data[3])
                .expiryDate(Arrays.copyOfRange(data, 4, 6))
                .txnDataId(Arrays.copyOfRange(data, 6, 8))
                .replacedCardId(Arrays.copyOfRange(data, 8, 16))
                .merchendizeManagementCode(Arrays.copyOfRange(data, 16, 24))
                .negativeValue(Arrays.copyOfRange(data, 24, 26))
                .rechargeType(data[26])
                .reserved(Arrays.copyOfRange(data, 27, 32))
                .build();
    }

    /**
     * Serializes this AttributeInfo object into a 32-byte array.
     * <p>
     * This method converts all attribute fields into a contiguous byte array
     * suitable for writing to a FeliCa card. The byte array follows the exact
     * format specification with each field at its designated position.
     * </p>
     * 
     * @return a 32-byte array containing all attribute information in the correct format
     * 
     * @example
     * <pre>
     * AttributeInfo info = AttributeInfo.builder()
     *     .cardFunctionCode(new byte[]{0x01, 0x02})
     *     .cardControlCode((byte) 0x03)
     *     // ... other fields
     *     .build();
     * byte[] dataToWrite = info.getData();
     * // dataToWrite can now be written to the card
     * </pre>
     */
    public byte[] getData(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(32);
        byteBuffer.put(getCardFunctionCode());
        byteBuffer.put(getCardControlCode());
        byteBuffer.put(getDiscountCode());
        byteBuffer.put(getExpiryDate());
        byteBuffer.put(getTxnDataId());
        byteBuffer.put(getReplacedCardId());
        byteBuffer.put(getMerchendizeManagementCode());
        byteBuffer.put(getNegativeValue());
        byteBuffer.put(getRechargeType());
        byteBuffer.put(getReserved());
        byte[] bytes = byteBuffer.array();
        byteBuffer = null;
        return bytes;
    }
}
