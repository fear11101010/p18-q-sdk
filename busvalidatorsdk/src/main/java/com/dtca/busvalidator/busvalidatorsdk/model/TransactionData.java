package com.dtca.busvalidator.busvalidatorsdk.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Data model representing complete transaction data for bus validator operations.
 * <p>
 * This class aggregates all relevant information about a transaction including
 * card identification, transaction type, timestamps, amounts, location, and device
 * information. It serves as a comprehensive record for backend processing,
 * reporting, and reconciliation.
 * </p>
 * 
 * <h3>Transaction Information:</h3>
 * <ul>
 *   <li>Card identification and recycle counter</li>
 *   <li>Transaction ID and service ID</li>
 *   <li>Timestamp and location data</li>
 *   <li>Fare and balance information</li>
 *   <li>Device serial number</li>
 *   <li>Card function and control codes</li>
 * </ul>
 * 
 * <h3>Usage:</h3>
 * <p>
 * This class is typically used for transmitting transaction data from validator
 * devices to backend systems for processing, fare settlement, and reporting.
 * </p>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
public class TransactionData {
    String cardId;
    String recycleCounter;
    Integer transactionDataId;
    String serviceId;  // hex serviceId
    String dateTimeStamp; // dateStamp + timeStamp
    Integer cardFunctionCode;
    Integer cardControlCode;
    Integer discountCode;
    String cardExpirationDate;
    Integer processUnfinishedFlag;

    String deviceSerialNumber;

    Integer svLogId;
    Integer svBalance; // remaining sv
    Integer svSpent; // spending value
    String processedLocation1;
    String processedLocation2;
    // Bus Info
    Integer statusFlag;
    Integer basicFareAmount;
    Integer distanceFareAmount;
    Integer discountFareAmount;
    String inStoppage;
    String outStoppage;
    Integer negativeValue;
    Integer negativeValueUsed;
    String metaData;

    String message = "NA";
}