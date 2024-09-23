package com.example.satellite;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WithdrawalActivity extends AppCompatActivity {

    String TAG = "WithdrawalActivity";

    CheckBox cb_withdrawal_confirm;
    TextInputEditText et_withdrawal_pw;
    Button btn_withdrawal_submit;
    Button btn_withdrawal_return;

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

    String user_data;
    String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_withdrawal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.withdrawal), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cb_withdrawal_confirm = findViewById(R.id.cb_withdrawal_confirm);
        et_withdrawal_pw = findViewById(R.id.et_withdrawal_pw);
        btn_withdrawal_submit = findViewById(R.id.btn_withdrawal_submit);
        btn_withdrawal_return = findViewById(R.id.btn_withdrawal_return);

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        user_data = user.getString("uniq_id", "");
        Log.i(TAG, "유저데이터" + user_data);


        HttpUrl.Builder urlBuilder = HttpUrl.parse("http://52.78.77.90/user_data.php").newBuilder();
        // get방식 파라미터 추가
        String url = urlBuilder.build().toString();

        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("uniq_id", user_data)
                .build();

        // 요청 만들기
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        // 응답 콜백
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(okhttp3.Call call, final okhttp3.Response response) throws IOException {

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
                                    Log.i(TAG, "유저 이메일 : " + email);
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

        btn_withdrawal_return.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WithdrawalActivity.this, MoreActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 체크박스에 체크 이벤트가 실행될 때마다 조건에 맞는지 확인하고 버튼을 활성화한다.
        cb_withdrawal_confirm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    if (cb_withdrawal_confirm.isChecked() && !et_withdrawal_pw.getText().toString().isEmpty()){
                        btn_withdrawal_submit.setEnabled(true);
                    }else{
                        btn_withdrawal_submit.setEnabled(false);
                    }
                }else{
                    btn_withdrawal_submit.setEnabled(false);
                }
            }
        });

        // 텍스트 인풋에 글을 쓸 때마다 조건에 맞는지 확인하고 버튼을 활성화한다.
        et_withdrawal_pw.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.i(TAG, "onTextChanged" + cb_withdrawal_confirm.isChecked());
                if (cb_withdrawal_confirm.isChecked() && !et_withdrawal_pw.getText().toString().isEmpty()){
                    btn_withdrawal_submit.setEnabled(true);
                }else{
                    btn_withdrawal_submit.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // 버튼 클릭시 서버와 소통 후 결과에 따라서 성공 혹은 오류
        btn_withdrawal_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "체크박스 확인" + cb_withdrawal_confirm.isChecked());
                ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
                Log.i(TAG, "레트로핏 객체 반환?" + service);

                String passwordvalue = et_withdrawal_pw.getText().toString();

                WithdrawRequest withdrawRequest = new WithdrawRequest();
                withdrawRequest.setEmail(email);
                withdrawRequest.setPassword(passwordvalue);

                Call<Integer> call = service.withdraw(withdrawRequest);

//                Call<NameResponse> call = service.getName(editText.getText().toString());

                call.enqueue(new Callback<Integer>() {
                    @Override
                    public void onResponse(Call<Integer> call, Response<Integer> response) {
                        if (response.isSuccessful()) {
                            Log.i(TAG, "response값" + response);

                            Integer result = response.body();
                            Log.i(TAG, "바디값" + result);

                            if (result != null) {
                                Log.i(TAG, "결과값 뭐임?" + result);
                                if (result == 1){
                                    Toast.makeText(WithdrawalActivity.this, "탈퇴가 완료되었습니다.", Toast.LENGTH_LONG).show();

                                    Intent intent = new Intent(WithdrawalActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                }else{
                                    showDialog();
                                }
                            }
                        } else {
                            Toast.makeText(WithdrawalActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Integer> call, Throwable t) {
                        Toast.makeText(WithdrawalActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            private void showDialog() {
                AlertDialog.Builder menu = new AlertDialog.Builder(WithdrawalActivity.this);
                menu.setIcon(R.mipmap.ic_launcher);
                menu.setMessage("비밀번호가 일치하지 않습니다."); // 문구


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

        });
    }
}