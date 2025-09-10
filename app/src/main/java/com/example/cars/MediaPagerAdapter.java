package com.example.cars;// Change to your package name

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MediaPagerAdapter extends RecyclerView.Adapter<MediaPagerAdapter.MediaViewHolder> {

    private Context context;
    private List<String> mediaUrls; // URLs from Firebase

    public MediaPagerAdapter(Context context, List<String> mediaUrls) {
        this.context = context;
        this.mediaUrls = mediaUrls;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_media, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        String url = mediaUrls.get(position);

        // Check if it's a video
        if (url.endsWith(".mp4") || url.endsWith(".mov") || url.endsWith(".avi")) {
            holder.imageView.setVisibility(View.GONE);
            holder.videoView.setVisibility(View.VISIBLE);

            holder.videoView.setVideoURI(Uri.parse(url));
            holder.videoView.seekTo(1); // Preview first frame
            holder.videoView.setOnClickListener(v -> {
                if (!holder.videoView.isPlaying()) {
                    holder.videoView.start();
                } else {
                    holder.videoView.pause();
                }
            });

        } else { // It's an image
            holder.videoView.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);

            Glide.with(context)
                .load(url)
                .into(holder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return mediaUrls.size();
    }

    public static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        VideoView videoView;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            videoView = itemView.findViewById(R.id.videoView);
        }
    }
}

