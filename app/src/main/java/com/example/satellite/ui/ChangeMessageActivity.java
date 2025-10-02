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

public class ChangeMessageActivity extends AppCompatActivity {

    String TAG = "ChangeMessageActivity";

    SharedPreferences user;
    SharedPreferences.Editor user_editor;


    Button btn_change_message_return;
    Button btn_change_message_submit;

    TextInputLayout et_change_message_layout;
    TextInputEditText et_change_message;

    String uniq_id;
    int is_artist;
    String email;
    String message;
    String change_message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_message);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.change_message), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        btn_change_message_return = findViewById(R.id.btn_change_message_return);
        btn_change_message_submit = findViewById(R.id.btn_change_message_submit);
        et_change_message_layout = findViewById(R.id.et_change_message_layout);
        et_change_message = findViewById(R.id.et_change_message);

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        uniq_id = user.getString("uniq_id", "");
        is_artist = user.getInt("is_artist", -1);

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
                                    message = user_data.getString("message");
                                    Log.i(TAG, "유저 이메일 : " + email + "유저 메세지 : " + message);
                                    if (!message.isEmpty()){
                                        et_change_message.setHint(message);
                                    }else{
                                        et_change_message.setHint("상태 메시지를 입력해주세요.");
                                    }

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

        btn_change_message_return.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChangeMessageActivity.this, EditProfileActivity.class);
                startActivity(intent);
                finish();
            }
        });

        et_change_message.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!et_change_message.getText().toString().isEmpty() && et_change_message.getText().toString().length() <= 30 && !et_change_message.getText().toString().equals(message)) {
                    // 변경 가능한 메시지
                    et_change_message_layout.setError(null);
                    change_message = et_change_message.getText().toString();
                    btn_change_message_submit.setEnabled(true);
                    btn_change_message_submit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ChangeMessageActivity.this, R.color.colorPrimary)));
                    Log.i(TAG, "어떤 메세지?" + change_message);
                }else{
                    // 조건에 맞지 않을 경우
                    et_change_message_layout.setError("유효한 메시지를 입력해주세요.");
                    btn_change_message_submit.setEnabled(false);
                    btn_change_message_submit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ChangeMessageActivity.this, R.color.disabled_color)));

                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        btn_change_message_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RequestParams params = new RequestParams();
                params.put("email", email);
                params.put("is_artist", is_artist);
                params.put("message", et_change_message.getText().toString().trim());
                makePostRequest(params, "http://52.78.77.90/satellite/change_message.php");
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
                    Log.i(TAG, "상태메시지 변경 성공" + response);
                    Toast.makeText(ChangeMessageActivity.this, "상태메시지를 변경했습니다.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), EditProfileActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(ChangeMessageActivity.this, "상태메시지 변경에 실패했습니다." , Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "상태메시지 변경 실패" + response);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                Toast.makeText(ChangeMessageActivity.this, "Request failed", Toast.LENGTH_LONG).show();
            }
        });

    }
}