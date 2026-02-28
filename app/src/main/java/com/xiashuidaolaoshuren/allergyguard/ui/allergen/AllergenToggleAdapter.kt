package com.xiashuidaolaoshuren.allergyguard.ui.allergen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xiashuidaolaoshuren.allergyguard.data.Allergen
import com.xiashuidaolaoshuren.allergyguard.databinding.ItemAllergenToggleBinding

class AllergenToggleAdapter(
    private val onToggleChanged: (allergen: Allergen, isEnabled: Boolean) -> Unit
) : ListAdapter<Allergen, AllergenToggleAdapter.AllergenViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllergenViewHolder {
        val binding = ItemAllergenToggleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AllergenViewHolder(binding, onToggleChanged)
    }

    override fun onBindViewHolder(holder: AllergenViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AllergenViewHolder(
        private val binding: ItemAllergenToggleBinding,
        private val onToggleChanged: (allergen: Allergen, isEnabled: Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(allergen: Allergen) {
            binding.textAllergenName.text = allergen.name

            binding.switchAllergenEnabled.setOnCheckedChangeListener(null)
            binding.switchAllergenEnabled.isChecked = allergen.isEnabled
            binding.switchAllergenEnabled.setOnCheckedChangeListener { _, isChecked ->
                if (bindingAdapterPosition != RecyclerView.NO_POSITION && isChecked != allergen.isEnabled) {
                    onToggleChanged(allergen, isChecked)
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Allergen>() {
        override fun areItemsTheSame(oldItem: Allergen, newItem: Allergen): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Allergen, newItem: Allergen): Boolean {
            return oldItem == newItem
        }
    }
}