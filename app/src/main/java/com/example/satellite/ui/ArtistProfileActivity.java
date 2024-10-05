package com.example.satellite.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.satellite.R;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ArtistProfileActivity extends AppCompatActivity {
    private static final String TAG = "ArtistProfileActivity";

    ImageView iv_artist_profile_close_btn;
    ImageView iv_artist_profile_more_btn;
    ImageView iv_artist_profile_profile;

    TextView tv_artist_profile_nickname;
    TextView tv_artist_profile_message;

    Button artist_profile_btn;

    int artist_id;
    int usertype;
    String artist_nickname;
    String artist_message;
    String artist_image;
    int user_id;

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_artist_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.artist_profile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        iv_artist_profile_close_btn = findViewById(R.id.iv_artist_profile_close_btn);
        iv_artist_profile_more_btn = findViewById(R.id.iv_artist_profile_more_btn);
        iv_artist_profile_profile = findViewById(R.id.iv_artist_profile_profile);

        tv_artist_profile_nickname = findViewById(R.id.tv_artist_profile_nickname);
        tv_artist_profile_message = findViewById(R.id.tv_artist_profile_message);

        artist_profile_btn = findViewById(R.id.artist_profile_btn);

        Intent intent = getIntent();
        artist_id = intent.getIntExtra("id", -1);
        usertype = intent.getIntExtra("usertype", -1);
        Log.i(TAG, "artist_id : " + artist_id);
        Log.i(TAG, "usertype : " + usertype);
        artist_nickname = intent.getStringExtra("nickname");
        artist_message = intent.getStringExtra("message");
        artist_image = intent.getStringExtra("image");

        if (artist_image == null) {
            iv_artist_profile_profile.setImageResource(R.drawable.baseline_person_150);
        }else{
            Glide.with(this).load(artist_image).circleCrop().into(iv_artist_profile_profile);
        }


        tv_artist_profile_nickname.setText(artist_nickname);
        tv_artist_profile_message.setText(artist_message);
        if (usertype == 2) {
            artist_profile_btn.setText("채팅하기");
        } else if (usertype == 3) {
            artist_profile_btn.setText("아티스트 추가");
        }

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        user_id = user.getInt("user_id", -1);
        Log.i(TAG, "user_id" + user_id);
        Log.i(TAG, "artist_id" + artist_id);



        iv_artist_profile_close_btn.setOnClickListener(new View.OnClickListener() {
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

        iv_artist_profile_more_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 아티스트 삭제 기능 추가
                showCustomDialog();
            }
        });

        artist_profile_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (usertype == 2) {
                    // 클릭시 아티스트와 채팅 화면으로 이동
                    Log.i(TAG, "onClick: 유저타입2");
                    Intent intent = new Intent(getApplicationContext(), FanChatActivity.class);
                    // 채팅방 식별을 위한 아티스트 고유 ID
                    intent.putExtra("artist_id", artist_id);
                    startActivity(intent);
                    finish();
                } else if (usertype == 3) {
                    // 클릭시 아티스트가 내 planet리스트에 포함됨.
                    addArtistToMyList();
                }
            }
        });
    }

    private void addArtistToMyList() {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("http://52.78.77.90/satellite/save_planet_artist.php").newBuilder();
        String url = urlBuilder.build().toString();

        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("user_id", String.valueOf(user_id))
                .add("artist_id", String.valueOf(artist_id))
                .build();

        // 요청 만들기
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        // 응답 콜백
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {

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
                                final String responseData = response.body().string();
                                Log.i(TAG, "무슨 데이터? : " + responseData);


                                if (responseData.equals("1")) {
                                    Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);

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

    private void deleteArtistToMyList() {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("http://52.78.77.90/satellite/delete_planet_artist.php").newBuilder();
        String url = urlBuilder.build().toString();

        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("user_id", String.valueOf(user_id))
                .add("artist_id", String.valueOf(artist_id))
                .build();

        // 요청 만들기
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        // 응답 콜백
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {

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
                                final String responseData = response.body().string();
                                Log.i(TAG, "무슨 데이터? : " + responseData);


                                if (responseData.equals("1")) {
                                    Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);

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

    private void showCustomDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom);

        Button btnDeleteArtist = dialog.findViewById(R.id.btn_select_image);
        Button btnDefaultImage = dialog.findViewById(R.id.btn_default_image);

        // dialog_custom.xml을 변형함.
        // 기본 이미지 선택 버튼을 안보이게 바꾸고 앨범에서 선택 버튼의 텍스트를 아티스트 삭제로 변경.
        btnDeleteArtist.setText("아티스트 삭제");
        btnDefaultImage.setVisibility(View.INVISIBLE);
        Window window = dialog.getWindow();

        // dialog_custom.xml의 전체 크기 조절
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;  // 너비 설정 (픽셀 단위)
            layoutParams.height = 300;  // 높이는 콘텐츠에 맞춤
            window.setAttributes(layoutParams);
        }

        btnDeleteArtist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteArtistToMyList();
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}