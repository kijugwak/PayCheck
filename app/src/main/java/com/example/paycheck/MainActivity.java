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

import java.util.Random;


public class MainActivity extends AppCompatActivity {
    EditText editTextSalary;
    TextView textViewHourlyWage;
    TextView textViewDailyEarning;
    Button buttonStartTime;

    private TextView textViewQuote;
    private Handler handler;
    private Runnable updateQuoteRunnable;


    double salary = 0;
    double dailyEarning = 0;
    double hourlySalary = 0;
    public String formattedDailyEarning;

    final double taxRate = 0.033; // 세금 비율 (3.3%)
    final int workDaysPerWeek = 5; // 주당 근무일 수
    final int workHoursPerDay = 9; // 하루 근무 시간

    Integer startHour = null;
    Integer startMinute = null;
    Integer endHour = null;
    Integer endMinute = null;

//    Handler handler = new Handler();
    Runnable updateEarningRunnable = new Runnable() {
        @Override
        public void run() {
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);

            if (startHour != null && startMinute != null &&
                    ((currentHour > startHour && currentHour < endHour) ||
                            (currentHour == startHour && currentMinute >= startMinute) ||
                            (currentHour == endHour && currentMinute <= endMinute))) {
                dailyEarning += hourlySalary / 3600; // 초 단위로 증가
                updateDailyEarning();
                handler.postDelayed(this, 1000); // 1초마다 돈이 올라가도록 설정
            } else if (startHour != null && startMinute != null &&
                    currentHour == endHour && currentMinute > endMinute) {
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

        // 명언을 표시할 TextView 찾기
        textViewQuote = findViewById(R.id.textViewQuote);

        // Handler 및 Runnable 초기화
        handler = new Handler();
        Runnable updateQuoteRunnable = new Runnable() {
            @Override
            public void run() {
                // 명언 업데이트
                MoneyQuote[] quotes = MoneyQuote.values();
                Random random = new Random();
                MoneyQuote randomQuote = quotes[random.nextInt(quotes.length)];
                textViewQuote.setText(randomQuote.getQuote());

                // 10초 후에 다시 실행
                handler.postDelayed(this, 10000);
            }
        };

        // 첫 번째 명언 업데이트 시작
        handler.post(updateQuoteRunnable);

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

                // 출근 시간 버튼 텍스트 업데이트
                buttonStartTime.setText(String.format("출근시간 : %02d:%02d", startHour, startMinute));
            }, startHour != null ? startHour : 0, startMinute != null ? startMinute : 0, true);
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
                    if (s.toString().isEmpty()) {
                        salary = 0;
                        textViewHourlyWage.setText("시급: ");
                        textViewDailyEarning.setText("오늘 번 돈: 0 원");

                        // 연봉이 변경되었으므로 현재까지의 수입을 초기화하고 다시 계산
                        resetDailyEarning();
                    } else {
                        salary = Double.parseDouble(s.toString()) * 10000; // 만원 단위 입력을 원 단위로 변환
                        updateHourlyWage();
                    }
                } catch (NumberFormatException e) {
                    salary = 0;
                    textViewHourlyWage.setText("시급: ");
                    textViewDailyEarning.setText("오늘 번 돈: 0 원"); // 연봉이 없는 경우 오늘 번 돈을 0으로 설정

                    // 연봉이 변경되었으므로 현재까지의 수입을 초기화하고 다시 계산
                    resetDailyEarning();
                }
            }
        });

        // 액티비티가 화면에 보여질 때 SharedPreferences에서 저장된 출근 시간과 연봉을 불러옴
        loadPreferences();

        // 현재까지의 수입
        resetDailyEarning(); // 현재까지의 수입 초기화
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티가 종료될 때 Handler의 작업을 중지
        handler.removeCallbacks(updateQuoteRunnable);
    }

    private void updateQuote() {
        // MoneyQuote enum에서 랜덤한 명언 선택
        MoneyQuote[] quotes = MoneyQuote.values();
        Random random = new Random();
        MoneyQuote randomQuote = quotes[random.nextInt(quotes.length)];
        // 선택된 명언을 TextView에 설정
        textViewQuote.setText(randomQuote.getQuote());
    }


    @Override
    protected void onResume() {
        super.onResume();

        // SharedPreferences에서 저장된 출근 시간과 연봉을 불러옴
        loadPreferences();
        if (salary > 0) {
            updateHourlyWage();
        }
        // 현재까지의 수입
        resetDailyEarning(); // 현재까지의 수입 초기화
    }

    @Override
    protected void onPause() {
        super.onPause();

        // SharedPreferences에 출근 시간과 연봉을 저장
        savePreferences();
    }

    private void scheduleNotification() {
        // AlarmManager를 사용하여 30초마다 알림 예약
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        long intervalMillis = 30 * 1000; // 30초 간격 (밀리초 단위)

        // 알림을 위한 PendingIntent 생성
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        // AlarmManager를 사용하여 30초 간격으로 알림 예약
        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), intervalMillis, pendingIntent);
        }
    }

    private void setStartTime() {
        startTimeInMillis = Calendar.getInstance().getTimeInMillis();
    }

    private double calculateEarningSinceStartTime() {
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        // 출근 시간부터 경과한 시간(초)
        double elapsedTimeInSeconds = (now.getTimeInMillis() - startTimeInMillis) / 1000.0;

        // 현재까지의 근무 시간(초)
        double workingTimeInSeconds = (currentHour - startHour) * 3600 + (currentMinute - startMinute) * 60;

        // 경과 시간과 근무 시간을 9시간(32400초)으로 제한
        double maxWorkingTimeInSeconds = 9 * 3600.0; // 9시간을 초로 변환
        elapsedTimeInSeconds = Math.min(elapsedTimeInSeconds, maxWorkingTimeInSeconds);
        workingTimeInSeconds = Math.min(workingTimeInSeconds, maxWorkingTimeInSeconds);

        // 출근 시간부터 현재까지의 수입 계산
        double earnedSinceStart = hourlySalary * (elapsedTimeInSeconds / 3600.0);

        // 수입이 음수인 경우를 처리
        if ((earnedSinceStart + hourlySalary * (workingTimeInSeconds / 3600.0)) < 0) {
            return 0;
        } else {
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
        if (startHour != null && startMinute != null) {
            double earningSinceStartTime = calculateEarningSinceStartTime(); // 출근 시간부터의 수입 계산
            DecimalFormat decimalFormat = new DecimalFormat("#,##0 원");
            if (editTextSalary != null){
                formattedDailyEarning = "오늘 번 돈: " + decimalFormat.format(earningSinceStartTime);
            } else {
                formattedDailyEarning = "오늘 번 돈: " + 0;
            }

            Calendar now = Calendar.getInstance();

            // 선택한 출근 시간에 9시간을 더한 시간
            Calendar startCalendar = Calendar.getInstance();
            startCalendar.set(Calendar.HOUR_OF_DAY, startHour);
            startCalendar.set(Calendar.MINUTE, startMinute);
            startCalendar.add(Calendar.HOUR_OF_DAY, 9); // 선택한 출근 시간에 9시간을 더함

            // 현재 시간과 선택한 출근 시간에 9시간을 더한 시간을 비교하여 메시지 표시 여부 결정
            if (now.compareTo(startCalendar) >= 0) {
                // 현재 시간이 출근 시간 이후 9시간이 지난 경우 메시지 추가
                formattedDailyEarning += "\n오늘 하루도 고생했어요!";
                // 디버깅을 위해 메시지가 추가되는지 확인합니다.
                System.out.println("오늘 하루도 고생했어요! 메시지가 추가되었습니다.");
                onStop();
            } else {
                // 디버깅을 위해 조건이 맞지 않는 경우를 출력합니다.
                System.out.println("퇴근 시간이 지나지 않았습니다.");
            }

            textViewDailyEarning.setText(formattedDailyEarning);
        } else {
            // 연봉을 입력하지 않은 경우 오늘 번 돈을 0원으로 설정합니다.
            textViewDailyEarning.setText("오늘 번 돈: 0 원");
        }
    }


    private void savePreferences() {
        SharedPreferences preferences = getSharedPreferences("work_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (startHour != null && startMinute != null) {
            editor.putInt("startHour", startHour);
            editor.putInt("startMinute", startMinute);
            editor.putInt("endHour", endHour);
            editor.putInt("endMinute", endMinute);
        }
        editor.putFloat("salary", (float) salary);
        editor.putFloat("hourlySalary", (float) hourlySalary);
        editor.putFloat("dailyEarning", (float) dailyEarning);
        editor.apply();
    }

    private void loadPreferences() {
        SharedPreferences preferences = getSharedPreferences("work_preferences", MODE_PRIVATE);
        if (preferences.contains("startHour") && preferences.contains("startMinute")) {
            startHour = preferences.getInt("startHour", 9);
            startMinute = preferences.getInt("startMinute", 0);
            endHour = preferences.getInt("endHour", 18);
            endMinute = preferences.getInt("endMinute", 0);
            salary = preferences.getFloat("salary", 0);
            hourlySalary = preferences.getFloat("hourlySalary", 0);
            dailyEarning = preferences.getFloat("dailyEarning", 0);

            // 저장된 출근 시간이 있으면 출근 시간 버튼에 텍스트 설정
            buttonStartTime.setText(String.format("출근시간 : %02d:%02d", startHour, startMinute));
            setStartTime(); // 출근 시작 시간 설정

            // 시급을 업데이트합니다.
            updateHourlyWage();

            // 연봉이 저장되어 있는 경우 입력 칸에 설정
            if (salary > 0) {
                DecimalFormat decimalFormat = new DecimalFormat("#,##0");
                editTextSalary.setText(decimalFormat.format(salary / 10000)); // 만원 단위로 변환하여 설정
            }

            // 현재 번 돈을 화면에 반영
            updateDailyEarning();
        }
    }
}
