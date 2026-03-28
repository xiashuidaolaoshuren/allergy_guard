package com.xiashuidaolaoshuren.allergyguard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xiashuidaolaoshuren.allergyguard.R
import com.xiashuidaolaoshuren.allergyguard.data.Allergen
import com.xiashuidaolaoshuren.allergyguard.data.AllergenAlias
import com.xiashuidaolaoshuren.allergyguard.data.AppDatabase
import com.xiashuidaolaoshuren.allergyguard.data.RoomAllergenAliasRepository
import com.xiashuidaolaoshuren.allergyguard.data.RoomAllergenRepository
import com.xiashuidaolaoshuren.allergyguard.databinding.ActivityAllergenListBinding
import com.xiashuidaolaoshuren.allergyguard.databinding.DialogAddCustomAllergenBinding
import com.xiashuidaolaoshuren.allergyguard.databinding.DialogManageAliasesBinding
import com.xiashuidaolaoshuren.allergyguard.databinding.ItemAllergenAliasBinding
import com.xiashuidaolaoshuren.allergyguard.logic.AllergenSynonymMap
import com.xiashuidaolaoshuren.allergyguard.ui.allergen.AllergenListViewModel
import com.xiashuidaolaoshuren.allergyguard.ui.allergen.AllergenToggleAdapter
import kotlinx.coroutines.launch

class AllergenListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllergenListBinding
    private lateinit var allergenAdapter: AllergenToggleAdapter
    private var addCustomAllergenDialog: AlertDialog? = null
    private var addCustomAllergenDialogBinding: DialogAddCustomAllergenBinding? = null

    private val viewModel: AllergenListViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val repository = RoomAllergenRepository(database.allergenDao())
        val aliasRepository = RoomAllergenAliasRepository(database.allergenAliasDao())
        AllergenListViewModel.Factory(repository, aliasRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAllergenListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.allergen_list_title)
        setupInsets()
        setupRecyclerView()
        setupActions()
        observeAllergens()
        observeAddCustomAllergenEvents()
    }

    override fun onDestroy() {
        addCustomAllergenDialog?.dismiss()
        addCustomAllergenDialog = null
        addCustomAllergenDialogBinding = null
        super.onDestroy()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.allergenListRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        allergenAdapter = AllergenToggleAdapter(
            onToggleChanged = { allergen, isEnabled ->
                viewModel.onAllergenToggled(allergen.id, isEnabled)
            },
            onSynonymsClicked = { allergen ->
                showSynonymsDialog(allergen)
            },
            onDeleteClicked = { allergen ->
                showDeleteCustomAllergenDialog(allergen)
            }
        )
        binding.recyclerAllergens.apply {
            layoutManager = LinearLayoutManager(this@AllergenListActivity)
            adapter = allergenAdapter
        }
    }

    private fun observeAllergens() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allergens.collect { allergens ->
                    allergenAdapter.submitList(allergens)
                }
            }
        }
    }

    private fun setupActions() {
        binding.fabAddCustomAllergen.setOnClickListener {
            showAddCustomAllergenDialog()
        }
    }

    private fun showAddCustomAllergenDialog() {
        addCustomAllergenDialogBinding = DialogAddCustomAllergenBinding.inflate(layoutInflater)
        val dialogBinding = addCustomAllergenDialogBinding ?: return

        dialogBinding.textInputCustomAllergen.error = null

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_custom_allergen)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_add, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                dialogBinding.textInputCustomAllergen.error = null
                val input = dialogBinding.editTextCustomAllergenName.text?.toString().orEmpty()
                viewModel.addCustomAllergen(input)
            }
        }

        addCustomAllergenDialog?.dismiss()
        addCustomAllergenDialog = dialog
        dialog.show()
    }

    private fun observeAddCustomAllergenEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addCustomAllergenEvents.collect { event ->
                    when (event) {
                        AllergenListViewModel.AddCustomAllergenEvent.Added -> {
                            addCustomAllergenDialog?.dismiss()
                            addCustomAllergenDialog = null
                            addCustomAllergenDialogBinding = null
                        }

                        is AllergenListViewModel.AddCustomAllergenEvent.ValidationError -> {
                            val messageResId = when (event.error) {
                                AllergenListViewModel.AddCustomAllergenError.BLANK -> R.string.error_custom_allergen_blank
                                AllergenListViewModel.AddCustomAllergenError.DUPLICATE -> R.string.error_custom_allergen_duplicate
                            }
                            addCustomAllergenDialogBinding?.textInputCustomAllergen?.error =
                                getString(messageResId)
                        }
                    }
                }
            }
        }
    }

    private fun showDeleteCustomAllergenDialog(allergen: Allergen) {
        if (!allergen.isCustom) {
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_allergen_confirm_title)
            .setMessage(getString(R.string.delete_allergen_confirm_message, allergen.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteCustomAllergen(allergen.id)
            }
            .show()
    }

    //  Synonyms / Alias dialog 

    private fun showSynonymsDialog(allergen: Allergen) {
        val dialogBinding = DialogManageAliasesBinding.inflate(layoutInflater)
        val dialogTitle = getString(R.string.allergen_synonyms_title) + ": " + allergen.name

        if (!allergen.isCustom) {
            // Built-in allergen: read-only synonym chips
            dialogBinding.sectionBuiltinSynonyms.visibility = android.view.View.VISIBLE
            dialogBinding.sectionUserAliases.visibility = android.view.View.GONE
            AllergenSynonymMap.getSynonyms(allergen.name).forEach { synonym ->
                val chip = Chip(this).apply {
                    text = synonym
                    isClickable = false
                    isCheckable = false
                }
                dialogBinding.chipGroupSynonyms.addView(chip)
            }
            MaterialAlertDialogBuilder(this)
                .setTitle(dialogTitle)
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.action_ok, null)
                .show()
        } else {
            // Custom allergen: alias management
            dialogBinding.sectionBuiltinSynonyms.visibility = android.view.View.GONE
            dialogBinding.sectionUserAliases.visibility = android.view.View.VISIBLE

            val aliasAdapter = AliasAdapter { aliasId -> viewModel.deleteAlias(aliasId) }
            dialogBinding.recyclerAliases.apply {
                layoutManager = LinearLayoutManager(this@AllergenListActivity)
                adapter = aliasAdapter
            }

            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle(dialogTitle)
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.action_ok, null)
                .create()

            dialogBinding.buttonAddAlias.setOnClickListener {
                dialogBinding.textInputAlias.error = null
                val raw = dialogBinding.editTextAliasName.text?.toString().orEmpty()
                viewModel.addAlias(allergen.id, raw)
            }

            lifecycleScope.launch {
                viewModel.getAliasesForAllergen(allergen.id).collect { aliases ->
                    aliasAdapter.submitList(aliases)
                }
            }

            lifecycleScope.launch {
                viewModel.aliasEvents.collect { event ->
                    if (!dialog.isShowing) return@collect
                    when (event) {
                        AllergenListViewModel.AliasEvent.Added -> {
                            dialogBinding.editTextAliasName.text?.clear()
                            dialogBinding.textInputAlias.error = null
                        }
                        is AllergenListViewModel.AliasEvent.ValidationError -> {
                            val msgRes = when (event.error) {
                                AllergenListViewModel.AliasError.BLANK -> R.string.error_alias_blank
                                AllergenListViewModel.AliasError.DUPLICATE -> R.string.error_alias_duplicate
                            }
                            dialogBinding.textInputAlias.error = getString(msgRes)
                        }
                    }
                }
            }

            dialog.show()
        }
    }

    //  Inner adapter for alias rows inside the dialog 

    private class AliasAdapter(
        private val onDeleteClicked: (aliasId: String) -> Unit
    ) : ListAdapter<AllergenAlias, AliasAdapter.AliasViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AliasViewHolder {
            val binding = ItemAllergenAliasBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return AliasViewHolder(binding, onDeleteClicked)
        }

        override fun onBindViewHolder(holder: AliasViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class AliasViewHolder(
            private val binding: ItemAllergenAliasBinding,
            private val onDeleteClicked: (aliasId: String) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(alias: AllergenAlias) {
                binding.textAliasName.text = alias.alias
                binding.buttonDeleteAlias.setOnClickListener { onDeleteClicked(alias.id) }
            }
        }

        private object DiffCallback : DiffUtil.ItemCallback<AllergenAlias>() {
            override fun areItemsTheSame(oldItem: AllergenAlias, newItem: AllergenAlias) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: AllergenAlias, newItem: AllergenAlias) =
                oldItem == newItem
        }
    }
}
