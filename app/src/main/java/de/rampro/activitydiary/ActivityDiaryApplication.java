/*
 * ActivityDiary
 *
 * Copyright (C) 2017 Raphael Mack http://www.raphael-mack.de
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

package de.rampro.activitydiary;

import android.app.Application;
import android.content.Context;
import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(mailTo = "activity-diary@rampro.de",
        mode = ReportingInteractionMode.DIALOG,
        customReportContent = { ReportField.APP_VERSION_CODE,
                ReportField.APP_VERSION_NAME,
                ReportField.USER_COMMENT,
                ReportField.SHARED_PREFERENCES,
                ReportField.ANDROID_VERSION,
                ReportField.BRAND,
                ReportField.PHONE_MODEL,
                ReportField.CUSTOM_DATA,
                ReportField.STACK_TRACE,
                ReportField.BUILD,
                ReportField.BUILD_CONFIG,
                ReportField.CRASH_CONFIGURATION,
                ReportField.DISPLAY
        },
        alsoReportToAndroidFramework = true,
        resDialogText = R.string.crash_dialog_text,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. When defined, adds a user text field input with this text resource as a label
        resDialogOkToast = R.string.crash_dialog_ok_toast // optional. displays a Toast message when the user accepts to send a report.
)
public class ActivityDiaryApplication extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        ActivityDiaryApplication.context = getApplicationContext();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        ACRA.init(this);
    }

    public static Context getAppContext() {
        return ActivityDiaryApplication.context;
    }
}
