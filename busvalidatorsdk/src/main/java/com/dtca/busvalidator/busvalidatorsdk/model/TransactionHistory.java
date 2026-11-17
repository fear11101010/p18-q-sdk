package com.dtca.busvalidator.busvalidatorsdk.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Data model representing a simplified transaction history entry for display purposes.
 * <p>
 * This class provides a user-friendly view of transaction history, suitable for
 * displaying in mobile apps or customer portals. It contains essential information
 * about each transaction in a readable format.
 * </p>
 * 
 * <h3>Display Fields:</h3>
 * <ul>
 *   <li>Date: When the transaction occurred</li>
 *   <li>Operation: Type of transaction (entry, exit, reload, etc.)</li>
 *   <li>Service ID: Service or route identifier</li>
 *   <li>Amount: Transaction amount (positive or negative)</li>
 *   <li>Balance: Remaining balance after transaction</li>
 * </ul>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
public class TransactionHistory {
    private String date;
    private String operation;
    private String serviceId;
    private int amount;
    private int balance;
}
