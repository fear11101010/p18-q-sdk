package com.dtca.busvalidator.busvalidatorsdk.db.repository;

import com.dtca.busvalidator.busvalidatorsdk.db.AppDatabase;
import com.dtca.busvalidator.busvalidatorsdk.db.dao.TripsDao;
import com.dtca.busvalidator.busvalidatorsdk.db.entity.TripsEntity;

public class TripsRepository {
    private static TripsRepository tripsRepository = null;
    private final AppDatabase appDatabase;

    private TripsRepository(AppDatabase appDatabase){
        if(appDatabase == null) throw new IllegalArgumentException("App database can not be null");
        this.appDatabase = appDatabase;
    }

    public static synchronized TripsRepository getInstance(AppDatabase appDatabase){
        if(tripsRepository == null){
            tripsRepository = new TripsRepository(appDatabase);
        }
        return tripsRepository;
    }

    public synchronized void insert(String cardId, String direction) {
        TripsDao tripsDao = appDatabase.getTripsDao();
        TripsEntity tripsEntity = new TripsEntity();
        tripsEntity.cardId = cardId;
        tripsEntity.direction = direction;
        tripsDao.insertTrips(tripsEntity);
    }
    public synchronized void updateDirection(String cardId, String direction) {
        TripsDao tripsDao = appDatabase.getTripsDao();
        tripsDao.update(direction,cardId);
    }

    public synchronized void updateFromStation(String cardId, String station) {
        TripsDao tripsDao = appDatabase.getTripsDao();
        tripsDao.updateFromStation(station,cardId);
    }

    public synchronized void updateToStation(String cardId, String station) {
        TripsDao tripsDao = appDatabase.getTripsDao();
        tripsDao.updateToStation(station,cardId);
    }
    public synchronized void updateCashBackAmount(String cardId, int amount) {
        TripsDao tripsDao = appDatabase.getTripsDao();
        tripsDao.updateCashBackData(amount,cardId);
    }

    public synchronized void deleteByCardId(String cardId) {
        TripsDao tripsDao = appDatabase.getTripsDao();
        tripsDao.deleteByCardId(cardId);
    }

    public synchronized void truncate() {
        TripsDao tripsDao = appDatabase.getTripsDao();
        tripsDao.truncateTable();
        tripsDao.resetAutoIncrement();
    }



}
