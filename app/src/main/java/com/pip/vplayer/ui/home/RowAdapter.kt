package com.pip.vplayer.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.pip.vplayer.R
import com.pip.vplayer.ui.data.RowModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.card.MaterialCardView
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream


class RowAdapter(
    val context: Context,
    var rowModels: MutableList<RowModel>,
    var iVideoCallback: IVideoCallback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var actionLock = false

    class ParentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var nameTv: TextView = itemView.findViewById(R.id.parentTv) as TextView
        internal var toggleBtn: ImageView = itemView.findViewById(R.id.toggle_btn) as ImageView
        internal var parentRow: MaterialCardView =
            itemView.findViewById(R.id.parentRow) as MaterialCardView

    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var videoItem: MaterialCardView =
            itemView.findViewById(R.id.videoItem) as MaterialCardView
        internal var nameTv: TextView = itemView.findViewById(R.id.name) as TextView
        internal var durationTv: TextView = itemView.findViewById(R.id.duration) as TextView
        internal var image: ImageView = itemView.findViewById(R.id.image) as ImageView
        internal var options: ImageView = itemView.findViewById(R.id.options) as ImageView
    }

    override fun getItemViewType(position: Int): Int {
        return rowModels[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            RowModel.VIDEO_PARENT -> ParentViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.video_parent_item_view,
                    parent,
                    false
                )
            )
            RowModel.VIDEO_ITEM -> VideoViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.video_item_view,
                    parent,
                    false
                )
            )
            else -> ParentViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.video_parent_item_view,
                    parent,
                    false
                )
            )
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = rowModels[position]

        when (row.type) {
            RowModel.VIDEO_PARENT -> {
                (holder as ParentViewHolder).nameTv.text =
                    "${row.parent.name} (${row.parent.videoList?.size})"

                if (row.parent.videoList == null || row.parent.videoList!!.size == 0) {
                    holder.toggleBtn.visibility = View.GONE
                } else {
                    if (holder.toggleBtn.visibility == View.GONE) {
                        holder.toggleBtn.visibility = View.VISIBLE
                    }

                    if (row.isExpanded) {
                        holder.toggleBtn.setImageResource(R.drawable.ic_remove_circle_outline_black_24dp)
                    } else {
                        holder.toggleBtn.setImageResource(R.drawable.ic_control_point_black_24dp)
                    }

                    holder.parentRow.setOnClickListener {
                        if (!actionLock) {
                            actionLock = true
                            if (row.isExpanded) {
                                row.isExpanded = false
                                collapse(position)
                            } else {
                                row.isExpanded = true
                                expand(position)
                            }
                        }
                    }
                }
            }

            RowModel.VIDEO_ITEM -> {
                holder as VideoViewHolder
                (holder).nameTv.text = row.video.videoName
                (holder).durationTv.text = row.video.videoDuration
                val path = row.video.videoPath!!
                val file = File(path)
                if (file.extension == "mp3" ||
                    file.extension == "wav" ||
                    file.extension == "3ga" ||
                    file.extension == "aa3" ||
                    file.extension == "ac3" ||
                    file.extension == "ogg"
                ) {
                    val bitmap = getImageFromFile(file)
                    if (bitmap == null) {
                        Glide.with(context).load(R.drawable.icon).into((holder).image)
                    } else {
                        Glide.with(context).load(bitmap).into((holder).image)
                    }
                } else {
                    (holder).image.loadWithPlaceHolder(Uri.fromFile(file))
                }


                (holder).videoItem.setOnClickListener {
                    iVideoCallback.clicked(position)
                }
                (holder).options.setOnClickListener {
                    showVideoMenu((holder.options), position)
                }
            }
        }
    }

    private fun getImageFromFile(file: File): Bitmap? {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, Uri.fromFile(file))
        var inputStream: InputStream? = null
        if (mmr.embeddedPicture != null) {
            inputStream = ByteArrayInputStream(mmr.embeddedPicture)
        }
        mmr.release()
        return BitmapFactory.decodeStream(inputStream)
    }


    @SuppressLint("CheckResult")
    fun ImageView.loadWithPlaceHolder(uri: Uri) {
        val circularProgressDrawable = CircularProgressDrawable(context)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 30f
        circularProgressDrawable.start()

        val requestOptions = RequestOptions()
        requestOptions.placeholder(circularProgressDrawable)
        requestOptions.error(R.drawable.icon)
        Glide.with(context)
            .load(uri)
            .apply(requestOptions)
            .into(this)

    }

    private fun expand(position: Int) {
        var nextPosition = position
        val row = rowModels[position]
        when (row.type) {
            RowModel.VIDEO_PARENT -> {
                for (video in row.parent.videoList!!) {
                    rowModels.add(
                        ++nextPosition,
                        RowModel(
                            RowModel.VIDEO_ITEM,
                            video
                        )
                    )
                }
                notifyDataSetChanged()
            }
        }
        actionLock = false
    }

    private fun collapse(position: Int) {
        val row = rowModels[position]
        val nextPosition = position + 1
        when (row.type) {
            RowModel.VIDEO_PARENT -> {
                while (true) {
                    if (nextPosition == rowModels.size || rowModels[nextPosition].type == RowModel.VIDEO_PARENT) {
                        break
                    }
                    rowModels.removeAt(nextPosition)
                }
                notifyDataSetChanged()
            }
        }
        actionLock = false
    }

    override fun getItemCount() = rowModels.size

    private fun showVideoMenu(optionIcon: ImageView, position: Int) {
        val popup = PopupMenu(context, optionIcon)
        popup.inflate(R.menu.video_menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.rename -> {
                    iVideoCallback.rename(position)
                }
            }
            false
        }
        popup.show()
    }

}

interface IVideoCallback {
    fun clicked(position: Int)
    fun rename(position: Int)
}