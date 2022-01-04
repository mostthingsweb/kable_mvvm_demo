package io.laplante.kmd_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import dagger.hilt.android.AndroidEntryPoint
import io.laplante.kmd_app.databinding.FragmentScanBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val vm: BleViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        binding.viewModel = vm
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemAdapter = ItemAdapter<AdvertisedDeviceItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        binding.recyclerView2.adapter = fastAdapter
        binding.recyclerView2.layoutManager = LinearLayoutManager(binding.root.context)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    vm.advertisements.collect {
                        itemAdapter.setNewList(it.map { item ->
                            AdvertisedDeviceItem(item, vm)
                        })
                    }
                }
                launch {
                    vm.onConnectEventFlow.collect {
                        this@ScanFragment.findNavController()
                            .navigate(ScanFragmentDirections.actionScanFragmentToSecondFragment())
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}