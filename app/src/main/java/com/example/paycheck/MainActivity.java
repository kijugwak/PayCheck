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
import android.util.Log;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.os.Build;


import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.util.Calendar;

import java.util.Random;


public class MainActivity extends AppCompatActivity {
    EditText editTextSalary; // 연봉을 입력하는 곳
    TextView textViewHourlyWage; // 시급을 표시하는
    TextView textViewDailyEarning; // 일일 수입을 표시
    Button buttonStartTime; // 출근 시간을 설정하는 button
//    Button buttonTestNotification; // 알림 테스트 버튼

    private TextView textViewQuote; // 명언을 표시하는 textview
    private Handler handler; // 주기적인 작업을 처리하는 handler
    private Runnable updateQuoteRunnable; // 명언을 주기적으로 업데이트하는 Runnable


    double salary = 0; // 연봉
    double dailyEarning = 0; // 일일 수입
    double hourlySalary = 0; // 시급
    public String formattedDailyEarning; // 포맷팅된 일일 수입을 저장할 변수를 선언하고 초기화

    final double taxRate = 0.033; // 세금 비율 (3.3%)
    final int workDaysPerWeek = 5; // 주당 근무일 수
    final int workHoursPerDay = 9; // 하루 근무 시간

    // 출근 및 퇴근 시간을 저장할 변수를 선언한다. 초기값은 'null'
    Integer startHour = null;
    Integer startMinute = null;
    Integer endHour = null;
    Integer endMinute = null;

//    Handler handler = new Handler();
    // 여기서는 수입을 주기적으로 업데이트하는 'Runnable'을 정의한다
    // 현재 시간을 얻어온다.
    Runnable updateEarningRunnable = new Runnable() {
        @Override
        public void run() {
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);
    // -----------------------------------------------------
            // 하단은 출근 시간과 현재 시간을 비교하여 근무 시간 동안 매초 수입을 업데이트한다.
            // 근무 시간이 끝나면 핸들러를 중지한다.
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
    // -----------------------------------------------------

    private long startTimeInMillis = 0; // 출근 시작 시간을 밀리초 단위로 저장

    // 'onCreate' 메서드는 액티비티가 생성될 때 호출된다.
    // 여기서는 레이아웃을 설정한다.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        createNotificationChannel();

        // 명언을 표시할 TextView 찾기
        textViewQuote = findViewById(R.id.textViewQuote);
        // -----------------------------------------------------
        // 하단은 'Handler'와 'Runnable'을 초기화하여 명언을 10초마다 업데이트하도록 설정한다.
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
        // -----------------------------------------------------
        // 하단에는 각 UI 요소를 찾는다.
        editTextSalary = findViewById(R.id.editTextSalary);
        textViewHourlyWage = findViewById(R.id.textViewHourlyWage);
        textViewDailyEarning = findViewById(R.id.textViewDailyEarning);
        buttonStartTime = findViewById(R.id.buttonStartTime);
//        buttonTestNotification = findViewById(R.id.buttonTestNotification); // 알림 테스트 버튼
//        scheduleNotification();
        // -----------------------------------------------------
        // 출근 시간을 설정하는 버튼 클릭 리스너를 정의한다.
        // 시간 선택 다이얼로그를 띄우고, 선택한 시간에 따라 출근 및 퇴근 시간을 설정한다.
        // 출근 시간을 설정하면 연봉 입력을 활성화하고, 수입을 초기화하고 시급을 업데이트한다.
        buttonStartTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
//                    R.style.CustomTimePickerDialogTheme, // 커스텀 테마 적용
                    (view, hourOfDay, minute) -> {
                        startHour = hourOfDay;
                        startMinute = minute;
                        endHour = startHour + workHoursPerDay;
                        endMinute = startMinute;
                        setStartTime(); // 출근 시작 시간 설정
                        editTextSalary.setEnabled(true); // 연봉 입력 활성화
                        editTextSalary.requestFocus(); // 연봉 입력으로 포커스 이동

                        resetDailyEarning();
                        updateHourlyWage(); // 시급 업데이트
                        handler.post(updateEarningRunnable); // Runnable 시작

                        // 출근 시간 버튼 텍스트 업데이트
                        buttonStartTime.setText(String.format("출근시간 : %02d:%02d", startHour, startMinute));
                    },
                    startHour != null ? startHour : 0,
                    startMinute != null ? startMinute : 0,
                    true
            );
            timePickerDialog.show();
        });
        // -----------------------------------------------------
        editTextSalary.setEnabled(false); // 초기에는 연봉 입력 비활성화
        // -----------------------------------------------------
        // 여기서는 연봉 입력란에 대한 'TextWatcher'를 정의한다.
        // 연봉이 변경될 때마다 시급과 일일 수입을 업데이트한다.
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
                        savePreferences(); // 연봉을 변경할 때마다 저장
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
        // -----------------------------------------------------
        // 액티비티가 화면에 보여질 때 SharedPreferences에서 저장된 출근 시간과 연봉을 불러옴
        loadPreferences();

        // 현재까지의 수입
        resetDailyEarning(); // 현재까지의 수입 초기화
    }
    // -----------------------------------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티가 종료될 때 Handler의 작업을 중지
        handler.removeCallbacks(updateQuoteRunnable);
    }
    // -----------------------------------------------------
    // 'MoneyQuote' enum에서 랜덤한 명언을 선택하여 'TextView'에 설정한다.
    private void updateQuote() {
        // MoneyQuote enum에서 랜덤한 명언 선택
        MoneyQuote[] quotes = MoneyQuote.values();
        Random random = new Random();
        MoneyQuote randomQuote = quotes[random.nextInt(quotes.length)];
        // 선택된 명언을 TextView에 설정
        textViewQuote.setText(randomQuote.getQuote());
    }
    // -----------------------------------------------------
    // 액티비티가 다시 화면에 보일 때('onResume')와 사라질때('onPause')
    // 'SharedPreferences'에서 데이터를 불러오고 저장한다.
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
    // -----------------------------------------------------
    // 여기서는 'AlarmManager'를 사용하여 30초마다 알림을 예약한다.
    private void scheduleNotification() {
        // 알림 예약을 위한 메소드
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); // 플래그 추가

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // 5초 후에 알림 트리거 (테스트용)
        long triggerTime = System.currentTimeMillis() + 5000;
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
    }

    // -----------------------------------------------------
    // 출근 시작 시간을 밀리초 단위로 저장한다.
    private void setStartTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, startHour);
        calendar.set(Calendar.MINUTE, startMinute);
        startTimeInMillis = calendar.getTimeInMillis();
    }

    // -----------------------------------------------------
    // 출근 시간부터 현재까지 경과한 시간과 근무 시간을 계산하여 현재까지의 수입을 계산한다.
    private double calculateEarningSinceStartTime() {
        Calendar now = Calendar.getInstance();
        long elapsedTimeInMillis = now.getTimeInMillis() - startTimeInMillis;

        // 경과 시간(초)
        double elapsedTimeInSeconds = elapsedTimeInMillis / 1000.0;
        double maxWorkingTimeInSeconds = workHoursPerDay * 3600.0; // 하루 근무 시간(초)로 변환
        elapsedTimeInSeconds = Math.min(elapsedTimeInSeconds, maxWorkingTimeInSeconds);

        // 출근 시간부터 현재까지의 수입 계산
        double earnedSinceStart = hourlySalary * (elapsedTimeInSeconds / 3600.0);

        return Math.max(0, earnedSinceStart); // 음수 수입 방지
    }
    // -----------------------------------------------------
    // 일일 수입을 초기화하고 화면에 반영한 후, 주기적인 수입 업데이트를 시작한다.
    private void resetDailyEarning() {
        dailyEarning = calculateEarningSinceStartTime(); // 출근 시간부터의 수입 재계산
        updateDailyEarning(); // 수입을 초기화 후 바로 화면에 반영
        handler.post(updateEarningRunnable); // Runnable 다시 시작
    }
    // -----------------------------------------------------
    // 연봉을 기반으로 세후 시급을 계산하고, 이를 포맷팅하여 'TextView'에 표시한다.
    // 또한 현재 번 돈을 업데이트한다.
    private void updateHourlyWage() {
        double annualSalaryAfterTax = salary * (1 - taxRate); // 세후 연봉
        double monthlySalary = annualSalaryAfterTax / 12; // 세후 월급
        double weeklySalary = monthlySalary / 4.345; // 세후 주급 (1년을 52주로 나눔)
        double dailySalary = weeklySalary / workDaysPerWeek; // 세후 일급
        hourlySalary = dailySalary / workHoursPerDay; // 세후 시급

        // 시급을 원으로 포맷팅
        DecimalFormat decimalFormat = new DecimalFormat("#,##0 원");
        if (editTextSalary.getText().toString().isEmpty()){
            String formattedHourlyWage = "시급: " + decimalFormat.format(0);
            textViewHourlyWage.setText(formattedHourlyWage);
        } else {
            String formattedHourlyWage = "시급: " + decimalFormat.format(hourlySalary);
            textViewHourlyWage.setText(formattedHourlyWage);
        }
        // 현재 번 돈을 업데이트
        updateDailyEarning();
    }
    // -----------------------------------------------------
    // 출근 시간부터 현재까지 번 돈을 계산하고 이를 'TextView'에 업데이트한다.
    // 퇴근 시간이 지난 경우 '오늘 하루도 고생했어요!' 메시지를 추가한다.

    private void updateDailyEarning() {
        if (startHour != null && startMinute != null) {
            double earningSinceStartTime = calculateEarningSinceStartTime(); // 출근 시간부터의 수입 계산
            DecimalFormat decimalFormat = new DecimalFormat("#,##0 원");
            if (editTextSalary.getText().toString().isEmpty()){
                formattedDailyEarning = "오늘 번 돈 : " + decimalFormat.format(0);
                String formattedHourlyWage = "시급: " + decimalFormat.format(0);
                textViewHourlyWage.setText(formattedHourlyWage);
                Log.d("TAG", "editTextSalary" + editTextSalary.getText());
            } else{
                formattedDailyEarning = "오늘 번 돈 : " + decimalFormat.format(earningSinceStartTime);
                Log.d("TAG", "editTextSalary" + editTextSalary.getText() + "0이상");

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
            }

            textViewDailyEarning.setText(formattedDailyEarning);
        } else {
            // 연봉을 입력하지 않은 경우 오늘 번 돈을 0원으로 설정합니다.
            textViewDailyEarning.setText("오늘 번 돈: 0 원");
        }
    }

    // -----------------------------------------------------
    // 출근 시간과 연봉, 시급, 일일 수입을 'SharedPreferences'에 저장한다.
    private void savePreferences() {
        SharedPreferences preferences = getSharedPreferences("work_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (startHour != null && startMinute != null) {
            editor.putInt("startHour", startHour);
            editor.putInt("startMinute", startMinute);
            editor.putInt("endHour", endHour);
            editor.putInt("endMinute", endMinute);
            editor.putLong("startTimeInMillis", startTimeInMillis);
        }
        editor.putFloat("salary", (float) salary);
        editor.putFloat("hourlySalary", (float) hourlySalary);
        editor.putFloat("dailyEarning", (float) dailyEarning);
        editor.apply();

        Log.d("Preferences", "Saved salary: " + salary);
        Log.d("Preferences", "Saved hourlySalary: " + hourlySalary);
        Log.d("Preferences", "Saved dailyEarning: " + dailyEarning);
    }
    // -----------------------------------------------------
    // 'SharedPreferences' 에서 저장된 출근 시간과 연봉, 시급, 일일 수입을 불러온다.
    private void loadPreferences() {
        SharedPreferences preferences = getSharedPreferences("work_preferences", MODE_PRIVATE);
        if (preferences.contains("startHour") && preferences.contains("startMinute")) {
            startHour = preferences.getInt("startHour", 9);
            startMinute = preferences.getInt("startMinute", 0);
            endHour = preferences.getInt("endHour", 18);
            endMinute = preferences.getInt("endMinute", 0);
            salary = preferences.getFloat("salary", 0);
            hourlySalary = preferences.getFloat("hourlySalary", 0);
            startTimeInMillis = preferences.getLong("startTimeInMillis", 0);
            dailyEarning = preferences.getFloat("dailyEarning", 0);

            Log.d("Preferences", "Loaded salary: " + salary);
            Log.d("Preferences", "Loaded hourlySalary: " + hourlySalary);
            Log.d("Preferences", "Loaded dailyEarning: " + dailyEarning);

            // 저장된 출근 시간이 있으면 출근 시간 버튼에 텍스트 설정
            buttonStartTime.setText(String.format("출근시간 : %02d:%02d", startHour, startMinute));

            DecimalFormat decimalFormat = new DecimalFormat("#,##0 원");
//            // 연봉이 저장되어 있는 경우 입력 칸에 설정
//            if (salary > 0) {
//                editTextSalary.setText(decimalFormat.format(salary / 10000)); // 만원 단위로 변환하여 설정
//            }
            if (salary != 0) {
                editTextSalary.setText(String.valueOf((int) (salary / 10000))); // 만원 단위로 표시
            }
            String formattedHourlyWage = "시급: " + decimalFormat.format(hourlySalary);
            textViewHourlyWage.setText(formattedHourlyWage);
            // 현재 번 돈을 업데이트
            updateDailyEarning();
            // 현재 번 돈을 화면에 반영
            resetDailyEarning();
            handler.post(updateEarningRunnable); // Runnable 시작
        }
    }
    // 알림 채널 생성 메소드
//    private void createNotificationChannel() {
//        // Android 버전이 Oreo(API 레벨 26) 이상인지 확인
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            CharSequence name = "MyNotificationChannel";
//            String description = "Channel description";
//            int importance = NotificationManager.IMPORTANCE_DEFAULT;
//            NotificationChannel channel = new NotificationChannel("my_channel_id", name, importance);
//            channel.setDescription(description);
//
//            // NotificationManager를 통해 채널 등록
//            NotificationManager notificationManager = getSystemService(NotificationManager.class);
//            notificationManager.createNotificationChannel(channel);
//
//            // 로그 추가
//            Log.d("Notification", "Notification channel created successfully.");
//        }
//    }

}
