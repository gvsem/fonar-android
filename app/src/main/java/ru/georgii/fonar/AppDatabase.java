package ru.georgii.fonar;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import ru.georgii.fonar.core.server.Server;
import ru.georgii.fonar.core.server.ServerDao;

@Database(entities = {Server.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ServerDao serverDao();
}
