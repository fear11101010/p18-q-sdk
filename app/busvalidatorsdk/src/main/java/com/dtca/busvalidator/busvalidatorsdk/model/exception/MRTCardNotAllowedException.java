package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class MRTCardNotAllowedException extends Exception {

    public MRTCardNotAllowedException() {
    }

    public MRTCardNotAllowedException(String message) {
        super(message);
    }

    public MRTCardNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }

    public MRTCardNotAllowedException(Throwable cause) {
        super(cause);
    }
}
