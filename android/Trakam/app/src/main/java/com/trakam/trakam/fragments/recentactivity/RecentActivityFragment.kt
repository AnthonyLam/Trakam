package com.trakam.trakam.fragments.recentactivity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.net.toUri
import com.squareup.picasso.Picasso
import com.trakam.trakam.R
import com.trakam.trakam.activities.ImagePreviewActivity
import com.trakam.trakam.activities.SettingsActivity
import com.trakam.trakam.data.Log
import com.trakam.trakam.fragments.base.BaseFragment
import com.trakam.trakam.services.OnLogEventListener
import com.trakam.trakam.services.ServerPollingService
import com.trakam.trakam.util.*
import java.io.File
import java.io.IOException
import java.text.DateFormat

class RecentActivityFragment : BaseFragment(), OnLogEventListener, View.OnClickListener {

    companion object {
        val TAG = RecentActivityFragment::class.qualifiedName
        private const val REQ_CAMERA = 1
        private const val TEMP_FILE_NAME = "pic.jpg"
        private const val PICS_DIR = "pics"
    }

    private lateinit var mRecyclerViewAdapter: MyRecyclerViewAdapter
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    private lateinit var mNoActivityMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mRecyclerViewAdapter = MyRecyclerViewAdapter(activity!!, this)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View =
            inflateLayout(R.layout.frag_recent_activity)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mNoActivityMessage = view.findViewById(R.id.noActivityMessage)

        mRecyclerView = view.findViewById(R.id.recyclerView)
        mLinearLayoutManager = LinearLayoutManager(view.context)
        mRecyclerView.layoutManager = mLinearLayoutManager
        mRecyclerView.itemAnimator = null
        mRecyclerView.addItemDecoration(ListDividerItemDecoration(activity!!,
                ListDividerItemDecoration.VERTICAL_LIST,
                activity!!.dipToPix(16.0f + 40.0f + 16.0f).toInt()))
        mRecyclerView.adapter = mRecyclerViewAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.recent_activity_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_person -> {
                startCamera()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(activity!!, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(activity!!.packageManager) != null) {
            val picsDir = File(activity!!.filesDir, PICS_DIR)
            if (!picsDir.exists()) {
                if (!picsDir.mkdir()) {
                    MyLogger.logError(this::class, "Failed to create pics dir")
                    return
                }
            }

            try {
                val file = File(picsDir, TEMP_FILE_NAME)
                if (file.exists()) {
                    if (!file.delete()) {
                        MyLogger.logError(this::class, "Failed to delete existing tmp pic")
                        return
                    }
                }

                val uri = FileProvider.getUriForFile(activity!!,
                        "${activity!!.packageName}.fileprovider", file)
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                startActivityForResult(cameraIntent, REQ_CAMERA)
            } catch (e: IOException) {
                MyLogger.logError(this::class, "Failed to create tmp file: ${e.message}")
            }
        } else {
            activity!!.showToast("Camera app not found")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQ_CAMERA -> {
                if (resultCode == Activity.RESULT_OK) {
                    val file = File(File(activity!!.filesDir, PICS_DIR), TEMP_FILE_NAME)
                    if (file.exists() && file.length() > 0) {
                        val intent = Intent(activity!!, ImagePreviewActivity::class.java)
                        intent.data = file.absolutePath.toUri()
                        startActivity(intent)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStop() {
        getServerPollingService()?.setOnLogEventListener(null)
        super.onStop()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.recent_activity_list_root -> {
                val pos = mLinearLayoutManager.getPosition(v)
                if (pos != RecyclerView.NO_POSITION) {
                    val host = activity!!.getDefaultSharedPreferences()
                            .getString(PrefKeys.Server.KEY_SERVER_HOST,
                                    PrefKeys.Server.Default.SERVER_HOST)
                    val port = activity!!.getDefaultSharedPreferences()
                            .getString(PrefKeys.Server.KEY_SERVER_PORT,
                                    PrefKeys.Server.Default.SERVER_PORT)
                    val url = "http://$host:$port/%s.jpg".format(mRecyclerViewAdapter[pos].log.uuid)
                    EventPictureViewerDialogFragment.newInstance(url)
                            .show(fragmentManager, EventPictureViewerDialogFragment.TAG)
                }
            }
        }
    }

    override fun onServerPollingServiceBound(serverPollingService: ServerPollingService) {
        serverPollingService.pause()

        updateList(serverPollingService.getLogs())
        serverPollingService.setOnLogEventListener(this)

        serverPollingService.resume()
    }

    private fun updateList(logs: List<Log>) {
        mRecyclerViewAdapter.setItems(logs.map { ListItem(it) })
        if (mRecyclerViewAdapter.itemCount > 0) {
            mNoActivityMessage.visibility = View.GONE
        }
    }

    override fun onLogsEvent(logs: List<Log>) {
        updateList(logs)
        if ((activity as AppCompatActivity).supportActionBar?.subtitle != null) {
            (activity as AppCompatActivity).supportActionBar?.subtitle = null
        }
    }

    override fun onServerError() {
        (activity as AppCompatActivity).supportActionBar?.subtitle = getString(R.string.server_error)
    }
}

internal class ListItem(val log: Log) {
}

private class MyRecyclerViewAdapter(private val context: Context,
                                    private val onClickListener: View.OnClickListener) :
        RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder>() {

    private val mData = mutableListOf<ListItem>()
    private val mTransformation: BitmapTransformation = BitmapTransformation.Builder(context)
            .setBorderEnabled(false)
            .build()

    private val mUrl: String

    init {
        val host = context.getDefaultSharedPreferences().getString(PrefKeys.Server.KEY_SERVER_HOST,
                PrefKeys.Server.Default.SERVER_HOST)
        val port = context.getDefaultSharedPreferences().getString(PrefKeys.Server.KEY_SERVER_PORT,
                PrefKeys.Server.Default.SERVER_PORT)
        mUrl = "http://$host:$port/%s.jpg"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.frag_recent_activity_list_item,
                        parent, false)
        view.setOnClickListener(onClickListener)
        return MyViewHolder(view)
    }

    fun setItems(items: List<ListItem>) {
        mData.clear()
        mData += items
        notifyDataSetChanged()
    }

    operator fun get(index: Int) = mData[index]

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

        Picasso.with(context)
                .cancelRequest(holder.pic)

        Picasso.with(context)
                .load(mUrl.format(listItem.log.uuid))
                .transform(mTransformation)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.ic_account_circle_grey_700_48dp)
                .into(holder.pic)
    }

    private class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pic: ImageView = itemView.findViewById(R.id.imageView)
        val name: TextView = itemView.findViewById(R.id.name)
        val timeStamp: TextView = itemView.findViewById(R.id.timeStamp)
    }
}

internal class EventPictureViewerDialogFragment : DialogFragment() {
    companion object {
        val TAG = EventPictureViewerDialogFragment::class.qualifiedName
        private val KEY_URL = TAG + "_key_url"

        fun newInstance(url: String): EventPictureViewerDialogFragment {
            val args = Bundle()
            args.putString(KEY_URL, url)

            val frag = EventPictureViewerDialogFragment()
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflateLayout(R.layout.event_picture_viewer_dialog)
        val imageView = view.findViewById<ImageView>(R.id.imageView)

        val url = arguments!!.getString(KEY_URL)
        Picasso.with(activity!!)
                .load(url)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.ic_account_circle_grey_700_48dp)
                .into(imageView)

        return AlertDialog.Builder(activity!!)
                .setView(view)
                .create()
    }
}