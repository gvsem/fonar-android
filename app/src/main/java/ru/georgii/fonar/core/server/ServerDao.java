package ru.georgii.fonar.core.server;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ServerDao {

    @Query("SELECT * FROM server")
    List<Server> getAll();

    @Query("SELECT * FROM server WHERE id = :id LIMIT 1")
    Server findById(Long id);

    @Insert
    long insert(Server server);

    @Delete
    void delete(Server server);

    @Update
    void update(Server server);

}

