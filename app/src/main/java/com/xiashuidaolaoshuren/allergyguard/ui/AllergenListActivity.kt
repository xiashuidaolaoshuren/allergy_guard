package com.xiashuidaolaoshuren.allergyguard.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.xiashuidaolaoshuren.allergyguard.R
import com.xiashuidaolaoshuren.allergyguard.data.AppDatabase
import com.xiashuidaolaoshuren.allergyguard.data.RoomAllergenRepository
import com.xiashuidaolaoshuren.allergyguard.databinding.ActivityAllergenListBinding
import com.xiashuidaolaoshuren.allergyguard.ui.allergen.AllergenListViewModel
import com.xiashuidaolaoshuren.allergyguard.ui.allergen.AllergenToggleAdapter
import kotlinx.coroutines.launch

class AllergenListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllergenListBinding
    private lateinit var allergenAdapter: AllergenToggleAdapter

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
        observeAllergens()
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
}