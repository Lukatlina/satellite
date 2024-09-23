package com.example.satellite;

import android.annotation.SuppressLint;
import android.content.Intent;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JoinActivity extends AppCompatActivity {

    // 변수 초기설정
    String TAG = "JoinActivity";
    String sessionId = null;
    // 버튼
    Button btn_join_email_check;
    Button btn_join_auth_code_check;
    Button btn_join_submit;
    Button btn_join_return;

    // 에딧 텍스트
    TextInputEditText et_join_email;
    TextInputEditText et_join_auth_code;
    TextInputEditText et_join_pw;
    TextInputEditText et_join_pw_check;
    TextInputEditText et_join_nickname;

    // 에딧 텍스트 레이아웃
    TextInputLayout et_join_email_layout;
    TextInputLayout et_join_auth_code_layout;
    TextInputLayout et_join_pw_layout;
    TextInputLayout et_join_pw_check_layout;
    TextInputLayout et_join_nickname_layout;

    String email;
    String authcode;
    String password;
    String check_password;
    String nickname;

    int email_check_complete;
    int authcode_check_complete;
    boolean password_check_complete;
    boolean nickname_check_complete;

    String pattern;
    Pattern regex;
    Matcher matcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_join);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.join), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // UI 요소 연결
        // 버튼
        btn_join_email_check = findViewById(R.id.btn_join_email_check);
        btn_join_auth_code_check = findViewById(R.id.btn_join_auth_code_check);
        btn_join_submit = findViewById(R.id.btn_join_submit);
        btn_join_return = findViewById(R.id.btn_join_return);

        // 에딧 텍스트
        et_join_email = findViewById(R.id.et_join_email);
        et_join_auth_code = findViewById(R.id.et_join_auth_code);
        et_join_pw = findViewById(R.id.et_join_pw);
        et_join_pw_check = findViewById(R.id.et_join_pw_check);
        et_join_nickname = findViewById(R.id.et_join_nickname);

        // 에딧 텍스트 레이아웃
        et_join_email_layout = findViewById(R.id.et_join_email_layout);
        et_join_auth_code_layout = findViewById(R.id.et_join_auth_code_layout);
        et_join_pw_layout = findViewById(R.id.et_join_pw_layout);
        et_join_pw_check_layout = findViewById(R.id.et_join_pw_check_layout);
        et_join_nickname_layout = findViewById(R.id.et_join_nickname_layout);

        // 뒤로 가기 버튼 클릭
        btn_join_return.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(JoinActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 이메일 확인 버튼 클릭
        btn_join_email_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // HttpUrlConnection

                // 에딧 텍스트에 입력된 값 변수에 저장
                String email_edit = et_join_email.getText().toString().trim();

                final Thread th = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String page = "http://52.78.77.90/satellite/email_check.php";
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
                                                et_join_email_layout.setError("이미 가입된 이메일입니다. 새로운 이메일을 입력해주세요.");
                                                btn_join_email_check.setEnabled(false);
                                                btn_join_email_check.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(JoinActivity.this, R.color.disabled_color)));
                                                Log.i(TAG, "email_check_complete0이 실행되나?" + email_check_complete);
                                            } else if (email_check_complete == 1) {
                                                et_join_email_layout.setHelperText("인증메일을 발송했습니다.\n메일 확인 후 인증번호를 입력해주세요.");
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


        btn_join_auth_code_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // HttpUrlConnection

                // 에딧 텍스트에 입력된 값 변수에 저장
                String auth_code_edit = et_join_auth_code.getText().toString().trim();

                final Thread th = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String page = "http://52.78.77.90/authcode_check.php";
                            // URL 객체 생성
                            URL url = new URL(page);
                            Log.i("페이지 확인 ---------------------", page);
                            // 연결 객체 생성
                            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

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
                                                et_join_auth_code_layout.setError("인증번호를 다시 입력해주세요.");
                                                Log.i(TAG, "authcode_check_complete0이 실행되나?" + authcode_check_complete);
                                            } else if (authcode_check_complete == 1) {
                                                et_join_auth_code_layout.setHelperText("비밀번호를 설정해주세요.");
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

        // 회원가입 버튼
        // OKHTTP 이용
        btn_join_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 회원가입 버튼 활성화를 위한 조건문. 모든 조건을 충족해야지 버튼이 활성화 된다.
                if (email_check_complete == 1 &&
                        authcode_check_complete == 1 &&
                        password_check_complete &&
                        nickname_check_complete) {

                    btn_join_submit.setEnabled(false);

                    // get방식 파라미터 추가
                    HttpUrl.Builder urlBuilder = HttpUrl.parse("http://52.78.77.90/save_user.php").newBuilder();
                    String url = urlBuilder.build().toString();

                    // POST 파라미터 추가
                    RequestBody formBody = new FormBody.Builder()
                            .add("email", et_join_email.getText().toString().trim())
                            .add("password", et_join_pw.getText().toString().trim())
                            .add("nickname", et_join_nickname.getText().toString().trim())
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
                                            btn_join_submit.setEnabled(true);
                                            Toast.makeText(getApplicationContext(), "네트워크 문제 발생", Toast.LENGTH_SHORT).show();

                                        } else {
                                            // 응답 성공
                                            Log.i(TAG, "응답 성공");
                                            final String responseData = response.body().string();
                                            Log.i(TAG, "무슨 데이터? : " + responseData);
                                            if (responseData.equals("1")) {
                                                Toast.makeText(getApplicationContext(), "회원가입에 성공했습니다.", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                startActivity(intent);

                                            } else {
                                                btn_join_submit.setEnabled(true);
                                                Toast.makeText(getApplicationContext(), "회원가입에 실패했습니다." + responseData, Toast.LENGTH_SHORT).show();
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
        });

        et_join_email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.i(TAG, "beforeTextChanged");
            }

            @SuppressLint("ResourceType")
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                email = et_join_email.getText().toString().trim();
                // 안드로이드에서 기본으로 제공하는 이메일 형식 검사
                Pattern email_pattern = Patterns.EMAIL_ADDRESS;
                if (TextUtils.isEmpty(email) || !email_pattern.matcher(email).matches()) {
                    // 이메일이 비어있거나 이메일 형식에 맞지 않으면 실행될 문구
                    Log.i(TAG, "이메일 입력 실패: ");
                    et_join_email_layout.setError("유효한 이메일을 입력해주세요.");
                    btn_join_email_check.setEnabled(false);
                    btn_join_email_check.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(JoinActivity.this, R.color.disabled_color)));
                    email_check_complete = 0;
                } else {
                    Log.i(TAG, "이메일 입력 성공: ");
                    et_join_email_layout.setError(null);
                    et_join_email_layout.setHelperText(null);
                    btn_join_email_check.setEnabled(true);
                    btn_join_email_check.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(JoinActivity.this, R.color.colorPrimary)));
                    email_check_complete = 0;
                }


            }

            @Override
            public void afterTextChanged(Editable editable) {
                Log.i(TAG, "afterTextChanged");
            }
        });

        // 인증번호 입력시마다 체크해서 인증번호 확인 버튼의 색상을 바꾼다
        et_join_auth_code.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                authcode = et_join_auth_code.getText().toString();

                if (TextUtils.isEmpty(authcode)) {
                    // 인증번호가 비어있으면
                    Log.i(TAG, "인증번호 입력 실패: ");
                    btn_join_auth_code_check.setEnabled(false);
                    btn_join_auth_code_check.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(JoinActivity.this, R.color.disabled_color)));
                } else {
                    Log.i(TAG, "인증번호 입력 성공: ");
                    btn_join_auth_code_check.setEnabled(true);
                    btn_join_auth_code_check.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(JoinActivity.this, R.color.colorPrimary)));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // 비밀번호 입력시마다 조건 일치 여부 확인
        et_join_pw.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                password = et_join_pw.getText().toString();
                if (check_password != null) {
                    Log.i(TAG, "비밀번호 : " + password + "재확인 비밀번호 : " + check_password);
                }
                // 비밀번호 유효성 검사 패턴
                // 영문 대문자, 소문자, 숫자, 특수문자가 들어갈 수 있으며 한가지로만 구성될 수 없음
                // 10-13자여야함
//                String pattern = "^(?!((?:[A-Z]+)|(?:[a-z]+)|(?:[~!@#$%^&*()_+=]+)|(?:[0-9]+))$)[A-Za-z\\d~!@#$%^&*()_+=]{10,13}$";
                pattern = "^(?=.*[A-Z].*[a-z]|.*[a-z].*[A-Z]|.*[A-Z].*\\d|.*\\d.*[A-Z]|.*[A-Z].*[~!@#$%^&*()_+=]|.*[~!@#$%^&*()_+=].*[A-Z]|.*[a-z].*\\d|.*\\d.*[a-z]|.*[a-z].*[~!@#$%^&*()_+=]|.*[~!@#$%^&*()_+=].*[a-z]|.*\\d.*[~!@#$%^&*()_+=]|.*[~!@#$%^&*()_+=].*\\d)[A-Za-z\\d~!@#$%^&*()_+=]{10,13}$";

                regex = Pattern.compile(pattern);
                matcher = regex.matcher(password);
                Log.i(TAG, "비밀번호 조건 검사" + matcher.matches());

                if (matcher.matches()) {
                    // 비밀번호 조건에 맞으면
                    et_join_pw_layout.setError(null);
                    if (check_password != null) {
                        if (password.equals(check_password)){
                            et_join_pw_check_layout.setError(null);
                            password_check_complete = true;
                            check_submit_button();
                            Log.i(TAG, "체크 패스워드 확인 if : " + password_check_complete);
                        }else{
                            password_check_complete = false;
                            et_join_pw_check_layout.setError("비밀번호가 일치하지 않습니다.");
                            check_submit_button();
                            Log.i(TAG, "체크 패스워드 확인 else : " + password_check_complete);
                        }
                    }
                    Log.i(TAG, "비밀번호 입력 성공 ");
                } else {
                    Log.i(TAG, "인증번호 입력 실패: ");
                    et_join_pw_layout.setError("영문 대문자, 소문자, 숫자, 특수문자 중 2가지 이상 조합하여, 10-13자리의 비밀번호를 입력해주세요.");
                    password_check_complete = false;
                    check_submit_button();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // 비밀번호 재확인시 일치하는 비밀번호인지 확인
        et_join_pw_check.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                check_password = et_join_pw_check.getText().toString();
                Log.i(TAG, "비밀번호 : " + password + "재확인 비밀번호 : " + check_password);
                if (password == null) {
                    et_join_pw_layout.setError("비밀번호를 먼저 입력해주세요.");
                    Log.i(TAG, "비밀번호 null");
                } else if (password.equals(check_password)) {
                    et_join_pw_check_layout.setError(null);
                    if (matcher.matches()) {
                        password_check_complete = true;
                        check_submit_button();
                    }
                    Log.i(TAG, "비밀번호 확인 성공");
                } else {
                    et_join_pw_check_layout.setError("비밀번호가 일치하지 않습니다.");
                    password_check_complete = false;
                    Log.i(TAG, "비밀번호 확인 실패");
                    check_submit_button();
                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


        // 닉네임이 비어있는지 확인
        et_join_nickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                nickname = et_join_nickname.getText().toString();
                if (!nickname.isEmpty() && nickname.length() <= 20) {
                    nickname_check_complete = true;
                    Log.i(TAG, "nickname_check_complete : " + nickname_check_complete);
                    check_submit_button();
                }else {
                    nickname_check_complete = false;
                    et_join_pw_layout.setError("닉네임을 입력해주세요.");
                    check_submit_button();
                    Log.i(TAG, "nickname_check_complete : " + nickname_check_complete);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void check_submit_button() {
        if (email_check_complete == 1 &&
                authcode_check_complete == 1 &&
                password_check_complete == true &&
                nickname_check_complete == true) {
            Log.i(TAG, "check_submit_button: 성공" + email_check_complete + authcode_check_complete + password_check_complete + nickname_check_complete);
            btn_join_submit.setEnabled(true);
            btn_join_submit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(JoinActivity.this, R.color.colorPrimary)));
        }else{
            btn_join_submit.setEnabled(false);
            btn_join_submit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(JoinActivity.this, R.color.disabled_color)));
            Log.i(TAG, "check_submit_button: 실패" + email_check_complete + authcode_check_complete + password_check_complete + nickname_check_complete);

        }
    }
}