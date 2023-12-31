package de.rampro.activitydiary.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class VideoDb extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "video_db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "videos";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_ACTIVITY_ID = "activity_id";
    private static final String COLUMN_VIDEO_URI = "video_uri";

    public VideoDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ACTIVITY_ID + " TEXT, " +
                COLUMN_VIDEO_URI + " TEXT)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // 插入视频记录
    public void insertVideo(String activityId, Uri videoUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ACTIVITY_ID, activityId);
        values.put(COLUMN_VIDEO_URI, videoUri.toString());
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    // 查询指定activityID对应的所有视频URI
    public List<Uri> getVideos(String activityId) {
        List<Uri> videos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_VIDEO_URI};
        String selection = COLUMN_ACTIVITY_ID + "=?";
        String[] selectionArgs = {activityId};
        Cursor cursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String videoUriString = cursor.getString(cursor.getColumnIndex(COLUMN_VIDEO_URI));
                Uri videoUri = Uri.parse(videoUriString);
                videos.add(videoUri);
            } while (cursor.moveToNext());
            cursor.close();
        }

        db.close();
        return videos;
    }
}
