package com.dtca.busvalidator.busvalidatorsdk.model;

import lombok.Getter;

/**
 * Enumeration representing master configuration parameters for the bus validator system.
 * <p>
 * This enum defines system-wide configuration values that control various aspects
 * of the bus validation process, including minimum balance requirements, timing
 * constraints, and expiry periods.
 * </p>
 * 
 * <p>Each configuration has:</p>
 * <ul>
 *   <li>A name identifier (String)</li>
 *   <li>A default value (Integer)</li>
 * </ul>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
@Getter
public enum MasterConfigName {
    
    /**
     * Minimum balance required on the card to initiate a ride.
     * <p>Default value: 10 units</p>
     */
    MINIMUM_RIDE_BALANCE("MINIMUM_RIDE_BALANCE", 10),
    
    /**
     * Minimum time (in minutes) required to cancel an entry transaction.
     * <p>Default value: 5 minutes</p>
     */
    MINIMUM_CANCEL_OF_ENTRY_TIME("MINIMUM_CANCEL_OF_ENTRY_TIME", 5),
    
    /**
     * Time duration (in minutes) after which an alight action expires.
     * <p>Default value: 240 minutes (4 hours)</p>
     */
    ALIGHT_EXPIRY_TIME("ALIGHT_EXPIRY_TIME", 240);

    /**
     * The configuration name identifier.
     */
    private final String name;
    
    /**
     * The default integer value for this configuration.
     */
    private final Integer value;
    
    /**
     * Constructs a MasterConfigName enum constant.
     * 
     * @param name  the configuration name identifier
     * @param value the default value for this configuration
     */
    MasterConfigName(String name, Integer value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Finds and returns a MasterConfigName enum constant by its name value.
     * <p>
     * This method performs a case-insensitive search across all enum values
     * to find a matching configuration by name.
     * </p>
     * 
     * @param value the configuration name to search for (case-insensitive)
     * @return the matching MasterConfigName enum constant
     * @throws IllegalArgumentException if no matching configuration name is found
     * 
     * @example
     * <pre>
     * MasterConfigName config = MasterConfigName.fromValue("MINIMUM_RIDE_BALANCE");
     * // Returns: MINIMUM_RIDE_BALANCE enum constant
     * </pre>
     */
    public static MasterConfigName fromValue(String value) {
        for (MasterConfigName status : MasterConfigName.values()) {
            if (status.getName().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
