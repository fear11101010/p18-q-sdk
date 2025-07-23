package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class RouteNotFoundException extends Exception {

    public RouteNotFoundException() {
    }

    public RouteNotFoundException(String message) {
        super(message);
    }

    public RouteNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public RouteNotFoundException(Throwable cause) {
        super(cause);
    }
}
