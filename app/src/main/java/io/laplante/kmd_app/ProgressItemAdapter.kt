package io.laplante.kmd_app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import javax.inject.Inject
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.withNullability

// This class uses reflection in a proof-of-concept manner so that the ListAdapter can live in this
// library module and be re-used in applications. I hate having to write a new ListAdapter every time.
class ProgressItemAdapter<B : ViewDataBinding> @Inject constructor(
    private val klass: KClass<B>,
) :
    ListAdapter<ProgressItem, ProgressItemAdapter.ItemViewHolder<B>>(
        StringComparator()
    ) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    private val _viewBindingInvokeMethod = klass.members.first {
        it.name == "inflate" && it.parameters.size == 1 && it.parameters.first().type.withNullability(
            false
        ) == LayoutInflater::class.createType()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder<B> {
        val layoutInflater = LayoutInflater.from(parent.context)

        @Suppress("UNCHECKED_CAST")
        val itemBinding = _viewBindingInvokeMethod.call(layoutInflater) as B

        val viewBindingItemSetMethod = klass.members.first {
            it.name.startsWith("set") && it.parameters[1].type.withNullability(false) == ProgressItem::class.createType()
        }

        return ItemViewHolder.create(itemBinding, viewBindingItemSetMethod)
    }

    override fun onBindViewHolder(holder: ItemViewHolder<B>, position: Int) {
        holder.bind(getItem(position))
    }

    class ItemViewHolder<B : ViewDataBinding> constructor(
        private val binding: B,
        private val viewBindingItemSetMethod: KCallable<*>
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(itemAdapter: ProgressItem) {
            viewBindingItemSetMethod.call(binding, itemAdapter)
            binding.executePendingBindings();
        }

        companion object {
            fun <B : ViewDataBinding> create(
                parent: B,
                viewBindingItemSetMethod: KCallable<*>
            ): ItemViewHolder<B> {
                return ItemViewHolder(parent, viewBindingItemSetMethod)
            }
        }
    }

    class StringComparator : DiffUtil.ItemCallback<ProgressItem>() {
        override fun areItemsTheSame(
            oldItem: ProgressItem,
            newItem: ProgressItem
        ): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(
            oldItem: ProgressItem,
            newItem: ProgressItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
