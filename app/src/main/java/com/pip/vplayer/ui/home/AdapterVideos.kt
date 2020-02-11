package com.pip.vplayer.ui.home

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.ImageView
import com.pip.vplayer.R
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import java.io.File
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.pip.vplayer.ui.data.VideoItem


class AdapterVideos : BaseQuickAdapter<VideoItem, BaseViewHolder>(R.layout.video_item_view) {

    override fun convert(helper: BaseViewHolder, item: VideoItem) {

        helper.addOnClickListener(R.id.videoItem)

        helper.getView<ImageView>(R.id.image).loadWithPlaceHolder(Uri.fromFile(File(item.videoPath)))
        helper.setText(R.id.name, item.videoName)
        helper.setText(R.id.duration, item.videoDuration)

    }


    @SuppressLint("CheckResult")
    fun ImageView.loadWithPlaceHolder(uri: Uri) {
        val circularProgressDrawable = CircularProgressDrawable(mContext)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 30f
        circularProgressDrawable.start()

        val requestOptions = RequestOptions()
        requestOptions.placeholder(circularProgressDrawable)
        requestOptions.override(500, 500)
        Glide.with(mContext)
            .load(uri)
            .apply(requestOptions)
            .into(this)

    }

}