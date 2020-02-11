package com.pip.vplayer.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.pip.vplayer.R
import com.pip.vplayer.ui.data.RowModel
import com.pip.vplayer.ui.data.VideoItem
import com.pip.vplayer.ui.data.VideoParent
import com.pip.vplayer.ui.player.PlayerActivity
import com.pip.vplayer.ui.settings.SettingsActivity
import com.pip.vplayer.uitiles.PreferencesHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import java.io.File

private const val STORAGE_PERMISSION_CODE = 100

class MainActivity : AppCompatActivity(), IVideoCallback, SwipeRefreshLayout.OnRefreshListener {

    lateinit var rowAdapter: RowAdapter
    lateinit var rows: MutableList<RowModel>

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        add.setOnClickListener {
            addStreamLink()
        }

        settings.setOnClickListener {
            SettingsActivity.start(this)
        }

        sync.setOnClickListener {
            setUpVideosList()
        }

        rows = mutableListOf()
        rowAdapter = RowAdapter(this, rows, this)

        videosRv.adapter = rowAdapter
        videosRv.setHasFixedSize(true)

        checkStoragePermission()

        refresh.setOnRefreshListener(this);
        refresh.setColorScheme(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun setAppTheme() {
        when (PreferencesHelper(this).themeNumber) {
            1 -> setTheme(R.style.AppThemeOne)
            2 -> setTheme(R.style.AppThemeTwo)
            3 -> setTheme(R.style.AppThemeThree)
            4 -> setTheme(R.style.AppThemeFour)
            5 -> setTheme(R.style.AppThemeFive)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkStoragePermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        } else {
            setUpVideosList()
        }
    }

    private fun setUpVideosList() {

        if (loading.visibility == View.GONE) {
            loading.visibility = View.VISIBLE

            val mediaList: ArrayList<File> = ArrayList()

            rows.clear()
            rowAdapter.rowModels.clear()
            rowAdapter.notifyDataSetChanged()
            videosRv.recycledViewPool.clear()
            doAsync {

                val sortOrder = MediaStore.Video.Media.DISPLAY_NAME;

                val uriVideo: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val projectionVideo = arrayOf(
                    MediaStore.Video.VideoColumns.MIME_TYPE,
                    MediaStore.Video.VideoColumns.DATA
                )

                val cursorVideo: Cursor? =
                    applicationContext.contentResolver.query(
                        uriVideo,
                        projectionVideo,
                        null,
                        null,
                        sortOrder
                    )

                if (cursorVideo != null) {
                    while (cursorVideo.moveToNext()) {
                        val type =
                            cursorVideo.getString(cursorVideo.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE))
                        val dataIndex =
                            cursorVideo.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                        val path = cursorVideo.getString(dataIndex)

                        mediaList.add(File(path))
                    }
                    cursorVideo.close()
                }

                if (PreferencesHelper(this@MainActivity).audioAllowed == 1) {
                    val uriAudio: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    val projectionAudio = arrayOf(
                        MediaStore.Audio.AudioColumns.MIME_TYPE,
                        MediaStore.Audio.AudioColumns.DATA
                    )
                    val cursorAudio: Cursor? =
                        applicationContext.contentResolver.query(
                            uriAudio,
                            projectionAudio,
                            null,
                            null,
                            sortOrder
                        )

                    if (cursorAudio != null) {
                        while (cursorAudio.moveToNext()) {
                            val type =
                                cursorAudio.getString(cursorAudio.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE))
                            val dataIndex =
                                cursorAudio.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                            val path =
                                cursorAudio.getString(dataIndex)

                            mediaList.add(File(path))
                        }
                        cursorAudio.close()
                    }
                }

                rows.clear()
                val result = mediaList.groupBy { it.parentFile!!.name }
                result.entries.forEach { entry: Map.Entry<String, List<File>> ->
                    rows.add(
                        RowModel(
                            RowModel.VIDEO_PARENT,
                            VideoParent(
                                if (entry.key == "0") getString(R.string.internalMemory) else entry.key,
                                entry.value.sortedBy { it.nameWithoutExtension }.mapNotNull {
                                    if (it.exists()) {
                                        val duration = formatFileDuration(it.path)
                                        VideoItem(it.nameWithoutExtension, it.path, duration)
                                    } else {
                                        null
                                    }
                                }.toMutableList()
                            )
                        )
                    )
                }

//          rows.filterNot { it.parent.videoList!!.isEmpty() }
                rows.sortBy { rowModel -> rowModel.parent.name }

                runOnUiThread {
                    rowAdapter.notifyDataSetChanged()
                    refresh.isRefreshing = false
                    loading.visibility = View.GONE
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun addStreamLink() {
        val dialogView = layoutInflater.inflate(R.layout.custom_dialog, null);
        val editText = dialogView.findViewById<EditText>(R.id.editText);
        editText.hint = getString(R.string.tapToAddLink)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.streamLink))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.play)) { dialog, which ->
                val mUrl = editText.text.toString().trim()
                val streamList = ArrayList<VideoItem>()
                val fileName = mUrl.substring(mUrl.lastIndexOf('/') + 1)
                streamList.add(VideoItem(fileName, mUrl, ""))
                PlayerActivity.start(this, streamList, 0)
            }.setNegativeButton(getString(R.string.download)) { dialog, which ->
                val mUrl = editText.text.toString().trim()
                if (URLUtil.isValidUrl(mUrl)) {
                    downLoadLink(mUrl)
                }
            }.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false

        // Now set the text change listener for edittext
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val mUrl = editText.text.toString().trim()
                if (TextUtils.isEmpty(mUrl) ||
                    !URLUtil.isValidUrl(mUrl) ||
                    !URLUtil.isNetworkUrl(mUrl)
                ) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
                } else {
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = !mUrl.endsWith("ts")
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                }

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })
    }

    private fun getFileName(url: String): String {
        return url.substring(url.lastIndexOf('/') + 1)
    }

    private fun downLoadLink(mUrl: String) {
        try {
            Toast.makeText(
                this,
                getString(R.string.downloading),
                Toast.LENGTH_LONG
            ).show()

            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val fileName = getFileName(mUrl)
            val downloadUri = Uri.parse(mUrl) as Uri
            val request = DownloadManager.Request(downloadUri)
            request.setTitle(fileName)
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            val ex = getEx(mUrl)
            if (ex == "mp3" ||
                ex == "wav" ||
                ex == "3ga" ||
                ex == "aa3" ||
                ex == "ac3" ||
                ex == "ogg"
            ) {
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_MUSIC + "/VPlayer Music/",
                    fileName
                )
            } else {
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_MOVIES + "/VPlayer Videos/",
                    fileName
                )
            }

            val downloadId = downloadManager.enqueue(request)

//            val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId));
//
//            if (cursor != null && cursor.moveToNext()) {
//                when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
//                    DownloadManager.STATUS_FAILED -> {
//                        // do something when failed
//                        Toast.makeText(
//                            this,
//                            "Download link is broken or not available for download",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//                    DownloadManager.STATUS_PENDING, DownloadManager.STATUS_PAUSED -> {
//                        // do something pending or paused
//                    }
//                    DownloadManager.STATUS_SUCCESSFUL -> {
//                        Toast.makeText(
//                            this,
//                            "$fileName is downloaded successfully",
//                            Toast.LENGTH_LONG
//                        ).show()
//                        setUpVideosList()
//                    }
//                    DownloadManager.STATUS_RUNNING -> {
//                        // do something when running
//                    }
//                }
//            }
//            cursor.close();

        } catch (ex: java.lang.Exception) {
            Toast.makeText(
                this,
                getString(R.string.errorDownloading),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getEx(url: String): String {
        return url.substring(url.lastIndexOf(".") + 1);
    }

    private fun formatFileDuration(path: String): String? {
        return try {
            val retriever = MediaMetadataRetriever()
//            retriever.setDataSource(this, Uri.parse(path))
            retriever.setDataSource(path)
//            retriever.setDataSource(Uri.parse(path).toString(), HashMap<String, String>())
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val totalS = Integer.valueOf(time) / 1000
            String.format(
                "%02d:%02d:%02d",
                totalS / 3600,
                (totalS % 3600) / 60,
                (totalS % 60)
            )
        } catch (ex: Exception) {
            null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setUpVideosList()
            } else {
                Toast.makeText(this, getString(R.string.storageDenied), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun clicked(position: Int) {
        //get selected video parent from adapter list
        rows.forEach {
            if (it.isExpanded) {
                //parent
                if (it.parent.videoList?.contains(rows[position].video)!!) {
                    val parent = it.parent
                    val videosList = ArrayList<VideoItem>()
                    videosList.addAll(parent.videoList!!)
                    val pos = it.parent.videoList?.indexOf(rows[position].video)!!
                    PlayerActivity.start(
                        this,
                        videosList,
                        pos
                    )
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    override fun rename(position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.custom_dialog, null);
        val editText = dialogView.findViewById<EditText>(R.id.editText);
        editText.hint = getString(R.string.typeVideoName)
        editText.setText(rows[position].video.videoName)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.renameFile))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.ok)) { dialog, which ->
                val newName = editText.text.toString().trim()
                if (!TextUtils.isEmpty(newName)) {
                    val path = rows[position].video.videoPath!!
                    val file = File(path)
                    val newFile = ifNewFileExists(newName, file)
                    if (newFile != null) {
                        val renamed = file.renameTo(newFile)
                        if (renamed) {

                            sendBroadcast(
                                Intent(
                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                    Uri.fromFile(newFile)
                                )
                            )

                            rows[position].video.videoPath = newFile.path
                            rows[position].video.videoName = newFile.nameWithoutExtension

                            rowAdapter.notifyItemChanged(position)
                        }
                    } else {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(getString(R.string.renameFile))
                            .setMessage(getString(R.string.fileExists))
                            .setPositiveButton(getString(R.string.ok), null).show()
                    }
                }
            }.setNegativeButton(getString(R.string.cancel), null).show()

        // Now set the text change listener for edittext
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val mUrl = editText.text.toString().trim()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !TextUtils.isEmpty(mUrl)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

    }

    private fun ifNewFileExists(newName: String, oldFile: File?): File? {
        return File("${oldFile!!.parent}/$newName.${oldFile.extension}")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRefresh() {
        checkStoragePermission()
    }


}

