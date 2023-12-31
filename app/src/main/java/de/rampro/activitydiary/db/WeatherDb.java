/*
 * ActivityDiary
 *
 * Copyright (C) 2023 Raphael Mack http://www.raphael-mack.de
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.rampro.activitydiary.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WeatherDb extends SQLiteOpenHelper {

    // 数据库名称和版本
    private static final String DATABASE_NAME = "weather_database";
    private static final int DATABASE_VERSION = 1;

    // 表名和列名
    private static final String TABLE_NAME = "weather_table";
    private static final String COLUMN_ID = "activity_id";
    private static final String COLUMN_WEATHER_JSON = "weather_json";

    // 创建表的 SQL 语句
    private static final String CREATE_TABLE_QUERY =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " TEXT PRIMARY KEY," +
                    COLUMN_WEATHER_JSON + " TEXT)";

    public WeatherDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 如果有版本更新，可以在这里进行相应的处理
    }

    // 插入数据
    public void insertWeather(String activityId, String weatherJson) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT OR REPLACE INTO " + TABLE_NAME + " (" + COLUMN_ID + ", " + COLUMN_WEATHER_JSON + ") VALUES (?, ?)",
                new Object[]{activityId, weatherJson});
        db.close();
    }

    // 查询数据
    public String getWeather(String activityId) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT " + COLUMN_WEATHER_JSON + " FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + "=?";
        String[] selectionArgs = {activityId};
        String weatherJson = null;
        Cursor cursor = db.rawQuery(query, selectionArgs);
        if (cursor.moveToFirst()) {
            weatherJson = cursor.getString(cursor.getColumnIndex(COLUMN_WEATHER_JSON));
        }
        cursor.close();
        db.close();
        return weatherJson;
    }
}
