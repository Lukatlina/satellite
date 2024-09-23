package com.example.satellite;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FanChatActivity extends AppCompatActivity  {
    private static final String TAG = "FanChatActivity";
    private ChatService chatService;
    private boolean isBound = false;

    ImageView iv_message_search;

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

    String uniq_id;
    int user_id;
    int artist_id;

    RecyclerView recyclerView;
    LinearLayoutManager linear;
    ChatAdapter adapter;

    ImageView iv_fan_chat_back_btn;
    EditText et_chat_message;
    ImageButton btn_fan_chat_send_message;
    TextView tv_fan_chat_room_name;

    ArrayList<chat_user> messages = new ArrayList<>();

    chat_user currentUser;

    String fan_image;
    String fan_nickname;

    int chat_id;
    String chatroom_name;
    int sender_id;
    int is_artist;
    String image;
    String nickname;
    String message;
    String sent_time;
    String formattedTime;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 바인딩된 서비스의 IBinder를 통해 서비스 인스턴스를 가져옵니다.
            ChatService.LocalBinder binder = (ChatService.LocalBinder) service;
            chatService = binder.getService();
            isBound = true;
            Log.i(TAG, "서비스 연결됨");

            // 필요 시 서비스와 상호작용하는 코드를 작성합니다.
            // 메시지 데이터를 가져오는 Retrofit HTTP 요청 설정
            fetchChatData();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            chatService = null;
            isBound = false;
            Log.i(TAG, "서비스 연결이 해제됨");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fan_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fan_chat), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        tv_fan_chat_room_name = findViewById(R.id.tv_fan_chat_room_name);
        recyclerView = findViewById(R.id.chatroom_recy);

        iv_fan_chat_back_btn = findViewById(R.id.iv_fan_chat_back_btn);
        et_chat_message = findViewById(R.id.et_chat_message);
        btn_fan_chat_send_message = findViewById(R.id.btn_fan_chat_send_message);

        linear = new LinearLayoutManager(getApplicationContext());
        linear.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linear);
        adapter = new ChatAdapter(messages, getApplicationContext());
        recyclerView.setAdapter(adapter);

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        // 먼저 인텐트에서 user_id를 꺼내오기
        Intent intent = getIntent();
        artist_id = intent.getIntExtra("artist_id", -1);
        uniq_id = user.getString("uniq_id", "");
        user_id = user.getInt("user_id", -1);
        is_artist = user.getInt("is_artist",0);



        Log.i(TAG, "uniq_id : " + uniq_id);
        Log.i(TAG, "user_id : " + user_id);
        Log.i(TAG, "is_artist : " + is_artist);
        Log.i(TAG, "artist_id : " + artist_id);


        Log.i(TAG, "onCreate: http 요청 전 메시지 리스트 크기" + messages.size());

        // 서비스 시작 및 바인딩
        Intent serviceIntent = new Intent(FanChatActivity.this, ChatService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // BroadcastReceiver 등록
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
                new IntentFilter("com.example.test.ACTION_RECEIVE_MESSAGE"));

        // 유저 정보를 가져오는 Retrofit HTTP 요청 설정
        loadUserData(uniq_id, is_artist);



        btn_fan_chat_send_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 텍스트 메시지로 부터 작성된 메시지 가져오기
                String sending_message = et_chat_message.getText().toString().trim();

                if (!sending_message.isEmpty()) {
                    // 1. UI에 메시지를 즉시 추가 (보낸 사람의 메시지로 추가)
                    chat_user currentMessage = new chat_user(chat_id, chatroom_name, user_id, 0, fan_image, fan_nickname, sending_message, changeFormattedTime(getCurrentTime()));
                    // 작성 유저의 화면에 바로 보일 수 있도록 리스트에 추가
                    messages.add(currentMessage);
                    // 포지션은 0부터 시작하기 때문에 전체 크기의 -1을 해준다.
                    adapter.notifyItemInserted(messages.size() - 1);
                    scrollToBottom();

                    // 2. chat_user 객체를 JSON 문자열로 변환
                    JSONObject jsonMessage = new JSONObject();
                    try {
                        jsonMessage.put("chat_id", currentMessage.getChat_id());
                        jsonMessage.put("chatroom_name", currentMessage.getChatroom_name());
                        jsonMessage.put("sender_id", currentMessage.getSender_id());
                        jsonMessage.put("is_artist", currentMessage.getIs_artist());
                        jsonMessage.put("image", currentMessage.getImage());
                        jsonMessage.put("nickname", currentMessage.getNickname());
                        jsonMessage.put("message", currentMessage.getMessage());
                        jsonMessage.put("sent_time", getCurrentTime());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    // 3. 서버에 JSON 형태 메시지를 전송
                    sendMessageToService(jsonMessage.toString());

                    Log.i(TAG, "보내기 전 데이터 확인 : " + jsonMessage);

                    // 4. 전송 후 입력창 비우기
                    et_chat_message.setText("");
                }
            }
        });

        iv_fan_chat_back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }


    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String changeFormattedTime(String sent_time) {
        // 원래 형식의 시간 파싱을 위한 SimpleDateFormat
        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 원하는 형식으로 변환하기 위한 SimpleDateFormat
        SimpleDateFormat newFormat = new SimpleDateFormat("hh:mm");

        // 문자열을 Date 객체로 파싱
        Date date = null;
        try {
            date = originalFormat.parse(sent_time);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        // Date 객체를 새로운 형식으로 포맷
        formattedTime = newFormat.format(date);

        // 포맷된 시간 출력
        System.out.println("Formatted Time: " + formattedTime);
        return formattedTime;
    }



    private void fetchChatData() {
        // 기존 DB에서 데이터를 불러오는 로직
        // 유저의 정보와 아티스트 여부, 아티스트 id를 가지고 기존의 DB에서 데이터를 불러온다.
        ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
        Call<ArrayList<chat_user>> call = service.sendChatImformaition(user_id, is_artist, artist_id);

        call.enqueue(new Callback<ArrayList<chat_user>>() {
            @Override
            public void onResponse(Call<ArrayList<chat_user>> call, Response<ArrayList<chat_user>> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "response값" + response);

                    currentUser = null;

                    ArrayList<chat_user> data = response.body();
                    Log.i(TAG, "바디값" + data);
                    Log.i(TAG, "제대로 받았나?" + data.get(0).getNickname());
                    Log.i(TAG, "데이터 길이는?" + data.size());

                    for (int i = 0; i < data.size(); i++) {
                        chat_user user = data.get(i);
                        chat_id = user.getChat_id();
                        chatroom_name = user.getChatroom_name();
                        tv_fan_chat_room_name.setText(chatroom_name);
                        sender_id = user.getSender_id();
                        is_artist = user.getIs_artist();

                        image = user.getImage();

                        System.out.println("image == null 밖" + i);
                        if (image == null || image.isEmpty()){
                            System.out.println("image == null 안" + i);
                            image = "";
                        }
                        nickname = user.getNickname();
                        message = user.getMessage();
                        sent_time = user.getSent_time();




//                        // 원래 형식의 시간 파싱을 위한 SimpleDateFormat
//                        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//                        // 원하는 형식으로 변환하기 위한 SimpleDateFormat
//                        SimpleDateFormat newFormat = new SimpleDateFormat("hh:mm");
//
//                        try {
//                            // 문자열을 Date 객체로 파싱
//                            Date date = originalFormat.parse(sent_time);
//
//                            // Date 객체를 새로운 형식으로 포맷
//                            formattedTime = newFormat.format(date);
//
//                            // 포맷된 시간 출력
//                            System.out.println("Formatted Time: " + formattedTime);
//                        } catch (ParseException e) {
//                            e.printStackTrace();
//                        }


                        if (is_artist == 0) {
                            if (currentUser == null) {
                                currentUser = new chat_user(chat_id, chatroom_name, sender_id, is_artist, image, nickname, message, changeFormattedTime(sent_time));
                            }
                            messages.add(new chat_user(chat_id, chatroom_name, sender_id, is_artist, image, nickname, message, changeFormattedTime(sent_time)));
                            Log.i(TAG, "팬 메시지 추가 : " + nickname);
                        } else {
                            messages.add(new chat_user(chat_id, chatroom_name, sender_id, is_artist, image, nickname, message, changeFormattedTime(sent_time)));
                            Log.i(TAG, "아티스트 메시지 추가 : " + nickname);
                            Log.i(TAG, "아티스트의 chat_id 확인 : " + chatroom_name);
                            Log.i(TAG, "아티스트의 sender_id 확인 : " + sender_id);
                            Log.i(TAG, "아티스트의 is_artist 확인 : " + is_artist);
                            Log.i(TAG, "아티스트의 image 확인 : " + image);
                            Log.i(TAG, "아티스트의 nickname 확인 : " + nickname);
                            Log.i(TAG, "아티스트의 message 확인 : " + message);
                            Log.i(TAG, "아티스트의 sent_time 확인 : " + formattedTime);
                            Log.i(TAG, "messages size: " + messages.size());
                        }
                    }
                    adapter.notifyDataSetChanged();
                    scrollToBottom();

                    // 성공적으로 데이터를 가져왔을 때 서비스에 메시지 전송
                    if (chatService != null) {
                        chatService.sendChatRoomInfoToServer(chat_id);
                    } else {
                        Log.e(TAG, "ChatService is not bound.");
                        // 필요한 경우 서비스가 null일 때의 처리
                    }


//                    if (isBound && chatService != null) {
//                        try {
//                            JSONObject userData = new JSONObject();
//                            userData.put("chat_id : 어디냐 0", chat_id);
//                            sendMessageToService(userData.toString());
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                    }

                } else {
                    Toast.makeText(FanChatActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ArrayList<chat_user>> call, Throwable t) {
                Toast.makeText(FanChatActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.i(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    private void loadUserData(String uniq_id, int is_artist) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("http://52.78.77.90/satellite/user_data.php").newBuilder();
        // get방식 파라미터 추가
        String url = urlBuilder.build().toString();

        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("uniq_id", uniq_id)
                .add("is_artist", String.valueOf(is_artist))
                .build();

        // 요청 만들기
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        // 응답 콜백
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(okhttp3.Call call, final okhttp3.Response response) throws IOException {

                // 서브 스레드 Ui 변경 할 경우 에러
                // 메인스레드 Ui 설정
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            if (!response.isSuccessful()) {
                                // 응답 실패
                                Log.i(TAG, "응답실패");
                                Toast.makeText(getApplicationContext(), "네트워크 문제 발생", Toast.LENGTH_SHORT).show();

                            } else {
                                // 응답 성공
                                Log.i(TAG, "응답 성공");
                                String responseData = response.body().string();
                                Log.i(TAG, "무슨 데이터? : " + responseData);
                                JSONObject user_data = new JSONObject(responseData);

                                if (user_data.getInt("result") == 1) {
                                    if (user_data.getString("image").isEmpty() || user_data.isNull("image")) {
                                        fan_image = "";
                                    }else{
                                        fan_image = user_data.getString("image");
                                    }
                                    fan_nickname = user_data.getString("nickname");
                                    Log.i(TAG, "유저 닉네임 : " + nickname);

                                } else {
                                    Toast.makeText(getApplicationContext(), "다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }


    // BroadcastReceiver 구현
    // 서버로부터 메시지 수신하면 사용
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.i(TAG, "Received message: " + message);

            // String -> Json 객체로 변환 후에 각 값을 변수에 저장
            // 2. chat_user 객체를 JSON 문자열로 변환

            try {
                JSONObject received_Message = new JSONObject(message);
                int received_chat_id = received_Message.getInt("chat_id");
                String received_chatroom_name = received_Message.getString("chatroom_name");
                int received_sender_id = received_Message.getInt("sender_id");
                int received_is_artist = received_Message.getInt("is_artist");
                String received_image = received_Message.getString("image");
                String received_nickname = received_Message.getString("nickname");
                String received_message = received_Message.getString("message");
                String received_sent_time = received_Message.getString("sent_time");

                if (received_image == null || received_image.isEmpty()){
                    received_image = "";
                }

                // UI 업데이트 처리
                chat_user received_user = new chat_user
                        (received_chat_id, received_chatroom_name, received_sender_id, received_is_artist, received_image, received_nickname, received_message, changeFormattedTime(received_sent_time));
                messages.add(received_user);

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

                // 새로운 메시지를 받은 후 스크롤을 마지막으로 이동
                scrollToBottom();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    };




    // 서비스로 메시지를 보내는 메서드
    private void sendMessageToService(String message) {
        if (isBound && chatService != null) {
            chatService.sendMessageToServer(message);
        } else {
            Log.i(TAG, "서비스에 연결되지 않았습니다.");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 서비스 바인딩 해제
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        // BroadcastReceiver 해제
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    // 메시지가 추가될 때 RecyclerView의 스크롤을 마지막으로 설정
    private void scrollToBottom() {
        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
    }
}