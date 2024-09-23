package com.example.satellite;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
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

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

    String uniq_id;
    int is_artist;

    RecyclerView recyclerView;
    LinearLayoutManager linear;
    MyAdapter adapter;
    ArrayList<home_user> home_users;

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
        iv_home_search = findViewById(R.id.iv_home_search);
        btn_home_more = findViewById(R.id.btn_home_more);
        recyclerView = findViewById(R.id.home_recy);

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();


        uniq_id = user.getString("uniq_id", "");
        is_artist = user.getInt("is_artist", -1);
        Log.i(TAG, "유저데이터" + uniq_id);

        // 아티스트일 경우 검색 버튼 보이지 않음
        if (is_artist == 1){
            iv_home_search.setVisibility(View.INVISIBLE);
        }

        ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
        retrofit2.Call<ArrayList<home_user>> call = service.sendUniq_id(uniq_id, is_artist);

        call.enqueue(new Callback<ArrayList<home_user>>() {
            @Override
            public void onResponse(retrofit2.Call<ArrayList<home_user>> call, Response<ArrayList<home_user>> response) {
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
                        int id = user.getId();
                        String message = user.getMessage();
                        String nickname = user.getNickname();
                        String image = user.getImage();

                        if (usertype == 0) {
                            if (!userSectionAdded) {
                                home_users.add(new home_user(home_user.TYPE_HEADER, "내 프로필"));
                                userSectionAdded = true;
                            }
                            home_users.add(new home_user(home_user.TYPE_USER, id, message, nickname, image));
                        } else if (usertype == 1) {
                            if (!planetSectionAdded) {
                                home_users.add(new home_user(home_user.TYPE_HEADER, "아티스트"));
                                planetSectionAdded = true;
                            }
                            home_users.add(new home_user(home_user.TYPE_PLANET , id ,message, nickname, image));
                        } else if (usertype == 2) {
                            if (!artistSectionAdded) {
                                home_users.add(new home_user(home_user.TYPE_HEADER, "추천 아티스트"));
                                artistSectionAdded = true;
                            }
                            home_users.add(new home_user(home_user.TYPE_ARTIST , id ,message, nickname, image));
                        }
                    }
                    linear = new LinearLayoutManager(getApplicationContext());


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
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        btn_home_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, MoreActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }
}