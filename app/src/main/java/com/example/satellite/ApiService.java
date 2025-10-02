package com.example.satellite;

import com.example.satellite.model.ChatResponse;
import com.example.satellite.model.ChatRoom;
import com.example.satellite.model.WithdrawRequest;
import com.example.satellite.model.chat_user;
import com.example.satellite.model.home_user;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiService {
    @POST("withdrawal.php")
    Call<Integer> withdraw(@Body WithdrawRequest request);

    @FormUrlEncoded
    @POST("satellite/home_artist_data.php")
    Call<ArrayList<home_user>> sendUniq_id(@Field("uniq_id")  String uniq_id,
                                           @Field("is_artist") int is_artist);

    @FormUrlEncoded
    @POST("satellite/search_artist.php")
    Call<ArrayList<home_user>> sendKeyword(@Field("user_id")  int user_id,
                                           @Field("keyword")  String keyword);

    @FormUrlEncoded
    @POST("satellite/search_chatrooms.php")
    Call<ArrayList<ChatRoom>> sendKeywordfindChatroom(@Field("user_id")  int user_id,
                                                      @Field("is_artist")  int is_artist,
                                                      @Field("keyword")  String keyword);

    @FormUrlEncoded
    @POST("satellite/load_chat_message.php")
    Call<ArrayList<chat_user>> sendChatImformaition(@Field("user_id")  int user_id,
                                                    @Field("is_artist") int is_artist,
                                                    @Field("artist_id") int artist_id);

    @FormUrlEncoded
    @POST("satellite/load_more_message.php")
    Call<ArrayList<chat_user>> sendMoreChatImformaition(@Field("chat_id")  int chat_id,
                                                        @Field("user_id")  int user_id,
                                                        @Field("message_id") int message_id);

    @FormUrlEncoded
    @POST("satellite/load_artist_chat_message.php")
    Call<ArrayList<chat_user>> sendArtistChatImformaition(@Field("user_id")  int user_id,
                                                          @Field("is_artist") int is_artist,
                                                          @Field("artist_id") int artist_id);

    @FormUrlEncoded
    @POST("satellite/load_fan_chat_message.php")
    Call<ArrayList<chat_user>> sendFanChatImformaition(@Field("chat_id")  int chat_id,
                                                       @Field("message_id") int message_id);

    @FormUrlEncoded
    @POST("satellite/load_chatroom.php")
    Call<ArrayList<ChatRoom>> sendChatroomsImformation(@Field("user_id")  int user_id,
                                                       @Field("is_artist") int is_artist);

    @FormUrlEncoded
    @POST("satellite/load_more_artist_message.php")
    Call<ArrayList<chat_user>> sendMoreArtistChatImformaition(@Field("chat_id")  int chat_id,
                                                              @Field("user_id")  int user_id,
                                                              @Field("message_id") int message_id);

    @FormUrlEncoded
    @POST("satellite/search_chat_message.php")
    Call<ChatResponse> sendSearchChatImformaition(@Field("chat_id")  int chat_id,
                                                  @Field("user_id")  int user_id,
                                                  @Field("message_id") int message_id,
                                                  @Field("keyword") String keyword);

    @FormUrlEncoded
    @POST("satellite/load_more_fan_message.php")
    Call<ArrayList<chat_user>> sendMoreFanChatImformaition(@Field("chat_id")  int chat_id,
                                                           @Field("message_id")  int message_id,
                                                           @Field("first_message_id") int first_message_id);

    @FormUrlEncoded
    @POST("satellite/search_fan_message.php")
    Call<ChatResponse> sendSearchFanChatImformaition(@Field("chat_id")  int chat_id,
                                                     @Field("message_id")  int message_id,
                                                     @Field("first_message_id") int first_message_id,
                                                     @Field("keyword") String keyword);
}
