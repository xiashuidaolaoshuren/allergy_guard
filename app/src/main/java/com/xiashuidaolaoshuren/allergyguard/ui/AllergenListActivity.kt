package com.xiashuidaolaoshuren.allergyguard.ui

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xiashuidaolaoshuren.allergyguard.R
import com.xiashuidaolaoshuren.allergyguard.data.AppDatabase
import com.xiashuidaolaoshuren.allergyguard.data.RoomAllergenRepository
import com.xiashuidaolaoshuren.allergyguard.databinding.ActivityAllergenListBinding
import com.xiashuidaolaoshuren.allergyguard.databinding.DialogAddCustomAllergenBinding
import com.xiashuidaolaoshuren.allergyguard.ui.allergen.AllergenListViewModel
import com.xiashuidaolaoshuren.allergyguard.ui.allergen.AllergenToggleAdapter
import kotlinx.coroutines.launch

class AllergenListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllergenListBinding
    private lateinit var allergenAdapter: AllergenToggleAdapter
    private var addCustomAllergenDialog: AlertDialog? = null
    private var addCustomAllergenDialogBinding: DialogAddCustomAllergenBinding? = null

    private val viewModel: AllergenListViewModel by viewModels {
        val allergenDao = AppDatabase.getInstance(applicationContext).allergenDao()
        val repository = RoomAllergenRepository(allergenDao)
        AllergenListViewModel.Factory(repository)
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
        allergenAdapter = AllergenToggleAdapter { allergen, isEnabled ->
            viewModel.onAllergenToggled(allergen.id, isEnabled)
        }
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
                            addCustomAllergenDialogBinding?.textInputCustomAllergen?.error = getString(messageResId)
                        }
                    }
                }
            }
        }
    }
}