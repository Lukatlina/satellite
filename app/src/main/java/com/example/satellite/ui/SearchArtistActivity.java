package com.example.satellite.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.example.satellite.R;
import com.example.satellite.RetrofitClientInstance;
import com.example.satellite.adapter.MyAdapter;
import com.example.satellite.model.home_user;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchArtistActivity extends AppCompatActivity {

    String TAG = "SearchArtistActivity";

    ImageView iv_search_artist_back_btn;
    SearchView sv_artist;
    ImageView searchButton;
    TextView empty_view;

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

//    String uniq_id;
    String user_id;
    String keyword;

    RecyclerView recyclerView;
    LinearLayoutManager linear;
    MyAdapter adapter;
    ArrayList<home_user> search_artists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_artist);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.search_artist), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        iv_search_artist_back_btn = findViewById(R.id.iv_search_artist_back_btn);
        sv_artist = findViewById(R.id.sv_artist);
        recyclerView = findViewById(R.id.recy_search_artist);
        empty_view = findViewById(R.id.empty_view);

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        user_id = user.getString("user_id", "");
        Log.i(TAG, "user_id" + user_id);

        search_artists = new ArrayList<>();
        linear = new LinearLayoutManager(getApplicationContext());
        linear.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linear);
        adapter = new MyAdapter(search_artists, getApplicationContext());
        recyclerView.setAdapter(adapter);

        iv_search_artist_back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SearchArtistActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        sv_artist.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String keyword) {
                searchArtists(keyword);
                sv_artist.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String keyword) {
                if (keyword.isEmpty()) {
                    empty_view.setText("좋아하는 ARTIST의 이름 또는\n그룹명을 입력해 보세요.");
                    empty_view.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
                return false;
            }
        });

        int searchIconId = sv_artist.getContext().getResources().getIdentifier("android:id/search_mag_icon", null, null);
        ImageView searchButton = sv_artist.findViewById(searchIconId);
        if (searchButton != null) {
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 검색어 제출 시의 동작
                    keyword = sv_artist.getQuery().toString();
                    searchArtists(keyword);
                    // 키보드 숨기기
                    sv_artist.clearFocus();
                }
            });
        }
    }

    private void searchArtists(String keyword) {
        search_artists.clear();
        if (!keyword.isEmpty()) {
            Log.i(TAG, "키워드 클릭 시작" + keyword);
            Log.i(TAG, "유저 아이디 확인" + user_id);
            ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
            Call<ArrayList<home_user>> call = service.sendKeyword(user_id, keyword);

            call.enqueue(new Callback<ArrayList<home_user>>() {
                @Override
                public void onResponse(Call<ArrayList<home_user>> call, Response<ArrayList<home_user>> response) {
                    Log.i(TAG, "onResponse: 시작");
                    if (response.isSuccessful()) {
                        Log.i(TAG, "response값" + response);

                        ArrayList<home_user> data = response.body();
                        Log.i(TAG, "바디값" + data);
                        Log.i(TAG, "제대로 받았나? 0번째 usertype :" + data.get(0).getUsertype());
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
                            Log.i(TAG, "usertype" + nickname + " : " + usertype);

                            if (usertype == 0) {
                                if (!userSectionAdded) {
                                    search_artists.add(new home_user(home_user.TYPE_HEADER, "내 프로필"));
                                    userSectionAdded = true;
                                }
                                search_artists.add(new home_user(home_user.TYPE_USER, id, message, nickname, image));
                            } else if (usertype == 1) {
                                if (!planetSectionAdded) {
                                    search_artists.add(new home_user(home_user.TYPE_HEADER, "My planet"));
                                    planetSectionAdded = true;
                                }
                                search_artists.add(new home_user(home_user.TYPE_PLANET , id ,message, nickname, image));
                            } else if (usertype == 2) {
                                if (!artistSectionAdded) {
                                    search_artists.add(new home_user(home_user.TYPE_HEADER, "추천 아티스트"));
                                    artistSectionAdded = true;
                                }
                                search_artists.add(new home_user(home_user.TYPE_ARTIST , id ,message, nickname, image));
                            }
                            adapter.notifyDataSetChanged();
                            updateUI(keyword);
                        }

                    } else {
                        Toast.makeText(SearchArtistActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                        showEmptyView(keyword);
                    }
                    Log.i(TAG, "onResponse: 끝");
                }

                @Override
                public void onFailure(Call<ArrayList<home_user>> call, Throwable t) {
                    Log.i(TAG, "onFailure: 시작");
                    showEmptyView(keyword);
                    Log.i(TAG, "onFailure: " + t.getMessage());
                }
            });
        }
    }

    private void updateUI(String keyword) {
        if (search_artists.isEmpty()) {
            showEmptyView(keyword);
        } else {
            showRecyclerView();
        }
    }



    private void showEmptyView(String keyword) {
        recyclerView.setVisibility(View.GONE);
        if (keyword.isEmpty()) {
            empty_view.setText("좋아하는 ARTIST의 이름 또는\n그룹명을 입력해 보세요.");
        }else{
            empty_view.setText("검색 결과가 없습니다.");
        }
        empty_view.setVisibility(View.VISIBLE);
    }

    private void showRecyclerView() {
        recyclerView.setVisibility(View.VISIBLE);
        empty_view.setVisibility(View.GONE);
    }
}