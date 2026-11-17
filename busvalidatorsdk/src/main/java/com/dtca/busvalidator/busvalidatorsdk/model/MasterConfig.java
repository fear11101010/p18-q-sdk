package com.dtca.busvalidator.busvalidatorsdk.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Data model representing a master configuration entry in the bus validator system.
 * <p>
 * This class stores system-wide configuration parameters that can be dynamically
 * modified without code changes. Each configuration consists of a name-value pair
 * with optional remarks for documentation purposes.
 * </p>
 * 
 * <h3>Typical Usage:</h3>
 * <p>
 * Master configurations are typically loaded from a database or configuration file
 * at application startup and used to control various aspects of the system behavior.
 * </p>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 * @see MasterConfigName
 */
@Data
public class MasterConfig {

    /**
     * Unique identifier for this configuration entry.
     */
    private Long id;
    
    /**
     * The configuration name/key identifier.
     * <p>
     * This should correspond to one of the predefined values in {@link MasterConfigName}.
     * </p>
     */
    private String configName;
    
    /**
     * The configuration value as a string.
     * <p>
     * Values are stored as strings to allow flexibility in data types.
     * They should be parsed to the appropriate type (Integer, Boolean, etc.) when used.
     * </p>
     */
    private String value;
    
    /**
     * Optional remarks or description about this configuration.
     * <p>
     * Used for documentation and understanding the purpose of the configuration.
     * </p>
     */
    private String remarks;
}
