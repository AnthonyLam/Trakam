package com.trakam.trakam.activities

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicColorMatrix
import android.support.design.widget.FloatingActionButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.microsoft.projectoxford.face.FaceServiceClient
import com.microsoft.projectoxford.face.FaceServiceRestClient
import com.microsoft.projectoxford.face.rest.ClientException
import com.trakam.trakam.R
import com.trakam.trakam.util.inflateLayout
import com.trakam.trakam.util.showToast
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.ref.WeakReference

class ImagePreviewActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)
        val imageView = findViewById<ImageView>(R.id.imageView)
        val fab = findViewById<FloatingActionButton>(R.id.sendFAB)

        val bitmap = convertBitmapToGrayScale(BitmapFactory.decodeFile(intent.data.path))
        imageView.setImageBitmap(bitmap)

        fab.setOnClickListener {
            GetNameDialogFragment.newInstance(bitmap)
                    .show(fragmentManager, GetNameDialogFragment.TAG)
        }
    }

    private fun convertBitmapToGrayScale(input: Bitmap): Bitmap {
        val bitmapOutput = Bitmap.createBitmap(input)

        val rs = RenderScript.create(this)
        val script = ScriptIntrinsicColorMatrix.create(rs)
        script.setGreyscale()

        val allocationInput = Allocation.createFromBitmap(rs, input)
        val allocationOutput = Allocation.createFromBitmap(rs, bitmapOutput)

        script.forEach(allocationInput, allocationOutput)

        allocationOutput.copyTo(bitmapOutput)

        return bitmapOutput
    }
}

class GetNameDialogFragment : DialogFragment() {
    companion object {
        val TAG = GetNameDialogFragment::class.qualifiedName

        fun newInstance(bitmap: Bitmap): GetNameDialogFragment {
            val frag = GetNameDialogFragment()
            frag.mBitmap = bitmap
            return frag
        }
    }

    private lateinit var mBitmap: Bitmap

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
                        SendDialogFragment.newInstance(mBitmap, name)
                                .show(activity!!.fragmentManager, SendDialogFragment.TAG)
                    }
                    dismiss()
                })
                .setNegativeButton(android.R.string.cancel, { _, _ ->
                    dismiss()
                })
                .create()
    }
}

class SendDialogFragment : DialogFragment() {
    companion object {
        val TAG = SendDialogFragment::class.qualifiedName

        fun newInstance(bitmap: Bitmap, name: String): SendDialogFragment {
            val frag = SendDialogFragment()
            frag.mBitmap = bitmap
            frag.mName = name
            return frag
        }

        private const val SUB_KEY = "fb648f47875e4dba98ed1267aec784e7"
        private const val PERSON_GROUP_ID = "people"
    }

    private val mFaceServiceClient = FaceServiceRestClient(SUB_KEY)

    private lateinit var mBitmap: Bitmap
    private lateinit var mName: String

    private var mSendTask: SendTask? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflateLayout(R.layout.progress_dialog)
        val textView = view.findViewById<TextView>(R.id.message)
        textView.text = getString(R.string.sending_photo)
        isCancelable = false

        return AlertDialog.Builder(activity!!)
                .setView(view)
                .setCancelable(false)
                .create()
    }

    override fun onStart() {
        super.onStart()

        if (mSendTask == null) {
            mSendTask = SendTask(this, mFaceServiceClient, mName, mBitmap)
            mSendTask?.execute()
        }
    }

    class SendTask(frag: SendDialogFragment,
                   private val faceServiceClient: FaceServiceClient,
                   private val name: String,
                   private val bitmap: Bitmap) : AsyncTask<Any, Any, Pair<Boolean, String>>() {

        private val mFragRef = WeakReference(frag)

        override fun doInBackground(vararg params: Any): Pair<Boolean, String> {
            try {
                val result = faceServiceClient.createPerson(PERSON_GROUP_ID,
                        name, "")

                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

                val inputStream = ByteArrayInputStream(outputStream.toByteArray())

                faceServiceClient.addPersonFace(PERSON_GROUP_ID,
                        result.personId, inputStream,
                        "", null)
                faceServiceClient.trainPersonGroup(PERSON_GROUP_ID)
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
                frag.activity!!.finish()
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
