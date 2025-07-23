package com.dtca.busvalidator.busvalidatorsdk.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.dtca.busvalidator.busvalidatorsdk.db.dao.BlackListDao;
import com.dtca.busvalidator.busvalidatorsdk.db.dao.TripsDao;
import com.dtca.busvalidator.busvalidatorsdk.db.entity.BlackListEntity;
import com.dtca.busvalidator.busvalidatorsdk.db.entity.TripsEntity;

@Database(entities = {BlackListEntity.class, TripsEntity.class},version = 4)
public abstract class AppDatabase extends RoomDatabase {
    public abstract BlackListDao getBlackListDao();
    public abstract TripsDao getTripsDao();
}
