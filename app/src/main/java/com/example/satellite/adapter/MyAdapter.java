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
import com.example.satellite.ui.ArtistProfileActivity;
import com.example.satellite.ui.LoginActivity;
import com.example.satellite.ui.MyProfileActivity;
import com.example.satellite.R;
import com.example.satellite.model.home_user;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MyAdapter";
    private Context context;
    private ArrayList<home_user> localDataSet;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     * 사용 중인 view 유형에 대한 참조를 제공합니다.
     *      * (사용자 지정 뷰홀더)
     */

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView tv_home_title;

        public HeaderViewHolder(View view) {
            super(view);
            tv_home_title = view.findViewById(R.id.tv_home_title);
        }

        public TextView getTitleTextView() {
            return tv_home_title;
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView iv_home_profile;
        private TextView tv_home_nickname;
        private TextView tv_home_message;

        public ItemViewHolder(View view, final Context context, final ArrayList<home_user> localDataSet) {
            super(view);
            // 뷰홀더의 뷰에 대한 클릭 리스너를 정의합니다.
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Element " + getAdapterPosition() + " clicked.");

                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Intent intent;
                        home_user clickedItem = localDataSet.get(position);
                        if (clickedItem.getUsertype() == 1) {
                            intent = new Intent(context, MyProfileActivity.class);
                        } else if (clickedItem.getUsertype() == 2 || clickedItem.getUsertype() == 3) {
                            intent = new Intent(context, ArtistProfileActivity.class);
                        }else {
                            intent = new Intent(context, LoginActivity.class);
                        }
                        Log.i(TAG, "눌려서 값이 보이나 닉네임?" + clickedItem.getNickname());
                        Log.i(TAG, "눌려서 값이 보이나 이미지?" + clickedItem.getImage());
                        Log.i(TAG, "눌려서 값이 보이나 유저타입?" + clickedItem.getUsertype());
                        intent.putExtra("id", clickedItem.getId());
                        intent.putExtra("nickname", clickedItem.getNickname());
                        intent.putExtra("message", clickedItem.getMessage());
                        intent.putExtra("image", clickedItem.getImage());
                        intent.putExtra("usertype", clickedItem.getUsertype());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                }
            });

            iv_home_profile = (ImageView) view.findViewById(R.id.iv_home_profile);
            tv_home_nickname = (TextView) view.findViewById(R.id.tv_home_nickname);
            tv_home_message = (TextView) view.findViewById(R.id.tv_home_message);
        }

        public ImageView getProfileImageView() {
            return iv_home_profile;
        }

        public TextView getNicknameTextView() {
            return tv_home_nickname;
        }

        public TextView getMessageTextView() {
            return tv_home_message;
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
    public MyAdapter(ArrayList<home_user> dataSet, Context context) {
        localDataSet = dataSet;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return localDataSet.get(position).getUsertype();
    }

    // Create new views (invoked by the layout manager)
    // 새 views 만들기(레이아웃 관리자가 호출)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        if (viewType == home_user.TYPE_HEADER) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.header_layout, viewGroup, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.home_user_item, viewGroup, false);
            return new ItemViewHolder(view, context, localDataSet);
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    // 뷰의 콘텐츠 바꾸기(레이아웃 관리자가 호출)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int position) {
        Log.d(TAG, "Element " + position + " set.");
        home_user currentItem = localDataSet.get(position);
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        if (viewHolder.getItemViewType() == home_user.TYPE_HEADER) {
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) viewHolder;
            headerViewHolder.getTitleTextView().setText(currentItem.getTitle());
        } else {
            ItemViewHolder itemViewHolder = (ItemViewHolder) viewHolder;
            if (localDataSet.get(position).getImage() == null) {
                Log.i(TAG, "onBindViewHolder: if문 안 프로필 있냐???" + localDataSet.get(position).getImage());
                itemViewHolder.getProfileImageView().setImageResource(R.drawable.baseline_person_60);
            }else{
                Log.i(TAG, "onBindViewHolder: else문 안 프로필 있냐???" + localDataSet.get(position).getImage());
                Glide.with(itemViewHolder.getProfileImageView().getContext()).load(localDataSet.get(position).getImage()).circleCrop().into(itemViewHolder.getProfileImageView());
            }
            itemViewHolder.getNicknameTextView().setText(currentItem.getNickname());
            itemViewHolder.getMessageTextView().setText(currentItem.getMessage());
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    // 데이터 세트의 크기를 반환합니다(레이아웃 관리자가 호출).
    @Override
    public int getItemCount() {
        return localDataSet.size();
    }
}
