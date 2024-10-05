package com.example.satellite.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.satellite.ApiService;
import com.example.satellite.R;
import com.example.satellite.RetrofitClientInstance;
import com.example.satellite.adapter.RoomAdapter;
import com.example.satellite.model.ChatRoom;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatsActivity extends AppCompatActivity {

    private static final String TAG = "ChatsActivity";

    Button btn_chats_home;
    Button btn_chats_more;

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

    int user_id;
    int is_artist;

    RecyclerView recyclerView;
    LinearLayoutManager linear;
    RoomAdapter adapter;
    ArrayList<ChatRoom> chat_rooms = new ArrayList<>();

    String formattedTime;


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

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        user_id = user.getInt("user_id", -1);
        is_artist = user.getInt("is_artist", -1);
        Log.i(TAG, "유저데이터" + user_id);

        loadChatroomsData ();

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
                    Log.i(TAG, "바디값" + data.get(0));
                    Log.i(TAG, "데이터 길이는?" + data.size());

                    for (int i = 0; i < data.size(); i++) {
                        ChatRoom room = data.get(i);
                        int chat_id = room.getChat_id();
                        int artist_id = room.getArtist_id();
                        String artist_image = room.getArtist_image();
                        String artist_nickname = room.getArtist_nickname();
                        String last_message = room.getLast_message();
                        String sent_time = room.getSent_time();

                        chat_rooms.add(new ChatRoom(chat_id, artist_id, artist_image, artist_nickname, last_message, changeFormattedTime(sent_time) ));
                    }
                    linear = new LinearLayoutManager(getApplicationContext());
                    linear.setOrientation(RecyclerView.VERTICAL);
                    recyclerView.setLayoutManager(linear);
                    adapter = new RoomAdapter(chat_rooms, getApplicationContext(), is_artist);
                    recyclerView.setAdapter(adapter);

                    adapter.notifyDataSetChanged();


                } else {
                    Toast.makeText(ChatsActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onFailure(Call<ArrayList<ChatRoom>> call, Throwable t) {
                Toast.makeText(ChatsActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.i(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    // 채팅방 마지막 메시지 전송 시간의 포맷을 수정하기 위한 메서드
    private String changeFormattedTime(String sent_time) {
        // 원래 형식의 시간 파싱을 위한 SimpleDateFormat
        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 문자열을 Date 객체로 파싱
        Date dateTime = null;
        try {
            dateTime = originalFormat.parse(sent_time);
        } catch (ParseException e) {
            e.printStackTrace();
            return sent_time;
        }

        // 원하는 형식으로 변환하기 위한 SimpleDateFormat
        // 만약 오늘 주고 받은 메시지라면 HH:mm의 형식이며, 어제 주고 받았다면 어제, 그 이전은 MM월 dd일의 형식이며, 해가 지났다면, yyyy.MM.dd의 형태로 표시되게 된다.

        // 받은 데이터의 날짜
        Calendar date = Calendar.getInstance();
        date.setTime(dateTime);

        // 1. 오늘인지 확인
        Calendar today = Calendar.getInstance();

        // 2. 어제인지 확인
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        // 3. 시간 포맷 선언 (재사용)
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM월 dd일");
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy.MM.dd");

        if (isSameDay(today, date)) {
            Log.i(TAG, "오늘 시간 : " + today.get(Calendar.DAY_OF_YEAR));
            return timeFormat.format(dateTime);
        } else if (isSameDay(yesterday, date)) {
            Log.i(TAG, "어제 시간 : "  + yesterday.get(Calendar.DAY_OF_YEAR));
            return "어제";
        } else if (today.get(Calendar.YEAR) == date.get(Calendar.YEAR)) {
            Log.i(TAG, "올해 : ");
            return dateFormat.format(dateTime);
        } else {
            Log.i(TAG, "모두 해당 안됨 : ");
            return yearFormat.format(dateTime);
        }
    }

    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}
