/*
 * Copyright 2026 deepcodecreate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.keyline.ime.clipboard;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ClipboardDao {
    @Query("SELECT * FROM clips ORDER BY pinned DESC, createdAt DESC")
    List<ClipEntity> all();

    @Query("SELECT * FROM clips WHERE text = :text LIMIT 1")
    ClipEntity find(String text);

    @Insert
    long insert(ClipEntity e);

    @Update
    void update(ClipEntity e);

    @Delete
    void delete(ClipEntity e);

    @Query("DELETE FROM clips")
    void clear();

    @Query("DELETE FROM clips WHERE pinned = 0")
    void clearUnpinned();

    @Query("SELECT * FROM clips WHERE pinned = 0 ORDER BY createdAt ASC")
    List<ClipEntity> unpinnedOldest();
}
