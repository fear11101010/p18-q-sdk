package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class RideNotAllowedException extends Exception {

    public RideNotAllowedException() {
    }

    public RideNotAllowedException(String message) {
        super(message);
    }

    public RideNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RideNotAllowedException(Throwable cause) {
        super(cause);
    }
}
