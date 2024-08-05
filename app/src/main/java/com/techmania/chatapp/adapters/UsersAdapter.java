package com.techmania.chatapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.techmania.chatapp.R;
import com.techmania.chatapp.databinding.UsersItemLayoutBinding;
import com.techmania.chatapp.models.User;

import java.util.ArrayList;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UsersViewHolder> {

    ArrayList<User> usersList;
    OnUserClickListener onUserClickListener;

    public UsersAdapter(ArrayList<User> usersList, OnUserClickListener onUserClickListener) {
        this.usersList = usersList;
        this.onUserClickListener = onUserClickListener;
    }

    @NonNull
    @Override
    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        UsersItemLayoutBinding usersItemLayoutBinding = UsersItemLayoutBinding.inflate(
                LayoutInflater.from(parent.getContext()),parent,false
        );

        return new UsersViewHolder(usersItemLayoutBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {

        User user = usersList.get(position);
        holder.usersItemLayoutBinding.textViewUserItem.setText(user.getUserName());
        String url = user.getImageUrl();
        if (url.equals("null")){
            holder.usersItemLayoutBinding.imageViewUserItem.setImageResource(R.drawable.default_profile_photo);
        }else {
            Picasso.get().load(url).into(holder.usersItemLayoutBinding.imageViewUserItem);
        }

        holder.usersItemLayoutBinding.linearLayoutUserItem.setOnClickListener(v -> {

            onUserClickListener.onUserClicked(user);

        });

    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder {

        UsersItemLayoutBinding usersItemLayoutBinding;

        public UsersViewHolder(@NonNull UsersItemLayoutBinding usersItemLayoutBinding) {
            super(usersItemLayoutBinding.getRoot());

            this.usersItemLayoutBinding = usersItemLayoutBinding;

        }
    }

    public interface OnUserClickListener{
        void onUserClicked(User user);
    }

}
