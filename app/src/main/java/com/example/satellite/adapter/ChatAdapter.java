package com.example.satellite.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.satellite.R;
import com.example.satellite.model.chat_user;

import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENDER = 0;
    private static final int VIEW_TYPE_RECEIVER = 1;
    private static final int VIEW_TYPE_HEADER= 2;

    private ArrayList<chat_user> chatMessages;
    private Context context;

    String TAG = "ChatAdapter";

    public ChatAdapter(ArrayList<chat_user> chatMessages, Context context) {
        this.chatMessages = chatMessages;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        chat_user message = chatMessages.get(position);
        if (message.getUsertype() == 0) { // 보내는 사람
            Log.i(TAG, "getItemViewType: VIEW_TYPE_USER : " + message.getUsertype());
            return VIEW_TYPE_SENDER;
        } else if (message.getUsertype() == 1) { // 받는 사람
            Log.i(TAG, "getItemViewType: VIEW_TYPE_ARTIST : " + message.getUsertype());
            return VIEW_TYPE_RECEIVER;
        } else {
            Log.i(TAG, "getItemViewType: VIEW_TYPE_HEADER : " + message.getUsertype());
            return VIEW_TYPE_HEADER;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.i(TAG, "onCreateViewHolder: 시작");
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENDER) {
            View view = inflater.inflate(R.layout.item_chat_sender, parent, false);
            Log.i(TAG, "onCreateViewHolder: VIEW_TYPE_SENDER");
            return new SenderViewHolder(view);
        } else if (viewType == VIEW_TYPE_RECEIVER) {
            View view = inflater.inflate(R.layout.item_chat_receiver, parent, false);
            Log.i(TAG, "onCreateViewHolder: item_chat_receiver");
            return new ReceiverViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_date_header, parent, false);
            Log.i(TAG, "onCreateViewHolder: item_date_header");
            return new HeaderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        chat_user chatMessage = chatMessages.get(position);
        if (holder instanceof SenderViewHolder) {
            Log.i(TAG, "onBindViewHolder: SenderViewHolder");
            ((SenderViewHolder) holder).bind(chatMessage.getMessage(), chatMessage.getSent_time());
        } else if (holder instanceof ReceiverViewHolder) {
            Log.i(TAG, "onBindViewHolder: ReceiverViewHolder");
            ((ReceiverViewHolder) holder).bind(chatMessage.getMessage(),chatMessage.getNickname(), chatMessage.getImage(), chatMessage.getSent_time());
        } else if (holder instanceof HeaderViewHolder) {
            Log.i(TAG, "onBindViewHolder: HeaderViewHolder");
            ((HeaderViewHolder) holder).bind(chatMessage.getSent_time());
        }
    }

    @Override
    public int getItemCount() {
        Log.i(TAG, "getItemCount: 내부");
        return chatMessages.size();
    }

    class SenderViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserMessage;
        TextView tv_sender_message_time;

        public SenderViewHolder(View itemView) {
            super(itemView);
            tvUserMessage = itemView.findViewById(R.id.tv_user_message);
            tv_sender_message_time = itemView.findViewById(R.id.tv_sender_message_time);
        }

        void bind(String message, String sentTime) {
            tvUserMessage.setText(message);
            tv_sender_message_time.setText(sentTime);
        }
    }

    class ReceiverViewHolder extends RecyclerView.ViewHolder {
        ImageView iv_receiver_profile_image;
        TextView tv_receiver_user_nickname;
        TextView tv_receiver_message;
        TextView tv_receiver_message_time;

        public ReceiverViewHolder(View itemView) {
            super(itemView);
            iv_receiver_profile_image = itemView.findViewById(R.id.iv_receiver_profile_image);
            tv_receiver_user_nickname = itemView.findViewById(R.id.tv_receiver_user_nickname);
            tv_receiver_message = itemView.findViewById(R.id.tv_receiver_message);
            tv_receiver_message_time = itemView.findViewById(R.id.tv_receiver_message_time);
        }

        void bind(String message, String nickname, String profileImage, String sent_time) {
            tv_receiver_message.setText(message);
            tv_receiver_message_time.setText(sent_time);
            tv_receiver_user_nickname.setText(nickname);
            if (profileImage != null && !profileImage.isEmpty()) {
                Glide.with(context).load(profileImage).circleCrop().into(iv_receiver_profile_image);
            }else{
                iv_receiver_profile_image.setImageResource(R.drawable.baseline_person_60);
            }
        }
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView dateTime;


        public HeaderViewHolder(View itemView) {
            super(itemView);
            dateTime = itemView.findViewById(R.id.dateTextView);
        }

        void bind(String sentTime) {
            dateTime.setText(sentTime);
        }
    }
}
