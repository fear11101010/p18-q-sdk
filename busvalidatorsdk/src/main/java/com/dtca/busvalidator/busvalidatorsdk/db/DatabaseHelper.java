package com.dtca.busvalidator.busvalidatorsdk.db;

import android.content.Context;

import androidx.room.Room;

import com.dtca.busvalidator.busvalidatorsdk.db.dao.BlackListDao;
import com.dtca.busvalidator.busvalidatorsdk.db.dao.TripsDao;
import com.dtca.busvalidator.busvalidatorsdk.db.entity.BlackListEntity;
import com.dtca.busvalidator.busvalidatorsdk.db.entity.TripsEntity;

import java.util.List;

import lombok.Getter;

public class DatabaseHelper {

    // Database Name
    private static final String DATABASE_NAME = "bus_validator.db";
    private static DatabaseHelper databaseHelper;
    @Getter
    private final AppDatabase appDatabase;

    private DatabaseHelper(Context context) {
        appDatabase = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build();
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(context);
        }
        return databaseHelper;
    }

    // This method is called when the database is created for the first time.

    public void insertIntoBlacklistTable(String cardId, String reason) {
        BlackListDao blackListDao = appDatabase.getBlackListDao();
        BlackListEntity blackListEntity = new BlackListEntity();
        blackListEntity.cardId = cardId;
        blackListEntity.reason = reason;
        blackListDao.insertBlackListData(blackListEntity);
    }

    public BlackListEntity fetchBlacklistDataByCardId(String cardId) {
        BlackListDao blackListDao = appDatabase.getBlackListDao();
        return blackListDao.getBlackListDataByCardId(cardId);
    }

    public List<BlackListEntity> fetchAllBlacklistData() {
        BlackListDao blackListDao = appDatabase.getBlackListDao();
        return blackListDao.getAllBlackList();
    }
    public void truncateTable(){
        BlackListDao blackListDao = appDatabase.getBlackListDao();
        blackListDao.truncateTable();
        blackListDao.resetAutoIncrement();
    }

    //-----------------------------trips------------------------------------

    public void insertIntoTripsTable(String cardId, String direction) {
        TripsDao tripsDao = appDatabase.getTripsDao();
        TripsEntity tripsEntity = new TripsEntity();
        tripsEntity.cardId = cardId;
        tripsEntity.direction = direction;
        tripsDao.insertTrips(tripsEntity);
    }
    public void updateTripsTable(String cardId, String direction) {
        TripsDao tripsDao = appDatabase.getTripsDao();
        tripsDao.update(direction,cardId);
    }

    public void updateTripsTableFromStation(String cardId, String station) {
        TripsDao tripsDao = appDatabase.getTripsDao();
        tripsDao.updateFromStation(station,cardId);
    }

    public void updateTripsTableToStation(String cardId, String station) {
        TripsDao tripsDao = appDatabase.getTripsDao();
        tripsDao.updateToStation(station,cardId);
    }
    public void updateTripsTableCashBackAmount(String cardId, int amount) {
        TripsDao tripsDao = appDatabase.getTripsDao();
        tripsDao.updateCashBackData(amount,cardId);
    }

    public TripsEntity getTripsByCardId(String cardId) {
        TripsDao tripsDao = appDatabase.getTripsDao();
        return tripsDao.getTripsByCardId(cardId);
    }
    public void truncateTripsTable(){
        TripsDao tripsDao = appDatabase.getTripsDao();
        tripsDao.truncateTable();
        tripsDao.resetAutoIncrement();
    }
}
