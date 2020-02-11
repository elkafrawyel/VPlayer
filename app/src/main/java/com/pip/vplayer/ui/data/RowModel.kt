package com.pip.vplayer.ui.data

import androidx.annotation.IntDef

class RowModel {
    companion object {
        @IntDef(
            VIDEO_PARENT,
            VIDEO_ITEM
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class RowType
        const val VIDEO_PARENT = 1
        const val VIDEO_ITEM = 2
    }

    @RowType
    var type: Int
    lateinit var parent: VideoParent
    lateinit var video: VideoItem

    var isExpanded: Boolean

    constructor(@RowType type: Int, parent: VideoParent, isExpanded: Boolean = false) {
        this.type = type
        this.parent = parent
        this.isExpanded = isExpanded
    }

    constructor(@RowType type: Int, videoItem: VideoItem, isExpanded: Boolean = false) {
        this.type = type
        this.video = videoItem
        this.isExpanded = isExpanded
    }

}