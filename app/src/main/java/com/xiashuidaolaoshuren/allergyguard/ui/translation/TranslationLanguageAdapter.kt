package com.xiashuidaolaoshuren.allergyguard.ui.translation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xiashuidaolaoshuren.allergyguard.R
import com.xiashuidaolaoshuren.allergyguard.databinding.ItemTranslationLanguageBinding

data class TranslationLanguageUiModel(
    val languageTag: String,
    val displayName: String,
    val isDownloaded: Boolean,
    val isDownloading: Boolean
)

class TranslationLanguageAdapter(
    private val onDownloadClicked: (TranslationLanguageUiModel) -> Unit
) : ListAdapter<TranslationLanguageUiModel, TranslationLanguageAdapter.TranslationLanguageViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TranslationLanguageViewHolder {
        val binding = ItemTranslationLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TranslationLanguageViewHolder(binding, onDownloadClicked)
    }

    override fun onBindViewHolder(holder: TranslationLanguageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TranslationLanguageViewHolder(
        private val binding: ItemTranslationLanguageBinding,
        private val onDownloadClicked: (TranslationLanguageUiModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TranslationLanguageUiModel) {
            binding.textTranslationLanguageName.text = item.displayName
            binding.textTranslationLanguageStatus.text = when {
                item.isDownloading -> binding.root.context.getString(R.string.translation_status_downloading)
                item.isDownloaded -> binding.root.context.getString(R.string.translation_status_downloaded)
                else -> binding.root.context.getString(R.string.translation_status_not_downloaded)
            }

            binding.buttonDownloadLanguage.apply {
                isEnabled = !item.isDownloaded && !item.isDownloading
                text = when {
                    item.isDownloading -> context.getString(R.string.translation_action_downloading)
                    item.isDownloaded -> context.getString(R.string.translation_action_ready)
                    else -> context.getString(R.string.translation_action_download)
                }
                setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        onDownloadClicked(item)
                    }
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<TranslationLanguageUiModel>() {
        override fun areItemsTheSame(
            oldItem: TranslationLanguageUiModel,
            newItem: TranslationLanguageUiModel
        ): Boolean = oldItem.languageTag == newItem.languageTag

        override fun areContentsTheSame(
            oldItem: TranslationLanguageUiModel,
            newItem: TranslationLanguageUiModel
        ): Boolean = oldItem == newItem
    }
}
