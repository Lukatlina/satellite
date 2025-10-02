package com.example.satellite.ui;

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
import android.widget.ImageView;
import android.widget.TextView;
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
import com.example.satellite.MyApplication;
import com.example.satellite.R;
import com.example.satellite.RetrofitClientInstance;
import com.example.satellite.adapter.RoomAdapter;
import com.example.satellite.model.ChatRoom;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatsActivity extends AppCompatActivity {

    private static final String TAG = "ChatsActivity";
    private ChatService chatService;
    private boolean isBound = false;

    Button btn_chats_home;
    Button btn_chats_more;
    ImageView iv_chats_search, iv_chats_delete;
    TextView ev_chats, chats_notification_badge;

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

    int user_id;
    int is_artist;

    RecyclerView recyclerView;
    LinearLayoutManager linear;
    RoomAdapter adapter;
    ArrayList<ChatRoom> chat_rooms = new ArrayList<>();
    int totalUnreadCount;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 바인딩된 서비스의 IBinder를 통해 서비스 인스턴스를 가져옵니다.
            ChatService.LocalBinder binder = (ChatService.LocalBinder) service;
            chatService = binder.getService();
            isBound = true;
            Log.i(TAG, "서비스 연결됨");

            // 필요 시 서비스와 상호작용하는 코드를 작성합니다.
            // 채팅방 데이터를 가져오는 Retrofit HTTP 요청 설정
            loadChatroomsData();
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
        setContentView(R.layout.activity_chats);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chats), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.chats_recy);

        btn_chats_home = findViewById(R.id.btn_chats_home);
        btn_chats_more = findViewById(R.id.btn_chats_more);
        iv_chats_search = findViewById(R.id.iv_chats_search);
        iv_chats_delete = findViewById(R.id.iv_chats_delete);
        ev_chats = findViewById(R.id.ev_chats);
        chats_notification_badge = findViewById(R.id.chats_notification_badge);

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        user_id = user.getInt("user_id", -1);
        is_artist = user.getInt("is_artist", -1);
        Log.i(TAG, "유저데이터" + user_id);

        // 서비스가 이미 시작된 상태라면 바인딩만 해줌.
        Intent serviceIntent = new Intent(ChatsActivity.this, ChatService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // BroadcastReceiver 등록
        LocalBroadcastManager.getInstance(this).registerReceiver(lastMessageReceiver,
                new IntentFilter("com.example.satellite.ACTION_UPDATE_CHAT_LIST"));

        // 아티스트일 경우 검색, 삭제 버튼 보이지 않음
        if (is_artist == 1){
            iv_chats_search.setVisibility(View.INVISIBLE);
            iv_chats_delete.setVisibility(View.INVISIBLE);
        }

        iv_chats_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChatsActivity.this, SearchChatsActivity.class);
                startActivity(intent);
            }
        });

        btn_chats_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChatsActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        btn_chats_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChatsActivity.this, MoreActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });
    }

    private void loadChatroomsData () {
        ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
        Call<ArrayList<ChatRoom>> call = service.sendChatroomsImformation(user_id, is_artist);

        call.enqueue(new Callback<ArrayList<ChatRoom>>() {
            @Override
            public void onResponse(Call<ArrayList<ChatRoom>> call, Response<ArrayList<ChatRoom>> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "response값" + response);

                    chat_rooms = new ArrayList<>();

                    ArrayList<ChatRoom> data = response.body();
                    Log.i(TAG, "바디값" + data);
                    Log.i(TAG, "데이터 길이는?" + data.size());

                    for (int i = 0; i < data.size(); i++) {
                        ChatRoom room = data.get(i);
                        int chat_id = room.getChat_id();
                        int artist_id = room.getArtist_id();
                        String artist_image = room.getArtist_image();
                        String artist_nickname = room.getArtist_nickname();
                        String last_message = room.getLast_message();
                        String sent_time = room.getSent_time();
                        int unread_count= room.getUnread_count();

                        chat_rooms.add(new ChatRoom(chat_id, artist_id, artist_image, artist_nickname, last_message, sent_time, unread_count));
                    }
                    linear = new LinearLayoutManager(getApplicationContext());
                    linear.setOrientation(RecyclerView.VERTICAL);
                    recyclerView.setLayoutManager(linear);
                    adapter = new RoomAdapter(chat_rooms, getApplicationContext(), is_artist);
                    recyclerView.setAdapter(adapter);

                    adapter.notifyDataSetChanged();

                    totalUnreadCount = 0;
                    for (ChatRoom chatRoom : chat_rooms) {
                        totalUnreadCount += chatRoom.getUnread_count(); // 각 chatRoom의 unread_count를 더함
                    }

                    System.out.println("Total Unread Count: " + totalUnreadCount);

                    updateUI();

                } else {
                    Toast.makeText(ChatsActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                    Log.i(TAG, "Error: " + response.code());
                }

            }

            @Override
            public void onFailure(Call<ArrayList<ChatRoom>> call, Throwable t) {
                Toast.makeText(ChatsActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.i(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    // 서버로부터 메시지 수신하면 사용
    private BroadcastReceiver lastMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.i(TAG, "Received message: " + message);

            // String -> Json 객체로 변환 후에 각 값을 변수에 저장
            // 2. chat_user 객체를 JSON 문자열로 변환

            try {
                JSONObject received_Message = new JSONObject(message);
                int received_chat_id = received_Message.getInt("chat_id");
                String received_message = received_Message.getString("message");
                String received_sent_time = received_Message.getString("sent_time");
                int received_unread_count = received_Message.has("unreadMessages") ? received_Message.getInt("unreadMessages") : 0;

                Log.i(TAG, "브로드캐스트로 받은 unreadMessages" + received_unread_count);


                // UI 업데이트 처리
                // 만약 chat_id가 같은 값이 있다면, 메시지를 바꿔준다.
                // 같은 값이 없다면 새로 생성한다.
                for (ChatRoom chatroom : chat_rooms) {
                    if (chatroom.getChat_id() == received_chat_id && is_artist == 0) {
                        chatroom.setLast_message(received_message);
                        chatroom.setSent_time(received_sent_time);
                        chatroom.setUnread_count(received_unread_count);
                        // chat_rooms arraylist를 업데이트 한 후에 시간이 최신순으로 보이게 만들어야 함.
                    } else if (chatroom.getChat_id() == received_chat_id && is_artist == 1) {
                        chatroom.setUnread_count(received_unread_count);
                    }
                    Log.i(TAG, "onReceive: 시간" + chatroom.getSent_time());
                }

                // 최신 메시지 시간 기준으로 정렬 (내림차순)
                Collections.sort(chat_rooms, (chatroom1, chatroom2) ->
                        chatroom2.getSent_time().compareTo(chatroom1.getSent_time()));

                totalUnreadCount += 1;
                updateUI();

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    };

    private void updateUI() {
        if (chat_rooms.isEmpty()) {
            showEmptyView();
        } else {
            showRecyclerView();
        }
        if (is_artist == 0) {
            if (totalUnreadCount > 0) {
                chats_notification_badge.setText(String.valueOf(totalUnreadCount));
                chats_notification_badge.setVisibility(View.VISIBLE);
            }else{
                chats_notification_badge.setVisibility(View.GONE);
                user_editor.putInt("totalUnreadCount", totalUnreadCount);
                Log.i(TAG, "updateUI: totalUnreadCount " + totalUnreadCount);
                user_editor.apply();
            }

        }
    }

    private void showEmptyView() {
        recyclerView.setVisibility(View.GONE);

        ev_chats.setVisibility(View.VISIBLE);
    }

    private void showRecyclerView() {
        recyclerView.setVisibility(View.VISIBLE);
        ev_chats.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChatroomsData ();
        // 현재 액티비티가 채팅 화면임을 추적
        Log.i(TAG, "onResume: ");
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 채팅 화면을 떠날 때 null로 설정
        Log.i(TAG, "onPause: ");
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentActivity(null);
    }
}
