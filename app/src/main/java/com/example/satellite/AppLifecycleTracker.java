package com.example.satellite;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.example.satellite.ui.ArtistChatActivity;
import com.example.satellite.ui.ChatsActivity;
import com.example.satellite.ui.FanChatActivity;
import com.example.satellite.ui.MessageBoxActivity;

public class AppLifecycleTracker implements Application.ActivityLifecycleCallbacks {
    private int activityCount = 0; // 포그라운드에 있는 액티비티 수
    private Activity currentActivity; // 현재 표시 중인 액티비티
    String TAG = "AppLifecycleTracker";
    private boolean isForeground = false;
    private int currentArtistId = -1; // 현재 보고 있는 artist의 ID (-1은 기본값으로, 아무도 보고 있지 않은 상태)
    private int currentMessageId = -1; // 현재 보고 있는 메시지 ID (-1은 기본값으로, 아무도 보고 있지 않은 상태)
    private int currentChatId = -1; // 현재 보고 있는 메시지 ID (-1은 기본값으로, 아무도 보고 있지 않은 상태)



    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }

    @Override
    public void onActivityStarted(Activity activity) {
        // activityCount가 0일 때만 증가 (포그라운드로 전환된 첫 액티비티)
//        if (activityCount == 0) {
        activityCount++;
        Log.d(TAG, "onActivityStarted: " + activity.getLocalClassName() + ", activityCount: " + activityCount);
        checkForegroundState();
        Log.d(TAG, "앱이 포그라운드로 전환되었습니다." + activityCount);
//        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        // 액티비티가 사용자에게 표시됨
        // Resumed 상태일 때 포그라운드로 전환됨을 체크 (activityCount 증가는 여기서 하지 않음)
        setCurrentActivity(activity);
        Log.d(TAG, "onActivityResumed: " + activity.getLocalClassName() + ", activityCount: " + activityCount);
        checkForegroundState();
        Log.d(TAG, "앱이 포그라운드로 전환되었습니다." + activityCount);
//        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // 액티비티가 사용자에게 표시되지 않음
        Log.d(TAG, "onActivityPaused: " + activity.getLocalClassName());
        setCurrentActivity(null); // 포커스를 잃었으므로 currentActivity를 null로 설정
    }

    @Override
    public void onActivityStopped(Activity activity) {
        // activityCount가 0 이상일 때만 감소 (백그라운드로 전환된 마지막 액티비티)
        if (activity.isFinishing()) {
//            if (activityCount > 0) {
            activityCount--;
            Log.d(TAG, "onActivityStopped: " + activity.getLocalClassName() + ", activityCount: " + activityCount);
            checkForegroundState();
            Log.d(TAG, "앱이 백그라운드로 전환되었습니다." + activityCount);
//            }
        }else{
            Log.d(TAG, "onActivityStopped (not finishing): " + activity.getLocalClassName());
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }

    @Override
    public void onActivityDestroyed(Activity activity) { }

    // 포그라운드 상태를 확인하고 로그 출력
    private void checkForegroundState() {
        Log.i(TAG, "checkForegroundState: ");
        boolean newForegroundState = activityCount > 0;
        if (newForegroundState != isForeground) {
            isForeground = newForegroundState;
            if (isForeground) {
                Log.d(TAG, "앱이 포그라운드로 전환되었습니다. ActivityCount: " + activityCount);
            } else {
                Log.d(TAG, "앱이 백그라운드로 전환되었습니다. ActivityCount: " + activityCount);
            }
        }
    }

    // 현재 활성화된 액티비티 설정
    public void setCurrentActivity(Activity activity) {
        Log.i(TAG, "Current activity set to: " + (activity != null ? activity.getClass().getSimpleName() : "null"));
        this.currentActivity = activity;
    }

    // 포그라운드인지 백그라운드인지 체크
    public boolean isAppInForeground() {
        Log.i(TAG, "isAppInForeground" + activityCount + " + " + isForeground);
        return isForeground;
    }

    // 현재 채팅 화면을 보고 있는지 확인
    public boolean isChatScreenOpen() {
        Log.i(TAG, "isChatScreenOpen: currentActivity = " + (currentActivity != null ? currentActivity.getClass().getSimpleName() : "null"));
        return currentActivity instanceof FanChatActivity;
    }

    // 현재 채팅 화면을 보고 있는지 확인
    public boolean isChatsListOpen() {
        Log.i(TAG, "isChatsListOpen: currentActivity = " + (currentActivity != null ? currentActivity.getClass().getSimpleName() : "null"));
        return currentActivity instanceof ChatsActivity;
    }

    // 현재 아티스트 채팅 화면을 보고 있는지 확인
    public boolean isArtistChatScreenOpen() {
        Log.i(TAG, "isArtistChatScreenOpen: currentActivity = " + (currentActivity != null ? currentActivity.getClass().getSimpleName() : "null"));
        return currentActivity instanceof ArtistChatActivity;
    }

    // 현재 메시지 박스 화면을 보고 있는지 확인
    public boolean isMessageBoxScreenOpen() {
        Log.i(TAG, "isMessageBoxScreenOpen: currentActivity = " + (currentActivity != null ? currentActivity.getClass().getSimpleName() : "null"));
        return currentActivity instanceof MessageBoxActivity;
    }

    public int getCurrentArtistId() {
        Log.i(TAG, "getCurrentArtistId: " + currentArtistId);
        return currentArtistId;
    }

    public void setCurrentArtistId(int currentArtistId) {
        Log.i(TAG, "setCurrentArtistId: " + currentArtistId);
        this.currentArtistId = currentArtistId;
    }


    public int getCurrentMessageId() {
        return currentMessageId;
    }

    public void setCurrentMessageId(int currentMessageId) {
        this.currentMessageId = currentMessageId;
    }

    public int getCurrentChatId() {
        return currentChatId;
    }

    public void setCurrentChatId(int currentChatId) {
        this.currentChatId = currentChatId;
    }
}
