package com.example.satellite.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.satellite.ChatService;
import com.example.satellite.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    // 변수 초기설정
    String TAG = "LoginActivity";

    Button btn_login_submit;
    Button btn_login_signin;
    Button btn_login_findpw;

    TextInputLayout et_login_email_layout;
    TextInputLayout et_login_pw_layout;

    TextInputEditText et_login_email;
    TextInputEditText et_login_pw;

    SharedPreferences user;
    SharedPreferences.Editor user_editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // enable : 활성화하다, EdgeToEdge 활성화 의미
        EdgeToEdge.enable(this);
        // activity_main 레이아웃을 찾아서 연결
        setContentView(R.layout.activity_login);
        // View 관련해서 sdk 버전이 바뀌어도 지원이 되도록 처리해줌
        // 이 뷰에 창을 삽입하는
        // insets 클래스 : int left, int top, int right, int bottom
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //  UI 요소 연결
        btn_login_submit  = findViewById(R.id.btn_login_submit);
        btn_login_signin = findViewById(R.id.btn_login_signin);
        btn_login_findpw = findViewById(R.id.btn_login_findpw);

        et_login_email_layout = findViewById(R.id.et_login_email_layout);
        et_login_pw_layout = findViewById(R.id.et_login_pw_layout);

        et_login_email = findViewById(R.id.et_login_email);
        et_login_pw = findViewById(R.id.et_login_pw);

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        // 회원가입 화면으로 이동
        btn_login_signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, JoinActivity.class);
                startActivity(intent);
            }
        });

        btn_login_findpw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
                startActivity(intent);
            }
        });

        btn_login_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendPostRequest();
            }
        });

        // 로그인 버튼


    }

    private void sendPostRequest() {
        String url = "http://52.78.77.90/satellite/login.php";
        JSONObject postData = new JSONObject();
        try {
            postData.put("email", et_login_email.getText().toString().trim());
            postData.put("password", et_login_pw.getText().toString().trim());
            Log.i(TAG, "발리 포스트데이터" + postData);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, postData,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Handle response
                        Log.i(TAG, "response" + response);

                        try {
                            int result = response.getInt("result");
                            Log.i(TAG, "인트값 : " + result);

                            if (result == 1) {
                                Log.i(TAG, "if문 작동하나?" + response.getInt("result"));
                                String uniq_id = response.getString("uniq_id");
                                int user_id = response.getInt("user_id");
                                int is_artist = response.getInt("is_artist");
                                Log.i(TAG, "아티스트인가???" + is_artist);

                                // id를 key값으로 해서 저장한다.
                                user_editor.putString("uniq_id", uniq_id);
                                user_editor.putInt("user_id", user_id);
                                user_editor.putInt("is_artist", is_artist);
                                user_editor.apply();
                                Toast.makeText(getApplicationContext(), "로그인에 성공했습니다.", Toast.LENGTH_SHORT).show();

                                // 서비스 시작 (로그인 성공 후에만)
                                Intent serviceIntent = new Intent(LoginActivity.this, ChatService.class);
                                startService(serviceIntent);

                                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                            } else if (result == -1) {
                                showDialog("탈퇴한 회원입니다. 다른 계정으로 가입해주세요.");
                            } else{
                                Log.i(TAG, "0일때 여기로 값 들어가는지 확인");
                                showDialog("등록되어 있지 않은 이메일이거나 비밀번호를 잘못 입력하셨습니다.");
                            }

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Handle error
                Toast.makeText(LoginActivity.this, "오류가 생겼습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show();
                Log.i(TAG, "에러" + error.getMessage());
            }
        });

        // Add the request to the RequestQueue
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private void showDialog(String message) {
        AlertDialog.Builder menu = new AlertDialog.Builder(LoginActivity.this);
        menu.setIcon(R.mipmap.ic_launcher);
        menu.setMessage(message); // 문구


        // 확인 버튼
        menu.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dialog 제거
                dialog.dismiss();
            }
        });

        menu.show();
    }
}