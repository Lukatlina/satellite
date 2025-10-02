package com.example.satellite;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatService extends Service {

    private static final String TAG = "ChatService";
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isRunning;
    private AppLifecycleTracker appLifecycleTracker;
    private int is_artist;
    SharedPreferences user;

    // 서비스 바인더 정의
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public ChatService getService() {
            return ChatService.this; // 서비스 인스턴스를 반환하여 액티비티가 서비스와 상호작용할 수 있게 합니다.
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: 시작");

        // Application에서 AppLifecycleTracker 가져오기
        appLifecycleTracker = ((MyApplication) getApplication()).getAppLifecycleTracker();

        // 서비스가 실행 중임을 나타내는 플래그
        isRunning = true;

        // 별도의 스레드에서 서버에 연결 시도 (UI 스레드가 막히지 않도록)
        new Thread(this::connectToServer).start(); // 서버 연결 스레드를 시작

        // 알림 채널 생성 및 알림 설정
        NotificationHelper.createNotificationChannel(this);

        user = getSharedPreferences("user", Context.MODE_PRIVATE);
        // SharedPreferences에서 아티스트 여부 확인
        is_artist = user.getInt("is_artist", -1);

        // 아티스트라면 고정 알림 표시
        if (is_artist == 1) {
            NotificationHelper.showPersistentArtistNotification(this);
        }
    }

    // START_STICKY를 반환하여 서비스가 강제로 종료되었을 때 시스템이 서비스를 다시 시작하도록 설정합니다.
    // 이는 채팅 앱처럼 지속적인 연결이 필요한 서비스에 적합한 설정입니다.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isRunning) {
            Log.i(TAG, "서비스가 이미 실행 중입니다.");
            return START_STICKY;  // 이미 실행 중이면 다시 시작하지 않음
        }
        isRunning = true;
        new Thread(this::connectToServer).start();  // 서버 연결 시도
        return START_STICKY;
    }

    // 서비스가 바인딩되었을 때 액티비티가 binder를 통해 서비스와 상호작용할 수 있습니다.
    @Override
    public IBinder onBind(Intent intent) {
        return binder; // 이미 정의된 binder 반환
    }

    private void connectToServer() {
        try {
            // 서버 IP 주소 및 포트 설정
//            String serverAddress = "192.168.0.12"; // 서버 IP 주소
            int serverPort = 6078; // 서버 포트 번호
            String serverAddress = "192.168.0.2";

            // 소켓 생성 및 연결 시도
            socket = new Socket(serverAddress, serverPort);
            Log.i(TAG, "서버 연결 성공");

            // 서버와의 데이터 송수신 스트림 설정
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // 서버에 로그인한 사용자 정보를 전송
            // 유저의 정보를 JSON으로 변환한 뒤 보내준다.
            // 연결 될 때 딱 한번 실행됨
            sendUserInfoToServer();

            // 서버로부터 메시지를 지속적으로 수신 (별도의 스레드에서 실행)
            while (isRunning) {
                String message;
                if ((message = in.readLine()) != null) {
                    Log.i(TAG, "서버 메시지: " + message);
                    onMessageReceived(message);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "서버 연결 실패: " + e.getMessage());
        }
    }

    private void onMessageReceived(String message) {
        Log.i(TAG, "포그라운드 상태인가? : " + appLifecycleTracker.isAppInForeground());
        Log.i(TAG, "채팅 화면이 열려있는가? : " + appLifecycleTracker.isChatScreenOpen());


        // 현재 보고 있는 채팅방의 ID (이 값을 추적하는 변수가 필요합니다)
        int currentArtistId = appLifecycleTracker.getCurrentArtistId();

        // 현재 보고 있는 채팅방의 ID (이 값을 추적하는 변수가 필요합니다)
        int currentMessageId = appLifecycleTracker.getCurrentMessageId();

        // 현재 보고 있는 채팅방의 ID (이 값을 추적하는 변수가 필요합니다)
        int currentChatId = appLifecycleTracker.getCurrentChatId();

        // 수신한 메시지의 채팅방 ID를 파싱 (예: JSON 형식의 메시지에서 가져오기)

        int receivedArtistId;
        int receivedChatId;
        int receivedMessageId;
        try {
            JSONObject received_Message = new JSONObject(message);
            if (is_artist == 0) {
                receivedArtistId = received_Message.getInt("sender_id");
            }else{
                receivedArtistId = user.getInt("user_id", -1);
            }
            receivedChatId = received_Message.getInt("chat_id");
            receivedMessageId = received_Message.getInt("message_id");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        Log.i(TAG, "currentArtistId: " + currentArtistId);
        Log.i(TAG, "receivedArtistId: " + receivedArtistId);

        Log.i(TAG, "currentChatId: " + currentChatId);
        Log.i(TAG, "receivedChatId: " + receivedChatId);

        Log.i(TAG, "currentMessageId: " + currentMessageId);
        Log.i(TAG, "receivedMessageId: " + receivedMessageId);


        if (appLifecycleTracker.isAppInForeground()) {
            // 앱이 포그라운드 상태인지 확인
            if (((appLifecycleTracker.isChatScreenOpen() || appLifecycleTracker.isArtistChatScreenOpen() )
                    && (receivedChatId == currentChatId || currentArtistId == receivedArtistId ))
                    || (appLifecycleTracker.isMessageBoxScreenOpen()
                    && (receivedChatId == currentChatId || currentArtistId == receivedArtistId ) && receivedMessageId == currentMessageId )
            ) {
                // 1. 채팅창을 보고 있을 때: UI에 즉시 메시지 표시
                Log.d(TAG, "채팅창에서 메시지를 수신했습니다.");
                sendMessageToActivity(message); // 메시지 내용을 UI에 반영
            } else if (appLifecycleTracker.isChatsListOpen() && is_artist == 0) {
                // 2. 채팅방 리스트 화면일 때: 채팅 리스트 아이템 업데이트 및 알림
                Log.d(TAG, "채팅방 리스트 화면에서 메시지를 수신했습니다.");
                sendMessageToChatList(message); // 채팅방 아이템 업데이트
                NotificationHelper.sendChatNotification(this, message); // 알림 띄우기
            } else if (is_artist == 1 && !appLifecycleTracker.isArtistChatScreenOpen() && !appLifecycleTracker.isMessageBoxScreenOpen()) {
                Log.i(TAG, "onMessageReceived: 아티스트");
            } else {
                // 3. 다른 화면일 때: 알림만 띄움
                Log.d(TAG, "다른 화면에서 메시지를 수신했습니다.");
                NotificationHelper.sendChatNotification(this, message);
            }
        }else {
            // 앱이 백그라운드일 때: 알림만 띄움
            Log.d(TAG, "앱이 백그라운드에 있습니다. 알림을 띄웁니다.");
            NotificationHelper.sendChatNotification(this, message);
        }
    }

    // 이 메서드는 클라이언트에서 서버로 메시지를 전송하는 기능을 수행합니다.
    // 서버와의 통신이 네트워크 작업이므로 별도의 스레드에서 실행됩니다.
    public void sendMessageToServer(String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (out != null) {
                    out.println(message);  // 서버로 메시지 전송
                    Log.i(TAG, "서버로 메시지 전송: " + message);
                }
            }
        }).start();
    }

    // 서버에서 받은 메시지를 브로드캐스트로 액티비티에 전달하여 UI를 업데이트할 수 있습니다.
    private void sendMessageToActivity(String message) {
        Log.i(TAG, "sendMessageToActivity: 시작 " + message);
        Intent intent = new Intent("com.example.satellite.ACTION_RECEIVE_MESSAGE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // 채팅방 리스트로 메시지를 전달하는 메서드
    private void sendMessageToChatList(String message) {
        Log.i(TAG, "sendMessageToChatList: 시작 " + message);
        Intent intent = new Intent("com.example.satellite.ACTION_UPDATE_CHAT_LIST");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // 서버와 연결될 때 사용자 정보를 서버로 전송하는 메서드입니다.
    // SharedPreferences에서 사용자 정보를 가져와 JSON 형식으로 서버에 전송합니다.
    public void sendUserInfoToServer() {
        try {
            // SharedPreferences에서 저장된 user_id와 is_artist를 가져옴
            int userId = getUserIdFromPreferences();
            int isArtist = getIsArtistFromPreferences();

            // JSON 객체 생성
            JSONObject userInfo = new JSONObject();
            userInfo.put("user_id", userId);
            userInfo.put("is_artist", isArtist);

            // JSON 객체를 문자열로 변환하여 서버로 전송
            String jsonString = userInfo.toString();
            out.println("/login " + jsonString);

            Log.i(TAG, "유저 정보가 서버로 전송되었습니다 : " + jsonString);

        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON object", e);
        }
    }

    public void sendChatRoomInfoToServer(int chat_id) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // JSON 객체 생성
                    JSONObject chatInfo = new JSONObject();
                    chatInfo.put("chat_id", chat_id);

                    Log.i(TAG, "run: chat_id 제대로 나오는지 확인 : " + chat_id);

                    // JSON 객체를 문자열로 변환하여 서버로 전송
                    String jsonString = chatInfo.toString();
                    out.println("/chatroom " + jsonString);

                    Log.i(TAG, "채팅방 정보가 서버로 전송되었습니다 : " + jsonString);
                } catch (Exception e) {
                    Log.e(TAG, "Error creating JSON object", e);
                }
            }
        }).start(); // 네트워크 작업을 별도의 스레드에서 실행
    }

    private int getUserIdFromPreferences() {
        // SharedPreferences에서 저장된 user_id를 가져오는 메서드
        return getSharedPreferences("user", MODE_PRIVATE).getInt("user_id", -1);
    }

    private int getIsArtistFromPreferences() {
        return getSharedPreferences("user", MODE_PRIVATE).getInt("is_artist", -1);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false; // 서비스 종료 시 스레드를 중지
        try {
            if (socket != null) {
                socket.close(); // 소켓 종료
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
