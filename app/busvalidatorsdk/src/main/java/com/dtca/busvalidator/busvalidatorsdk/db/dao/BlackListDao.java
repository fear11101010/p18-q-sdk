package com.dtca.busvalidator.busvalidatorsdk.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.dtca.busvalidator.busvalidatorsdk.db.entity.BlackListEntity;

import java.util.List;

@Dao
public interface BlackListDao {
    @Insert
    void insertBlackListData(BlackListEntity blackListEntity);

    @Query("SELECT * FROM black_list_data WHERE cardId=:cardId")
    BlackListEntity getBlackListDataByCardId(String cardId);

    @Query("SELECT * FROM black_list_data")
    List<BlackListEntity> getAllBlackList();
    @Query("DELETE FROM black_list_data")
    void truncateTable();

    @Query("DELETE FROM sqlite_sequence WHERE name = 'black_list_data'")
    void resetAutoIncrement();
}
