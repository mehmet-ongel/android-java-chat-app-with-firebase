package com.techmania.chatapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.techmania.chatapp.databinding.ReceivedMessagesItemBinding;
import com.techmania.chatapp.databinding.SendMessagesItemBinding;
import com.techmania.chatapp.models.MessagesModel;

import java.util.ArrayList;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT_MESSAGE = 1;
    private static final int VIEW_TYPE_RECEIVED_MESSAGE = 2;

    ArrayList<MessagesModel> messagesList;
    String currentUserId;
    OnMessageClickListener onMessageClickListener;

    public MessagesAdapter(ArrayList<MessagesModel> messagesList, String currentUserId, OnMessageClickListener onMessageClickListener) {
        this.messagesList = messagesList;
        this.currentUserId = currentUserId;
        this.onMessageClickListener = onMessageClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_SENT_MESSAGE){
            SendMessagesItemBinding sendMessagesItemBinding = SendMessagesItemBinding.inflate(inflater,parent,false);
            viewHolder = new SentMessagesViewHolder(sendMessagesItemBinding);
        } else if (viewType == VIEW_TYPE_RECEIVED_MESSAGE) {
            ReceivedMessagesItemBinding receivedMessagesItemBinding = ReceivedMessagesItemBinding.inflate(inflater,parent,false);
            viewHolder = new ReceivedMessagesViewHolder(receivedMessagesItemBinding);
        }else {
            return null;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        MessagesModel message = messagesList.get(position);
        if (holder instanceof SentMessagesViewHolder){
            ((SentMessagesViewHolder) holder).sendMessagesItemBinding.textViewMessageSent.setText(message.getMessage());

            ((SentMessagesViewHolder) holder).sendMessagesItemBinding.textViewMessageSent.setOnLongClickListener(v -> {

                onMessageClickListener.onMessageClicked(message);
                return true;

            });

        } else if (holder instanceof ReceivedMessagesViewHolder) {
            ((ReceivedMessagesViewHolder) holder).receivedMessagesItemBinding.textViewMessageReceived.setText(message.getMessage());

            ((ReceivedMessagesViewHolder) holder).receivedMessagesItemBinding.textViewMessageReceived.setOnLongClickListener(v -> {

                onMessageClickListener.onMessageClicked(message);
                return true;

            });

        }

    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    public static class SentMessagesViewHolder extends RecyclerView.ViewHolder {

        SendMessagesItemBinding sendMessagesItemBinding;

        public SentMessagesViewHolder(@NonNull SendMessagesItemBinding sendMessagesItemBinding) {
            super(sendMessagesItemBinding.getRoot());
            this.sendMessagesItemBinding = sendMessagesItemBinding;
        }
    }

    public static class ReceivedMessagesViewHolder extends RecyclerView.ViewHolder {

        ReceivedMessagesItemBinding receivedMessagesItemBinding;

        public ReceivedMessagesViewHolder(@NonNull ReceivedMessagesItemBinding receivedMessagesItemBinding) {
            super(receivedMessagesItemBinding.getRoot());
            this.receivedMessagesItemBinding = receivedMessagesItemBinding;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (messagesList.get(position).getSenderId().equals(currentUserId)){
            return VIEW_TYPE_SENT_MESSAGE;
        }else {
            return VIEW_TYPE_RECEIVED_MESSAGE;
        }

    }

    public interface OnMessageClickListener{

        void onMessageClicked(MessagesModel message);

    }
}















