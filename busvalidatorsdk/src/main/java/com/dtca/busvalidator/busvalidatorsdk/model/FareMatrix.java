package com.dtca.busvalidator.busvalidatorsdk.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FareMatrix {
    private int routeId;
    private String routeName;
    private boolean isFlatFare;
    private boolean isCircular;
    private int numberOfStoppage;
    private List<Route.Station> stations;
    private Map<String, Map<String,Integer>> fareMatrix;


}
