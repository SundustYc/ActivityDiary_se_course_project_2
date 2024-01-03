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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import de.rampro.activitydiary.ui.history.HistoryActivity;

public class CalendarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        // 获取 CalendarView 对象
        CalendarView calendarView = findViewById(R.id.calendarView);

        // 设置日期改变监听器
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                // 处理选中日期的操作，例如显示日期或执行其他逻辑
                String selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
                Toast.makeText(CalendarActivity.this, "Selected Date: " + selectedDate, Toast.LENGTH_SHORT).show();
            }
        });

        // 获取按钮对象
        Button addEventButton = findViewById(R.id.addEventButton);

        // 设置按钮点击事件监听器
        addEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 处理按钮点击事件
                Intent intent = new Intent(CalendarActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });
    }
}

