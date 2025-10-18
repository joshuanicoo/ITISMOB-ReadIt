package com.mobdeve.s17.group39.itismob_mco.utils

import com.mobdeve.s17.group39.itismob_mco.ui.homepage.Volume

object ImageUtils {

    fun getEnhancedImageUrl(volume: Volume): String? {
        val baseUrl = volume.volumeInfo.imageLinks?.thumbnail
            ?: volume.volumeInfo.imageLinks?.smallThumbnail
            ?: return null

        return baseUrl
            .replace("http://", "https://")
            .replace("&edge=curl", "")
            .replace("zoom=1", "zoom=2")
            .replace("imgmax=128", "imgmax=512")
            .replace("&printsec=frontcover", "&printsec=frontcover&img=1&zoom=2")
    }
}