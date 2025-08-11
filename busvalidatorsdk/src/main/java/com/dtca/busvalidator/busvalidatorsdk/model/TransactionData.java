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