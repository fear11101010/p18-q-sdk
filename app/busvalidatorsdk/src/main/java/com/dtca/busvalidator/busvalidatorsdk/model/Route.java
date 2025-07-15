package com.dtca.busvalidator.busvalidatorsdk.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
