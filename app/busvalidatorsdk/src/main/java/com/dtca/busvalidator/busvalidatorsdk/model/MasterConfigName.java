package com.dtca.busvalidator.busvalidatorsdk.model;

import lombok.Getter;

@Getter
public enum MasterConfigName {
    MINIMUM_RIDE_BALANCE("MINIMUM_RIDE_BALANCE",10),
    MINIMUM_CANCEL_OF_ENTRY_TIME("MINIMUM_CANCEL_OF_ENTRY_TIME",5),
    ALIGHT_EXPIRY_TIME("ALIGHT_EXPIRY_TIME",240);

    private final String name;
    private final Integer value;
    MasterConfigName(String name, Integer value) {
        this.name = name;
        this.value = value;
    }

    // Static method to find enum by value
    public static MasterConfigName fromValue(String value) {
        for (MasterConfigName status : MasterConfigName.values()) {
            if (status.getName().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
