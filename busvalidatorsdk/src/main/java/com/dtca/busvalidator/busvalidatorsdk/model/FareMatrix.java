package com.dtca.busvalidator.busvalidatorsdk.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Data model representing fare matrix information for bus routes.
 * <p>
 * This class contains comprehensive fare calculation data for a specific route,
 * including station-to-station fare mappings, route characteristics, and pricing information.
 * It supports both flat fare and distance-based fare systems.
 * </p>
 * 
 * <h3>Fare Calculation Types:</h3>
 * <ul>
 *   <li><b>Flat Fare:</b> Single price regardless of distance</li>
 *   <li><b>Distance-based:</b> Fare varies based on origin and destination stations</li>
 * </ul>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
public class FareMatrix {
    private int routeId;
    private String routeName;
    private boolean isFlatFare;
    private boolean isCircular;
    private int numberOfStoppage;
    private List<Route.Station> stations;
    private Map<String, Map<String,Integer>> fareMatrix;


}
