package com.media.vplayer.ui.player

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.database.DatabaseUtils
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.media.vplayer.BuildConfig
import com.media.vplayer.R
import com.media.vplayer.ui.data.VideoItem
import com.media.vplayer.uitiles.PreferencesHelper
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.android.synthetic.main.activity_player.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.Exception


class PlayerActivity : AppCompatActivity(), Player.EventListener, VideoListener,
    PlayerControlView.VisibilityListener {

    companion object {
        const val TEST_URL = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"
        const val TEST_URL1 =
            "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
        const val TAG = "VPlayer"

        const val UrlExtra_List = "videoUrl_List"
        const val UrlExtra = "videoUrl"
        const val UrlExtra_Position = "video_position"

        fun start(context: Context, videoList: ArrayList<VideoItem>, position: Int) {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.setClass(context, PlayerActivity::class.java)
            intent.putExtra(UrlExtra_List, videoList)
            intent.putExtra(UrlExtra_Position, position)
            context.startActivity(intent)
        }

        fun start(context: Context, mUrl: String) {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.setClass(context, PlayerActivity::class.java)
            intent.putExtra(UrlExtra, mUrl)
            context.startActivity(intent)
        }
    }

    private var BANDWIDTH = DefaultBandwidthMeter()
    var player: SimpleExoPlayer? = null

    var singleVideo = false
    var mUrlPosition = 0
    var mUrlList: MutableList<VideoItem> = mutableListOf()
    var mUrl: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)


        handleIntent(intent, false)


        val pipBtn = video_view.findViewById<ImageButton>(R.id.exo_pip_button)
        pipBtn.setOnClickListener { enterPIPMode() }
        back.setOnClickListener { onBackPressed() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            pipBtn.visibility = View.VISIBLE
        } else {
            pipBtn.visibility = View.GONE
        }
    }

    private fun setAppTheme() {
        when (PreferencesHelper(this).themeNumber) {
            1 -> setTheme(R.style.AppThemeOne)
            2 -> setTheme(R.style.AppThemeTwo)
            3 -> setTheme(R.style.AppThemeThree)

        }
    }

    private fun handleIntent(intent: Intent, newIntent: Boolean) {
        if (!newIntent)
            initiatePlayer()

        if (intent.data != null) {
            //outside the app intent
            mUrl = getRealPathFromUri(intent.data!!)
            singleVideo = true
            mUrl?.let { play(it) }

        } else {
            singleVideo = false
            mUrlList = intent.extras?.getParcelableArrayList<VideoItem>(UrlExtra_List)!!
            mUrlPosition = intent.extras?.getInt(UrlExtra_Position, 0)!!
            play(mUrlList)
        }

    }

    override fun onResume() {
        super.onResume()
        if (player != null) {
            player?.playWhenReady = true
            video_view.useController = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }

    private fun release() {
        if (player != null) {
            player?.removeListener(this)
            player?.removeVideoListener(this)
            player?.release()
            player = null
        }
    }

    override fun onStart() {
        hideSystemUI()
        super.onStart()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onPause() {
        super.onPause()
        if (player != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            ) {
                player?.playWhenReady =
                    PreferencesHelper(this).pipMode == 1 && isInPictureInPictureMode
            } else {
                player?.playWhenReady = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStop() {
        super.onStop()
        if (player != null) {
            player?.playWhenReady = false
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideSystemUI()
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUI() {
        rootView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    private fun initiatePlayer() {

        release()

        val adaptiveTrackSelectionFactory = AdaptiveTrackSelection.Factory()

        player = ExoPlayerFactory.newSimpleInstance(
            this,
            DefaultRenderersFactory(
                this,
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            ),
            DefaultTrackSelector(adaptiveTrackSelectionFactory),
            DefaultLoadControl()
        )

        video_view.useController = true

        if (video_view.player == null)
            video_view.player = player

        player?.addListener(this)

        player?.addVideoListener(this)

    }

    private fun play(mUrl: String) {

        video_view.visibility = View.VISIBLE

        val videoUri = Uri.parse(mUrl)

        Log.i(TAG, "Url to play : $videoUri")

        val mediaSource = buildMediaSource(videoUri)

        player?.prepare(mediaSource)

        player?.playWhenReady = true
    }

    private fun play(mUrlList: MutableList<VideoItem>) {

        video_view.visibility = View.VISIBLE

        player?.removeListener(this)

        player?.removeVideoListener(this)

        val mediaSources = arrayListOf<MediaSource>()
        mUrlList.forEachIndexed { index, it ->
            mediaSources.add(buildMediaSource(Uri.parse(mUrlList[index].videoPath)))
        }

        val mediaSource: MediaSource = if (mediaSources.size == 1) {
            mediaSources[0]
        } else {
            ConcatenatingMediaSource(true, *(mediaSources).map { it }.toTypedArray())
        }

        player?.prepare(mediaSource)

        player?.seekTo(mUrlPosition, C.TIME_UNSET)

        player?.playWhenReady = true

        player?.addListener(this)

        player?.addVideoListener(this)

        video_view.setControllerVisibilityListener(this)
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val defaultExtractorsFactory = DefaultExtractorsFactory()
        defaultExtractorsFactory.setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS)
        defaultExtractorsFactory.setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES)

        return ExtractorMediaSource.Factory {
            DefaultDataSourceFactory(
                this,
                BANDWIDTH,
                DefaultHttpDataSourceFactory(
                    Util.getUserAgent(this, resources.getString(R.string.app_name)),
                    BANDWIDTH
                )
            ).createDataSource()
        }.setExtractorsFactory(defaultExtractorsFactory)
            .createMediaSource(uri)
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_IDLE -> {
                loading.visibility = View.GONE
            }
            Player.STATE_BUFFERING -> {
                loading.visibility = View.VISIBLE
            }
            Player.STATE_READY -> {
                loading.visibility = View.GONE
            }
            Player.STATE_ENDED -> {
                loading.visibility = View.GONE
                if (singleVideo || mUrlList.size == 1 || mUrlPosition == mUrlList.size - 1)
                    finish()
            }
            else -> {
                loading.visibility = View.GONE
            }
        }
    }

    override fun onVisibilityChange(visibility: Int) {
        toolbar.visibility = visibility
        when (visibility) {
            View.GONE -> toolbar.animate().translationY(-(toolbar.bottom).toFloat()).setInterpolator(
                AccelerateInterpolator()
            ).start();

            View.VISIBLE -> toolbar.animate().translationY(0F).setInterpolator(
                DecelerateInterpolator()
            ).start();

        }
    }

    override fun onTracksChanged(
        trackGroups: TrackGroupArray?,
        trackSelections: TrackSelectionArray?
    ) {
        super.onTracksChanged(trackGroups, trackSelections)
        mUrlPosition = player?.currentWindowIndex!!
        if (singleVideo) {
            titleTv.text = getVideoName(mUrl!!)
            setImageIfAudio(mUrl!!)
        } else {
            setImageIfAudio(mUrlList[mUrlPosition].videoPath!!)
            titleTv.text = getVideoName(mUrlList[mUrlPosition].videoPath!!)
        }

//        Toast.makeText(this, "$mUrlPosition", Toast.LENGTH_LONG).show()
    }

    private fun setImageIfAudio(path: String) {
        val ex = getEx(path)
        if (ex == "mp3" ||
            ex == "wav" ||
            ex == "3ga" ||
            ex == "aa3" ||
            ex == "ac3" ||
            ex == "ogg"
        ) {
            try {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(path, HashMap<String, String>())
                var inputStream: InputStream? = null
                if (mmr.embeddedPicture != null) {
                    inputStream = ByteArrayInputStream(mmr.embeddedPicture)
                }
                mmr.release()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap == null) {
                    video_view.defaultArtwork = getDrawable(R.drawable.icon)
                } else {
                    video_view.defaultArtwork = BitmapDrawable(resources, bitmap);
                }

            } catch (ex: Exception) {
                video_view.defaultArtwork = getDrawable(R.drawable.icon)
            }

        }
    }

    private fun getEx(url: String): String {
        return url.substring(url.lastIndexOf(".") + 1);
    }

    private fun getVideoName(url: String): String {
        return url.substring(url.lastIndexOf('/') + 1)
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        loading.visibility = View.GONE
        Toast.makeText(this, getString(R.string.errorPlaying), Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            && PreferencesHelper(this).pipMode == 1
        ) {
            enterPIPMode()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        if (isInPictureInPictureMode) {
            player?.playWhenReady = true
        }

        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent!!, true)
    }

    //Called when the user touches the Home or Recents button to leave the app.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (PreferencesHelper(this).pipMode == 1)
            enterPIPMode()
    }

    @Suppress("DEPRECATION")
    fun enterPIPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            video_view.useController = false
            toolbar.visibility = View.GONE
            player?.playWhenReady = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                this.enterPictureInPictureMode(params.build())
            } else {
                this.enterPictureInPictureMode()
            }
        }
    }

    //=============================== Image Real Path =============================
    @SuppressLint("ObsoleteSdkInt")
    fun Context.getRealPathFromUri(uri: Uri): String? {
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(applicationContext, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return "${Environment.getExternalStorageDirectory()}/${split[1]}"
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {

                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)
                )

                return getDataColumn(contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(contentUri, selection, selectionArgs)
            }// MediaProvider
            // DownloadsProvider
        } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {
            return getDataColumn(uri, null, null)
        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
            return uri.path
        }// File
        // MediaStore (and general)

        return null
    }

    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun getDataColumn(
        uri: Uri?, selection: String?,
        selectionArgs: Array<String>?
    ): String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = contentResolver
                .query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                if (BuildConfig.DEBUG)
                    DatabaseUtils.dumpCursor(cursor)

                val column_index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }


//=============================================================================================


}