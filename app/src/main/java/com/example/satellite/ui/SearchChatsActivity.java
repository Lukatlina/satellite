package com.example.satellite.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.satellite.ApiService;
import com.example.satellite.ChatService;
import com.example.satellite.R;
import com.example.satellite.RetrofitClientInstance;
import com.example.satellite.adapter.RoomAdapter;
import com.example.satellite.model.ChatRoom;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchChatsActivity extends AppCompatActivity {

    private static final String TAG = "SearchChatsActivity";
    private ChatService chatService;
    private boolean isBound = false;

    ImageView iv_search_chats_back_btn;
    SearchView sv_chats;
    TextView ev_search_chats;

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

    int user_id;
    int is_artist;
    String keyword;

    RecyclerView recyclerView;
    LinearLayoutManager linear;
    RoomAdapter adapter;
    ArrayList<ChatRoom> search_chatrooms = new ArrayList<>();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 바인딩된 서비스의 IBinder를 통해 서비스 인스턴스를 가져옵니다.
            ChatService.LocalBinder binder = (ChatService.LocalBinder) service;
            chatService = binder.getService();
            isBound = true;
            Log.i(TAG, "서비스 연결됨");
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
        setContentView(R.layout.activity_search_chats);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.search_chats), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        iv_search_chats_back_btn = findViewById(R.id.iv_search_chats_back_btn);
        sv_chats = findViewById(R.id.sv_chats);
        recyclerView = findViewById(R.id.recy_search_chats);
        ev_search_chats = findViewById(R.id.ev_search_chats);

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        user_id = user.getInt("user_id", -1);
        is_artist = user.getInt("is_artist", -1);

        linear = new LinearLayoutManager(getApplicationContext());
        linear.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linear);
        adapter = new RoomAdapter(search_chatrooms, getApplicationContext(), is_artist);
        recyclerView.setAdapter(adapter);

        iv_search_chats_back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SearchChatsActivity.this, ChatsActivity.class);
                startActivity(intent);
                finish();
            }
        });

        sv_chats.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String keyword) {
                searchChatRooms(user_id, is_artist, keyword);
                sv_chats.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String keyword) {
                if (keyword.isEmpty()) {
                    ev_search_chats.setText("검색된 결과가 없습니다.");
                    ev_search_chats.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
                return false;
            }
        });

        int searchIconId = sv_chats.getContext().getResources().getIdentifier("android:id/search_mag_icon", null, null);
        ImageView searchButton = sv_chats.findViewById(searchIconId);
        if (searchButton != null) {
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 검색어 제출 시의 동작
                    keyword = sv_chats.getQuery().toString();
                    searchChatRooms(user_id, is_artist, keyword);
                    // 키보드 숨기기
                    sv_chats.clearFocus();
                }
            });
        }
    }

    private void searchChatRooms(int user_id, int is_artist, String keyword) {
        search_chatrooms.clear();
        if (!keyword.isEmpty()) {
            Log.i(TAG, "키워드 클릭 시작" + keyword);
            ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
            Call<ArrayList<ChatRoom>> call = service.sendKeywordfindChatroom(user_id, is_artist, keyword);

            call.enqueue(new Callback<ArrayList<ChatRoom>>() {
                @Override
                public void onResponse(Call<ArrayList<ChatRoom>> call, Response<ArrayList<ChatRoom>> response) {
                    Log.i(TAG, "onResponse: 시작");
                    if (response.isSuccessful()) {
                        Log.i(TAG, "response값" + response);

                        ArrayList<ChatRoom> data = response.body();
                        Log.i(TAG, "데이터 길이는?" + data.size());

                        for (int i = 0; i < data.size(); i++) {
                            ChatRoom room = data.get(i);
                            int chat_id = room.getChat_id();
                            int artist_id = room.getArtist_id();
                            String artist_image = room.getArtist_image();
                            String artist_nickname = room.getArtist_nickname();
                            String last_message = room.getLast_message();
                            String sent_time = room.getSent_time();

                            search_chatrooms.add(new ChatRoom(chat_id, artist_id, artist_image, artist_nickname, last_message, sent_time));
                        }
                        adapter.notifyDataSetChanged();
                        updateUI(keyword);

                    } else {
                        Toast.makeText(SearchChatsActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                        showEmptyView(keyword);
                    }
                    Log.i(TAG, "onResponse: 끝");
                }

                @Override
                public void onFailure(Call<ArrayList<ChatRoom>> call, Throwable t) {
                    Log.i(TAG, "onFailure: 시작");
                    showEmptyView(keyword);
                    Log.i(TAG, "onFailure: " + t.getMessage());
                }
            });
        }
    }

    private void updateUI(String keyword) {
        if (search_chatrooms.isEmpty()) {
            showEmptyView(keyword);
        } else {
            showRecyclerView();
        }
    }

    private void showEmptyView(String keyword) {
        recyclerView.setVisibility(View.GONE);
        if (keyword.isEmpty()) {
            ev_search_chats.setText("채팅방 이름을 입력해 보세요.");
        }else{
            ev_search_chats.setText("검색된 결과가 없습니다.");
        }
        ev_search_chats.setVisibility(View.VISIBLE);
    }

    private void showRecyclerView() {
        recyclerView.setVisibility(View.VISIBLE);
        ev_search_chats.setVisibility(View.GONE);
    }
}