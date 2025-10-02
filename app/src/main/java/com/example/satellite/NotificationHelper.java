package com.example.satellite;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.satellite.ui.ArtistChatActivity;
import com.example.satellite.ui.FanChatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    public static final String ARTIST_CHANNEL_ID = "artist_notification_channel";
    public static final String FAN_CHANNEL_ID = "fan_notification_channel";
    public static final String GROUP_CHANNEL_ID = "group_notification_channel";
    public static final String CHANNEL_NAME_ARTIST = "Artist Notifications";
    public static final String CHANNEL_NAME_FAN = "Fan Notifications";
    public static final String CHANNEL_NAME_GROUP = "Group Notifications";
    private static final String GROUP_KEY_CHAT = "com.example.satellite.CHAT_GROUP";

    // 알림 채널을 생성하는 메서드 (Android 8.0 이상)
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel artistChannel = new NotificationChannel(
                    ARTIST_CHANNEL_ID,
                    CHANNEL_NAME_ARTIST,
                    NotificationManager.IMPORTANCE_LOW // 소리 없이 고정된 알림
            );
            artistChannel.setDescription("Channel for artist notifications without sound");

            // 팬용 채널
            NotificationChannel fanChannel = new NotificationChannel(
                    FAN_CHANNEL_ID,
                    CHANNEL_NAME_FAN,
                    NotificationManager.IMPORTANCE_HIGH // 소리와 진동을 포함한 알림

            );
            fanChannel.setDescription("Channel for fan notifications with sound");

            // 요약 알림 채널 - 중요도가 낮음 (소리 없이 요약된 알림)
            NotificationChannel groupChannel = new NotificationChannel(
                    GROUP_CHANNEL_ID,
                    CHANNEL_NAME_GROUP,
                    NotificationManager.IMPORTANCE_DEFAULT // 중요도 낮음
            );

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(artistChannel);
                notificationManager.createNotificationChannel(fanChannel);
                notificationManager.createNotificationChannel(groupChannel);
            }
        }
    }

    // 알림 권한이 있는지 확인하는 메서드 (Android 13 이상)
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Android 13 미만에서는 true 반환
    }

    // 권한을 요청하는 메서드 (Android 13 이상)
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    // 알림을 보내는 메서드 (SecurityException 명시적 처리 추가)
    public static void sendChatNotification(Context context, String message) {
        try {
            Log.i(TAG, "sendChatNotification: 시작");
            // 받은 메시지가 현재 보고 있는 채팅방과 같다면 보여줄 것
            JSONObject received_Message = new JSONObject(message);
            int received_message_id = received_Message.getInt("message_id");
            int received_chat_id = received_Message.getInt("chat_id");
            String received_chatroom_name = received_Message.getString("chatroom_name");
            int received_sender_id = received_Message.getInt("sender_id");
            int received_is_artist = received_Message.getInt("is_artist");
            String received_image = received_Message.getString("image");
            String received_nickname = received_Message.getString("nickname");
            String received_message = received_Message.getString("message");
            String received_sent_time = received_Message.getString("sent_time");

            if (received_image == null || received_image.isEmpty()){
                received_image = "";
            }

            // `received_sent_time`이 날짜 문자열이므로 이를 타임스탬프로 변환해야 함
            long sentTime = 0;
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = dateFormat.parse(received_sent_time);
                if (date != null) {
                    sentTime = date.getTime(); // 밀리초 타임스탬프로 변환
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }



            // 채팅방으로 이동하는 Intent 생성
            Intent intent = new Intent(context, FanChatActivity.class);
            intent.putExtra("artist_id", received_sender_id);  // 채팅방 ID를 인텐트에 추가
            intent.putExtra("chat_id", received_chat_id);

            // 알림 클릭 시 실행될 PendingIntent 생성
            // 다른 앱(또는 시스템)에서 지정된 인텐트를 특정 시점에 실행하도록 허용하는 객체
            PendingIntent pendingIntent = PendingIntent.getActivity( // 알림 클릭시 해당 채팅방을 보여주는 액티비티 이동
                    context, // Activity나 Service 등의 컨텍스트
                    received_sender_id,  // 각 채팅방의 artistId로 고유 PendingIntent 생성
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE  // 최신 Android 버전에서의 보안 플래그
            );

            // 1. 개별 알림 보내기
//            sendIndividualNotification(context, received_message_id, received_chatroom_name, received_message, pendingIntent, sentTime, received_nickname);

            // 읽지 않은 메시지 수 업데이트 및 저장
            SharedPreferences user = context.getSharedPreferences("user", Context.MODE_PRIVATE);
            SharedPreferences.Editor user_editor = user.edit();

            // 현재 unread count 가져오기
            int totalUnreadCount = user.getInt("totalUnreadCount", -1);
            Log.i(TAG, "sendChatNotification: 안읽은 메시지개수 가져오자마자" + totalUnreadCount);

            int is_artist = user.getInt("is_artist", -1);
            totalUnreadCount += 1;  // 메시지 수 증가
            user_editor.putInt("totalUnreadCount", totalUnreadCount);  // 업데이트된 값 저장
            user_editor.apply();
            Log.i(TAG, "sendChatNotification: totalUnreadCount" + totalUnreadCount);

//            updateSummaryNotification(context, received_chat_id, received_message, totalUnreadCount);


            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

//            // InboxStyle로 메시지를 누적시켜서 보여줌
//            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
//            inboxStyle.setSummaryText(totalUnreadCount + "개의 안 읽은 메시지"); // 요약 문구 설정 (필요시)


//            // 이전 알림의 내용을 유지하고 새 메시지를 추가
//            if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
//
//                // CharSequence 타입 리스트로 이전 알림들을 수집
//                List<CharSequence> messages = new ArrayList<>();
//
//                if (activeNotifications != null && activeNotifications.length > 0) {
//                    for (int i = activeNotifications.length - 1; i >= 0; i--) {
//                        StatusBarNotification activeNotification = activeNotifications[i];
//                        if (activeNotification.getId() == received_chat_id) {
//                            CharSequence[] lines = activeNotification.getNotification().extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES);
//                            if (lines != null) {
//                                messages.addAll(Arrays.asList(lines));
//                            }
//                        }
//                    }
//                }
//                // 새로 받은 메시지를 가장 최신으로 추가
//                messages.add(0, received_message);
////                // 최신 5개 메시지만 유지
////                int startIndex = 0;
////                int endIndex = Math.min(messages.size(), MAX_NOTIFICATIONS);
////                List<CharSequence> latestMessages = messages.subList(startIndex, endIndex);
//
//                // InboxStyle에 최신 메시지들을 추가
//                for (CharSequence msg : messages) {
//                    inboxStyle.addLine(msg);
//                }
//            }

            // 개별 메시지 알림 생성
            NotificationCompat.Builder individualBuilder = new NotificationCompat.Builder(context, FAN_CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_person_60)
                    .setContentTitle(received_chatroom_name)
                    .setContentText(received_message)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setGroup(GROUP_KEY_CHAT)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setPriority(NotificationCompat.PRIORITY_LOW);

            // 개별 메시지 알림을 알림 매니저에 추가
            notificationManager.notify(received_message_id, individualBuilder.build());
            Log.i(TAG, "개별 알림 생성");

            // 그룹 요약 알림 생성
            NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle("You")
                    .setConversationTitle(received_chatroom_name);

            // 기존 알림에서 메시지를 가져와 추가
            if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
                if (activeNotifications != null && activeNotifications.length > 0) {
                    for (StatusBarNotification activeNotification : activeNotifications) {
                        if (activeNotification.getId() == received_chat_id) {
                            Notification oldNotification = activeNotification.getNotification();
                            Bundle extras = oldNotification.extras;
                            Parcelable[] messages = extras.getParcelableArray(NotificationCompat.EXTRA_MESSAGES);
                            if (messages != null) {
                                for (Parcelable p : messages) {
                                    Bundle messageBundle = (Bundle) p;
                                    String text = messageBundle.getString("text");
                                    String sender = messageBundle.getString("sender");
                                    long timestamp = messageBundle.getLong("time");

                                    messagingStyle.addMessage(text, timestamp, sender);
                                }
                            }
                        }
                    }
                }
            }

            // 새로운 메시지 추가
            messagingStyle.addMessage(received_message, sentTime, received_nickname);

            // 그룹 알림 생성 및 갱신
            NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, GROUP_CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_person_60)
                    .setContentTitle("팬 메시지")
                    .setContentText(totalUnreadCount + "개의 안 읽은 메시지")
                    .setStyle(messagingStyle)
                    .setGroup(GROUP_KEY_CHAT)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);


            // 그룹 알림을 알림 매니저에 추가
            notificationManager.notify(received_chat_id, summaryBuilder.build());
            Log.i(TAG, "그룹 알림 생성");

//            // 기존 알림에서 메시지를 가져와 추가
//            if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
//                if (activeNotifications != null && activeNotifications.length > 0) {
//                    for (StatusBarNotification activeNotification : activeNotifications) {
//                        if (activeNotification.getId() == received_chat_id) {
//                            Notification oldNotification = activeNotification.getNotification();
//                            Bundle extras = oldNotification.extras;
//                            Parcelable[] messages = extras.getParcelableArray(NotificationCompat.EXTRA_MESSAGES);
//                            if (messages != null) {
//                                for (Parcelable p : messages) {
//                                    Bundle messageBundle = (Bundle) p;
//                                    String text = messageBundle.getString("text");
//                                    String sender = messageBundle.getString("sender");
//                                    long timestamp = messageBundle.getLong("time");
//
//                                    messagingStyle.addMessage(text, timestamp, sender);
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            // 새로 받은 메시지를 추가
//            messagingStyle.addMessage(received_message, sentTime, received_nickname);


//            // 요약 알림 생성 (전체 알림의 요약)
//            NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, FAN_CHANNEL_ID)
//                    .setSmallIcon(R.drawable.baseline_person_60)
//                    .setContentTitle("팬 메시지")
//                    .setContentText(received_message)
//                    .setStyle(inboxStyle)
//                    .setGroup(GROUP_KEY_CHAT)
//                    .setGroupSummary(true)
//                    .setAutoCancel(true)
//                    .setPriority(NotificationCompat.PRIORITY_HIGH);
//
//            notificationManager.notify(received_chat_id, summaryBuilder.build());
//            Log.i(TAG, "그룹 알림");

//            // NotificationCompat.Builder로 알림 빌더 생성
//            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FAN_CHANNEL_ID)
//                    .setSmallIcon(R.drawable.baseline_person_60)
//                    .setContentTitle(received_chatroom_name)
//                    .setContentText(received_message)
//                    .setContentIntent(pendingIntent)
//                    .setAutoCancel(true)
//                    .setGroup(GROUP_KEY_CHAT)
////                    .setFullScreenIntent(pendingIntent, true)  // 전체 화면 인텐트 사용
//                    .setDefaults(Notification.DEFAULT_ALL) // 팬은 기본 알림, 아티스트는 조용한 알림
//                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);  // 소리, 진동 등 기본 알림 설정 추가

//            // NotificationCompat.Builder로 알림 빌더 생성
//            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FAN_CHANNEL_ID)
//                    .setSmallIcon(R.drawable.baseline_person_60)
//                    .setStyle(messagingStyle)
//                    .setContentText(received_message)
//                    .setContentIntent(pendingIntent)
//                    .setAutoCancel(true)
//                    .setGroup(GROUP_KEY_CHAT)
//                    .setDefaults(Notification.DEFAULT_ALL) // 팬은 기본 알림, 아티스트는 조용한 알림
//                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);  // 소리, 진동 등 기본 알림 설정 추가
//
//            // 권한이 없는 경우 SecurityException 발생 가능 -> 예외 처리 필요
//            notificationManager.notify(received_message_id, builder.build());
//            Log.i(TAG, "개별 알림");
//
//
//            // 알림 요약 생성 (모든 알림을 요약하는 형태)
//            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
//                    .setSummaryText(totalUnreadCount + "개의 안 읽은 메시지");
//
//            // 요약 알림의 내용을 갱신하기 위한 설정
//            StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
//            List<CharSequence> messages = new ArrayList<>();
//            if (activeNotifications != null) {
//                for (StatusBarNotification activeNotification : activeNotifications) {
//                    if (GROUP_KEY_CHAT.equals(activeNotification.getNotification().getGroup())) {
//                        CharSequence text = activeNotification.getNotification().extras.getCharSequence(NotificationCompat.EXTRA_TEXT);
////                        CharSequence[] lines = activeNotification.getNotification().extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES);
////                        if (lines != null) {
////                            messages.addAll(Arrays.asList(lines));
////                        }
//                        if (text != null) {
//                            messages.add(text);
//                        }
//                    }
//                }
//            }
//            for (CharSequence msg : messages) {
//                inboxStyle.addLine(msg);
//            }
//
//            // 요약 알림 생성 (전체 알림의 요약)
//            NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, FAN_CHANNEL_ID)
//                    .setSmallIcon(R.drawable.baseline_person_60)
//                    .setContentTitle("요약 알림")
//                    .setContentText(received_message)
//                    .setStyle(inboxStyle)
//                    .setGroup(GROUP_KEY_CHAT)
//                    .setGroupSummary(true)
//                    .setAutoCancel(true)
//                    .setPriority(NotificationCompat.PRIORITY_HIGH);
//
//            notificationManager.notify(received_chat_id, summaryBuilder.build());
//            Log.i(TAG, "그룹 알림");

            // Broadcast 전송
            Intent broadcastIntent = new Intent("com.example.UPDATE_UNREAD_COUNT");
            broadcastIntent.putExtra("totalUnreadCount", totalUnreadCount);
            LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);

        } catch (SecurityException e) {
            e.printStackTrace(); // 예외 발생 시 로그로 확인
            Toast.makeText(context, "Notification permission is not granted", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

//    public static void sendIndividualNotification(Context context, int messageId, String chatroomName, String message, PendingIntent pendingIntent, long sentTime, String senderNickname) {
//        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle("You")
//                .setConversationTitle(chatroomName)
//                .addMessage(message, sentTime, senderNickname);
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FAN_CHANNEL_ID)
//                .setSmallIcon(R.drawable.baseline_person_60)
//                .setStyle(messagingStyle)
//                .setContentIntent(pendingIntent)
//                .setAutoCancel(true)
//                .setGroup(GROUP_KEY_CHAT)
//                .setDefaults(Notification.DEFAULT_ALL)
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
//
//        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        if (notificationManager != null) {
//            notificationManager.notify(messageId, builder.build());
//        }
//    }

//    public static void updateSummaryNotification(Context context, int chatId, String receivedMessage, int totalUnreadCount) {
//        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        if (notificationManager == null) return;
//
//        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
//                .setSummaryText(totalUnreadCount + "개의 안 읽은 메시지");
//
//        // 기존 알림의 내용을 가져와 갱신하기 위한 설정
//        StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
//        List<CharSequence> messages = new ArrayList<>();
//        if (activeNotifications != null) {
//            for (StatusBarNotification activeNotification : activeNotifications) {
//                if (activeNotification.getId() == chatId) {
//                    CharSequence[] lines = activeNotification.getNotification().extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES);
//                    if (lines != null) {
//                        messages.addAll(Arrays.asList(lines));
//                    }
//                }
//            }
//        }
//
//        // 새로운 메시지를 추가하고, InboxStyle에 설정
//        messages.add(0, receivedMessage);
//        for (CharSequence msg : messages) {
//            inboxStyle.addLine(msg);
//        }
//
//        // 요약 알림 빌더 생성
//        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, FAN_CHANNEL_ID)
//                .setSmallIcon(R.drawable.baseline_person_60)
//                .setContentTitle("요약 알림")
//                .setContentText(receivedMessage)
//                .setStyle(inboxStyle)
//                .setGroup(GROUP_KEY_CHAT)
//                .setGroupSummary(true)
//                .setAutoCancel(true)
//                .setPriority(NotificationCompat.PRIORITY_HIGH);
//
//        notificationManager.notify(chatId, summaryBuilder.build());
//    }

    public static void showPersistentArtistNotification(Context context) {
        SharedPreferences user = context.getSharedPreferences("user", Context.MODE_PRIVATE);
        int artist_id = user.getInt("artist_id", -1);
        String channelId = ARTIST_CHANNEL_ID;
        String received_message = "팬들이 메시지를 기다리고 있어요";

        try {
            Intent intent = new Intent(context, ArtistChatActivity.class);
            intent.putExtra("artist_id", artist_id);  // 채팅방 ID를 인텐트에 추가

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    9999, // 고정 알림용 ID
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.baseline_person_60)
                    .setContentTitle("팬들이 메시지를 기다리고 있어요")
                    .setContentText(received_message)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true) // 고정 알림
                    .setAutoCancel(false);

            NotificationManagerCompat.from(context).notify(9999, builder.build());


        } catch (SecurityException e) {
            e.printStackTrace(); // 예외 발생 시 로그로 확인
            Toast.makeText(context, "Notification permission is not granted", Toast.LENGTH_SHORT).show();
        }
    }
}
