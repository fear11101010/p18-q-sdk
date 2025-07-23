package com.dtca.busvalidator.busvalidatorsdk.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.dtca.busvalidator.busvalidatorsdk.db.entity.BlackListEntity;
import com.dtca.busvalidator.busvalidatorsdk.db.entity.TripsEntity;

import java.util.List;

@Dao
public interface TripsDao {
    @Insert
    void insertTrips(TripsEntity tripsEntity);

    @Query("SELECT * FROM trips_data WHERE cardId=:cardId")
    TripsEntity getTripsByCardId(String cardId);

    @Query("SELECT * FROM trips_data")
    List<TripsEntity> getAllTrips();

    @Query("UPDATE trips_data SET direction = :direction WHERE cardId = :cardId")
    void update(String direction,String cardId);

    @Query("UPDATE trips_data SET fromStation = :station WHERE cardId = :cardId")
    void updateFromStation(String station,String cardId);

    @Query("UPDATE trips_data SET toStation = :station WHERE cardId = :cardId")
    void updateToStation(String station,String cardId);

    @Query("UPDATE trips_data SET cashBackAmount = :cashBackData WHERE cardId = :cardId")
    void updateCashBackData(int cashBackData,String cardId);

    @Query("DELETE FROM trips_data WHERE cardId = :cardId")
    void deleteByCardId(String cardId);

    @Query("DELETE FROM trips_data")
    void truncateTable();

    @Query("DELETE FROM sqlite_sequence WHERE name = 'trips_data'")
    void resetAutoIncrement();
}
