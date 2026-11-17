package com.dtca.busvalidator.busvalidatorsdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * Data model representing a single transaction history record on a FeliCa card.
 * <p>
 * This class stores details about an individual transaction including the service type,
 * equipment used, payment method, and monetary amounts. History records are used
 * for transaction tracking and customer service.
 * </p>
 * 
 * <h3>Record Contents:</h3>
 * <ul>
 *   <li>Equipment and service classification</li>
 *   <li>Context code (entry, exit, recharge, etc.)</li>
 *   <li>Payment method used</li>
 *   <li>Transaction amounts</li>
 * </ul>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
public class HistoryRecord {
    /**
     * length 1 byte
     */
    private byte binEquipmentClass;
    /**
     * length 1 byte
     */
    private byte binServiceClass;
    /**
     * length 1 byte
     */
    private byte binContextCode;
    /**
     * length 1 byte
     */
    private byte binPaymentMethod;
    /**
     * length 2 byte
     */
    private byte[] binDate;
    /**
     * length 1 byte
     */
    private byte binTime;
    /**
     * length 2 byte
     */
    private byte[] binPlace1;
    /**
     * length 2 byte
     */
    private byte[] binPlace2;
    /**
     * length 3 byte
     */
    private byte[] binCardBalance;
    /**
     * length 2 byte
     */
    private byte[] binSVLogId;
}
