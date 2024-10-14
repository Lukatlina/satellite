package com.example.satellite.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.satellite.ApiService;
import com.example.satellite.ChatService;
import com.example.satellite.R;
import com.example.satellite.RetrofitClientInstance;
import com.example.satellite.adapter.ChatAdapter;
import com.example.satellite.model.chat_user;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageBoxActivity extends AppCompatActivity {

    private static final String TAG = "MessageBoxActivity";
    private ChatService chatService;
    private boolean isBound = false;

    private LinearLayout searchLayout;
    private ImageView searchIcon, closeSearch;
    ImageView iv_message_box_back_btn;

    // 팬 메시지를 받기 위한 정보
    int chat_id;
    int message_id;

    RecyclerView recyclerView;
    LinearLayoutManager linear;
    ChatAdapter adapter;

    ArrayList<chat_user> fanMessages = new ArrayList<>();

    int sender_id;
    int is_artist;
    String image;
    String nickname;
    String message;
    String sent_time;
    String formattedTime;
    String lastDate = "";

    LinearLayout message_box_linear, search_layout;

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
            fetchFanChatData();
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
        setContentView(R.layout.activity_message_box);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.message_box), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // XML 요소들 초기화
        message_box_linear = findViewById(R.id.message_box_linear);
        searchLayout = findViewById(R.id.search_layout);
        searchIcon = findViewById(R.id.iv_message_box_search_icon);
        closeSearch = findViewById(R.id.iv_message_box_close_search);
        iv_message_box_back_btn = findViewById(R.id.iv_message_box_back_btn);

        recyclerView = findViewById(R.id.message_box_recy);

        linear = new LinearLayoutManager(getApplicationContext());
        linear.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linear);
        adapter = new ChatAdapter(fanMessages, getApplicationContext());
        recyclerView.setAdapter(adapter);

        // 먼저 인텐트에서 user_id를 꺼내오기
        Intent intent = getIntent();
        chat_id = intent.getIntExtra("chat_id", -1);
        message_id = intent.getIntExtra("message_id",-1);

        Log.i(TAG, "chat_id: " + chat_id);
        Log.i(TAG, "message_id: " + message_id);

        // 서비스 시작 및 바인딩
        Intent serviceIntent = new Intent(MessageBoxActivity.this, ChatService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // BroadcastReceiver 등록
        LocalBroadcastManager.getInstance(this).registerReceiver(fanMessageReceiver,
                new IntentFilter("com.example.test.ACTION_RECEIVE_MESSAGE"));

        iv_message_box_back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ArtistChatActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        // SearchView 표시 설정
        searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchLayout.setVisibility(View.VISIBLE); // SearchView 보이기
                message_box_linear.setVisibility(View.GONE); // Search Icon 숨기기
            }
        });

        // SearchView 닫기 설정
        closeSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchLayout.setVisibility(View.GONE); // SearchView 숨기기
                message_box_linear.setVisibility(View.VISIBLE); // Search Icon 보이기
            }
        });
    }

    private String changeFormattedTime(String sent_time) {
        // 원래 형식의 시간 파싱을 위한 SimpleDateFormat
        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 원하는 형식으로 변환하기 위한 SimpleDateFormat
        SimpleDateFormat newFormat = new SimpleDateFormat("HH:mm");

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



    private void fetchFanChatData() {
        // 기존 DB에서 데이터를 불러오는 로직
        // 유저의 정보와 아티스트 여부, 아티스트 id를 가지고 기존의 DB에서 데이터를 불러온다.
        ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
        Call<ArrayList<chat_user>> call = service.sendFanChatImformaition(chat_id, message_id);

        call.enqueue(new Callback<ArrayList<chat_user>>() {
            @Override
            public void onResponse(Call<ArrayList<chat_user>> call, Response<ArrayList<chat_user>> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "response값" + response);

                    ArrayList<chat_user> data = response.body();
                    Log.i(TAG, "바디값" + data);
                    Log.i(TAG, "제대로 받았나?" + data.get(0).getNickname());
                    Log.i(TAG, "데이터 길이는?" + data.size());

                    for (int i = 0; i < data.size(); i++) {
                        chat_user user = data.get(i);
                        sender_id = user.getSender_id();
                        nickname = user.getNickname();
                        message = user.getMessage();
                        sent_time = user.getSent_time();

                        // 원래 형식의 시간 파싱을 위한 SimpleDateFormat
                        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        // 원하는 형식으로 변환하기 위한 SimpleDateFormat
                        SimpleDateFormat newFormat = new SimpleDateFormat("yyyy년 MM월 dd일 EEEE");

                        try {
                            // 문자열을 Date 객체로 파싱
                            Date date = originalFormat.parse(sent_time);

                            // Date 객체를 새로운 형식으로 포맷
                            formattedTime = newFormat.format(date);

                            // 포맷된 시간 출력
                            System.out.println("Formatted Time: " + formattedTime);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        if (!lastDate.equals(formattedTime)) {
                            fanMessages.add(new chat_user(2, formattedTime));
                            lastDate = formattedTime;
                        }


                        fanMessages.add(new chat_user(1, sender_id, nickname, message, changeFormattedTime(sent_time)));
                        Log.i(TAG, "팬 sender_id 확인 : " + sender_id);
                        Log.i(TAG, "팬 nickname 확인 : " + nickname);
                        Log.i(TAG, "팬 message 확인 : " + message);
                        Log.i(TAG, "팬 sent_time 확인 : " + formattedTime);
                        Log.i(TAG, "팬 messages size: " + fanMessages.size());

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

                } else {
                    Toast.makeText(MessageBoxActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ArrayList<chat_user>> call, Throwable t) {
                Toast.makeText(MessageBoxActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.i(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    // BroadcastReceiver 구현
    // 서버로부터 메시지 수신하면 사용
    private BroadcastReceiver fanMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.i(TAG, "Received message: " + message);

            // String -> Json 객체로 변환 후에 각 값을 변수에 저장
            // 2. chat_user 객체를 JSON 문자열로 변환

            try {
                JSONObject received_Message = new JSONObject(message);
                int received_sender_id = received_Message.getInt("sender_id");
                String received_nickname = received_Message.getString("nickname");
                String received_message = received_Message.getString("message");
                String received_sent_time = received_Message.getString("sent_time");


                // UI 업데이트 처리
                chat_user received_user = new chat_user
                        (1, received_sender_id, received_nickname, received_message, changeFormattedTime(received_sent_time));
                fanMessages.add(received_user);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 서비스 바인딩 해제
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        // BroadcastReceiver 해제
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fanMessageReceiver);
    }

    // 메시지가 추가될 때 RecyclerView의 스크롤을 마지막으로 설정
    private void scrollToBottom() {
        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
    }
}