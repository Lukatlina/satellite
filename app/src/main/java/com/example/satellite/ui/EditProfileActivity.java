package com.example.satellite.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.satellite.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import gun0912.tedimagepicker.builder.TedImagePicker;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditProfileActivity extends AppCompatActivity {

    String TAG = "EditProfileActivity";

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

    String image_uri = "";
    String nickname;
    String message;
    String fileType;
    String uniq_id;
    int is_artist;
    String email;

    Button btn_edit_nickname;
    Button btn_edit_message;
    Button btn_edit_password;
    Button btn_edit_profile_return;
    ImageView iv_profile;
    ImageView iv_photo;

    int CAMERA_PERMISSION_CODE = 1001;
    private final String[] permissionList = {
            android.Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.edit_profile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btn_edit_nickname = findViewById(R.id.btn_edit_nickname);
        btn_edit_message = findViewById(R.id.btn_edit_message);
        btn_edit_password = findViewById(R.id.btn_edit_password);
        btn_edit_profile_return = findViewById(R.id.btn_edit_profile_return);
        iv_profile = findViewById(R.id.iv_profile);
        iv_photo = findViewById(R.id.iv_photo);

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
                                String responseData = response.body().string();
                                Log.i(TAG, "무슨 데이터? : " + responseData);
                                JSONObject user_data = new JSONObject(responseData);

                                if (user_data.getInt("result") == 1) {
                                    if (user_data.getString("image").isEmpty() || user_data.isNull("image")) {
                                        iv_profile.setImageResource(R.drawable.baseline_person_150);
                                    }else{
                                        image_uri = user_data.getString("image");
                                        Glide.with(getApplicationContext()).load(image_uri).circleCrop().into(iv_profile);
                                    }
                                    email = user_data.getString("email");
                                    message = user_data.getString("message");
                                    nickname = user_data.getString("nickname");
                                    Log.i(TAG, "유저 메시지 : " + message + "유저 닉네임 : " + nickname);
                                    btn_edit_nickname.setText(nickname);
                                    if (!message.isEmpty()){
                                        btn_edit_message.setText(message);
                                    }else{
                                        btn_edit_message.setText("상태 메시지를 입력해주세요.");
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

        btn_edit_profile_return.setOnClickListener(new View.OnClickListener() {
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

        btn_edit_nickname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(EditProfileActivity.this, ChangeNicknameActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btn_edit_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(EditProfileActivity.this, ChangeMessageActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btn_edit_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(EditProfileActivity.this, ResetPasswordActivity.class);
                startActivity(intent);
                finish();
            }
        });
        // 프로필 사진 등록 버튼
        // 만약 이미지가 있다면 다이어로그가 뜨게 만든다.
        iv_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (image_uri == null || image_uri.isEmpty()) {
                    // 카메라 권한 확인
                    if (isPermissionGranted(permissionList)) {
                        // 권한이 이미 있다면, TedImagePicker를 사용해서 이미지 선택 후 가져오기
                        selectImage();
                    } else {
                        // 권한이 없다면, 권한 요청하기
                        ActivityCompat.requestPermissions(
                                EditProfileActivity.this,
                                permissionList,
                                CAMERA_PERMISSION_CODE
                        );
                    }
                }else{
                    showCustomDialog();
                }
            }
        });
    }


    private void uploadImage(Uri uri) {
        try {
            File file = createTempFileFromUri(uri);
            Log.i(TAG, "파일이 어떻게 되어있지? : " + file);
            OkHttpClient client = new OkHttpClient();

            RequestBody fileBody = RequestBody.create(file, MediaType.parse(fileType));
            Log.i(TAG, "fileType: " + fileType);
            Log.i(TAG, "MediaType.parse(fileType): " + MediaType.parse(fileType));
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("email", email)
                    .addFormDataPart("is_artist", String.valueOf(is_artist))
                    .addFormDataPart("file", file.getName(), fileBody)
                    .build();

            Log.i(TAG, "파일 이름 : " + file.getName());
            Log.i(TAG, "파일 바디 : " + fileBody);

            Request request = new Request.Builder()
                    .url("http://52.78.77.90/satellite/save_image_profile.php")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    if (response.isSuccessful()) {
                        // 응답 성공
                        Log.i(TAG, "응답 성공");
                        String image_responseData = response.body().string();
                        Log.i(TAG, "무슨 데이터? : " + image_responseData);
                        JSONObject image_data = null;
                        try {
                            image_data = new JSONObject(image_responseData);

                            if (image_data.getInt("result") == 1) {
                                if (!image_data.getString("image").isEmpty()) {
                                    image_uri = image_data.getString("image");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
//                                            Glide.with(getApplicationContext()).load(image_uri).circleCrop().into(iv_profile);
                                            Toast.makeText(getApplicationContext(), "이미지를 변경했습니다.", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                }
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "이미지 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }

                            Log.i(TAG, "onResponse: 성공???" + image_responseData);

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }


                    } else {
                        Log.i(TAG, "onResponse: 실패???");
                        // 업로드 실패 처리
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteImage() {

        HttpUrl.Builder urlBuilder = HttpUrl.parse("http://52.78.77.90/satellite/delete_image_profile.php").newBuilder();
        String url = urlBuilder.build().toString();

        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .add("is_artist", String.valueOf(is_artist))
                .build();

        // 요청 만들기
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.isSuccessful()) {
                    // 응답 성공
                    Log.i(TAG, "응답 성공");
                    String deleteimage_responseData = response.body().string();
                    Log.i(TAG, "무슨 데이터? : " + deleteimage_responseData);

                    int result = Integer.parseInt(deleteimage_responseData);
                    if (result == 1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                iv_profile.setImageResource(R.drawable.baseline_person_150);
                                image_uri = "";
                                Toast.makeText(getApplicationContext(), "이미지를 변경했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "이미지 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                } else {
                    Log.i(TAG, "onResponse: 실패???");
                    // 업로드 실패 처리
                }
            }
        });

    }

    private File createTempFileFromUri(Uri uri) throws IOException {
        Log.i(TAG, "createTempFileFromUri: 시작");
        InputStream inputStream = this.getContentResolver().openInputStream(uri);
        Log.i(TAG, "InputStream getContentResolver().openInputStream(uri) 결과 : " + inputStream);

        fileType = this.getContentResolver().getType(uri);
        Log.i(TAG, "file type 뭐야?? : " + fileType);
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType);
        Log.i(TAG, "extension이 뭐야?? : " + extension);
        File tempFile = File.createTempFile("upload", '.' + extension, this.getCacheDir());
        Log.i(TAG, "이미지캐시주소" + this.getCacheDir());
        Log.i(TAG, "File File.createTempFile(); 결과 : " + tempFile);
        Log.i(TAG, "임시파일 주소 : " + tempFile.getAbsolutePath());
        tempFile.deleteOnExit();

        OutputStream outputStream = new FileOutputStream(tempFile);
        Log.i(TAG, "OutputStream 결과 : " + outputStream);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        outputStream.close();
        inputStream.close();

        Log.i(TAG, "createTempFileFromUri: 끝");
        return tempFile;
    }

    private void showCustomDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Button btnSelectImage = dialog.findViewById(R.id.btn_select_image);
        Button btnDefaultImage = dialog.findViewById(R.id.btn_default_image);

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 이미지 변경 로직 추가
                // 카메라 권한 확인
                if (isPermissionGranted(permissionList)) {
                    // 권한이 이미 있다면, TedImagePicker를 사용해서 이미지 선택 후 가져오기
                    selectImage();
                } else {
                    // 권한이 없다면, 권한 요청하기
                    ActivityCompat.requestPermissions(
                            EditProfileActivity.this,
                            permissionList,
                            CAMERA_PERMISSION_CODE
                    );
                }
                dialog.dismiss();
                Log.i(TAG, "image_uri : " + image_uri);
            }
        });

        btnDefaultImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 이미지 삭제 로직 추가
                deleteImage();
                dialog.dismiss();
                Log.i(TAG, "image_uri : " + image_uri);
            }
        });
        dialog.show();
    }
    // 이미 권한이 부여되어 있는지 여부를 확인하는 메서드
    private boolean isPermissionGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    //TedImagePicker를 사용해서 이미지 선택하거나 카메라로 사진 찍어서 가져오기
    private void selectImage() {
        TedImagePicker.with(this)
                .mediaType(gun0912.tedimagepicker.builder.type.MediaType.IMAGE)
                .start(uri -> {
                    Log.i("TedImagePicker", "선택된 이미지 : " + uri);
                    uploadImage(uri);
                    Glide.with(getApplicationContext()).load(uri).circleCrop().into(iv_profile);
//                    iv_profile.setImageURI(uri);
                });
    }

    // 권한 요청에 대해 사용자가 허용했을 경우, selectImage() 메서드 실행
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            Log.i("권한", "카메라 권한 허용됨");
            selectImage();
        }
    }
}