// AnnouncementsAdapter.java
package com.example.clockit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AnnouncementsAdapter extends RecyclerView.Adapter<AnnouncementsAdapter.AnnouncementViewHolder> {

    private final List<Announcement> announcementList;
    private OnItemClickListener listener;

    // Constructor
    public AnnouncementsAdapter(List<Announcement> announcementList) {
        this.announcementList = announcementList;
    }

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(Announcement announcement);
    }

    // Set the click listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement, parent, false);
        return new AnnouncementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementViewHolder holder, int position) {
        Announcement announcement = announcementList.get(position);
        holder.title.setText(announcement.getTitle());
        holder.date.setText(announcement.getDate());

        String previewMessage = announcement.getMessage().length() > 30 ?
                announcement.getMessage().substring(0, 30) + "..." : announcement.getMessage();
        holder.message.setText(previewMessage);

        // Set click listener on the item view
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(announcement);
            }
        });
    }

    @Override
    public int getItemCount() {
        return announcementList.size();
    }

    static class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, message;

        public AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
            date = itemView.findViewById(R.id.text_date);
            message = itemView.findViewById(R.id.text_message);
        }
    }
}
