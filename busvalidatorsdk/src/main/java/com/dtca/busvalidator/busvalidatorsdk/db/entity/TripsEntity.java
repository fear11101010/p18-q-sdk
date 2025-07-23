package com.dtca.busvalidator.busvalidatorsdk.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "trips_data")
public class TripsEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String cardId;
    public String direction;
    public String fromStation;
    public String toStation;
    public int cashBackAmount;
}
