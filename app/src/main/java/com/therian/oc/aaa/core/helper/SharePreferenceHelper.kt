package com.therian.oc.aaa.core.helper

import android.content.Context
import com.therian.oc.aaa.core.utils.key.PermissionKey.CAMERA_KEY
import com.therian.oc.aaa.core.utils.key.PermissionKey.NOTIFICATION_KEY
import com.therian.oc.aaa.core.utils.key.PermissionKey.QUANTITY_UNZIPPED
import com.therian.oc.aaa.core.utils.key.PermissionKey.STORAGE_KEY
import com.therian.oc.aaa.core.utils.key.SharePreferenceKey.COUNT_BACK_KEY
import com.therian.oc.aaa.core.utils.key.SharePreferenceKey.FIRST_LANG_KEY
import com.therian.oc.aaa.core.utils.key.SharePreferenceKey.FIRST_PERMISSION_KEY
import com.therian.oc.aaa.core.utils.key.SharePreferenceKey.KEY_LANGUAGE
import com.therian.oc.aaa.core.utils.key.SharePreferenceKey.MUSIC_KEY
import com.therian.oc.aaa.core.utils.key.SharePreferenceKey.PRIDE_CUSTOM_FLAGS
import com.therian.oc.aaa.core.utils.key.SharePreferenceKey.RATE_KEY
import com.therian.oc.aaa.data.local.PersistenceRepository
import com.therian.oc.aaa.data.model.pride.CustomFlagModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SharePreferenceHelper(val context: Context) {
    fun getPreLanguage(): String =
        PersistenceRepository.getStringSetting(context, KEY_LANGUAGE, "")

    fun setPreLanguage(language: String) =
        PersistenceRepository.setStringSetting(context, KEY_LANGUAGE, language)

    fun getIsFirstLang(): Boolean =
        PersistenceRepository.getBooleanSetting(context, FIRST_LANG_KEY, true)

    fun setIsFirstLang(value: Boolean) =
        PersistenceRepository.setBooleanSetting(context, FIRST_LANG_KEY, value)

    fun getIsFirstPermission(): Boolean =
        PersistenceRepository.getBooleanSetting(context, FIRST_PERMISSION_KEY, true)

    fun setIsFirstPermission(value: Boolean) =
        PersistenceRepository.setBooleanSetting(context, FIRST_PERMISSION_KEY, value)

    fun getIsRate(context: Context): Boolean =
        PersistenceRepository.getBooleanSetting(this.context, RATE_KEY, false)

    fun setIsRate(value: Boolean) =
        PersistenceRepository.setBooleanSetting(context, RATE_KEY, value)

    fun setCountBack(value: Int) =
        PersistenceRepository.setIntSetting(context, COUNT_BACK_KEY, value)

    fun getCountBack(): Int =
        PersistenceRepository.getIntSetting(context, COUNT_BACK_KEY, 0)

    fun getStoragePermission(): Int =
        PersistenceRepository.getIntSetting(context, STORAGE_KEY, 0)

    fun setStoragePermission(value: Int) =
        PersistenceRepository.setIntSetting(context, STORAGE_KEY, value)

    fun getNotificationPermission(): Int =
        PersistenceRepository.getIntSetting(context, NOTIFICATION_KEY, 0)

    fun setNotificationPermission(value: Int) =
        PersistenceRepository.setIntSetting(context, NOTIFICATION_KEY, value)

    fun getCameraPermission(): Int =
        PersistenceRepository.getIntSetting(context, CAMERA_KEY, 0)

    fun setCameraPermission(value: Int) =
        PersistenceRepository.setIntSetting(context, CAMERA_KEY, value)

    fun getQuantityUnzipped(): MutableSet<Int> {
        val json =
            PersistenceRepository.getStringSetting(context, QUANTITY_UNZIPPED, "[]")
        val type = object : TypeToken<MutableSet<Int>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun setQuantityUnzipped(value: MutableSet<Int>) =
        PersistenceRepository.setStringSetting(
            context,
            QUANTITY_UNZIPPED,
            Gson().toJson(value)
        )

    fun getCustomFlags(): MutableList<CustomFlagModel> {
        val json =
            PersistenceRepository.getStringSetting(context, PRIDE_CUSTOM_FLAGS, "[]")
        val type = object : TypeToken<MutableList<CustomFlagModel>>() {}.type
        return Gson().fromJson(json, type) ?: mutableListOf()
    }

    fun setCustomFlags(flags: List<CustomFlagModel>) =
        PersistenceRepository.setStringSetting(
            context,
            PRIDE_CUSTOM_FLAGS,
            Gson().toJson(flags)
        )

    fun isMusicEnabled(): Boolean =
        PersistenceRepository.getBooleanSetting(context, MUSIC_KEY, true)

    fun setMusicEnabled(enabled: Boolean) =
        PersistenceRepository.setBooleanSetting(context, MUSIC_KEY, enabled)
}
