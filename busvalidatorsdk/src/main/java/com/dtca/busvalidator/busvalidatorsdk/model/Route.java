package com.dtca.busvalidator.busvalidatorsdk.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * Data model representing a bus route with all its associated information.
 * <p>
 * This class contains comprehensive route data including route identification,
 * station list, fare type, and operational characteristics. It supports both
 * linear and circular routes with various fare calculation methods.
 * </p>
 * 
 * <h3>Route Types:</h3>
 * <ul>
 *   <li><b>Linear Route:</b> Has distinct start and end points</li>
 *   <li><b>Circular Route:</b> Returns to the starting point</li>
 * </ul>
 * 
 * <h3>Fare Types:</h3>
 * <ul>
 *   <li><b>Flat Fare:</b> Single price regardless of distance</li>
 *   <li><b>Distance-based:</b> Fare calculated based on origin-destination pairs</li>
 * </ul>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
public class Route {
    private int id;
    private int numberOfRoute;
    private String operatorCode;
    private int routeOrderNo;
    private String routeName;
    private int numberOfStoppage;
    private boolean isActive;
    private String lastUpdateAt;  // Dates can be handled with String or Date types
    private String effectiveAt;
    private String expirayAt;
    private boolean isFlatFare;
    private boolean isCircularRoute;
    private List<Station> stations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Station {
        private int id;
        private int routeId;
        private String stationName;
        private String stationNameBng;
        private String stationCode;
        private int stationOrderNo;
        private Double latitude;
        private Double longitude;
    }
}
