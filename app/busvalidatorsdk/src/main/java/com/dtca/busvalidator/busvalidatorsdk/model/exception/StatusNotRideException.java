package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class StatusNotRideException extends Exception {

    public StatusNotRideException() {
    }

    public StatusNotRideException(String message) {
        super(message);
    }

    public StatusNotRideException(String message, Throwable cause) {
        super(message, cause);
    }

    public StatusNotRideException(Throwable cause) {
        super(cause);
    }
}
