package com.monitor.telegramsite;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SiteListAdapter extends RecyclerView.Adapter<SiteListAdapter.ViewHolder> {

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    private final List<String>     sites;
    private final OnRemoveListener listener;

    public SiteListAdapter(List<String> sites, OnRemoveListener listener) {
        this.sites    = sites;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_site, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvSite.setText(sites.get(position));
        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) listener.onRemove(pos);
        });
    }

    @Override
    public int getItemCount() { return sites.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView    tvSite;
        ImageButton btnRemove;

        ViewHolder(View itemView) {
            super(itemView);
            tvSite    = itemView.findViewById(R.id.tv_site);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }
}
