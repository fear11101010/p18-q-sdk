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
 * Data model representing general identification and control information for a FeliCa card.
 * <p>
 * This class contains core card identity data including the unique card ID, card type,
 * recycling counter, and negative value tracking. This information is fundamental
 * to all card operations.
 * </p>
 * 
 * <h3>Key Fields:</h3>
 * <ul>
 *   <li>Card ID: Unique 8-byte identifier</li>
 *   <li>Recycle Counter: Tracks card reuse/replacement cycles</li>
 *   <li>Card Type: Indicates card category (adult, student, senior, etc.)</li>
 *   <li>Negative Value: Outstanding debt or pending charges</li>
 * </ul>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
public class GeneralInfo {
    /**
     * length 8 byte
     */
    byte[] binCardID;	//1-8
    /**
     * length 1 byte
     */
    byte binReCycleCounter;	//9
    /**
     * length 1 byte
     */
    byte binCardType;	//10
    /**
     * length probably 2 byte (11-12)
     */
    short intNegativeValue;			//11-12
    /**
     * length probably 8 byte 13-20
     */
    long lngRemainingSV;
    /**
     * length probably 8 byte 21-28
     */
    long lngCashBackData;
}
