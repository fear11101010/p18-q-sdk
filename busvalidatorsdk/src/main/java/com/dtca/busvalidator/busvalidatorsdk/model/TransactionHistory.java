package com.dtca.busvalidator.busvalidatorsdk.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistory {
    private String date;
    private String operation;
    private String serviceId;
    private int amount;
    private int balance;
}
