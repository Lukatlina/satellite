package com.example.satellite.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.satellite.ui.ArtistChatActivity;
import com.example.satellite.ui.FanChatActivity;
import com.example.satellite.R;
import com.example.satellite.model.ChatRoom;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class RoomAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "RoomAdapter";
    private Context context;
    private ArrayList<ChatRoom> localDataSet;
    private int is_artist;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     * 사용 중인 view 유형에 대한 참조를 제공합니다.
     *      * (사용자 지정 뷰홀더)
     */

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView iv_chats_profile;
        private TextView tv_chats_nickname;
        private TextView tv_chats_message;
        private TextView tv_chats_time;
        private TextView tv_unseen_message_badge;

        public ItemViewHolder(View view, final Context context, final ArrayList<ChatRoom> localDataSet, int is_artist) {
            super(view);
            // 뷰홀더의 뷰에 대한 클릭 리스너를 정의합니다.
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Element " + getAdapterPosition() + " clicked.");

                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Intent intent;
                        ChatRoom clickedItem = localDataSet.get(position);
                        if (is_artist == 0) {
                            intent = new Intent(context, FanChatActivity.class);
                        }else{
                            intent = new Intent(context, ArtistChatActivity.class);
                        }

                        Log.i(TAG, "아이템 클릭시 : " + clickedItem.getArtist_nickname());

                        intent.putExtra("artist_id", clickedItem.getArtist_id());
                        intent.putExtra("chat_id", clickedItem.getChat_id());

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                }
            });

            iv_chats_profile = (ImageView) view.findViewById(R.id.iv_chats_profile);
            tv_chats_nickname = (TextView) view.findViewById(R.id.tv_chats_nickname);
            tv_chats_message = (TextView) view.findViewById(R.id.tv_chats_message);
            tv_chats_time = (TextView) view.findViewById(R.id.tv_chats_time);
            tv_unseen_message_badge = (TextView) view.findViewById(R.id.tv_unseen_message_badge);
        }

        public ImageView getProfileImageView() {
            return iv_chats_profile;
        }

        public TextView getNicknameTextView() {
            return tv_chats_nickname;
        }

        public TextView getMessageTextView() {
            return tv_chats_message;
        }

        public TextView getTimeTextView() {
            return tv_chats_time;
        }

        public TextView getUnseenTextView() {
            return tv_unseen_message_badge;
        }

    }

    /**
     * Initialize the dataset of the Adapter
     *
     * @param dataSet String[] containing the data to populate views to be used
     * by RecyclerView
     *
     *      어댑터의 데이터 세트 초기화하기
     *
     * 사용할 뷰를 채울 데이터가 포함된 @param dataSet String[] * * @param 데이터셋
     * RecyclerView 기준
     */
    public RoomAdapter(ArrayList<ChatRoom> dataSet, Context context, int is_artist) {
        this.localDataSet = dataSet;
        this.context = context;
        this.is_artist = is_artist;
    }



    // Create new views (invoked by the layout manager)
    // 새 views 만들기(레이아웃 관리자가 호출)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_chat_room, viewGroup, false);
        return new ItemViewHolder(view, context, localDataSet, is_artist);
    }

    // Replace the contents of a view (invoked by the layout manager)
    // 뷰의 콘텐츠 바꾸기(레이아웃 관리자가 호출)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int position) {
        Log.d(TAG, "Element " + position + " set.");
        ChatRoom currentItem = localDataSet.get(position);
        ItemViewHolder itemViewHolder = (ItemViewHolder) viewHolder;
        if (currentItem.getArtist_image() == null || currentItem.getArtist_image().isEmpty()) {
            Log.i(TAG, "onBindViewHolder: if문 안 프로필 있냐???" + currentItem.getArtist_image());
            itemViewHolder.getProfileImageView().setImageResource(R.drawable.baseline_person_60);
        }else{
            Log.i(TAG, "onBindViewHolder: else문 안 프로필 있냐???" + currentItem.getArtist_image());
            Glide.with(itemViewHolder.getProfileImageView().getContext()).load(currentItem.getArtist_image()).circleCrop().into(itemViewHolder.getProfileImageView());
        }
        itemViewHolder.getNicknameTextView().setText(currentItem.getArtist_nickname());
        itemViewHolder.getMessageTextView().setText(currentItem.getLast_message());
        if (is_artist == 0) {
            itemViewHolder.getTimeTextView().setText(changeFormattedTime(currentItem.getSent_time()));
            if (currentItem.getUnread_count() != 0) {
                itemViewHolder.getUnseenTextView().setText(String.valueOf(currentItem.getUnread_count()));
                itemViewHolder.getUnseenTextView().setVisibility(View.VISIBLE);
            }
        }else{
            itemViewHolder.getTimeTextView().setVisibility(View.INVISIBLE);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    // 데이터 세트의 크기를 반환합니다(레이아웃 관리자가 호출).
    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    // 채팅방 마지막 메시지 전송 시간의 포맷을 수정하기 위한 메서드
    private String changeFormattedTime(String sent_time) {
        // 원래 형식의 시간 파싱을 위한 SimpleDateFormat
        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 문자열을 Date 객체로 파싱
        Date dateTime = null;
        try {
            dateTime = originalFormat.parse(sent_time);
        } catch (ParseException e) {
            e.printStackTrace();
            return sent_time;
        }

        // 원하는 형식으로 변환하기 위한 SimpleDateFormat
        // 만약 오늘 주고 받은 메시지라면 HH:mm의 형식이며, 어제 주고 받았다면 어제, 그 이전은 MM월 dd일의 형식이며, 해가 지났다면, yyyy.MM.dd의 형태로 표시되게 된다.

        // 받은 데이터의 날짜
        Calendar date = Calendar.getInstance();
        date.setTime(dateTime);

        // 1. 오늘인지 확인
        Calendar today = Calendar.getInstance();

        // 2. 어제인지 확인
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        // 3. 시간 포맷 선언 (재사용)
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM월 dd일");
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy.MM.dd");

        if (isSameDay(today, date)) {
            Log.i(TAG, "오늘 시간 : " + today.get(Calendar.DAY_OF_YEAR));
            return timeFormat.format(dateTime);
        } else if (isSameDay(yesterday, date)) {
            Log.i(TAG, "어제 시간 : "  + yesterday.get(Calendar.DAY_OF_YEAR));
            return "어제";
        } else if (today.get(Calendar.YEAR) == date.get(Calendar.YEAR)) {
            Log.i(TAG, "올해 : ");
            return dateFormat.format(dateTime);
        } else {
            Log.i(TAG, "모두 해당 안됨 : ");
            return yearFormat.format(dateTime);
        }
    }

    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}
