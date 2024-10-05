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

import java.util.ArrayList;

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

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                }
            });

            iv_chats_profile = (ImageView) view.findViewById(R.id.iv_chats_profile);
            tv_chats_nickname = (TextView) view.findViewById(R.id.tv_chats_nickname);
            tv_chats_message = (TextView) view.findViewById(R.id.tv_chats_message);
            tv_chats_time = (TextView) view.findViewById(R.id.tv_chats_time);
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
        itemViewHolder.getTimeTextView().setText(currentItem.getSent_time());
    }

    // Return the size of your dataset (invoked by the layout manager)
    // 데이터 세트의 크기를 반환합니다(레이아웃 관리자가 호출).
    @Override
    public int getItemCount() {
        return localDataSet.size();
    }
}
