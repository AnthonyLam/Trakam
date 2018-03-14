package com.trakam.trakam.activities

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.microsoft.projectoxford.face.FaceServiceRestClient
import com.microsoft.projectoxford.face.rest.ClientException
import com.squareup.picasso.Picasso
import com.trakam.trakam.R
import com.trakam.trakam.db.AppDatabase
import com.trakam.trakam.db.PeopleDao
import com.trakam.trakam.db.Person
import com.trakam.trakam.util.*
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.IOException
import java.lang.ref.WeakReference

class UnknownPersonActivity : BaseActivity() {

    companion object {
        val TAG = UnknownPersonActivity::class.qualifiedName
        val EXTRA_UUID = TAG + "_extra_uuid"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unknown_handler)
        enableNavigateUp()

        val imageView = findViewById<ImageView>(R.id.imageView)
        val blacklistBtn = findViewById<Button>(R.id.btnBlacklist)
        val whitelistBtn = findViewById<Button>(R.id.btnWhitelist)

        val host = getDefaultSharedPreferences()
                .getString(PrefKeys.Server.KEY_SERVER_HOST,
                        PrefKeys.Server.Default.SERVER_HOST)
        val port = getDefaultSharedPreferences()
                .getString(PrefKeys.Server.KEY_SERVER_PORT,
                        PrefKeys.Server.Default.SERVER_PORT)

        val uuid = intent.getStringExtra(EXTRA_UUID)
        val url = "http://$host:$port/%s.jpg".format(uuid)
        Picasso.with(this)
                .load(url)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.ic_account_circle_grey_700_48dp)
                .into(imageView)

        blacklistBtn.setOnClickListener {
            GetNameDialogFragment.newInstance(uuid, true)
                    .show(fragmentManager, GetNameDialogFragment.TAG)
        }

        whitelistBtn.setOnClickListener {
            GetNameDialogFragment.newInstance(uuid, false)
                    .show(fragmentManager, GetNameDialogFragment.TAG)
        }
    }

    class GetNameDialogFragment : DialogFragment() {
        companion object {
            val TAG = GetNameDialogFragment::class.qualifiedName

            fun newInstance(uuid: String, blackList: Boolean): GetNameDialogFragment {
                val frag = GetNameDialogFragment()
                frag.mUuid = uuid
                frag.mMarkAsBlacklist = blackList
                return frag
            }
        }

        private lateinit var mUuid: String
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
                            SendToAzureProgressDialog.newInstance(mUuid, name, mMarkAsBlacklist)
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

            fun newInstance(uuid: String, newName: String, blackList: Boolean):
                    SendToAzureProgressDialog {
                val frag = SendToAzureProgressDialog()
                frag.mUuid = uuid
                frag.mMarkBlacklist = blackList
                frag.mName = newName
                return frag
            }
        }

        private lateinit var mName: String
        private lateinit var mUuid: String
        private var mMarkBlacklist: Boolean = false
        private var mSendTask: SendTask? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            retainInstance = true
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val view = inflateLayout(R.layout.progress_dialog)
            val textView = view.findViewById<TextView>(R.id.message)

            if (mMarkBlacklist) {
                textView.text = "Adding to the blacklist..."
            } else {
                textView.text = "Adding to the whitelist..."
            }

            isCancelable = false
            return AlertDialog.Builder(activity!!)
                    .setView(view)
                    .setCancelable(false)
                    .create()
        }

        override fun onStart() {
            super.onStart()
            if (mSendTask == null) {
                mSendTask = SendTask(this, mUuid, mName, mMarkBlacklist)
                mSendTask?.execute()
            }
        }

        class SendTask(frag: SendToAzureProgressDialog,
                       uuid: String,
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
                mUrl = "http://$host:$port/%s-face.jpg".format(uuid)

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
                    frag.activity!!.showToast("Success")
                    frag.activity!!.finish()
                } else {
                    if (errMsg.isNotEmpty()) {
                        frag.activity!!.showToast(errMsg)
                    } else {
                        frag.activity!!.showToast("Fail")
                    }
                }
                frag.dismiss()
            }
        }
    }
}
