package com.dtca.busvalidator.busvalidatorsdk.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "black_list_data")
public class BlackListEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String cardId;
    public String reason;
}
