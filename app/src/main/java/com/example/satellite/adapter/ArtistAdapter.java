package com.example.satellite.adapter;

import android.content.Context;
import android.content.Intent;
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
import com.example.satellite.ui.MessageBoxActivity;

import java.util.ArrayList;

public class ArtistAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENDER = 0;
    private static final int VIEW_TYPE_RECEIVER = 1;
    private static final int VIEW_TYPE_HEADER= 2;

    private ArrayList<chat_user> chatMessages;
    private Context context;

    String TAG = "ArtistChatAdapter";

    public ArtistAdapter(ArrayList<chat_user> chatMessages, Context context) {
        this.chatMessages = chatMessages;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        chat_user message = chatMessages.get(position);
        if (message.getUsertype() == 0) { // 보내는 사람 / 유저 메시지일 때
            Log.i(TAG, "getItemViewType: VIEW_TYPE_ARTIST : " + message.getIs_artist());
            return VIEW_TYPE_SENDER;
        } else if (message.getUsertype() == 1) { // 받는 사람 / 아티스트 메시지일 때
            Log.i(TAG, "getItemViewType: VIEW_TYPE_USER : " + message.getIs_artist());
            return VIEW_TYPE_RECEIVER;
        } else {
            Log.i(TAG, "getItemViewType: VIEW_TYPE_HEADER : " + message.getIs_artist());
            return VIEW_TYPE_HEADER;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.i(TAG, "onCreateViewHolder: 시작");
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENDER) {
            View view = inflater.inflate(R.layout.item_chat_sender, parent, false);
            Log.i(TAG, "onCreateViewHolder: item_chat_sender");
            return new SenderViewHolder(view);
        } else if (viewType == VIEW_TYPE_RECEIVER) {
            // 팬들의 메시지를 하나로 묶어 보여주기 위한 메시지함
            View view = inflater.inflate(R.layout.item_chat_fan_message_box, parent, false);
            Log.i(TAG, "onCreateViewHolder: item_chat_fan_message_box");
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

        public ReceiverViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.i(TAG, "메시지함 클릭");
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        chat_user clickedItem = chatMessages.get(position);
                        Intent intent = new Intent(context, MessageBoxActivity.class);

                        Log.i(TAG, "눌려서 값이 보이나 chat_id?" + clickedItem.getChat_id());
                        Log.i(TAG, "눌려서 값이 보이나 message_id?" + clickedItem.getMessage_id());

                        intent.putExtra("chat_id", clickedItem.getChat_id());
                        intent.putExtra("message_id", clickedItem.getMessage_id());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                }
            });
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


