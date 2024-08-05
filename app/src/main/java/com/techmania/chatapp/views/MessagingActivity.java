package com.techmania.chatapp.views;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.techmania.chatapp.R;
import com.techmania.chatapp.adapters.MessagesAdapter;
import com.techmania.chatapp.databinding.ActivityMessagingBinding;
import com.techmania.chatapp.models.MessagesModel;

import java.util.ArrayList;

public class MessagingActivity extends AppCompatActivity implements MessagesAdapter.OnMessageClickListener {

    ActivityMessagingBinding messagingBinding;
    String targetUserName;
    String targetUserId;
    String targetUserImageUrl;
    String currentUserName;
    String currentUserId;

    String message;

    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference().child("Messages");

    MessagesAdapter messagesAdapter;
    ArrayList<MessagesModel> messagesList = new ArrayList<>();

    ValueEventListener messagesEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        messagingBinding = ActivityMessagingBinding.inflate(getLayoutInflater());
        setContentView(messagingBinding.getRoot());

        targetUserName = getIntent().getStringExtra("targetUserName");
        targetUserId = getIntent().getStringExtra("targetUserId");
        targetUserImageUrl = getIntent().getStringExtra("targetUserImageUrl");
        currentUserName = getIntent().getStringExtra("currentUserName");
        currentUserId = getIntent().getStringExtra("currentUserId");

        Log.d("targetUserName",targetUserName);
        Log.d("targetUserId",targetUserId);
        Log.d("targetUserImageUrl",targetUserImageUrl);
        Log.d("currentUserName",currentUserName);
        Log.d("currentUserId",currentUserId);

        messagingBinding.textViewFriendName.setText(targetUserName);
        if (targetUserImageUrl.equals("null")){
            messagingBinding.imageViewFriendProfile.setImageResource(R.drawable.default_profile_photo_light);
        }else {
            Picasso.get().load(targetUserImageUrl).into(messagingBinding.imageViewFriendProfile);
        }

        messagingBinding.imageViewGoMain.setOnClickListener(v -> {
            finish();
        });

        messagingBinding.imageViewSendMessage.setOnClickListener(v -> {

            message = messagingBinding.editTextMessage.getText().toString();

            if (message.isEmpty()){
                Toast.makeText(this, "Please write a message", Toast.LENGTH_SHORT).show();
            }else {
                sendMessage();
            }

        });

        messagingBinding.recyclerViewMessage.setLayoutManager(new LinearLayoutManager(this));
        messagesAdapter = new MessagesAdapter(messagesList,currentUserId,this);
        messagingBinding.recyclerViewMessage.setAdapter(messagesAdapter);
        getMessagesFromDatabase();


    }

    public void sendMessage(){

        String keyForEachMessage = databaseReference.child(currentUserId).child(targetUserId).push().getKey();

        MessagesModel messages = new MessagesModel(currentUserId,targetUserId,keyForEachMessage,message);

        databaseReference.child(currentUserId).child(targetUserId).child(keyForEachMessage).setValue(messages).addOnCompleteListener(task -> {

            if (task.isSuccessful()){

                databaseReference.child(targetUserId).child(currentUserId).child(keyForEachMessage).setValue(messages).addOnCompleteListener(task1 -> {

                    if (task1.isSuccessful()){

                        messagingBinding.editTextMessage.setText("");

                    }else {
                        Toast.makeText(this, task1.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }

                });

            }else {
                Toast.makeText(this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }

        });

    }

    public void getMessagesFromDatabase(){

        //Messages -> currentUserId -> targetUserId
        messagesEventListener = databaseReference.child(currentUserId).child(targetUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                messagesList.clear();
                for (DataSnapshot eachMessage : snapshot.getChildren()){

                    MessagesModel message = eachMessage.getValue(MessagesModel.class);
                    if (message != null){
                        messagesList.add(message);
                    }

                }

                messagesAdapter.notifyDataSetChanged();

                if (!messagesList.isEmpty()){

                    messagingBinding.recyclerViewMessage.scrollToPosition(messagesList.size() - 1);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(MessagingActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (messagesEventListener != null){
            databaseReference.child(currentUserId).child(targetUserId).removeEventListener(messagesEventListener);
        }

    }

    @Override
    public void onMessageClicked(MessagesModel message) {

        new AlertDialog.Builder(MessagingActivity.this)
                .setTitle("Chat App")
                .setMessage("Do you want to delete this message?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteMessage(message);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel",(dialog, which) -> {
                    dialog.dismiss();
                })
                .show();

    }

    public void deleteMessage(MessagesModel message){

        databaseReference.child(currentUserId).child(targetUserId)
                .child(message.getMessageId()).removeValue().addOnCompleteListener(task -> {

                    if (task.isSuccessful()){
                        Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }

        });

    }
}










