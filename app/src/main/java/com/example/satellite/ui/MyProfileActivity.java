package com.example.satellite.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.satellite.R;

public class MyProfileActivity extends AppCompatActivity {
    String TAG = "MyProfileActivity";

    ImageView iv_close_btn;
    ImageView iv_myprofile_profile;
    ImageView iv_profile_settings;

    TextView tv_myprofile_nickname;
    TextView tv_myprofile_message;
    TextView tv_edit_profile;

    Button iv_profile_chat_btn;

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

    int is_artist;
    int chat_id;
    String uniq_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.my_profile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        uniq_id = user.getString("uniq_id", "");
        is_artist = user.getInt("is_artist", -1);

        iv_close_btn = findViewById(R.id.iv_close_btn);
        iv_myprofile_profile = findViewById(R.id.iv_myprofile_profile);
        iv_profile_settings = findViewById(R.id.iv_profile_settings);
        iv_profile_chat_btn = findViewById(R.id.iv_profile_chat_btn);

        tv_myprofile_nickname = findViewById(R.id.tv_myprofile_nickname);
        tv_myprofile_message = findViewById(R.id.tv_myprofile_message);
        tv_edit_profile = findViewById(R.id.tv_edit_profile);

        // 아티스트일 경우 채팅하기 버튼 보이도록 만듬
        if (is_artist == 1){
            iv_profile_chat_btn.setVisibility(View.VISIBLE);
            iv_profile_settings.setVisibility(View.INVISIBLE);
            tv_edit_profile.setVisibility(View.INVISIBLE);
        }

        Intent intent = getIntent();
        String id = intent.getStringExtra("id");
        chat_id = intent.getIntExtra("chat_id", -1);
        String nickname = intent.getStringExtra("nickname");
        String message = intent.getStringExtra("message");
        String image = intent.getStringExtra("image");

        if (image == null) {
            iv_myprofile_profile.setImageResource(R.drawable.baseline_person_150);
        }else{
            Glide.with(this).load(image).circleCrop().into(iv_myprofile_profile);
        }


        tv_myprofile_nickname.setText(nickname);
        tv_myprofile_message.setText(message);

        iv_profile_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MyProfileActivity.this , EditProfileActivity.class);
                startActivity(intent);
            }
        });

        iv_close_btn.setOnClickListener(new View.OnClickListener() {
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

        iv_profile_chat_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ArtistChatActivity.class);
                intent.putExtra("chat_id", chat_id);
                startActivity(intent);
            }
        });

    }
}