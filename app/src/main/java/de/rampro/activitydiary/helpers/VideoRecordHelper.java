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

package de.rampro.activitydiary.helpers;

import android.content.Intent;
import android.content.SharedPreferences;

import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.io.File;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.ui.settings.SettingsActivity;

public class VideoRecordHelper extends AppCompatActivity
{
    private static final String TAG = "VideoRecordHelper";
    static final int REQUEST_VIDEO_CAPTURE = 1;

    public void startVideoRecording(Uri videoUri)
    {

        try {
            Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
            takeVideoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);

        }
        catch (Exception e)
        {
            Log.e(TAG, "Error in startVideoRecording: " + e.getMessage());
            throw e;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 检查请求码和结果码
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            // 获取录像的URI
            Uri videoUri = data.getData();

            Toast.makeText(this, "录像已保存到：" + videoUri.toString(), Toast.LENGTH_SHORT).show();
        } else {
            // 录像过程中发生错误或用户取消操作，显示提示信息
            Toast.makeText(this, "录像失败或已取消", Toast.LENGTH_SHORT).show();
        }
    }
    public static File videoStorageDirectory ()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext());
        File directory;
        directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        File root = new File(directory, sharedPreferences.getString(SettingsActivity.KEY_PREF_STORAGE_FOLDER, "ActivityDiary"));
        if (!root.exists()) {
            //原来你是用来创建文件的啊，啊哈哈哈...
            //我再手贱删代码我是nt
            if (!root.mkdirs()) {
                Log.e(TAG, "failed to create directory");
                throw new RuntimeException("failed to create directory " + root.toString());
            }
        }
        return root;
    }
}
