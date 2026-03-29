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
    private val onItemClick: (ScanResult) -> Unit,
    private val onDeleteClick: (ScanResult) -> Unit
) : ListAdapter<ScanResult, ScanHistoryAdapter.ScanHistoryViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanHistoryViewHolder {
        val binding = ItemScanHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScanHistoryViewHolder(binding, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ScanHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ScanHistoryViewHolder(
        private val binding: ItemScanHistoryBinding,
        private val onItemClick: (ScanResult) -> Unit,
        private val onDeleteClick: (ScanResult) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat: DateFormat = DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT
        )

        fun bind(scanResult: ScanResult) {
            val context = binding.root.context
            binding.textScanTimestamp.text = dateFormat.format(Date(scanResult.timestamp))
            if (scanResult.textContent.isBlank()) {
                binding.textScanSummary.text = context.getString(R.string.history_summary_safe)
                binding.textScanSummary.setTextColor(
                    context.getColor(android.R.color.darker_gray)
                )
            } else {
                binding.textScanSummary.text = scanResult.textContent
                val attrs = intArrayOf(android.R.attr.textColorPrimary)
                val ta = context.obtainStyledAttributes(attrs)
                binding.textScanSummary.setTextColor(ta.getColor(0, android.graphics.Color.BLACK))
                ta.recycle()
            }
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
            binding.buttonDeleteScan.setOnClickListener {
                onDeleteClick(scanResult)
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
