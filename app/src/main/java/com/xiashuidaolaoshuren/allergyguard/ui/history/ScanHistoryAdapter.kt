package com.xiashuidaolaoshuren.allergyguard.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xiashuidaolaoshuren.allergyguard.R
import com.xiashuidaolaoshuren.allergyguard.data.ScanResult
import com.xiashuidaolaoshuren.allergyguard.databinding.ItemScanHistoryBinding
import com.xiashuidaolaoshuren.allergyguard.logic.ScanLocationCodec
import java.text.DateFormat
import java.util.Date

class ScanHistoryAdapter(
    private val onItemClick: (ScanResult) -> Unit
) : ListAdapter<ScanResult, ScanHistoryAdapter.ScanHistoryViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanHistoryViewHolder {
        val binding = ItemScanHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScanHistoryViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ScanHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ScanHistoryViewHolder(
        private val binding: ItemScanHistoryBinding,
        private val onItemClick: (ScanResult) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat: DateFormat = DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT
        )

        fun bind(scanResult: ScanResult) {
            val context = binding.root.context
            binding.textScanTimestamp.text = dateFormat.format(Date(scanResult.timestamp))
            binding.textScanSummary.text = scanResult.textContent
            binding.textScanStatus.text = if (scanResult.hasAllergens) {
                context.getString(R.string.history_status_allergen_detected)
            } else {
                context.getString(R.string.history_status_safe)
            }

            val coordinate = ScanLocationCodec.decode(scanResult.location)
            binding.textScanLocation.text = if (coordinate != null) {
                context.getString(R.string.history_location_format, coordinate.latitude, coordinate.longitude)
            } else {
                context.getString(R.string.history_location_unknown)
            }

            binding.textScanStatus.setTextColor(
                if (scanResult.hasAllergens) {
                    context.getColor(R.color.scan_alert_red)
                } else {
                    context.getColor(R.color.scan_safe_green)
                }
            )

            binding.root.setOnClickListener {
                onItemClick(scanResult)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ScanResult>() {
        override fun areItemsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
            return oldItem == newItem
        }
    }
}
