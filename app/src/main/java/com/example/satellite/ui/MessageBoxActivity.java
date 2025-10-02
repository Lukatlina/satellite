package com.example.satellite.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageBoxActivity extends AppCompatActivity {

    private static final String TAG = "MessageBoxActivity";
    private ChatService chatService;
    private boolean isBound = false;
    private boolean isLoading = false;

    ImageView iv_message_box_back_btn;
    ImageButton btn_messagebox_next, btn_messagebox_prev;
    SearchView sv_message_box_message;

    // 팬 메시지를 받기 위한 정보
    int chat_id;
    int message_id;

    RecyclerView recyclerView;
    LinearLayoutManager linear;
    ChatAdapter adapter;

    ArrayList<chat_user> fanMessages = new ArrayList<>();

    int sender_id;
    int is_artist;
    String image;
    String nickname;
    String message;
    String sent_time;
    String formattedTime;
    String formattedDate;
    String lastDate = "";
    int first_message_id;
    int maxScrolls;
    int countScrolls = 1;
    String beforeDate;
    int fan_message_id;
    String keyword;

    int searchResultId;
    private ArrayList<Integer> searchResultIndices = new ArrayList<>();
    private int currentSearchIndex = -1;  // 현재 검색 결과 위치를 추적 (-1은 아직 검색되지 않은 상태)

    LinearLayout message_box_linear, search_layout, message_box_search_navigation_layout;


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
            fetchFanChatData();
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
        setContentView(R.layout.activity_message_box);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.message_box), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // XML 요소들 초기화
        // XML 요소들 초기화
        message_box_linear = findViewById(R.id.message_box_linear);
        iv_message_box_back_btn = findViewById(R.id.iv_message_box_back_btn);
        btn_messagebox_prev = findViewById(R.id.btn_messagebox_prev);
        btn_messagebox_next = findViewById(R.id.btn_messagebox_next);
        message_box_search_navigation_layout = findViewById(R.id.message_box_search_navigation_layout);
        sv_message_box_message = findViewById(R.id.sv_message_box_message);

        recyclerView = findViewById(R.id.message_box_recy);

        linear = new LinearLayoutManager(getApplicationContext());
        linear.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linear);
        adapter = new ChatAdapter(fanMessages, getApplicationContext());
        recyclerView.setAdapter(adapter);

        // 먼저 인텐트에서 user_id를 꺼내오기
        Intent intent = getIntent();
        chat_id = intent.getIntExtra("chat_id", -1);
        message_id = intent.getIntExtra("message_id",-1);

        Log.i(TAG, "chat_id: " + chat_id);
        Log.i(TAG, "message_id: " + message_id);

        // 서비스 시작 및 바인딩
        Intent serviceIntent = new Intent(MessageBoxActivity.this, ChatService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // BroadcastReceiver 등록
        LocalBroadcastManager.getInstance(this).registerReceiver(fanMessageReceiver,
                new IntentFilter("com.example.satellite.ACTION_RECEIVE_MESSAGE"));

        iv_message_box_back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ArtistChatActivity.class);
                intent.putExtra("chat_id", chat_id);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        // 첫 번째 레이아웃에서 검색 아이콘을 클릭하면 두 번째 레이아웃으로 전환
        ImageView searchIcon = findViewById(R.id.iv_message_box_search_icon);
        searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 채팅방 이름과 뒤로 가기 버튼이 가려짐
                message_box_linear.setVisibility(View.INVISIBLE);
                // 검색 버튼이 보이게 됨
                search_layout.setVisibility(View.VISIBLE);
                message_box_search_navigation_layout.setVisibility(View.VISIBLE);


            }
        });

        // 두 번째 레이아웃에서 뒤로가기 버튼을 클릭하면 첫 번째 레이아웃으로 전환
        ImageView backBtn = findViewById(R.id.iv_message_box_search_back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 검색 버튼과 검색 탐색용 버튼이 가려짐
                search_layout.setVisibility(View.GONE);
                message_box_search_navigation_layout.setVisibility(View.GONE);
                // 다시 원래의 메시지 작성화면과 채팅방 이름, 뒤로 가기 버튼 보이게 됨.
                message_box_linear.setVisibility(View.VISIBLE);
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
                    countScrolls += 1;
                    Log.i(TAG, "loadMoreChatData 실행되나?");
                    // 스크롤이 80% 이상인 경우
                    // 추가 작업을 수행하거나 다음 데이터를 불러옵니다
                    for (int i = 0; i <= fanMessages.size(); i++) {
                        if (fanMessages.get(i).getMessage_id() != -1) { // message_id가 있는지 확인
                            first_message_id = fanMessages.get(i).getMessage_id(); // 첫 번째 message_id가 있는 값을 저장
                            break; // 찾으면 반복문 종료
                        }
                    }
                    loadMoreChatData(first_message_id);
                }
            }
        });

        sv_message_box_message.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                filterMessages(s);
                // 키보드 숨기기
                sv_message_box_message.clearFocus();
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

        btn_messagebox_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (searchResultIndices.isEmpty()) {
                    Toast.makeText(MessageBoxActivity.this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.i(TAG, "btn_search_next: currentSearchIndex" + currentSearchIndex);
                Log.i(TAG, "btn_search_next: searchResultIndices.size" + (searchResultIndices.size() - 1) );
                if (currentSearchIndex > 0) {
                    currentSearchIndex--;
                    scrollToSearchResult(currentSearchIndex);
                } else {
                    Toast.makeText(MessageBoxActivity.this, "이전 검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_messagebox_prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (searchResultIndices.isEmpty()) {
                    Toast.makeText(MessageBoxActivity.this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

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

    private void loadMoreChatData(int first_message_id) {
        Log.i(TAG, "messages length : " + fanMessages.size());
        Log.i(TAG, "chat_id: " + chat_id);
        Log.i(TAG, "first_message_id: " + first_message_id);



        // 기존 DB에서 데이터를 불러오는 로직
        // 유저의 정보와 아티스트 여부, 아티스트 id를 가지고 기존의 DB에서 데이터를 불러온다.
        ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
        Call<ArrayList<chat_user>> call = service.sendMoreFanChatImformaition(chat_id, message_id, first_message_id);

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
                        if (fanMessages.get(0).getMessage_id() != -1) {
                            fanMessages.add(0, new chat_user(2, -1,  lastDate));
                            adapter.notifyItemInserted(0);
                        }
                        return; // 더 이상 불러올 데이터가 없을 때 종료
                    }

                    // 데이터가 오래된 순서이므로 맨 앞에 추가
                    ArrayList<chat_user> newMessages = new ArrayList<>(); // 불러온 데이터를 새 리스트에 저장

                    for (int i = 0; i < data.size(); i++) {
                        chat_user user = data.get(i);
                        fan_message_id = user.getMessage_id();
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
                            newMessages.add(new chat_user(2, -1,  lastDate));

                        }
                        lastDate = formattedDate;

                        Log.i(TAG, "값이 같은가? lastDate : " + lastDate);
                        Log.i(TAG, "값이 같은가? formattedDate : " + formattedDate);

                        newMessages.add(new chat_user(1, fan_message_id, sender_id, nickname, message, sent_time));
                    }
                    // 역순으로 정렬
                    Collections.reverse(newMessages);

                    fanMessages.addAll(0, newMessages);


                    keyword = sv_message_box_message.getQuery().toString();

                    Log.i(TAG, "에러확인: keyword" + keyword);
                    // 메시지 리스트를 검색하고 검색 결과 인덱스를 저장
                    if (!keyword.isEmpty() && keyword != null) {
                        searchResultIndices.clear();
                        for (int i = fanMessages.size() - 1; i >= 0; i--) {
                            chat_user message = fanMessages.get(i);
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
                    Toast.makeText(MessageBoxActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                }

                for (int i = 0; i < fanMessages.size(); i++) {
                    System.out.println("리스트 확인 Index: " + i + ", Value: " + fanMessages.get(i).getMessage_id());
                }
            }

            @Override
            public void onFailure(Call<ArrayList<chat_user>> call, Throwable t) {
                Toast.makeText(MessageBoxActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.i(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    private void fetchFanChatData() {
        // 기존 DB에서 데이터를 불러오는 로직
        // 유저의 정보와 아티스트 여부, 아티스트 id를 가지고 기존의 DB에서 데이터를 불러온다.
        ApiService service = RetrofitClientInstance.getRetrofitInstance().create(ApiService.class);
        Call<ArrayList<chat_user>> call = service.sendFanChatImformaition(chat_id, message_id);

        call.enqueue(new Callback<ArrayList<chat_user>>() {
            @Override
            public void onResponse(Call<ArrayList<chat_user>> call, Response<ArrayList<chat_user>> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "response값" + response);

                    ArrayList<chat_user> data = response.body();
                    Log.i(TAG, "바디값" + data);

//                    ArrayList<chat_user> first_data = data.getMessages();

//                    maxScrolls = data.getMaxScrolls();


                    for (int i = 0; i < data.size(); i++) {
                        chat_user user = data.get(i);
                        fan_message_id = user.getMessage_id();
                        sender_id = user.getSender_id();
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

                        if (!lastDate.isEmpty() && !lastDate.equals(formattedDate)) {
                            Log.i(TAG, "타이틀 if문 실행");
                            Log.i(TAG, "타이틀 lastDate 확인 " + lastDate);
                            Log.i(TAG, "타이틀 first_data.size 확인 " + (data.size()-1));
                            Log.i(TAG, "타이틀 maxScrolls 확인 " + maxScrolls);

                            fanMessages.add(new chat_user(2, -1,  lastDate));

                            lastDate = formattedDate;
                        } else{
                            lastDate = formattedDate;
                            beforeDate = formattedDate;
                            Log.i(TAG, "타이틀 else문 실행");
                        }


                        fanMessages.add(new chat_user(1, fan_message_id, sender_id, nickname, message, sent_time));
                        Log.i(TAG, "팬 sender_id 확인 : " + sender_id);
                        Log.i(TAG, "팬 nickname 확인 : " + nickname);
                        Log.i(TAG, "팬 message 확인 : " + message);
                        Log.i(TAG, "팬 sent_time 확인 : " + formattedTime);
                        Log.i(TAG, "팬 messages size: " + fanMessages.size());

                    }
                    Collections.reverse(fanMessages);
                    adapter.notifyDataSetChanged();
                    scrollToBottom();

                    // 성공적으로 데이터를 가져왔을 때 서비스에 메시지 전송
                    if (chatService != null) {
                        chatService.sendMessageToServer("/resume");
                    } else {
                        Log.e(TAG, "ChatService is not bound.");
                        // 필요한 경우 서비스가 null일 때의 처리
                    }

                } else {
                    Toast.makeText(MessageBoxActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ArrayList<chat_user>> call, Throwable t) {
                Toast.makeText(MessageBoxActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.i(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    // BroadcastReceiver 구현
    // 서버로부터 메시지 수신하면 사용
    private BroadcastReceiver fanMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.i(TAG, "Received message: " + message);

            // String -> Json 객체로 변환 후에 각 값을 변수에 저장
            // 2. chat_user 객체를 JSON 문자열로 변환

            try {
                JSONObject received_Message = new JSONObject(message);
                int received_message_id = received_Message.getInt("message_id");
                int received_sender_id = received_Message.getInt("sender_id");
                String received_nickname = received_Message.getString("nickname");
                String received_message = received_Message.getString("message");
                String received_sent_time = received_Message.getString("sent_time");

                // 원래 형식의 시간 파싱을 위한 SimpleDateFormat
                SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                // 원하는 형식으로 변환하기 위한 SimpleDateFormat
                SimpleDateFormat newFormat = new SimpleDateFormat("yyyy년 MM월 dd일 EEEE");

                try {
                    // 문자열을 Date 객체로 파싱
                    Date date = originalFormat.parse(received_sent_time);

                    // Date 객체를 새로운 형식으로 포맷
                    formattedDate = newFormat.format(date);

                    Date last_list_time = originalFormat.parse(fanMessages.get(fanMessages.size()-1).getSent_time());
                    beforeDate = newFormat.format(last_list_time);

                    // 포맷된 시간 출력
                    System.out.println("formattedDate: " + formattedDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (!beforeDate.equals(formattedDate)) {
                    fanMessages.add(new chat_user(2, -1,  formattedDate));
                    Log.i(TAG, "beforeDate: " + beforeDate);
                    Log.i(TAG, "formattedDate" + formattedDate);
                    beforeDate = formattedDate;
                }

                // UI 업데이트 처리
                chat_user received_user = new chat_user
                        (1, received_message_id, received_sender_id, received_nickname, received_message, received_sent_time);
                fanMessages.add(received_user);


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
            return; // 로딩 중일 때는 검색을 실행하지 않음
        }

        isLoading = true; // 검색 시작 시 로딩 중으로 설정

        searchResultId = -1;

        if (query == null || query.isEmpty()) {
            adapter.setQuery(""); // 검색어가 없을 때 빈 문자열 설정
            adapter.notifyDataSetChanged(); // UI 업데이트
            return; // 검색어가 null이면 메서드를 빠르게 종료
        }

        // 어댑터에 검색어 설정
        adapter.setQuery(query);


        // 메시지 리스트를 검색하고 검색 결과 인덱스를 저장
        searchResultIndices.clear();
        for (int i = fanMessages.size() - 1; i >= 0; i--) {
            chat_user message = fanMessages.get(i);
            if (message != null && message.getMessage() != null && message.getMessage().contains(query)) {
                // message_id를 통해서 비교하기 위해서 넣음
                searchResultIndices.add(i);  // 검색 결과의 인덱스를 저장
            }
        }
        Log.i(TAG, "searchResultIndices: " + searchResultIndices.size());


        if (!searchResultIndices.isEmpty() && currentSearchIndex == -1) {
            currentSearchIndex = 0;  // 첫 번째 검색 결과로 이동
            scrollToSearchResult(currentSearchIndex);
        } else {
//            Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
            // 불러온 값에 결과값이 없다면 서버에서 불러오기
            // 이 코드를 for문 안에 넣었기 때문에 계속 실행이 되서 문제가 됨
            searchResult(query);
            Log.i(TAG, "filterMessages: 끝 : " + query);
        }
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
        Call<ChatResponse> call = service.sendSearchFanChatImformaition(chat_id, message_id, first_message_id, keyword);

        Log.i(TAG, "searchResult: chat_id : " + chat_id);
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
                                search_messages.add(new chat_user(0, message_id, sender_id, is_artist, image, nickname, message, sent_time));
                            } else {
                                search_messages.add(new chat_user(1, message_id, sender_id, is_artist, image, nickname, message, sent_time));
                            }

                            if (i == data.getMessages().size()-1) {
                                search_messages.add(new chat_user(2, -1,  lastDate));
                            }
                        }
                        // 역순으로 정렬
                        Collections.reverse(search_messages);


                        fanMessages.addAll(0, search_messages);
                        for (int i = 0; i <= fanMessages.size(); i++) {
                            if (fanMessages.get(i).getMessage_id() != -1) { // message_id가 있는지 확인
                                first_message_id = fanMessages.get(i).getMessage_id(); // 첫 번째 message_id가 있는 값을 저장
                                break; // 찾으면 반복문 종료
                            }
                        }


                        // 어댑터 갱신 및 스크롤 위치 조정
                        adapter.notifyItemRangeInserted(0, search_messages.size());

                        searchResultIndices.clear();
                        // 메시지 리스트를 검색하고 검색 결과 인덱스를 저장
                        for (int i = fanMessages.size() - 1; i >= 0; i--) {
                            chat_user message = fanMessages.get(i);
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
                        Toast.makeText(MessageBoxActivity.this, "검색 결과가 없습니다", Toast.LENGTH_LONG).show();
                        // 버튼 비활성화

                    }
                } else {
                    Toast.makeText(MessageBoxActivity.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                Toast.makeText(MessageBoxActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.i(TAG, "onFailure: " + t.getMessage());
            }
        });
        Log.i(TAG, "searchResult: 끝 : ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 현재 액티비티가 채팅 화면임을 추적
        Log.i(TAG, "onResume: message_id : " + message_id);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentActivity(this);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentMessageId(message_id);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentChatId(chat_id);
        // 방을 다시 보기 시작할 때 서버로 알림
        if (isBound) {
            chatService.sendMessageToServer("/resume");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 채팅 화면을 떠날 때 null로 설정
        Log.i(TAG, "onPause: ");
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentActivity(null);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentMessageId(-1);
        ((MyApplication) getApplication()).getAppLifecycleTracker().setCurrentChatId(-1);

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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fanMessageReceiver);
    }
}