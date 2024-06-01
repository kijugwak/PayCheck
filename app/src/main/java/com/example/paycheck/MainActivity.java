package com.example.paycheck;

import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    EditText editTextSalary;
    TextView textViewHourlyWage;
    TextView textViewDailyEarning;
    Button buttonStartTime;

    double salary = 0;
    double dailyEarning = 0;
    double hourlySalary = 0;

    final double taxRate = 0.033; // 세금 비율 (3.3%)
    final int workDaysPerWeek = 5; // 주당 근무일 수
    final int workHoursPerDay = 9; // 하루 근무 시간

    int startHour = 0;
    int startMinute = 0;
    int endHour = 0;
    int endMinute = 0;

    Handler handler = new Handler();
    Runnable updateEarningRunnable = new Runnable() {
        @Override
        public void run() {
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);

            if ((currentHour > startHour && currentHour < endHour) ||
                    (currentHour == startHour && currentMinute >= startMinute) ||
                    (currentHour == endHour && currentMinute <= endMinute)) {
                dailyEarning += hourlySalary / 3600; // 초 단위로 증가
                updateDailyEarning();
                handler.postDelayed(this, 1000); // 1초마다 돈이 올라가도록 설정
            } else if (currentHour == endHour && currentMinute > endMinute) {
                // 근무 시간이 끝났을 때 핸들러를 중지합니다.
                handler.removeCallbacks(this);
            }
        }
    };

    private long startTimeInMillis = 0; // 출근 시작 시간을 밀리초 단위로 저장

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scheduleNotification();
        editTextSalary = findViewById(R.id.editTextSalary);
        textViewHourlyWage = findViewById(R.id.textViewHourlyWage);
        textViewDailyEarning = findViewById(R.id.textViewDailyEarning);
        buttonStartTime = findViewById(R.id.buttonStartTime);

        buttonStartTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this, (view, hourOfDay, minute) -> {
                startHour = hourOfDay;
                startMinute = minute;
                endHour = startHour + workHoursPerDay;
                endMinute = startMinute;

                setStartTime(); // 출근 시작 시간 설정
                editTextSalary.setEnabled(true); // 연봉 입력 활성화
                editTextSalary.requestFocus(); // 연봉 입력으로 포커스 이동

                // 출근 시간이 변경되었으므로 현재까지의 수입을 초기화하고 다시 계산
                resetDailyEarning();
                updateHourlyWage(); // 시급 업데이트
                handler.post(updateEarningRunnable); // Runnable 시작
            }, startHour, startMinute, true);
            timePickerDialog.show();
        });

        editTextSalary.setEnabled(false); // 초기에는 연봉 입력 비활성화

        editTextSalary.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    salary = Double.parseDouble(s.toString()) * 10000; // 만원 단위 입력을 원 단위로 변환
                    updateHourlyWage();
                } catch (NumberFormatException e) {
                    salary = 0;
                    textViewHourlyWage.setText("시급: ");
                    textViewDailyEarning.setText("오늘 번 돈: 0 원"); // 연봉이 없는 경우 오늘 번 돈을 0으로 설정
                }

                // 연봉이 변경되었으므로 현재까지의 수입을 초기화하고 다시 계산
                resetDailyEarning();
            }
        });

        handler.post(updateEarningRunnable);
    }

    private void scheduleNotification() {
        // AlarmManager를 사용하여 정각에 알림 예약
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0); // 현재 초를 0으로 설정하여 정각에 알림이 트리거되도록 함
        now.set(Calendar.MILLISECOND, 0);

        // 정각에 알림을 예약하기 위해 현재 시간에서 1시간을 더하고 분과 초를 0으로 설정
        now.add(Calendar.HOUR_OF_DAY, 1);

        // 알림을 위한 PendingIntent 생성
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        // AlarmManager를 사용하여 정각에 알림 예약
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, now.getTimeInMillis(), pendingIntent);
        }
    }

    private void setStartTime() {
        startTimeInMillis = Calendar.getInstance().getTimeInMillis();
    }

    private double calculateEarningSinceStartTime() {
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        double elapsedTimeInSeconds = (now.getTimeInMillis() - startTimeInMillis) / 1000.0; // 출근 시간부터 경과한 시간(초)
        double workingTimeInSeconds = (currentHour - startHour) * 3600 + (currentMinute - startMinute) * 60; // 현재까지의 근무 시간(초)
        double earnedSinceStart = hourlySalary * (elapsedTimeInSeconds / 3600.0); // 출근 시간부터 현재까지의 수입
        if ((earnedSinceStart + hourlySalary * (workingTimeInSeconds / 3600.0)) < 0){
            return (earnedSinceStart + hourlySalary * -(workingTimeInSeconds / 3600.0));
        } else{
            // 출근 시간부터의 수입에 현재까지의 근무 시간 동안의 수입을 추가하여 반환
            return earnedSinceStart + hourlySalary * (workingTimeInSeconds / 3600.0);
        }
    }

    private void resetDailyEarning() {
        dailyEarning = 0;
        updateDailyEarning(); // 수입을 초기화 후 바로 화면에 반영
    }

    private void updateHourlyWage() {
        double annualSalaryAfterTax = salary * (1 - taxRate); // 세후 연봉
        double monthlySalary = annualSalaryAfterTax / 12; // 세후 월급
        double weeklySalary = monthlySalary / 4.345; // 세후 주급 (1년을 52주로 나눔)
        double dailySalary = weeklySalary / workDaysPerWeek; // 세후 일급
        hourlySalary = dailySalary / workHoursPerDay; // 세후 시급

        // 시급을 원으로 포맷팅
        DecimalFormat decimalFormat = new DecimalFormat("#,##0 원");
        String formattedHourlyWage = "시급: " + decimalFormat.format(hourlySalary);

        textViewHourlyWage.setText(formattedHourlyWage);

        // 현재 번 돈을 업데이트
        updateDailyEarning();
    }

    private void updateDailyEarning() {
        double earningSinceStartTime = calculateEarningSinceStartTime(); // 출근 시간부터의 수입 계산
        DecimalFormat decimalFormat = new DecimalFormat("#,##0 원");
        String formattedDailyEarning = "오늘 번 돈: " + decimalFormat.format(earningSinceStartTime);

        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        if (startHour != 0 && currentHour > startHour || (currentHour == endHour && currentMinute > endMinute)) {
            formattedDailyEarning += "\n오늘 하루도 고생했어요!";
            // 디버깅을 위해 메시지가 추가되는지 확인합니다.
            System.out.println("오늘 하루도 고생했어요! 메시지가 추가되었습니다.");
        } else {
            // 디버깅을 위해 조건이 맞지 않는 경우를 출력합니다.
            System.out.println("퇴근 시간이 지나지 않았습니다.");
        }

        textViewDailyEarning.setText(formattedDailyEarning);
    }

    private void savePreferences() {
        SharedPreferences preferences = getSharedPreferences("work_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("startHour", startHour);
        editor.putInt("startMinute", startMinute);
        editor.putInt("endHour", endHour);
        editor.putInt("endMinute", endMinute);
        editor.apply();
    }

    private void loadPreferences() {
        SharedPreferences preferences = getSharedPreferences("work_preferences", MODE_PRIVATE);
        startHour = preferences.getInt("startHour", 9);
        startMinute = preferences.getInt("startMinute", 0);
        endHour = preferences.getInt("endHour", 18);
        endMinute = preferences.getInt("endMinute", 0);

        // 저장된 출근 시간이 있으면 연봉 입력을 활성화
        if (startHour != 9 || startMinute != 0 || endHour != 18 || endMinute != 0) {
            editTextSalary.setEnabled(true);
            buttonStartTime.setText(String.format("%02d:%02d 출근", startHour, startMinute));
            editTextSalary.requestFocus();
            setStartTime(); // 출근 시작 시간 설정
            updateHourlyWage(); // 시급 업데이트
        }
    }
}
