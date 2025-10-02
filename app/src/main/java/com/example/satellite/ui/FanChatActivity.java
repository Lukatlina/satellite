package com.example.satellite.ui;

import android.app.NotificationManager;
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
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.example.satellite.adapter.ChatAdapter;
import com.example.satellite.model.ChatResponse;
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

public class FanChatActivity extends AppCompatActivity  {
    private static final String TAG = "FanChatActivity";
    private ChatService chatService;
    private boolean isBound = false;
    // 중복 요청을 방지하는 플래그 추가
    private boolean isLoading = false;

    ImageView iv_message_search;

    SharedPreferences user;
    SharedPreferences.Editor user_editor;

    String uniq_id;
    int user_id;
    int artist_id;
    int message_id;

    RecyclerView recyclerView;
    LinearLayoutManager linear;
    ChatAdapter adapter;

    ImageView iv_fan_chat_back_btn, iv_fan_chat_search_icon;
    EditText et_chat_message;
    ImageButton btn_fan_chat_send_message, btn_search_prev, btn_search_next;
    TextView tv_fan_chat_room_name;
    LinearLayout fanChatLinear, fanChatSearchLayout, fan_search_navigation_layout, fan_send_message;
    SearchView sv_fan_chat_message;

    ArrayList<chat_user> messages = new ArrayList<>();

    chat_user currentUser;

    String fan_image;
    String fan_nickname;

    int chat_id;
    String chatroom_name;
    int sender_id;
    int is_artist;
    String image;
    String nickname;
    String message;
    String sent_time;
    String formattedDate;
    String lastDate = "";
    String keyword;
    int first_message_id;
    String beforeDate;
    int searchResultId;
    private ArrayList<Integer> searchResultIndices = new ArrayList<>();
    private int currentSearchIndex = -1;  // 현재 검색 결과 위치를 추적 (-1은 아직 검색되지 않은 상태)

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
        setContentView(R.layout.activity_fan_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fan_chat), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        tv_fan_chat_room_name = findViewById(R.id.tv_fan_chat_room_name);
        recyclerView = findViewById(R.id.chatroom_recy);

        iv_fan_chat_back_btn = findViewById(R.id.iv_fan_chat_back_btn);
        et_chat_message = findViewById(R.id.et_chat_message);
        btn_fan_chat_send_message = findViewById(R.id.btn_fan_chat_send_message);

        fanChatLinear = findViewById(R.id.fan_chat_linear);
        fanChatSearchLayout = findViewById(R.id.fan_chat_search_layout);
        fan_search_navigation_layout = findViewById(R.id.fan_search_navigation_layout);
        fan_send_message = findViewById(R.id.fan_send_message);

        btn_search_prev = findViewById(R.id.btn_search_prev);
        btn_search_next = findViewById(R.id.btn_search_next);

        linear = new LinearLayoutManager(getApplicationContext());
        linear.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linear);
        adapter = new ChatAdapter(messages, getApplicationContext());
        recyclerView.setAdapter(adapter);
        sv_fan_chat_message = findViewById(R.id.sv_fan_chat_message);

        user = this.getSharedPreferences("user", Context.MODE_PRIVATE);
        user_editor = user.edit();

        // 먼저 인텐트에서 user_id를 꺼내오기
        Intent intent = getIntent();
        artist_id = intent.getIntExtra("artist_id", -1);
        uniq_id = user.getString("uniq_id", "");
        user_id = user.getInt("user_id", -1);
        is_artist = user.getInt("is_artist",0);
        chat_id = intent.getIntExtra("chat_id", -1);

        Log.i(TAG, "uniq_id : " + uniq_id);
        Log.i(TAG, "user_id : " + user_id);
        Log.i(TAG, "is_artist : " + is_artist);
        Log.i(TAG, "artist_id : " + artist_id);
        Log.i(TAG, "onCreate: http 요청 전 메시지 리스트 크기" + messages.size());

        // 서비스 시작 및 바인딩
        Intent serviceIntent = new Intent(FanChatActivity.this, ChatService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // BroadcastReceiver 등록
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
                new IntentFilter("com.example.satellite.ACTION_RECEIVE_MESSAGE"));

        // 유저 정보를 가져오는 Retrofit HTTP 요청 설정
        loadUserData(uniq_id, is_artist);

        // 첫 번째 레이아웃에서 검색 아이콘을 클릭하면 두 번째 레이아웃으로 전환
        ImageView searchIcon = findViewById(R.id.iv_fan_chat_search_icon);
        searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 채팅방 이름과 뒤로 가기 버튼이 가려짐
                fanChatLinear.setVisibility(View.INVISIBLE);
                // 메시지 작성화면과 send 버튼 가려짐
                fan_send_message.setVisibility(View.INVISIBLE);
                // 검색 버튼이 보이게 됨
                fanChatSearchLayout.setVisibility(View.VISIBLE);
                fan_search_navigation_layout.setVisibility(View.VISIBLE);


            }
        });

        // 두 번째 레이아웃에서 뒤로가기 버튼을 클릭하면 첫 번째 레이아웃으로 전환
        ImageView backBtn = findViewById(R.id.iv_fan_chat_search_back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 2. 뒤로가기 했을 때 색 칠한거랑 검색어 없어져야함
                sv_fan_chat_message.setQuery("", false);
                sv_fan_chat_message.clearFocus();
                isLoading = false;

                // 검색 버튼과 검색 탐색용 버튼이 가려짐
                fanChatSearchLayout.setVisibility(View.GONE);
                fan_search_navigation_layout.setVisibility(View.GONE);
                // 다시 원래의 메시지 작성화면과 채팅방 이름, 뒤로 가기 버튼 보이게 됨.
                fanChatLinear.setVisibility(View.VISIBLE);
                fan_send_message.setVisibility(View.VISIBLE);
            }
        });

        btn_fan_chat_send_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 텍스트 메시지로 부터 작성된 메시지 가져오기
                String sending_message = et_chat_message.getText().toString().trim();

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

                    // 1. UI에 메시지를 즉시 추가 (보낸 사람의 메시지로 추가)
                    chat_user currentMessage = new chat_user(0, chat_id, chatroom_name, user_id, 0, fan_image, fan_nickname, sending_message, currentTime);
                    // 작성 유저의 화면에 바로 보일 수 있도록 리스트에 추가
                    messages.add(currentMessage);
                    // 포지션은 0부터 시작하기 때문에 전체 크기의 -1을 해준다.
                    adapter.notifyItemInserted(messages.size() - 1);
                    scrollToBottom();

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
                        jsonMessage.put("sent_time", currentTime);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // 3. 서버에 JSON 형태 메시지를 전송
                    sendMessageToService(jsonMessage.toString());

                    Log.i(TAG, "보내기 전 데이터 확인 : " + jsonMessage);

                    // 4. 전송 후 입력창 비우기
                    et_chat_message.setText("");
                }
            }
        });

        iv_fan_chat_back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ChatsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        sv_fan_chat_message.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                filterMessages(s);
                // 키보드 숨기기
                sv_fan_chat_message.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (s == null || s.isEmpty()) {
                    adapter.setQuery(""); // 검색어가 없을 때 빈 문자열 설정
                }
                adapter.notifyDataSetChanged(); // 검색어 변경에 따른 UI 업데이트
                return true;
            }
        });

        // 검색 버튼 클릭시
        int searchIconId = sv_fan_chat_message.getContext().getResources().getIdentifier("android:id/search_mag_icon", null, null);
        ImageView searchButton = sv_fan_chat_message.findViewById(searchIconId);
        if (searchButton != null) {
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "검색 버튼 눌림 시작");
                    // 검색어 제출 시의 동작
                    keyword = sv_fan_chat_message.getQuery().toString();
                    filterMessages(keyword);
                    // 키보드 숨기기
                    sv_fan_chat_message.clearFocus();}
            });
        }

        int closeButtonId = sv_fan_chat_message.getContext().getResources().getIdentifier("android:id/search_close_btn", null, null);
        ImageView closeButton = sv_fan_chat_message.findViewById(closeButtonId);

        // X버튼 클릭시
        if (closeButton != null) {
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // X 버튼을 눌렀을 때 추가 작업을 수행
                    Log.i(TAG, "X버튼 클릭");
                    // 버튼 클릭시 검색어 지워주기
                    sv_fan_chat_message.setQuery("", false);
                    sv_fan_chat_message.clearFocus();
                    // isLoading false로 변경
                    isLoading = false;
                }
            });
        }

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


                // 스크롤이 전체의 80% 이상인지 확인
                if (currentScrollOffset <= 0.1 * totalScrollRange) {
                    isLoading = true;
                    Log.i(TAG, "loadMoreChatData 실행되나?");
                    // 스크롤이 80% 이상인 경우
                    // 추가 작업을 수행하거나 다음 데이터를 불러옵니다
                    loadMoreChatData();
                }
            }
        });

        btn_search_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (searchResultIndices.isEmpty()) {
//                    Toast.makeText(FanChatActivity.this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
//                    return;
//                }
                Log.i(TAG, "btn_search_next: currentSearchIndex" + currentSearchIndex);
                Log.i(TAG, "btn_search_next: searchResultIndices.size" + (searchResultIndices.size() - 1) );
                if (currentSearchIndex > 0) {
                    currentSearchIndex--;
                    scrollToSearchResult(currentSearchIndex);
                } else {
                    Toast.makeText(FanChatActivity.this, "더 이상 최신 메시지 검색 결과가 없습니다", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_search_prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (searchResultIndices.isEmpty()) {
//                    Toast.makeText(FanChatActivity.this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
//                    return;
//                }

                Log.i(TAG, "btn_search_prev: currentSearchIndex" + currentSearchIndex);
                Log.i(TAG, "btn_search_prev: searchResultIndices.size" + (searchResultIndices.size() - 1) );

                if (currentSearchIndex < searchResultIndices.size() - 1) {
                    currentSearchIndex++;
                    scrollToSearchResult(currentSearchIndex);
                } else {
                    searchResult(keyword);
                }
            }
        });
    }

    // 스크롤이 80% 진행되었을 때 추가로 불러올 유저 데이터
    private void loadMoreChatData() {

        Log.i(TAG, "messages length : " + messages.size());
        Log.i(TAG, "chat_id: " + chat_id);
        Log.i(TAG, "user_id: " + user_id);
        Log.i(TAG, "first_message_id: " + first_message_id);

        // 기존 DB에서 데이터를 불러오는 로직
        // 유저의 정보와 아티스트 여부, 아티스트 id를 가지고 기존의 DB에서 데이터를 불러온다.
        ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
        Call<ArrayList<chat_user>> call = service.sendMoreChatImformaition(chat_id, user_id, first_message_id);

        call.enqueue(new Callback<ArrayList<chat_user>>() {
            @Override
            public void onResponse(Call<ArrayList<chat_user>> call, Response<ArrayList<chat_user>> response) {
//                isLoading = false; // 요청 완료 후 플래그 해제
                if (response.isSuccessful()) {
                    Log.i(TAG, "순서 loadMoreChatData" + response);

                    ArrayList<chat_user> data = response.body();
                    Log.i(TAG, "바디값" + data);
                    Log.i(TAG, "데이터 길이는?" + data.size());

                    if (data.isEmpty()) {
                        Log.i(TAG, "더 이상 불러올 데이터 없음");
                        if (messages.get(0).getMessage_id() != -1) {
                            messages.add(0, new chat_user(2, -1,  lastDate));
                            adapter.notifyItemInserted(0);
                        }
                        isLoading = false;
                        Log.i(TAG, "메세지 길이 : " + messages.size());
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
                            Log.i(TAG, "타이틀 if문 실행");
                            Log.i(TAG, "타이틀 lastDate 확인 " + lastDate);
                            Log.i(TAG, "타이틀 titleFormattedTime 확인 " + formattedDate);
                            Log.i(TAG, "타이틀 newMessages.size 확인 " + (data.size()-1));
                            newMessages.add(new chat_user(2, -1,  lastDate));
                        }
                        lastDate = formattedDate;

                        if (is_artist == 0) {
                            newMessages.add(new chat_user(0, message_id, chat_id, chatroom_name, sender_id, is_artist, image, nickname, message, sent_time));
                        } else {
                            newMessages.add(new chat_user(1, message_id, chat_id, chatroom_name, sender_id, is_artist, image, nickname, message, sent_time));
                        }
                    }
                    // 역순으로 정렬
                    Collections.reverse(newMessages);

                    messages.addAll(0, newMessages);

                    keyword = sv_fan_chat_message.getQuery().toString();

                    Log.i(TAG, "에러확인: keyword" + keyword);
                    // 메시지 리스트를 검색하고 검색 결과 인덱스를 저장
                    if (!keyword.isEmpty() && keyword != null) {
                        searchResultIndices.clear();
                        for (int i = messages.size() - 1; i >= 0; i--) {
                            chat_user message = messages.get(i);
                            if (message != null && message.getMessage() != null && message.getMessage().contains(keyword)) {
                                // message_id를 통해서 비교하기 위해서 넣음
                                searchResultIndices.add(i);  // 검색 결과의 인덱스를 저장
                            }
                        }
                        Log.i(TAG, "searchResultIndices: " + searchResultIndices.size());
                        for (int i = 0; i < searchResultIndices.size(); i++) {
                            Log.i(TAG, "searchResultIndices[" + i + "] = " + searchResultIndices.get(i));
                        }
                    }

                    for (int i = 0; i <= messages.size(); i++) {
                        if (messages.get(i).getMessage_id() != -1) { // message_id가 있는지 확인
                            first_message_id = messages.get(i).getMessage_id(); // 첫 번째 message_id가 있는 값을 저장
                            break; // 찾으면 반복문 종료
                        }
                    }

                    // 어댑터 갱신 및 스크롤 위치 조정
                    adapter.notifyItemRangeInserted(0, newMessages.size());

                    // 이전 위치를 기억하여 새로운 데이터가 추가되어도 스크롤이 튀지 않도록 함
                    // 현재 화면의 보이는 첫 번째 아이템의 포지션(인덱스) 찾음
                    int previousPosition = linear.findFirstVisibleItemPosition();
                    // 현재 화면에 보이는 첫 번째 뷰(view)를 가져옴
                    View firstVisibleItemView = recyclerView.getChildAt(0);
                    // 뷰의 현재 화면 내에서의 위치 오프셋(위쪽 경계선과 리사이클러뷰 경계선 사이 거리)
                    int offset = (firstVisibleItemView == null) ? 0 : firstVisibleItemView.getTop();
                    // 추가 전 첫 번째 아이템이 어디에 있었는지 반영해서 스크롤 위치 조정
                    linear.scrollToPositionWithOffset(previousPosition + newMessages.size(), offset);
                } else {
                    Toast.makeText(FanChatActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                }
                // 로딩 완료 후 플래그 해제
                isLoading = false;
            }

            @Override
            public void onFailure(Call<ArrayList<chat_user>> call, Throwable t) {
                Toast.makeText(FanChatActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.i(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void fetchChatData() {
        // 기존 DB에서 데이터를 불러오는 로직
        // 유저의 정보와 아티스트 여부, 아티스트 id를 가지고 기존의 DB에서 데이터를 불러온다.
        ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
        Call<ArrayList<chat_user>> call = service.sendChatImformaition(user_id, is_artist, artist_id);

        call.enqueue(new Callback<ArrayList<chat_user>>() {
            @Override
            public void onResponse(Call<ArrayList<chat_user>> call, Response<ArrayList<chat_user>> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "순서 fetchChatData" + response);

                    currentUser = null;

                    ArrayList<chat_user> data = response.body();
                    Log.i(TAG, "바디값" + data);

                    for (int i = 0; i < data.size(); i++) {
                        chat_user user = data.get(i);
                        message_id = user.getMessage_id();
                        chat_id = user.getChat_id();
                        chatroom_name = user.getChatroom_name();
                        tv_fan_chat_room_name.setText(chatroom_name);
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
                            System.out.println("formattedDate: " + formattedDate);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        Log.i(TAG, "first_data.size()" + data.size());

                        if (!lastDate.isEmpty() && !lastDate.equals(formattedDate)) {
                            messages.add(new chat_user(2, -1,  lastDate));
                            lastDate = formattedDate;
                        } else{
                            lastDate = formattedDate;
                            beforeDate = formattedDate;
                        }


                        if (is_artist == 0) {
                            if (currentUser == null) {
                                currentUser = new chat_user(0, chat_id, chatroom_name, sender_id, is_artist, image, nickname, message, sent_time);
                            }
                            messages.add(new chat_user(0, message_id, chat_id, chatroom_name, sender_id, is_artist, image, nickname, message, sent_time));
                        } else {
                            messages.add(new chat_user(1, message_id, chat_id, chatroom_name, sender_id, is_artist, image, nickname, message, sent_time));
                        }

                        if (data.size() < 30 && i == data.size() - 1) {
                            messages.add(new chat_user(2, -1,  lastDate));
                        }
                    }
                    Log.i(TAG, "메세지 길이 : " + messages.size());

                    Collections.reverse(messages);

                    for (int i = 0; i <= messages.size(); i++) {
                        if (messages.get(i).getMessage_id() != -1) { // message_id가 있는지 확인
                            first_message_id = messages.get(i).getMessage_id(); // 첫 번째 message_id가 있는 값을 저장
                            break; // 찾으면 반복문 종료
                        }
                    }

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
                    Toast.makeText(FanChatActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ArrayList<chat_user>> call, Throwable t) {
                Toast.makeText(FanChatActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
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
                                        fan_image = "";
                                    }else{
                                        fan_image = user_data.getString("image");
                                    }
                                    fan_nickname = user_data.getString("nickname");
                                    Log.i(TAG, "유저 닉네임 : " + nickname);

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


    // BroadcastReceiver 구현
    // 서버로부터 메시지 수신하면 사용
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.i(TAG, "Received message: " + message);

            // String -> Json 객체로 변환 후에 각 값을 변수에 저장
            // 2. chat_user 객체를 JSON 문자열로 변환

            try {
                // 받은 메시지가 현재 보고 있는 채팅방과 같다면 보여줄 것
                JSONObject received_Message = new JSONObject(message);
                int received_chat_id = received_Message.getInt("chat_id");
                int received_sender_id = received_Message.getInt("sender_id");
                int received_is_artist = received_Message.getInt("is_artist");
                String received_image = received_Message.getString("image");
                String received_nickname = received_Message.getString("nickname");
                String received_message = received_Message.getString("message");
                String received_sent_time = received_Message.getString("sent_time");

                if (received_image == null || received_image.isEmpty()){
                    received_image = "";
                }

                // 원래 형식의 시간 파싱을 위한 SimpleDateFormat
                SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                // 원하는 형식으로 변환하기 위한 SimpleDateFormat
                SimpleDateFormat newFormat = new SimpleDateFormat("yyyy년 MM월 dd일 EEEE");

                try {
                    // 문자열을 Date 객체로 파싱
                    Date date = originalFormat.parse(received_sent_time);

                    // Date 객체를 새로운 형식으로 포맷
                    formattedDate = newFormat.format(date);

                    Date last_list_time = originalFormat.parse(messages.get(messages.size()-1).getSent_time());
                    beforeDate = newFormat.format(last_list_time);

                    // 포맷된 시간 출력
                    System.out.println("formattedDate: " + formattedDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                Log.i(TAG, "beforeDate: " + beforeDate);
                Log.i(TAG, "formattedDate" + formattedDate);

                if (received_chat_id == chat_id) {
                    if (!beforeDate.equals(formattedDate)) {
                        messages.add(new chat_user(2, -1,  formattedDate));
                        Log.i(TAG, "beforeDate: " + beforeDate);
                        Log.i(TAG, "formattedDate" + formattedDate);
                        beforeDate = formattedDate;
                    }

                    // UI 업데이트 처리
                    chat_user received_user = new chat_user
                            (1, received_chat_id, received_sender_id, received_is_artist, received_image, received_nickname, received_message, received_sent_time);
                    messages.add(received_user);

                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }

                    // 새로운 메시지를 받은 후 스크롤을 마지막으로 이동
                    scrollToBottom();
                }

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

    // 메시지 검색시 비교 메서드
    private void filterMessages(String query) {
        Log.i(TAG, "filterMessages: 시작 : " + query);
        searchResultIndices.clear();  // 검색어가 변경될 때마다 기존 결과 초기화
        currentSearchIndex = -1;  // 현재 인덱스 초기화

        if (isLoading) {
            Log.i(TAG, "설마 여기 들어감?");
            return; // 로딩 중일 때는 검색을 실행하지 않음
        }

        isLoading = true; // 검색 시작 시 로딩 중으로 설정

        searchResultId = -1;

        if (query == null || query.isEmpty()) {
            adapter.setQuery(""); // 검색어가 없을 때 빈 문자열 설정
            adapter.notifyDataSetChanged(); // UI 업데이트
            Log.i(TAG, "설마 여기 들어감? 222");
            return; // 검색어가 null이면 메서드를 빠르게 종료
        }

        // 어댑터에 검색어 설정
        adapter.setQuery(query);

        Log.i(TAG, "filterMessages currentSearchIndex: " + currentSearchIndex);

        // 메시지 리스트를 검색하고 검색 결과 인덱스를 저장
        for (int i = messages.size() - 1; i >= 0; i--) {
            chat_user message = messages.get(i);
            if (message != null && message.getMessage() != null && message.getMessage().contains(query)) {
                // message_id를 통해서 비교하기 위해서 넣음
                searchResultIndices.add(i);  // 검색 결과의 인덱스를 저장
            }
        }
        Log.i(TAG, "searchResultIndices: " + searchResultIndices.size());


        if (!searchResultIndices.isEmpty() && currentSearchIndex == -1) {
            currentSearchIndex = 0;  // 첫 번째 검색 결과로 이동
            scrollToSearchResult(currentSearchIndex);
            Log.i(TAG, "filterMessages: ");
        } else {
//            Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
            // 불러온 값에 결과값이 없다면 서버에서 불러오기
            // 이 코드를 for문 안에 넣었기 때문에 계속 실행이 되서 문제가 됨
            searchResult(query);

        }

        Log.i(TAG, "filterMessages: 끝 : " + query);
    }

    private void scrollToSearchResult(int index) {
        if (index >= 0 && index < searchResultIndices.size()) {
            Log.i(TAG, "scrollToSearchResult: 시작 여기 들어감?");
            int position = searchResultIndices.get(index);
            recyclerView.scrollToPosition(position);

            // 강조 표시 (예: 배경색 변경)
            adapter.notifyDataSetChanged(); // 어댑터에 변경 사항 알림

        }
    }

    private void searchResult(String keyword) {
        Log.i(TAG, "searchResult: 시작 : " + keyword);
        ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
        Call<ChatResponse> call = service.sendSearchChatImformaition(chat_id, user_id, first_message_id, keyword);

        Log.i(TAG, "searchResult: chat_id : " + chat_id);
        Log.i(TAG, "searchResult: user_id : " + user_id);
        Log.i(TAG, "searchResult: first_message_id : " + first_message_id);
        Log.i(TAG, "searchResult: keyword : " + keyword);

        call.enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                isLoading = false; // 응답 수신 후 로딩 상태 해제
                if (response.isSuccessful()) {

                    Log.i(TAG, "순서 searchResult" + response);

                    ChatResponse data = response.body();
                    Log.i(TAG, "바디값" + data);

                    int result = data.getResult();

                    Log.i(TAG, "검색 후 result : " + result);

                    if (result == 1) {

                        // 데이터가 오래된 순서이므로 맨 앞에 추가
                        ArrayList<chat_user> search_messages = new ArrayList<>();

                        for (int i = 0; i < data.getMessages().size(); i++) {
                            chat_user user = data.getMessages().get(i);
                            message_id = user.getMessage_id();
                            chat_id = user.getChat_id();
                            chatroom_name = user.getChatroom_name();
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

                            if (!lastDate.equals(formattedDate) ) {
                                Log.i(TAG, "타이틀 if문 실행");
                                Log.i(TAG, "타이틀 lastDate 확인 " + lastDate);
                                Log.i(TAG, "타이틀 titleFormattedTime 확인 " + formattedDate);
                                search_messages.add(new chat_user(2, -1,  lastDate));

                            }
                            lastDate = formattedDate;

                            if (is_artist == 0) {
                                search_messages.add(new chat_user(0, message_id, chat_id, chatroom_name, sender_id, is_artist, image, nickname, message, sent_time));
                            } else {
                                search_messages.add(new chat_user(1, message_id, chat_id, chatroom_name, sender_id, is_artist, image, nickname, message, sent_time));
                            }
                        }
                        // 역순으로 정렬
                        Collections.reverse(search_messages);


                        messages.addAll(0, search_messages);
                        for (int i = 0; i <= messages.size(); i++) {
                            if (messages.get(i).getMessage_id() != -1) { // message_id가 있는지 확인
                                first_message_id = messages.get(i).getMessage_id(); // 첫 번째 message_id가 있는 값을 저장
                                break; // 찾으면 반복문 종료
                            }
                        }


                        // 어댑터 갱신 및 스크롤 위치 조정
                        adapter.notifyItemRangeInserted(0, search_messages.size());

                        searchResultIndices.clear();
                        // 메시지 리스트를 검색하고 검색 결과 인덱스를 저장
                        for (int i = messages.size() - 1; i >= 0; i--) {
                            chat_user message = messages.get(i);
                            if (message != null && message.getMessage() != null && message.getMessage().contains(keyword)) {
                                // message_id를 통해서 비교하기 위해서 넣음
                                searchResultIndices.add(i);
                            }
                        }
                        isLoading = false; // 검색 완료 후 플래그 해제
                        Log.i(TAG, "searchResultIndices: " + searchResultIndices.size());


                        if (!searchResultIndices.isEmpty() && currentSearchIndex == -1) {
                            currentSearchIndex = 0;  // 첫 번째 검색 결과로 이동
                        }
                        scrollToSearchResult(currentSearchIndex);

                    }else{
                        if (searchResultIndices.isEmpty()) {
                            Toast.makeText(FanChatActivity.this, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(FanChatActivity.this, "더 이상 이전 메시지 검색 결과가 없습니다", Toast.LENGTH_SHORT).show();
                        }
                        // 버튼 비활성화

                    }
                } else {
                    Toast.makeText(FanChatActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                Toast.makeText(FanChatActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.i(TAG, "onFailure: " + t.getMessage());
            }
        });
        Log.i(TAG, "searchResult: 끝 : ");
    }


    @Override
    protected void onResume() {
        super.onResume();
        // 현재 액티비티가 채팅 화면임을 추적
        Log.i(TAG, "onResume: artist_id : " + artist_id);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentActivity(this);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentChatId(chat_id);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentArtistId(artist_id);
        // 방을 다시 보기 시작할 때 서버로 알림
        if (isBound) {
            chatService.sendMessageToServer("/resume");
        }
        // 성공적으로 읽음 상태가 업데이트되면 알림 제거
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(chat_id);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 채팅 화면을 떠날 때 null로 설정
        Log.i(TAG, "onPause: ");
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentActivity(null);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentChatId(-1);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentArtistId(-1);
        // 방을 잠시 보지 않게 될 때 서버로 알림
        if (isBound) {
            chatService.sendMessageToServer("/pause");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 서비스 바인딩 해제
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        // BroadcastReceiver 해제
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }
}