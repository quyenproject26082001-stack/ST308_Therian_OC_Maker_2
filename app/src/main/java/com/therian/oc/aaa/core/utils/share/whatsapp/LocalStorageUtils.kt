package com.therian.oc.aaa.core.utils.share.whatsapp

import android.content.Context
import com.therian.oc.aaa.data.local.PersistenceRepository

object LocalStorageUtils {

    fun readData(context: Context, key: String): Any? {
        return PersistenceRepository.getStringSetting(context, key, "")
            .takeIf { it.isNotEmpty() }
    }
}
