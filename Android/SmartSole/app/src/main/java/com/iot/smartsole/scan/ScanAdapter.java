package com.iot.smartsole.scan;

import android.bluetooth.le.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.iot.smartsole.R;

import java.util.List;

public class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.ViewHolder> {
    private final List<ScanResult> localDataSet;
    private final OnItemClickListener onItemClickListener;

    public ScanAdapter(List<ScanResult> localDataSet,
                             OnItemClickListener onItemClickListener) {
        this.localDataSet = localDataSet;
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_scan,
                parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ScanAdapter.ViewHolder holder, int position) {
        holder.bind(localDataSet.get(position), onItemClickListener);
    }

    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public interface OnItemClickListener {
        void onItemClick(ScanResult scanResult);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView deviceImageView;
        private TextView deviceNameView;
        private TextView deviceMACView;
        private TextView deviceRssiView;

        public ViewHolder(View view) {
            super(view);
            deviceImageView = (ImageView) view.findViewById(R.id.device_image);
            deviceNameView = (TextView) view.findViewById(R.id.device_name);
        }

        public void bind(ScanResult scanResult, OnItemClickListener onItemClickListener) {
            String deviceName = (scanResult.getDevice().getName() == null) ?
                    "Unnamed" : scanResult.getDevice().getName();
            deviceNameView.setText(deviceName);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(scanResult);
                }
            });
        }

    }
}
