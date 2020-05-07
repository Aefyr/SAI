package com.aefyr.sai.common;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.aefyr.sai.backup2.impl.db.BackupDao;
import com.aefyr.sai.backup2.impl.db.BackupMetaEntity;

@Database(entities = {BackupMetaEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase sInstance;

    public synchronized static AppDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = Room.databaseBuilder(context, AppDatabase.class, "room").build();
        }

        return sInstance;
    }


    public abstract BackupDao backupDao();
}
