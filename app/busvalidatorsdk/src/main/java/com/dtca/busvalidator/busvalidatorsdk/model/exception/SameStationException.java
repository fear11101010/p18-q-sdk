package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class SameStationException extends Exception {

    public SameStationException() {
    }

    public SameStationException(String message) {
        super(message);
    }

    public SameStationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SameStationException(Throwable cause) {
        super(cause);
    }
}
