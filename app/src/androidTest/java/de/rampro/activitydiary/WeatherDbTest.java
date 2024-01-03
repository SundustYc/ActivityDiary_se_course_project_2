/*
 * ActivityDiary
 *
 * Copyright (C) 2024 Raphael Mack http://www.raphael-mack.de
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.rampro.activitydiary.db.WeatherDb;


@RunWith(AndroidJUnit4.class)
public class WeatherDbTest {

    @Test
    public void testGetWeather() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String testActivityId = "test_activity_id";
        String testWeatherJson = "{\"temperature\": 25, \"condition\": \"sunny\"}";
        WeatherDb weatherDb = new WeatherDb(context);
        weatherDb.insertWeather(testActivityId, testWeatherJson);
        String resultWeatherJson = weatherDb.getWeather(testActivityId);
        assertEquals(testWeatherJson, resultWeatherJson);

        weatherDb.insertWeather(testActivityId, null);
        resultWeatherJson = weatherDb.getWeather(testActivityId);
        assertNull(resultWeatherJson);
    }
}
