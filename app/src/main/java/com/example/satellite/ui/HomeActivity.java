package com.example.satellite.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.satellite.ApiService;
import com.example.satellite.NotificationHelper;
import com.example.satellite.R;
import com.example.satellite.RetrofitClientInstance;
import com.example.satellite.adapter.MyAdapter;
import com.example.satellite.model.home_user;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    // 변수 초기설정
    String TAG = "HomeActivity";

    Button btn_home_home;
    Button btn_home_chats;
    Button btn_home_more;

    ImageView iv_home_search;
    TextView home_notification_badge;

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

    String uniq_id;
    int is_artist;
    int totalUnreadCount;

    RecyclerView recyclerView;
    LinearLayoutManager linear;
    MyAdapter adapter;
    ArrayList<home_user> home_users;

    // BroadcastReceiver 정의
    private final BroadcastReceiver permissionRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "브로드캐스트 들어옴?");
            // 권한 요청 처리
            if ("com.example.REQUEST_NOTIFICATION_PERMISSION".equals(action)) {
                ActivityCompat.requestPermissions(HomeActivity.this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
            // totalUnreadCount 업데이트 처리
            else if ("com.example.UPDATE_UNREAD_COUNT".equals(action)) {
                Log.i(TAG, "브로드캐스트 리시버 UPDATE_UNREAD_COUNT");
                totalUnreadCount = intent.getIntExtra("totalUnreadCount", 0);
                if (is_artist == 0){
                    // UI 업데이트 (예: TextView에 표시)
                    home_notification_badge.setText(String.valueOf(totalUnreadCount));
                    home_notification_badge.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btn_home_chats = findViewById(R.id.btn_home_chats);
        btn_home_more = findViewById(R.id.btn_home_more);
        iv_home_search = findViewById(R.id.iv_home_search);
        recyclerView = findViewById(R.id.home_recy);
        home_notification_badge = findViewById(R.id.home_notification_badge);

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();


        uniq_id = user.getString("uniq_id", "");
        is_artist = user.getInt("is_artist", -1);
        Log.i(TAG, "유저데이터" + uniq_id);

        // 아티스트일 경우 검색 버튼 보이지 않음
        if (is_artist == 1){
            iv_home_search.setVisibility(View.INVISIBLE);
        }

        loadHomeArtistData(uniq_id, is_artist);

        iv_home_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, SearchArtistActivity.class);
                startActivity(intent);
            }
        });

        btn_home_chats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, ChatsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        btn_home_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, MoreActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        // 권한 확인 및 요청
        checkAndRequestNotificationPermission();

        // 권한 관련 브로드캐스트 리시버 등록 (onCreate에서 한 번만 등록)
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.REQUEST_NOTIFICATION_PERMISSION");
        filter.addAction("com.example.UPDATE_UNREAD_COUNT");
        LocalBroadcastManager.getInstance(this).registerReceiver(permissionRequestReceiver, filter);
    }

    private void checkAndRequestNotificationPermission() {
        if (!NotificationHelper.hasNotificationPermission(this)) {
            // 권한이 없으면 요청
            NotificationHelper.requestNotificationPermission(this);
        }
    }

    // 권한 요청 결과 처리
    // 사용자가 알림 권한을 허용했을 때 딱 한 번 호출됨
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            // 권한이 허용된 경우
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 필요한 권한이 허용되었으므로 알림을 보냄
                Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                // 권한이 거부된 경우 처리
                Toast.makeText(this, "알림 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadHomeArtistData(String uniq_id, int is_artist) {
        ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
        Call<ArrayList<home_user>> call = service.sendUniq_id(uniq_id, is_artist);

        call.enqueue(new Callback<ArrayList<home_user>>() {
            @Override
            public void onResponse(Call<ArrayList<home_user>> call, Response<ArrayList<home_user>> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "response값" + response);

                    home_users = new ArrayList<>();

                    ArrayList<home_user> data = response.body();
                    Log.i(TAG, "바디값" + data);
                    Log.i(TAG, "제대로 받았나?" + data.get(0).getNickname());
                    Log.i(TAG, "데이터 길이는?" + data.size());

                    boolean userSectionAdded = false;
                    boolean planetSectionAdded = false;
                    boolean artistSectionAdded = false;

                    for (int i = 0; i < data.size(); i++) {
                        home_user user = data.get(i);
                        int usertype = user.getUsertype();
                        int chat_id = user.getChat_id();
                        int id = user.getId();
                        String message = user.getMessage();
                        String nickname = user.getNickname();
                        String image = user.getImage();

                        if (usertype == 0) {
                            if (!userSectionAdded) {
                                home_users.add(new home_user(home_user.TYPE_HEADER, "내 프로필"));
                                userSectionAdded = true;
                            }
                            home_users.add(new home_user(home_user.TYPE_USER, chat_id, id, message, nickname, image));
                        } else if (usertype == 1) {
                            if (!planetSectionAdded) {
                                home_users.add(new home_user(home_user.TYPE_HEADER, "My planet"));
                                planetSectionAdded = true;
                            }
                            home_users.add(new home_user(home_user.TYPE_PLANET, chat_id, id, message, nickname, image));
                        } else if (usertype == 2) {
                            if (!artistSectionAdded) {
                                home_users.add(new home_user(home_user.TYPE_HEADER, "추천 아티스트"));
                                artistSectionAdded = true;
                            }
                            home_users.add(new home_user(home_user.TYPE_ARTIST,chat_id, id, message, nickname, image));
                        }
                    }
                    linear = new LinearLayoutManager(getApplicationContext());

                    totalUnreadCount = user.getInt("totalUnreadCount", -1);

                    updateUnreadMessageUI(is_artist);

                    linear.setOrientation(RecyclerView.VERTICAL);
                    recyclerView.setLayoutManager(linear);
                    adapter = new MyAdapter(home_users, getApplicationContext());
                    recyclerView.setAdapter(adapter);

                } else {
                    Toast.makeText(HomeActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onFailure(Call<ArrayList<home_user>> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.i(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    private void updateUnreadMessageUI(int is_artist) {
        if (is_artist == 0) {
            if (totalUnreadCount > 0) {
                user_editor.putInt("totalUnreadCount", totalUnreadCount);
                user_editor.apply();
                home_notification_badge.setText(String.valueOf(totalUnreadCount));
                home_notification_badge.setVisibility(View.VISIBLE);
            }else{
                home_notification_badge.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
        loadHomeArtistData(uniq_id, is_artist);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 리시버 해제 (onDestroy 또는 onStop에서 해제)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(permissionRequestReceiver);
        Log.i(TAG, "onDestroy: HomeActivity");
    }
}