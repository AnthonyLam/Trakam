package com.trakam.trakam.fragments.recentactivity

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import com.trakam.trakam.R
import com.trakam.trakam.data.Log
import com.trakam.trakam.fragments.base.BaseFragment
import com.trakam.trakam.services.OnLogEventListener
import com.trakam.trakam.services.ServerPollingService
import com.trakam.trakam.util.inflateLayout
import java.text.DateFormat

class RecentActivityFragment : BaseFragment(), OnLogEventListener {
    companion object {

        val TAG = RecentActivityFragment::class.qualifiedName

    }

    private lateinit var mRecyclerViewAdapter: MyRecyclerViewAdapter
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mLinearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mRecyclerViewAdapter = MyRecyclerViewAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View =
            inflateLayout(R.layout.frag_recent_activity)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mLinearLayoutManager = LinearLayoutManager(view.context)

        mRecyclerView = view.findViewById(R.id.recyclerView)
        mRecyclerView.layoutManager = mLinearLayoutManager
        mRecyclerView.itemAnimator = null
        mRecyclerView.addItemDecoration(DividerItemDecoration(view.context,
                DividerItemDecoration.VERTICAL))
        mRecyclerView.adapter = mRecyclerViewAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.recent_activity_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_whitelist -> {
                true
            }
            R.id.action_blacklist -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStop() {
        getServerPollingService()?.setOnLogEventListener(null)
        super.onStop()
    }

    override fun onServerPollingServiceBound(serverPollingService: ServerPollingService) {
        serverPollingService.pause()

        mRecyclerViewAdapter.setItems(serverPollingService.getLogs().map { ListItem(it) })
        serverPollingService.setOnLogEventListener(this)

        serverPollingService.resume()
    }

    override fun onLogsEvent(logs: List<Log>) {
        mRecyclerViewAdapter.setItems(logs.map { ListItem(it) })
    }
}

internal class ListItem(val log: Log) {
}

private class MyRecyclerViewAdapter : RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder>() {
    private val mData = mutableListOf<ListItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.frag_recent_activity_list_item,
                        parent, false)
        return MyViewHolder(view)
    }

    fun add(listItem: ListItem) {
        mData += listItem
        notifyItemInserted(mData.size - 1)
    }

    fun clear() {
        val size = mData.size
        mData.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun setItems(items: List<ListItem>) {
        clear()
        mData += items
        notifyItemRangeInserted(0, items.size)
    }

    override fun getItemCount() = mData.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val listItem = mData[position]
        if (listItem.log.lastName.isNotEmpty()) {
            holder.name.text = "${listItem.log.firstName} ${listItem.log.lastName}"
        } else {
            holder.name.text = listItem.log.firstName
        }
        holder.timeStamp.text = DateFormat.getDateTimeInstance().format(listItem.log.timestamp)
    }

    private class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.name)
        val timeStamp: TextView = itemView.findViewById(R.id.timeStamp)
    }
}