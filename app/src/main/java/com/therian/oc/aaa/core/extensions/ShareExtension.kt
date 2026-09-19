package com.therian.oc.aaa.core.extensions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun Activity.shareImagesUris(imageUris: ArrayList<Uri>) {
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "*/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, "Share Images"))
}

fun Activity.shareImagesPaths(imagePaths: ArrayList<String>) {
    val imageUris = ArrayList<Uri>()
    for (filePath in imagePaths) {
        val imageFile = File(filePath)
        val imageUri = FileProvider.getUriForFile(
            this, "${packageName}.provider", imageFile
        )
        imageUris.add(imageUri)
    }
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "*/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, "Share Images"))
}

fun Activity.shareImagePathToPackage(imagePath: String, targetPackage: String): Boolean {
    val imageFile = File(imagePath)
    if (!imageFile.exists()) return false

    val imageUri = FileProvider.getUriForFile(
        this, "${packageName}.provider", imageFile
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        setPackage(targetPackage)
        putExtra(Intent.EXTRA_STREAM, imageUri)
        clipData = ClipData.newRawUri("Shared image", imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    return try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

fun Activity.shareVideoUrl(videoUrl: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Check out this video")
        putExtra(Intent.EXTRA_TEXT, videoUrl)
    }
    startActivity(Intent.createChooser(intent, "Share video via"))
}
