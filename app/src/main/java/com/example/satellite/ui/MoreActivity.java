package com.example.satellite.ui;

import android.content.Context;
import android.content.DialogInterface;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.satellite.R;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MoreActivity extends AppCompatActivity {

    // 변수 초기설정
    String TAG = "MoreActivity";

    Button btn_more_home;
    Button btn_more_chats;
    Button btn_more_more;
    Button btn_more_edit_profile;
    Button btn_more_logout;
    Button btn_more_withdrawal;

    ImageView iv_more_profile;
    TextView tv_more_nickname;
    View view_line_2;
    View view_line_3;

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

    int is_artist;
    String uniq_id;
    String email;
    String nickname;
    String image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_more);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.more), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btn_more_home = findViewById(R.id.btn_more_home);
        btn_more_chats = findViewById(R.id.btn_more_chats);
        btn_more_edit_profile = findViewById(R.id.btn_more_edit_profile);
        btn_more_logout = findViewById(R.id.btn_more_logout);
        btn_more_withdrawal = findViewById(R.id.btn_more_withdrawal);

        iv_more_profile = findViewById(R.id.iv_more_profile);
        tv_more_nickname = findViewById(R.id.tv_more_nickname);

        view_line_2 = findViewById(R.id.view_line_2);
        view_line_3 = findViewById(R.id.view_line_3);

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        uniq_id = user.getString("uniq_id", "");
        is_artist = user.getInt("is_artist", -1);

        // 아티스트일 경우 탈퇴 버튼이 보이지 않도록 만듬
        if (is_artist == 1){
            btn_more_withdrawal.setVisibility(View.INVISIBLE);
            view_line_3.setVisibility(View.VISIBLE);
            view_line_2.setVisibility(View.INVISIBLE);
        }else{
            view_line_3.setVisibility(View.INVISIBLE);
        }

        loadUserdata();

        btn_more_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MoreActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        btn_more_chats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MoreActivity.this, ChatsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });


        btn_more_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 로그아웃 여부를 묻는 다이얼로그
                AlertDialog.Builder menu = new AlertDialog.Builder(MoreActivity.this);
                menu.setIcon(R.mipmap.ic_launcher);
                menu.setMessage("로그아웃 하시겠습니까?"); // 문구


                // 확인 버튼
                menu.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HttpUrl.Builder urlBuilder = HttpUrl.parse("http://52.78.77.90/satellite/logout.php").newBuilder();
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
                                                Log.i(TAG, "응답실패" + response);
                                                Toast.makeText(getApplicationContext(), "네트워크 문제 발생", Toast.LENGTH_SHORT).show();

                                            } else {
                                                // 응답 성공
                                                Log.i(TAG, "응답 성공");
                                                final String responseData = response.body().string();
                                                Log.i(TAG, "무슨 데이터? : " + responseData);

                                                if (responseData.equals("1")) {
                                                    user_editor.remove("uniq_id");
                                                    user_editor.remove("user_id");
                                                    user_editor.remove("is_artist");
                                                    user_editor.apply();

                                                    Toast.makeText(getApplicationContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                                    startActivity(intent);
                                                    finish();

                                                } else {
                                                    Toast.makeText(getApplicationContext(), "로그아웃에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                                }
                                            }

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                            }
                        });
                        // dialog 제거
                        dialog.dismiss();
                    }
                });

                // 취소 버튼
                menu.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // dialog 제거
                        dialog.dismiss();
                    }
                });

                menu.show();

            }
        });

        btn_more_withdrawal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MoreActivity.this, WithdrawalActivity.class);
                startActivity(intent);
            }
        });

        btn_more_edit_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MoreActivity.this, EditProfileActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserdata();
    }

    private void loadUserdata() {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("http://52.78.77.90/satellite/user_data.php").newBuilder();
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
                                JSONObject user_data = new JSONObject(responseData);

                                if (user_data.getInt("result") == 1) {
                                    email = user_data.getString("email");
                                    nickname = user_data.getString("nickname");
                                    if (user_data.getString("image").isEmpty() || user_data.isNull("image")) {
                                        iv_more_profile.setImageResource(R.drawable.baseline_person_150);
                                    }else{
                                        image_uri = user_data.getString("image");
                                        Glide.with(getApplicationContext()).load(image_uri).circleCrop().into(iv_more_profile);
                                    }
                                    Log.i(TAG, "유저 이메일 : " + email + "유저 닉네임 : " + nickname);
                                    btn_more_edit_profile.setText(email);
                                    tv_more_nickname.setText(nickname);
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
}