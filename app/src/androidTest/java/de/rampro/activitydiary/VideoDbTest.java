package de.rampro.activitydiary;

import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

import java.util.List;

import de.rampro.activitydiary.db.VideoDb;

public class VideoDbTest {

    @Test
    public void testGetVideos() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String testActivityId = "test_activity_id";
        Uri testVideoUri1 = Uri.parse("content://test/video1");
        Uri testVideoUri2 = Uri.parse("content://test/video2");
        VideoDb videoDb = new VideoDb(context);
        videoDb.insertVideo(testActivityId, testVideoUri1);
        videoDb.insertVideo(testActivityId, testVideoUri2);
        List<Uri> resultVideos = videoDb.getVideos(testActivityId);
        assertTrue(resultVideos != null);
        assertEquals(2, resultVideos.size());
        // 断言结果列表包含测试用的Video URI
        assertTrue(resultVideos.contains(testVideoUri1));
        assertTrue(resultVideos.contains(testVideoUri2));
    }
}
