package com.example.satellite.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.satellite.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import java.io.IOException;

import cz.msebera.android.httpclient.Header;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChangeNicknameActivity extends AppCompatActivity {

    String TAG = "ChangeNicknameActivity";

    SharedPreferences user;
    SharedPreferences.Editor user_editor;


    Button btn_change_nickname_return;
    Button btn_change_nickname_submit;

    TextInputLayout et_change_nickname_layout;
    TextInputEditText et_change_nickname;

    String uniq_id;
    String is_artist;
    String email;
    String nickname;
    String change_nickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_nickname);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.change_nickname), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btn_change_nickname_return = findViewById(R.id.btn_change_nickname_return);
        btn_change_nickname_submit = findViewById(R.id.btn_change_nickname_submit);
        et_change_nickname_layout = findViewById(R.id.et_change_nickname_layout);
        et_change_nickname = findViewById(R.id.et_change_nickname);

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        uniq_id = user.getString("uniq_id", "");
        is_artist = user.getString("is_artist", "");

        HttpUrl.Builder urlBuilder = HttpUrl.parse("http://52.78.77.90/satellite/user_data.php").newBuilder();
        String url = urlBuilder.build().toString();

        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("uniq_id", uniq_id)
                .add("is_artist", is_artist)
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
                                    Log.i(TAG, "유저 이메일 : " + email + "유저 닉네임 : " + nickname);
                                    et_change_nickname.setHint(nickname);
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

        btn_change_nickname_return.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChangeNicknameActivity.this, EditProfileActivity.class);
                startActivity(intent);
                finish();
            }
        });

        et_change_nickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!et_change_nickname.getText().toString().isEmpty() && et_change_nickname.getText().toString().length() <= 20 && !et_change_nickname.getText().toString().equals(nickname)) {
                    // 조건에 맞는 닉네임
                    et_change_nickname_layout.setError(null);
                    change_nickname = et_change_nickname.getText().toString();
                    btn_change_nickname_submit.setEnabled(true);
                    btn_change_nickname_submit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ChangeNicknameActivity.this, R.color.colorPrimary)));
                    Log.i(TAG, "어떤 닉네임?" + change_nickname);
                }else{
                    // 조건에 맞지 않을 경우
                    et_change_nickname_layout.setError("유효한 닉네임을 입력해주세요.");
                    btn_change_nickname_submit.setEnabled(false);
                    btn_change_nickname_submit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ChangeNicknameActivity.this, R.color.disabled_color)));

                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        btn_change_nickname_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RequestParams params = new RequestParams();
                params.put("email", email);
                params.put("is_artist", is_artist);
                params.put("nickname", et_change_nickname.getText().toString().trim());
                makePostRequest(params, "http://52.78.77.90/satellite/change_nickname.php");
            }
        });


    }

    private void makePostRequest(RequestParams params, String url) {
        AsyncHttpClient client = new AsyncHttpClient();


        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody);
                // Handle the response
                if (response.equals("1")) {
                    Log.i(TAG, "닉네임 변경 성공" + response);
                    Toast.makeText(ChangeNicknameActivity.this, "닉네임을 변경했습니다.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), EditProfileActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(ChangeNicknameActivity.this, "닉네임 변경에 실패했습니다." , Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "닉네임 변경 실패" + response);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                Toast.makeText(ChangeNicknameActivity.this, "Request failed", Toast.LENGTH_LONG).show();
            }
        });
    }
}