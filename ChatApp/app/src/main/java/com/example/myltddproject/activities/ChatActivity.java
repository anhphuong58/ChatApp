package com.example.myltddproject.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

import com.example.myltddproject.adapters.ChatAdapter;
import com.example.myltddproject.databinding.ActivityChatBinding;
import com.example.myltddproject.models.ChatMessage;
import com.example.myltddproject.models.User;
import com.example.myltddproject.utilities.Constants;
import com.example.myltddproject.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private User reciverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String coversionId = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessage();
    }
    private void sendMessgae(){
        HashMap<String,Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID,reciverUser.id);
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        if(coversionId != null){
            updateCoversion(binding.inputMessage.getText().toString());
        } else {
            HashMap<String, Object> coversion = new HashMap<>();
            coversion.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
            coversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            coversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            coversion.put(Constants.KEY_RECEIVER_ID,reciverUser.id);
            coversion.put(Constants.KEY_RECEIVER_NAME, reciverUser.name);
            coversion.put(Constants.KEY_RECEIVER_IMAGE,reciverUser.image);
            coversion.put(Constants.KEY_LAST_MESSAGE,binding.inputMessage.getText().toString());
            coversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(coversion);
        }
        binding.inputMessage.setText(null);
    }

    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                        chatMessages, preferenceManager.getString(Constants.KEY_USER_ID), getBitmapFromEncodedString(reciverUser.image)
                );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
        binding.inputMessage.setText(null);
    }

    private void listenMessage(){
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, reciverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,reciverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }


    private final EventListener<QuerySnapshot> eventListener =(value, error) -> {
        if(error != null){
            return;
        }
        if(value != null){
            int count = chatMessages.size();
            for(DocumentChange documentChange: value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages,(obj1,obj2)->obj1.dateObject.compareTo(obj2.dateObject));
            if(count == 0){
                chatAdapter.notifyDataSetChanged();
            } else{
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
//                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size()-1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if (coversionId == null){
            checkForCoversion();
        }
    };

    private Bitmap getBitmapFromEncodedString(String encodeImage){
        byte[] bytes = Base64.decode(encodeImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }
    private void  loadReceiverDetails(){
        reciverUser =(User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(reciverUser.name);
    }
    private void setListeners(){
        binding.imageBack.setOnClickListener(v->onBackPressed());
        binding.layoutSend.setOnClickListener(v->sendMessgae());
    }
    private String getReadableDateTime(Date date){
        return new SimpleDateFormat("dd/MMMM/yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String, Object> coversion){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(coversion)
                .addOnSuccessListener(documentReference -> coversionId= documentReference.getId());
    }

    private void updateCoversion(String message){
        DocumentReference documentReference=
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(coversionId);
        documentReference.update(Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, new Date());
    }

    private void checkForCoversion(){
        if(chatMessages.size() != 0){
            checkForCoversionRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    reciverUser.id);
            checkForCoversionRemotely(reciverUser.id,
                    preferenceManager.getString(Constants.KEY_USER_ID));
        }
    }
    private void checkForCoversionRemotely(String senderId ,String receiverId ){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverId)
                .get()
                .addOnCompleteListener(coversionOnCompleteListener);
    }
    private final OnCompleteListener<QuerySnapshot> coversionOnCompleteListener = task -> {
        if(task.isSuccessful() && task.getResult()!= null && task.getResult().getDocuments().size()>0){
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            coversionId = documentSnapshot.getId();
        }
    };
}