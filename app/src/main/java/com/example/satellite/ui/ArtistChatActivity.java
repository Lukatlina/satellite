package com.example.satellite.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.satellite.ApiService;
import com.example.satellite.ChatService;
import com.example.satellite.MyApplication;
import com.example.satellite.R;
import com.example.satellite.RetrofitClientInstance;
import com.example.satellite.adapter.ArtistAdapter;
import com.example.satellite.model.chat_user;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArtistChatActivity extends AppCompatActivity {

    private static final String TAG = "ArtistChatActivity";
    private ChatService chatService;
    private boolean isBound = false;
    private boolean isLoading = false;

    ImageView iv_artist_chat_back_btn;
    ImageView iv_message_search;

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

    String uniq_id;
    int user_id;
    int artist_id;

    RecyclerView recyclerView;
    LinearLayoutManager linear;
    ArtistAdapter adapter;

    EditText et_artist_chat_message;
    ImageButton btn_artist_chat_send_message;
    TextView tv_artist_chat_room_name;

    ArrayList<chat_user> messages = new ArrayList<>();

    String artist_image;
    String artist_nickname;

    chat_user currentUser;
    int chat_id;
    int message_id;
    String chatroom_name;
    int sender_id;
    int is_artist;
    String image;
    String nickname;
    String message;
    String sent_time;
    int fan_message_count;
    String formattedTime;
    String formattedDate;
    String lastDate = "";
    int first_message_id;
    String beforeDate;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 바인딩된 서비스의 IBinder를 통해 서비스 인스턴스를 가져옵니다.
            ChatService.LocalBinder binder = (ChatService.LocalBinder) service;
            chatService = binder.getService();
            isBound = true;
            Log.i(TAG, "서비스 연결됨");

            // 필요 시 서비스와 상호작용하는 코드를 작성합니다.
            // 메시지 데이터를 가져오는 Retrofit HTTP 요청 설정
            fetchChatData();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            chatService = null;
            isBound = false;
            Log.i(TAG, "서비스 연결이 해제됨");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_artist_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.artist_chat), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        tv_artist_chat_room_name = findViewById(R.id.tv_artist_chat_room_name);
        recyclerView = findViewById(R.id.artist_chatroom_recy);

        iv_artist_chat_back_btn = findViewById(R.id.iv_artist_chat_back_btn);
        et_artist_chat_message = findViewById(R.id.et_artist_chat_message);
        btn_artist_chat_send_message = findViewById(R.id.btn_artist_chat_send_message);

        linear = new LinearLayoutManager(getApplicationContext());
        linear.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linear);
        adapter = new ArtistAdapter(messages, getApplicationContext());
        recyclerView.setAdapter(adapter);

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        // 먼저 인텐트에서 user_id를 꺼내오기
        Intent intent = getIntent();
        artist_id = intent.getIntExtra("artist_id", -1);
        chat_id = intent.getIntExtra("chat_id", -1);
        user_id = user.getInt("user_id", -1);
        is_artist = user.getInt("is_artist", 1);
        uniq_id = user.getString("uniq_id", "");
        artist_id = user_id;

        Log.i(TAG, "uniq_id : " + uniq_id);
        Log.i(TAG, "user_id : " + user_id);
        Log.i(TAG, "is_artist : " + is_artist);
        Log.i(TAG, "artist_id : " + artist_id);

        // 서비스 시작 및 바인딩
        Intent serviceIntent = new Intent(ArtistChatActivity.this, ChatService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // BroadcastReceiver 등록
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
                new IntentFilter("com.example.satellite.ACTION_RECEIVE_MESSAGE"));

        // 유저 정보를 가져오는 Retrofit HTTP 요청 설정
        loadUserData(uniq_id, is_artist);


        btn_artist_chat_send_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sending_message = et_artist_chat_message.getText().toString().trim();

                if (!sending_message.isEmpty()) {
                    String currentTime = getCurrentTime();

                    // 원래 형식의 시간 파싱을 위한 SimpleDateFormat
                    SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    // 원하는 형식으로 변환하기 위한 SimpleDateFormat
                    SimpleDateFormat newFormat = new SimpleDateFormat("yyyy년 MM월 dd일 EEEE");

                    try {
                        // 문자열을 Date 객체로 파싱
                        Date date = originalFormat.parse(currentTime);

                        // Date 객체를 새로운 형식으로 포맷
                        formattedDate = newFormat.format(date);

                        // 포맷된 시간 출력
                        System.out.println("formattedDate: " + formattedDate);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, "beforeDate: " + beforeDate);
                    Log.i(TAG, "formattedDate: " + formattedDate);

                    // 1. UI에 메시지를 즉시 추가 (보낸 사람의 메시지로 추가)
                    chat_user currentMessage = new chat_user(0, chat_id, chatroom_name, user_id, 1, artist_image, artist_nickname, sending_message, currentTime);
                    // 작성 유저의 화면에 바로 보일 수 있도록 리스트에 추가
                    messages.add(currentMessage);
                    // 포지션은 0부터 시작하기 때문에 전체 크기의 -1을 해준다.
                    adapter.notifyItemInserted(messages.size() - 1);
                    scrollToBottom(); // 스크롤 마지막으로 이동

                    // 2. chat_user 객체를 JSON 문자열로 변환
                    JSONObject jsonMessage = new JSONObject();
                    try {
                        jsonMessage.put("chat_id", currentMessage.getChat_id());
                        jsonMessage.put("chatroom_name", currentMessage.getChatroom_name());
                        jsonMessage.put("sender_id", currentMessage.getSender_id());
                        jsonMessage.put("is_artist", currentMessage.getIs_artist());
                        jsonMessage.put("image", currentMessage.getImage());
                        jsonMessage.put("nickname", currentMessage.getNickname());
                        jsonMessage.put("message", currentMessage.getMessage());
                        jsonMessage.put("sent_time", getCurrentTime());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // 3. 서버에 JSON 형태 메시지를 전송
                    sendMessageToService(jsonMessage.toString());

                    et_artist_chat_message.setText(""); // 메시지 입력창 초기화
                }
            }
        });

        iv_artist_chat_back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ChatsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        // 스크롤시 실행될 이벤트
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (isLoading) {
                    // 데이터 로딩 중일 때는 페이징 요청을 하지 않음
                    return;
                }

                // 전체 스크롤 범위
                int totalScrollRange = recyclerView.computeVerticalScrollRange();
                // 현재 스크롤된 위치
                int currentScrollOffset = recyclerView.computeVerticalScrollOffset();

                Log.i(TAG, "onScrolled: totalScrollRange" + totalScrollRange);
                Log.i(TAG, "onScrolled: currentScrollOffset" + currentScrollOffset);


                // 스크롤이 전체의 80% 이상인지 확인
                if (currentScrollOffset <= 0.2 * totalScrollRange) {
                    isLoading = true;
//                    countScrolls += 1;
                    Log.i(TAG, "loadMoreChatData 실행되나?");
                    // 스크롤이 80% 이상인 경우
                    // 추가 작업을 수행하거나 다음 데이터를 불러옵니다
                    for (int i = 0; i <= messages.size(); i++) {
                        if (messages.get(i).getIs_artist() == 1) { // message_id가 있는지 확인
                            first_message_id = messages.get(i).getMessage_id(); // 첫 번째 message_id가 있는 값을 저장
                            break; // 찾으면 반복문 종료
                        }
                    }
                    loadMoreChatData(first_message_id);
                }
            }
        });
    }

    private void loadMoreChatData(int first_message_id) {
        Log.i(TAG, "messages length : " + messages.size());
        Log.i(TAG, "chat_id: " + chat_id);
        Log.i(TAG, "user_id: " + user_id);
        Log.i(TAG, "first_message_id: " + first_message_id);



        // 기존 DB에서 데이터를 불러오는 로직
        // 유저의 정보와 아티스트 여부, 아티스트 id를 가지고 기존의 DB에서 데이터를 불러온다.
        ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
        Call<ArrayList<chat_user>> call = service.sendMoreArtistChatImformaition(chat_id, user_id, first_message_id);

        call.enqueue(new Callback<ArrayList<chat_user>>() {
            @Override
            public void onResponse(Call<ArrayList<chat_user>> call, Response<ArrayList<chat_user>> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "response값" + response);

                    ArrayList<chat_user> data = response.body();
                    Log.i(TAG, "바디값" + data);
                    Log.i(TAG, "데이터 길이는?" + data.size());

                    if (data.isEmpty()) {
                        Log.i(TAG, "더 이상 불러올 데이터 없음");
                        if (messages.get(0).getMessage_id() != -1) {
                            messages.add(0, new chat_user(2, -1,  lastDate));
                            adapter.notifyItemInserted(0);
                        }
                        return; // 더 이상 불러올 데이터가 없을 때 종료
                    }

                    // 데이터가 오래된 순서이므로 맨 앞에 추가
                    ArrayList<chat_user> newMessages = new ArrayList<>(); // 불러온 데이터를 새 리스트에 저장

                    for (int i = 0; i < data.size(); i++) {
                        chat_user user = data.get(i);
                        message_id = user.getMessage_id();
                        chat_id = user.getChat_id();
                        chatroom_name = user.getChatroom_name();
                        sender_id = user.getSender_id();
                        is_artist = user.getIs_artist();
                        fan_message_count = user.getFan_message_count();

                        image = user.getImage();

                        System.out.println("image == null 밖" + i);
                        if (image == null || image.isEmpty()){
                            System.out.println("image == null 안" + i);
                            image = "";
                        }
                        nickname = user.getNickname();
                        message = user.getMessage();
                        sent_time = user.getSent_time();


                        // 원래 형식의 시간 파싱을 위한 SimpleDateFormat
                        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        // 원하는 형식으로 변환하기 위한 SimpleDateFormat
                        SimpleDateFormat newFormat = new SimpleDateFormat("yyyy년 MM월 dd일 EEEE");



                        try {
                            // 문자열을 Date 객체로 파싱
                            Date date = originalFormat.parse(sent_time);

                            // Date 객체를 새로운 형식으로 포맷
                            formattedDate = newFormat.format(date);

                            // 포맷된 시간 출력
                            System.out.println("load More Formatted Time: " + formattedDate);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }


                        if (!lastDate.equals(formattedDate)) {
                            newMessages.add(new chat_user(2, -1,  lastDate));
                        }
                        lastDate = formattedDate;

                        Log.i(TAG, "값이 같은가? lastDate : " + lastDate);
                        Log.i(TAG, "값이 같은가? formattedDate : " + formattedDate);

                        if (fan_message_count != 0) {
                            newMessages.add(new chat_user(1, chat_id, message_id, 0));
                            Log.i(TAG, "메시지함 추가 : " + message_id);
                        }

                        newMessages.add(new chat_user(0, message_id, chat_id, chatroom_name, sender_id, is_artist, image, nickname, message, sent_time));

                    }
                    // 역순으로 정렬
                    Collections.reverse(newMessages);

                    messages.addAll(0, newMessages);
                    // 어댑터 갱신 및 스크롤 위치 조정
                    adapter.notifyItemRangeInserted(0, newMessages.size());

                    // 이전 위치를 기억하여 새로운 데이터가 추가되어도 스크롤이 튀지 않도록 함
                    int previousPosition = linear.findFirstVisibleItemPosition();
                    View firstVisibleItemView = recyclerView.getChildAt(0);
                    int offset = (firstVisibleItemView == null) ? 0 : firstVisibleItemView.getTop();
                    linear.scrollToPositionWithOffset(previousPosition + newMessages.size(), offset);

                    // 로딩 완료 후 플래그 해제
                    isLoading = false;
                } else {
                    Toast.makeText(ArtistChatActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                }

                for (int i = 0; i < messages.size(); i++) {
                    System.out.println("리스트 확인 Index: " + i + ", Value: " + messages.get(i).getMessage_id());
                }
            }

            @Override
            public void onFailure(Call<ArrayList<chat_user>> call, Throwable t) {
                Toast.makeText(ArtistChatActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.i(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // BroadcastReceiver 구현
    // 서버로부터 메시지 수신하면 사용
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.i(TAG, "Artist 받은 메시지 : " + message);

            // String -> Json 객체로 변환 후에 각 값을 변수에 저장
            // 2. chat_user 객체를 JSON 문자열로 변환

            try {
                JSONObject received_Message = new JSONObject(message);
                int received_chat_id = received_Message.getInt("chat_id");
                int received_message_id = received_Message.getInt("message_id");
//                String received_sent_time = received_Message.getString("sent_time");

//                // 원래 형식의 시간 파싱을 위한 SimpleDateFormat
//                SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//                // 원하는 형식으로 변환하기 위한 SimpleDateFormat
//                SimpleDateFormat newFormat = new SimpleDateFormat("yyyy년 MM월 dd일 EEEE");
//
//                try {
//                    // 문자열을 Date 객체로 파싱
//                    Date date = originalFormat.parse(received_sent_time);
//
//                    // Date 객체를 새로운 형식으로 포맷
//                    formattedDate = newFormat.format(date);
//
//                    Date last_list_time = originalFormat.parse(messages.get(messages.size()-1).getSent_time());
//                    beforeDate = newFormat.format(last_list_time);
//
//                    // 포맷된 시간 출력
//                    System.out.println("formattedDate: " + formattedDate);
//                } catch (ParseException e) {
//                    e.printStackTrace();
//                }
//
//                if (!beforeDate.equals(formattedDate)) {
//                    messages.add(new chat_user(2, -1,  formattedDate));
//                    Log.i(TAG, "beforeDate: " + beforeDate);
//                    beforeDate = formattedDate;
//                }

                chat_user last_message = messages.get(messages.size() - 1);
                if (last_message.getUsertype() != 1) {
                    messages.add(new chat_user(1, received_chat_id, received_message_id, 0));
                }

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

                // 새로운 메시지를 받은 후 스크롤을 마지막으로 이동
                scrollToBottom();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    };


    // 서비스로 메시지를 보내는 메서드
    private void sendMessageToService(String message) {
        if (isBound && chatService != null) {
            chatService.sendMessageToServer(message);
        } else {
            Log.i(TAG, "서비스에 연결되지 않았습니다.");
        }
    }

    // 메시지가 추가될 때 RecyclerView의 스크롤을 마지막으로 설정
    private void scrollToBottom() {
        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
    }

    private void fetchChatData() {
        Log.i(TAG, "fetchChatData: 시작");
        // 기존 DB에서 데이터를 불러오는 로직
        // 유저의 정보와 아티스트 여부, 아티스트 id를 가지고 기존의 DB에서 데이터를 불러온다.
        ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
        Call<ArrayList<chat_user>> call = service.sendArtistChatImformaition(user_id, is_artist, artist_id);

        call.enqueue(new Callback<ArrayList<chat_user>>() {
            @Override
            public void onResponse(Call<ArrayList<chat_user>> call, Response<ArrayList<chat_user>> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "fetchChatData: 성공");
                    Log.i(TAG, "response값" + response);

                    ArrayList<chat_user> data = response.body();

                    for (int i = 0; i < data.size(); i++) {
                        chat_user user = data.get(i);
                        chat_id = user.getChat_id();
                        message_id = user.getMessage_id();
                        chatroom_name = user.getChatroom_name();
                        tv_artist_chat_room_name.setText(chatroom_name);
                        sender_id = user.getSender_id();
                        is_artist = user.getIs_artist();

                        image = user.getImage();
                        System.out.println("image == null 밖" + i);
                        if (image == null || image.isEmpty()){
                            System.out.println("image == null 안" + i);
                            image = "";
                        }
                        nickname = user.getNickname();
                        message = user.getMessage();
                        sent_time = user.getSent_time();
                        fan_message_count = user.getFan_message_count();

                        // 원래 형식의 시간 파싱을 위한 SimpleDateFormat
                        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        // 원하는 형식으로 변환하기 위한 SimpleDateFormat
                        SimpleDateFormat newFormat = new SimpleDateFormat("yyyy년 MM월 dd일 EEEE");

                        try {
                            // 문자열을 Date 객체로 파싱
                            Date date = originalFormat.parse(sent_time);

                            // Date 객체를 새로운 형식으로 포맷
                            formattedDate = newFormat.format(date);

                            // 포맷된 시간 출력
                            System.out.println("Formatted Time: " + formattedDate);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        if (!lastDate.isEmpty() && !lastDate.equals(formattedDate)) {
                            Log.i(TAG, "타이틀 else-if문 실행");
                            messages.add(new chat_user(2, -1,  lastDate));
                            lastDate = formattedDate;


                        }else{
                            lastDate = formattedDate;
                            beforeDate = formattedDate;
                        }

                        if (fan_message_count != 0) {
                            messages.add(new chat_user(1, chat_id, message_id, 0));
                            Log.i(TAG, "메시지함 추가 : " + message_id);
                        }

                        messages.add(new chat_user(0, message_id, chat_id, chatroom_name, sender_id, is_artist, image, nickname, message, sent_time));
                        if (data.size() != 30 && i == data.size()-1) {
                            messages.add(new chat_user(2, -1,  lastDate));
                        }
                    }
                    Collections.reverse(messages);
                    adapter.notifyDataSetChanged();
                    scrollToBottom();

                    // 성공적으로 데이터를 가져왔을 때 서비스에 메시지 전송
                    if (chatService != null) {
                        chatService.sendChatRoomInfoToServer(chat_id);
                    } else {
                        Log.e(TAG, "ChatService is not bound.");
                        // 필요한 경우 서비스가 null일 때의 처리
                    }

                } else {
                    Log.i(TAG, "fetchChatData: 실패" + response.code());
                    Toast.makeText(ArtistChatActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ArrayList<chat_user>> call, Throwable t) {
                Toast.makeText(ArtistChatActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.i(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    private void loadUserData(String uniq_id, int is_artist) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("http://52.78.77.90/satellite/user_data.php").newBuilder();
        // get방식 파라미터 추가
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
                                String responseData = response.body().string();
                                Log.i(TAG, "무슨 데이터? : " + responseData);
                                JSONObject user_data = new JSONObject(responseData);

                                if (user_data.getInt("result") == 1) {
                                    if (user_data.getString("image").isEmpty() || user_data.isNull("image")) {
                                        artist_image = null;
                                    }else{
                                        artist_image = user_data.getString("image");
                                    }
                                    artist_nickname = user_data.getString("nickname");
                                    Log.i(TAG, "유저 닉네임 : " + artist_nickname);

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

    @Override
    protected void onResume() {
        super.onResume();
        // 현재 액티비티가 채팅 화면임을 추적
        Log.i(TAG, "onResume: artist_id : " + artist_id);
        Log.i(TAG, "onResume: chat_id : " + chat_id);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentActivity(this);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentChatId(chat_id);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentArtistId(artist_id);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 채팅 화면을 떠날 때 null로 설정
        Log.i(TAG, "onPause: ");
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentActivity(null);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentChatId(-1);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentArtistId(-1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 서비스 바인딩 해제
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
