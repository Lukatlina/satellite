package com.example.satellite;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
        isRunning = true;
        new Thread(this::connectToServer).start(); // 서버 연결 스레드를 시작
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // 서비스가 강제로 종료되더라도 다시 시작하도록 설정
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder; // 이미 정의된 binder 반환
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

    private void connectToServer() {
        try {
            // 서버 IP 주소 및 포트 설정
            String serverAddress = "192.168.0.12"; // 집 서버 IP 주소
//            String serverAddress = "192.168.0.2"; // 3학원 서버 IP 주소
            int serverPort = 6078; // 서버 포트 번호

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

            // 서버로부터 메시지를 비동기적으로 수신하는 쓰레드 실행
            while (isRunning) {
                String message;
                if ((message = in.readLine()) != null) {
                    Log.i(TAG, "서버 메시지: " + message);
                    // 여기서 받은 메시지를 처리하거나 Broadcast로 액티비티에 전달
                    sendMessageToActivity(message);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "서버 연결 실패: " + e.getMessage());
        }
    }


    public void sendMessageToServer(String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (out != null) {
                    out.println(message);  // 서버로 메시지 전송
                    Log.i(TAG, "Message sent to server: " + message);
                }
            }
        }).start();
    }

    private void sendMessageToActivity(String message) {
        Log.i(TAG, "sendMessageToActivity: 시작 " + message);
        Intent intent = new Intent("com.example.test.ACTION_RECEIVE_MESSAGE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendUserInfoToServer() {
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
}
