package com.trakam.trakam.fragments.recentactivity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicColorMatrix
import android.support.v4.content.FileProvider
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.trakam.trakam.R
import com.trakam.trakam.data.Log
import com.trakam.trakam.fragments.base.BaseFragment
import com.trakam.trakam.services.OnLogEventListener
import com.trakam.trakam.services.ServerPollingService
import com.trakam.trakam.util.MyLogger
import com.trakam.trakam.util.inflateLayout
import com.trakam.trakam.util.showToast
import java.io.File
import java.io.IOException
import java.text.DateFormat

class RecentActivityFragment : BaseFragment(), OnLogEventListener {
    companion object {
        val TAG = RecentActivityFragment::class.qualifiedName
        private const val REQ_CAMERA = 1
        private const val TEMP_FILE_NAME = "pic.jpg"
        private const val PICS_DIR = "pics"
    }

    private lateinit var mRecyclerViewAdapter: MyRecyclerViewAdapter
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    private lateinit var mImageView: ImageView

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

        mImageView = view.findViewById(R.id.imageView)

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
            R.id.action_add_person -> {
                startCamera()
                true
            }
            R.id.action_whitelist -> {
                true
            }
            R.id.action_blacklist -> {
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
                        onPictureTaken(file)
                    }
                }
            }
        }
    }

    private fun onPictureTaken(file: File) {
        val input = BitmapFactory.decodeFile(file.absolutePath)
        convertBitmapToGrayScale(input)
    }

    private fun convertBitmapToGrayScale(input: Bitmap): Bitmap {
        val bitmapOutput = Bitmap.createBitmap(input)

        val rs = RenderScript.create(activity!!)
        val script = ScriptIntrinsicColorMatrix.create(rs)
        script.setGreyscale()

        val allocationInput = Allocation.createFromBitmap(rs, input)
        val allocationOutput = Allocation.createFromBitmap(rs, bitmapOutput)

        script.forEach(allocationInput, allocationOutput)

        allocationOutput.copyTo(bitmapOutput)
        mImageView.setImageBitmap(bitmapOutput)

        return bitmapOutput
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