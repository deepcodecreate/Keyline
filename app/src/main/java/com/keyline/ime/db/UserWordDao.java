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

package com.keyline.ime.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserWordDao {
    @Query("SELECT * FROM user_words WHERE lang = :lang")
    List<UserWordEntity> forLang(String lang);

    @Query("SELECT * FROM user_words ORDER BY lang, word")
    List<UserWordEntity> all();

    @Query("SELECT * FROM user_words WHERE source = 'manual' ORDER BY lang, word")
    List<UserWordEntity> manual();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserWordEntity w);

    @Query("DELETE FROM user_words WHERE lang = :lang AND word = :word")
    void delete(String lang, String word);

    @Query("DELETE FROM user_words WHERE source = 'learned'")
    void clearLearned();

    @Query("DELETE FROM user_words")
    void clearAll();
}
