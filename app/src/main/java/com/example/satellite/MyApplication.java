package com.example.satellite;

import android.app.Application;

// 안드로이드 애플리케이션이 처음 실행될 때, 즉 앱의 프로세스가 시작될 때 실행.
// Application 클래스는 앱의 전반적인 상태를 관리하는 클래스이며, 앱의 생명 주기 동안 한 번만 생성
public class MyApplication extends Application {
    private AppLifecycleTracker appLifecycleTracker;

    @Override
    public void onCreate() {
        super.onCreate();

        // 앱 라이프사이클 추적기 등록
        appLifecycleTracker = new AppLifecycleTracker();
        registerActivityLifecycleCallbacks(appLifecycleTracker);

        // 앱이 처음 실행될 때 한 번 실행.
        NotificationHelper.createNotificationChannel(this);

    }

    public AppLifecycleTracker getAppLifecycleTracker() {
        return appLifecycleTracker;
    }
}
