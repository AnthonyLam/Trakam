package com.trakam.trakam.fragments.recentactivity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.net.toUri
import com.github.niqdev.mjpeg.DisplayMode
import com.github.niqdev.mjpeg.Mjpeg
import com.github.niqdev.mjpeg.MjpegSurfaceView
import com.microsoft.projectoxford.face.FaceServiceRestClient
import com.microsoft.projectoxford.face.rest.ClientException
import com.squareup.picasso.Picasso
import com.trakam.trakam.R
import com.trakam.trakam.activities.ImagePreviewActivity
import com.trakam.trakam.activities.SettingsActivity
import com.trakam.trakam.data.Log
import com.trakam.trakam.db.AppDatabase
import com.trakam.trakam.db.PeopleDao
import com.trakam.trakam.db.Person
import com.trakam.trakam.fragments.base.BaseFragment
import com.trakam.trakam.services.OnLogEventListener
import com.trakam.trakam.services.ServerPollingService
import com.trakam.trakam.util.*
import okhttp3.Request
import rx.Subscription
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.DateFormat

class RecentActivityFragment : BaseFragment(), OnLogEventListener, View.OnClickListener {

    companion object {
        val TAG = RecentActivityFragment::class.qualifiedName
        private const val REQ_CAMERA = 1
        private const val TEMP_FILE_NAME = "pic.jpg"
        private const val PICS_DIR = "pics"
        private const val STREAM_URL = "http://192.168.0.189:8090/?action=stream"
    }

    private lateinit var mRecyclerViewAdapter: MyRecyclerViewAdapter
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    private lateinit var mNoActivityMessage: TextView
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mLiveFeedError: TextView
    private lateinit var mMjpegView: MjpegSurfaceView
    private lateinit var mMjpeg: Mjpeg
    private lateinit var mSubscription: Subscription

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mRecyclerViewAdapter = MyRecyclerViewAdapter(activity!!, this)
        mMjpeg = Mjpeg.newInstance()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view = inflateLayout(R.layout.frag_recent_activity)

        mMjpegView = view.findViewById(R.id.mjpegView)

        mNoActivityMessage = view.findViewById(R.id.noActivityMessage)
        mLiveFeedError = view.findViewById(R.id.liveFeedError)
        mProgressBar = view.findViewById(R.id.progressBar)

        mRecyclerView = view.findViewById(R.id.recyclerView)
        mLinearLayoutManager = LinearLayoutManager(view.context)
        mRecyclerView.layoutManager = mLinearLayoutManager
        mRecyclerView.itemAnimator = null
        mRecyclerView.addItemDecoration(ListDividerItemDecoration(activity!!,
                ListDividerItemDecoration.VERTICAL_LIST,
                activity!!.dipToPix(16.0f + 40.0f + 16.0f).toInt()))
        mRecyclerView.adapter = mRecyclerViewAdapter

        mLiveFeedError.setOnClickListener {
            mProgressBar.visibility = View.VISIBLE
            mLiveFeedError.visibility = View.GONE

            mSubscription.unsubscribe()
            startStreaming()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        startStreaming()
    }

    override fun onPause() {
        mMjpegView.stopPlayback()
        mSubscription.unsubscribe()
        super.onPause()
    }

    private fun startStreaming() {
        mSubscription = mMjpeg.open(STREAM_URL)
                .subscribe({
                    mMjpegView.setSource(it)
                    mMjpegView.setDisplayMode(DisplayMode.BEST_FIT)

                    mLiveFeedError.visibility = View.GONE
                    mProgressBar.visibility = View.GONE
                }, {
                    mLiveFeedError.visibility = View.VISIBLE
                    mProgressBar.visibility = View.GONE
                })
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

    override fun onStop() {
        getServerPollingService()?.setOnLogEventListener(null)
        super.onStop()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.recent_activity_list_root -> {
                val pos = mLinearLayoutManager.getPosition(v)
                if (pos != RecyclerView.NO_POSITION) {
                    EventPictureViewerDialogFragment.newInstance(mRecyclerViewAdapter[pos].log)
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
    private val mRegularTextColor: Int
    private val mBlackListTextColor: Int

    init {
        val host = context.getDefaultSharedPreferences().getString(PrefKeys.Server.KEY_SERVER_HOST,
                PrefKeys.Server.Default.SERVER_HOST)
        val port = context.getDefaultSharedPreferences().getString(PrefKeys.Server.KEY_SERVER_PORT,
                PrefKeys.Server.Default.SERVER_PORT)
        mUrl = "http://$host:$port/%s.jpg"

        mRegularTextColor = context.getAttrColor(R.attr.primary_text_color)
        mBlackListTextColor = Color.parseColor("#C2185B")
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

        if (listItem.log.blacklisted) {
            holder.name.setTextColor(mBlackListTextColor)
        } else {
            holder.name.setTextColor(mRegularTextColor)
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

        fun newInstance(log: Log): EventPictureViewerDialogFragment {
            val frag = EventPictureViewerDialogFragment()
            frag.mLog = log
            return frag
        }
    }

    private lateinit var mLog: Log

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflateLayout(R.layout.event_picture_viewer_dialog)
        val imageView = view.findViewById<ImageView>(R.id.imageView)

        val host = activity!!.getDefaultSharedPreferences()
                .getString(PrefKeys.Server.KEY_SERVER_HOST,
                        PrefKeys.Server.Default.SERVER_HOST)
        val port = activity!!.getDefaultSharedPreferences()
                .getString(PrefKeys.Server.KEY_SERVER_PORT,
                        PrefKeys.Server.Default.SERVER_PORT)
        val url = "http://$host:$port/%s.jpg".format(mLog.uuid)

        Picasso.with(activity!!)
                .load(url)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.ic_account_circle_grey_700_48dp)
                .into(imageView)

        val dialogBuilder = AlertDialog.Builder(activity!!)
                .setView(view)

        if (mLog.firstName == "Unknown person") {
            dialogBuilder.setPositiveButton("Whitelist", { _, _ ->
                GetNameDialogFragment.newInstance(mLog, false)
                        .show(fragmentManager, GetNameDialogFragment.TAG)
            })

            dialogBuilder.setNegativeButton("Blacklist", { _, _ ->
                GetNameDialogFragment.newInstance(mLog, true)
                        .show(fragmentManager, GetNameDialogFragment.TAG)
            })
        }

        return dialogBuilder.create()
    }
}


class GetNameDialogFragment : DialogFragment() {
    companion object {
        val TAG = GetNameDialogFragment::class.qualifiedName

        fun newInstance(log: Log, blackList: Boolean): GetNameDialogFragment {
            val frag = GetNameDialogFragment()
            frag.mLog = log
            frag.mMarkAsBlacklist = blackList
            return frag
        }
    }

    private lateinit var mLog: Log
    private var mMarkAsBlacklist = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflateLayout(R.layout.get_name_dialog)
        val editText = view.findViewById<EditText>(R.id.editText)
        isCancelable = false
        return AlertDialog.Builder(activity!!)
                .setTitle(getString(R.string.name))
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, { _, _ ->
                    val name = editText.text.toString()
                    if (name.isEmpty()) {
                        activity!!.showToast("Name is empty!")
                    } else {
                        SendToAzureProgressDialog.newInstance(mLog, name, mMarkAsBlacklist)
                                .show(fragmentManager, SendToAzureProgressDialog.TAG)
                    }
                    dismiss()
                })
                .setNegativeButton(android.R.string.cancel, { _, _ ->
                    dismiss()
                })
                .create()
    }
}

internal class SendToAzureProgressDialog : DialogFragment() {
    companion object {
        val TAG = SendToAzureProgressDialog::class.qualifiedName

        fun newInstance(log: Log, newName: String, blackList: Boolean): SendToAzureProgressDialog {
            val frag = SendToAzureProgressDialog()
            frag.mLog = log
            frag.mMarkBlacklist = blackList
            frag.mName = newName
            return frag
        }
    }

    private lateinit var mName: String
    private lateinit var mLog: Log
    private var mMarkBlacklist: Boolean = false
    private var mSendTask: SendTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflateLayout(R.layout.progress_dialog)
        val textView = view.findViewById<TextView>(R.id.message)
        textView.text = "Processing..."

        isCancelable = false
        return AlertDialog.Builder(activity!!)
                .setView(view)
                .setCancelable(false)
                .create()
    }

    override fun onStart() {
        super.onStart()
        if (mSendTask == null) {
            mSendTask = SendTask(this, mLog, mName, mMarkBlacklist)
            mSendTask?.execute()
        }
    }

    class SendTask(frag: SendToAzureProgressDialog,
                   log: Log,
                   private val mNewName: String,
                   private val mMarkAsBlacklist: Boolean) : AsyncTask<Any, Any, Pair<Boolean, String>>() {

        private val mUrl: String
        private val mFragRef = WeakReference(frag)
        private val mDao: PeopleDao

        init {
            val host = frag.activity!!.getDefaultSharedPreferences()
                    .getString(PrefKeys.Server.KEY_SERVER_HOST,
                            PrefKeys.Server.Default.SERVER_HOST)
            val port = frag.activity!!.getDefaultSharedPreferences()
                    .getString(PrefKeys.Server.KEY_SERVER_PORT,
                            PrefKeys.Server.Default.SERVER_PORT)
            mUrl = "http://$host:$port/%s-face.jpg".format(log.uuid)

            mDao = AppDatabase.getInstance(frag.activity!!).peopleDao()
        }

        override fun doInBackground(vararg params: Any): Pair<Boolean, String> {
            // get the face
            val trainResult = trainFace()
            if (trainResult.first && mMarkAsBlacklist) {
                val name = mNewName.trim()
                if (name.contains(' ')) {
                    val firstNameLastName = StringSplitter.splitOnEmptySequence(name)
                    mDao.insert(Person(firstName = firstNameLastName[0], lastName = firstNameLastName[1]))
                } else {
                    mDao.insert(Person(firstName = name, lastName = ""))
                }
                return Pair(true, "")
            } else {
                return trainResult
            }
        }

        private fun trainFace(): Pair<Boolean, String> {
            val req = Request.Builder()
                    .url(mUrl)
                    .build()

            val getFaceResult = ServerUtil.makeRequest(req) {
                val bytes = it.body()?.bytes()
                it.body()?.close()
                bytes
            }
            if (!getFaceResult.success || getFaceResult.data == null) {
                return Pair(false, "")
            }

            try {
                val faceServiceClient = FaceServiceRestClient(FaceAPI.SUB_KEY)
                val result = faceServiceClient.createPerson(FaceAPI.PERSON_GROUP_ID,
                        mNewName, "")
                val inputStream = ByteArrayInputStream(getFaceResult.data)

                faceServiceClient.addPersonFace(FaceAPI.PERSON_GROUP_ID,
                        result.personId, inputStream,
                        "", null)
                faceServiceClient.trainPersonGroup(FaceAPI.PERSON_GROUP_ID)
                return Pair(true, "")
            } catch (e: ClientException) {
                return Pair(false, e.message ?: "")
            } catch (e: IOException) {
                return Pair(false, "")
            }
        }

        override fun onPostExecute(result: Pair<Boolean, String>) {
            val frag = mFragRef.get() ?: return

            if (frag.activity == null) {
                return
            }

            val (success, errMsg) = result
            if (success) {
                frag.activity!!.showToast("Sent!")
            } else {
                if (errMsg.isNotEmpty()) {
                    frag.activity!!.showToast(errMsg)
                } else {
                    frag.activity!!.showToast("Send failed!")
                }
            }
            frag.dismiss()
        }
    }

}