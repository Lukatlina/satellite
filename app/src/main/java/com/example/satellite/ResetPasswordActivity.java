package com.example.satellite;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    String TAG = "ResetPasswordActivity";

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

    TextInputEditText et_reset_pw_email;
    TextInputEditText et_reset_pw_auth_code;
    TextInputEditText et_reset_pw;
    TextInputEditText et_reset_pw_check;

    TextInputLayout et_reset_pw_email_layout;
    TextInputLayout et_reset_pw_auth_code_layout;
    TextInputLayout et_reset_pw_layout;
    TextInputLayout et_reset_pw_check_layout;


    Button btn_reset_pw_email_check;
    Button btn_reset_pw_auth_code_check;
    Button btn_reset_pw_submit;
    Button btn_reset_pw_return;

    String uniq_id;
    String is_artist;
    String email;
    String nickname;
    String authcode;
    String password;
    String check_password;
    String sessionId = null;

    int email_check_complete;
    int authcode_check_complete;
    boolean password_check_complete;

    String pattern;
    Pattern regex;
    Matcher matcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reset_pw), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btn_reset_pw_return= findViewById(R.id.btn_reset_pw_return);
        et_reset_pw_email = findViewById(R.id.et_reset_pw_email);
        et_reset_pw_auth_code = findViewById(R.id.et_reset_pw_auth_code);
        et_reset_pw = findViewById(R.id.et_reset_pw);
        et_reset_pw_check = findViewById(R.id.et_reset_pw_check);
        et_reset_pw_email_layout = findViewById(R.id.et_reset_pw_email_layout);
        et_reset_pw_auth_code_layout = findViewById(R.id.et_reset_pw_auth_code_layout);
        et_reset_pw_layout = findViewById(R.id.et_reset_pw_layout);
        et_reset_pw_check_layout = findViewById(R.id.et_reset_pw_check_layout);

        btn_reset_pw_email_check = findViewById(R.id.btn_reset_pw_email_check);
        btn_reset_pw_auth_code_check = findViewById(R.id.btn_reset_pw_auth_code_check);
        btn_reset_pw_submit = findViewById(R.id.btn_reset_pw_submit);

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        uniq_id = user.getString("uniq_id", "");
        is_artist = user.getString("is_artist", "");

        // 쉐어드에 값이 있다면 로그인 된 것이기 때문에 email을 보여줌
        if (!uniq_id.isEmpty()) {
            HttpUrl.Builder urlBuilder = HttpUrl.parse("http://52.78.77.90/satellite/user_data.php").newBuilder();
            // get방식 파라미터 추가
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
                                        if (email.equals("null")) {
                                            et_reset_pw_email.setFocusableInTouchMode(true);
                                        }else{
                                            et_reset_pw_email.setText(email);
                                        }

                                        Log.i(TAG, "유저 이메일 : " + email + "유저 닉네임 : " + nickname);

                                    } else {
                                        Log.i(TAG, "여기 들어가나??? 확인 ---");
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
        }else{
            btn_reset_pw_email_check.setEnabled(false);
            btn_reset_pw_email_check.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ResetPasswordActivity.this, R.color.disabled_color)));
            et_reset_pw_email.setFocusableInTouchMode(true);
        }



        btn_reset_pw_return.setOnClickListener(new View.OnClickListener() {
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

        et_reset_pw_email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                email = et_reset_pw_email.getText().toString().trim();
                // 안드로이드에서 기본으로 제공하는 이메일 형식 검사
                Pattern email_pattern = Patterns.EMAIL_ADDRESS;
                if (TextUtils.isEmpty(email) || !email_pattern.matcher(email).matches()) {
                    // 이메일이 비어있거나 이메일 형식에 맞지 않으면 실행될 문구
                    Log.i(TAG, "이메일 입력 실패: ");
                    et_reset_pw_email_layout.setError("유효한 이메일을 입력해주세요.");
                    btn_reset_pw_email_check.setEnabled(false);
                    btn_reset_pw_email_check.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ResetPasswordActivity.this, R.color.disabled_color)));
                    email_check_complete = 0;
                } else {
                    Log.i(TAG, "이메일 입력 성공: ");
                    et_reset_pw_email_layout.setError(null);
                    et_reset_pw_email_layout.setHelperText(null);
                    btn_reset_pw_email_check.setEnabled(true);
                    btn_reset_pw_email_check.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ResetPasswordActivity.this, R.color.colorPrimary)));
                    email_check_complete = 0;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // 인증번호 보내는 확인 버튼
        btn_reset_pw_email_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email_edit = et_reset_pw_email.getText().toString().trim();
                // HttpUrlConnection

                final Thread th = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String page = "http://52.78.77.90/satellite/send_email.php";
                            // URL 객체 생성
                            URL url = new URL(page);
                            // 연결 객체 생성
                            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

                            // Post 파라미터
                            String params = "email=" + email_edit;

                            Log.i(TAG, "보내기 전 문자열 :" + params);



                            // 결과값 저장 문자열
                            final StringBuilder sb = new StringBuilder();
                            // 연결되면
                            if(conn != null) {
                                Log.i(TAG, "conn 연결");
                                // 응답 타임아웃 설정
                                conn.setRequestProperty("Accept", "application/json");
                                conn.setConnectTimeout(10000);
                                // POST 요청방식
                                conn.setRequestMethod("POST");
                                // 포스트 파라미터 전달
                                conn.getOutputStream().write(params.getBytes("utf-8"));
                                // url에 접속 성공하면 (200)
                                if(conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                    // 결과 값 읽어오는 부분
                                    BufferedReader br = new BufferedReader(new InputStreamReader(
                                            conn.getInputStream(), "utf-8"
                                    ));
                                    String line;
                                    while ((line = br.readLine()) != null) {
                                        sb.append(line);
                                    }

                                    // 버퍼리더 종료
                                    br.close();
                                    Log.i(TAG, "헤더필즈 확인용 ------------------------------------ " +conn.getHeaderFields());

                                    // Retrieve the session ID from the cookies
                                    Map<String, List<String>> headerFields = conn.getHeaderFields();
                                    List<String> cookiesHeader = headerFields.get("Set-Cookie");
                                    Log.i(TAG, "쿠키 확인" + cookiesHeader);

                                    if (cookiesHeader != null) {
                                        for (String cookie : cookiesHeader) {
                                            if (cookie.contains("PHPSESSID")) {
                                                sessionId = cookie.split(";")[0];
                                                Log.i(TAG, "세션쿠키아이디 확인" + sessionId);
                                                break;
                                            }
                                        }
                                    }

                                    Log.i(TAG, "결과 문자열 : " + sb);
                                    // 응답 Json 타입일 경우
                                    // JSONArray jsonResponse = new JSONArray(sb.toString());
                                    // Log.i("tag", "확인 jsonArray : " + jsonResponse);

                                    String result = sb.toString();
                                    email_check_complete = Integer.parseInt(result);
                                    Log.i(TAG, "확인 sb_result : " + email_check_complete);
                                    // UI 스레드에서 TextInputLayout에 값을 설정
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // 서버에서 돌아온 값을 가지고 TextInputLayout에 값을 설정
                                            if (email_check_complete == 0) {
                                                et_reset_pw_email_layout.setError("가입되지 않은 이메일입니다.");
                                                btn_reset_pw_email_check.setEnabled(false);
                                                btn_reset_pw_email_check.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ResetPasswordActivity.this, R.color.disabled_color)));
                                                Log.i(TAG, "email_check_complete0이 실행되나?" + email_check_complete);
                                            } else if (email_check_complete == 1) {
                                                et_reset_pw_email_layout.setHelperText("인증메일을 발송했습니다.\n메일 확인 후 인증번호를 입력해주세요.");
                                                Log.i(TAG, "email_check_complete1이 실행되나?" + email_check_complete);
                                            }
                                        }
                                    });
                                } else {
                                    // runOnUiThread 기본
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), "네트워크 문제 발생", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                // 연결 끊기
                                conn.disconnect();
                            }

                        }catch (Exception e) {
                            Log.i(TAG, "error :" + e);
                        }
                    }
                });
                th.start();

            }

        });

        // 인증번호 입력시마다 체크해서 인증번호 확인 버튼의 색상을 바꾼다
        et_reset_pw_auth_code.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                authcode = et_reset_pw_auth_code.getText().toString();

                if (TextUtils.isEmpty(authcode)) {
                    // 인증번호가 비어있으면
                    Log.i(TAG, "인증번호 입력 실패: ");
                    btn_reset_pw_auth_code_check.setEnabled(false);
                    btn_reset_pw_auth_code_check.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ResetPasswordActivity.this, R.color.disabled_color)));
                } else {
                    Log.i(TAG, "인증번호 입력 성공: ");
                    btn_reset_pw_auth_code_check.setEnabled(true);
                    btn_reset_pw_auth_code_check.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ResetPasswordActivity.this, R.color.colorPrimary)));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


        btn_reset_pw_auth_code_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // HttpUrlConnection

                // 에딧 텍스트에 입력된 값 변수에 저장
                String auth_code_edit = et_reset_pw_auth_code.getText().toString().trim();

                final Thread th = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String page = "http://52.78.77.90/satellite/authcode_check.php";
                            // URL 객체 생성
                            URL url = new URL(page);
                            Log.i("페이지 확인 ---------------------", page);
                            // 연결 객체 생성
                            HttpURLConnection conn = (HttpURLConnection)url.openConnection();


//                            MAP<String, List<String>> map = conn.getHeaderFields();
                            // Post 파라미터
                            String params = "authcode=" + auth_code_edit;
                            Log.i(TAG, "보내기 전 문자열 :" + params);

                            if (sessionId != null) {
                                conn.setRequestProperty("Cookie", sessionId);
                                Log.i(TAG, " 세션id 들어갔나? " + sessionId);
                            }

                            // 결과값 저장 문자열
                            final StringBuilder sb = new StringBuilder();
                            // 연결되면
                            if(conn != null) {
                                Log.i(TAG, "conn 연결");
                                // 응답 타임아웃 설정
                                conn.setRequestProperty("Accept", "application/json");
                                conn.setConnectTimeout(10000);
                                // POST 요청방식
                                conn.setRequestMethod("POST");
                                // 포스트 파라미터 전달
                                conn.getOutputStream().write(params.getBytes("utf-8"));
                                // url에 접속 성공하면 (200)
                                if(conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                    // 결과 값 읽어오는 부분
                                    BufferedReader br = new BufferedReader(new InputStreamReader(
                                            conn.getInputStream(), "utf-8"
                                    ));
                                    String line;
                                    while ((line = br.readLine()) != null) {
                                        sb.append(line);
                                    }
                                    // 버퍼리더 종료
                                    br.close();
                                    Log.i(TAG, "리턴된 헤더필즈 재확인용 ------------------------------------ " +conn.getHeaderFields());
                                    Log.i(TAG, "결과 문자열 :" +sb);

                                    String result = sb.toString();
                                    authcode_check_complete = Integer.parseInt(result);
                                    Log.i(TAG, "확인 sb_result : " + authcode_check_complete);
                                    // UI 스레드에서 TextInputLayout에 값을 설정
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // 서버에서 돌아온 값을 가지고 TextInputLayout에 값을 설정
                                            if (authcode_check_complete == 0) {
                                                et_reset_pw_auth_code_layout.setError("인증번호를 다시 입력해주세요.");
                                                check_submit_button();
                                                Log.i(TAG, "authcode_check_complete0이 실행되나?" + authcode_check_complete);
                                            } else if (authcode_check_complete == 1) {
                                                et_reset_pw_auth_code_layout.setHelperText("비밀번호를 설정해주세요.");
                                                Log.i(TAG, "authcode_check_complete1이 실행되나?" + authcode_check_complete);
                                                check_submit_button();
                                            }else{

                                            }
                                        }
                                    });

                                }else {

                                    // runOnUiThread 기본
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), "네트워크 문제 발생", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                // 연결 끊기
                                conn.disconnect();
                            }

                        }catch (Exception e) {
                            Log.i(TAG, "error :" + e);
                        }
                    }
                });
                th.start();

            }

        });

        // 비밀번호 입력시마다 조건 일치 여부 확인
        et_reset_pw.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                password = et_reset_pw.getText().toString();
                if (check_password != null) {
                    Log.i(TAG, "비밀번호 : " + password + "재확인 비밀번호 : " + check_password);
                }
                // 비밀번호 유효성 검사 패턴
                // 영문 대문자, 소문자, 숫자, 특수문자가 들어갈 수 있으며 한가지로만 구성될 수 없음
                // 10-13자여야함
                pattern = "^(?=.*[A-Z].*[a-z]|.*[a-z].*[A-Z]|.*[A-Z].*\\d|.*\\d.*[A-Z]|.*[A-Z].*[~!@#$%^&*()_+=]|.*[~!@#$%^&*()_+=].*[A-Z]|.*[a-z].*\\d|.*\\d.*[a-z]|.*[a-z].*[~!@#$%^&*()_+=]|.*[~!@#$%^&*()_+=].*[a-z]|.*\\d.*[~!@#$%^&*()_+=]|.*[~!@#$%^&*()_+=].*\\d)[A-Za-z\\d~!@#$%^&*()_+=]{10,13}$";

                regex = Pattern.compile(pattern);
                matcher = regex.matcher(password);
                Log.i(TAG, "비밀번호 조건 검사" + matcher.matches());

                if (matcher.matches()) {
                    // 비밀번호 조건에 맞으면
                    et_reset_pw_layout.setError(null);
                    if (check_password != null) {
                        if (password.equals(check_password)){
                            et_reset_pw_check_layout.setError(null);
                            password_check_complete = true;
                            check_submit_button();
                            Log.i(TAG, "체크 패스워드 확인 if : " + password_check_complete);
                        }else{
                            password_check_complete = false;
                            et_reset_pw_check_layout.setError("비밀번호가 일치하지 않습니다.");
                            check_submit_button();
                            Log.i(TAG, "체크 패스워드 확인 else : " + password_check_complete);
                        }
                    }
                    Log.i(TAG, "비밀번호 입력 성공 ");
                } else {
                    Log.i(TAG, "인증번호 입력 실패: ");
                    et_reset_pw_layout.setError("영문 대문자, 소문자, 숫자, 특수문자 중 2가지 이상 조합하여, 10-13자리의 비밀번호를 입력해주세요.");
                    password_check_complete = false;
                    check_submit_button();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // 비밀번호 재확인시 일치하는 비밀번호인지 확인
        et_reset_pw_check.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                check_password = et_reset_pw_check.getText().toString();
                Log.i(TAG, "비밀번호 : " + password + "재확인 비밀번호 : " + check_password);
                if (password == null) {
                    et_reset_pw_layout.setError("비밀번호를 먼저 입력해주세요.");
                    Log.i(TAG, "비밀번호 null");
                } else if (password.equals(check_password)) {
                    et_reset_pw_check_layout.setError(null);
                    if (matcher.matches()) {
                        password_check_complete = true;
                        check_submit_button();
                    }
                    Log.i(TAG, "비밀번호 확인 성공");
                } else {
                    et_reset_pw_check_layout.setError("비밀번호가 일치하지 않습니다.");
                    password_check_complete = false;
                    Log.i(TAG, "비밀번호 확인 실패");
                    check_submit_button();
                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // 비밀번호 변경 버튼
        // AsyncHttpClient 사용
        btn_reset_pw_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 비밀번호 재설정 버튼 활성화를 위한 조건문. 모든 조건을 충족해야지 버튼이 활성화 된다.
                if (email_check_complete == 1 &&
                        authcode_check_complete == 1 &&
                        password_check_complete == true) {

                    RequestParams params = new RequestParams();
                    params.put("email", et_reset_pw_email.getText().toString().trim());
                    params.put("is_artist", is_artist);
                    params.put("password", et_reset_pw.getText().toString().trim());

                    makePostRequest(params, "http://52.78.77.90/satellite/reset_password.php");

                }
            }
        });
    }

    private void check_submit_button() {
        if (email_check_complete == 1 &&
                authcode_check_complete == 1 &&
                password_check_complete == true) {
            Log.i(TAG, "check_submit_button 성공 : " + email_check_complete + authcode_check_complete + password_check_complete);
            btn_reset_pw_submit.setEnabled(true);
            btn_reset_pw_submit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ResetPasswordActivity.this, R.color.colorPrimary)));
        }else{
            btn_reset_pw_submit.setEnabled(false);
            btn_reset_pw_submit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ResetPasswordActivity.this, R.color.disabled_color)));
            Log.i(TAG, "check_submit_button 실패 : " + email_check_complete + authcode_check_complete + password_check_complete);

        }
    }

    private void makePostRequest(RequestParams params, String url) {
        AsyncHttpClient client = new AsyncHttpClient();


        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody);
                // Handle the response
                if (response.equals("1")) {
                    Log.i(TAG, "비밀번호 변경 성공" + response);
                    Toast.makeText(getApplicationContext(), "비밀번호를 변경했습니다.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), EditProfileActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(getApplicationContext(), "비밀번호 변경 실패했습니다." , Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "비밀번호 변경 실패" + response);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                Toast.makeText(ResetPasswordActivity.this, "Request failed", Toast.LENGTH_LONG).show();
            }
        });
    }
}