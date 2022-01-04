package io.laplante.kmd_app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.laplante.kmd_app.databinding.RecyclerviewScanItemBinding
import java.lang.Long.parseLong
import javax.inject.Inject

// TODO: use interface instead of the actual view model class
class AdvertisedDeviceAdapter @Inject constructor(private val viewModel: BleViewModel) :
    ListAdapter<AdvertisementWrapper, AdvertisedDeviceAdapter.BridgeViewHolder>(
        StringComparator()
    ) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return parseLong(this.getItem(position).address.replace(":", ""), 16)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BridgeViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding: RecyclerviewScanItemBinding =
            RecyclerviewScanItemBinding.inflate(layoutInflater, parent, false)

        return BridgeViewHolder.create(itemBinding, this.viewModel)
    }

    override fun onBindViewHolder(holder: BridgeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BridgeViewHolder constructor(
        private val binding: RecyclerviewScanItemBinding,
        private val viewModel: BleViewModel
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ble_peripheral: AdvertisementWrapper) {
            binding.obj = ble_peripheral
            binding.viewModel = viewModel
            binding.executePendingBindings();
        }

        companion object {
            fun create(
                parent: RecyclerviewScanItemBinding,
                viewModel: BleViewModel
            ): BridgeViewHolder {
                return BridgeViewHolder(parent, viewModel)
            }
        }
    }

    class StringComparator : DiffUtil.ItemCallback<AdvertisementWrapper>() {
        override fun areItemsTheSame(
            oldItem: AdvertisementWrapper,
            newItem: AdvertisementWrapper
        ): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(
            oldItem: AdvertisementWrapper,
            newItem: AdvertisementWrapper
        ): Boolean {
            return oldItem == newItem
        }
    }
}
