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
import androidx.recyclerview.widget.LinearLayoutManager
import io.laplante.kmd.BleViewModel
import io.laplante.kmd.ProgressItemAdapter
import io.laplante.kmd_app.databinding.FragmentConnectBinding
import io.laplante.kmd_app.databinding.RecyclerviewProgressItemBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ConnectFragment : Fragment() {

    private var _binding: FragmentConnectBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val vm: BleViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ProgressItemAdapter(RecyclerviewProgressItemBinding::class)

        binding.progressItemsRecycler.adapter = adapter
        binding.progressItemsRecycler.layoutManager = LinearLayoutManager(binding.root.context)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.progressItems.collect { adapter.submitList(it) } }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}