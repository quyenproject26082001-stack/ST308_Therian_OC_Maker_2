package com.therian.oc.aaa.core.helper

import android.util.Log
import com.therian.oc.aaa.data.model.AddCharacterCategoryModel
import com.therian.oc.aaa.data.model.SelectedModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object RemoteAssetHelper {
    private const val TAG = "RemoteAssetHelper"
    private const val BATCH_SIZE = 10

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3L, TimeUnit.SECONDS)
            .readTimeout(3L, TimeUnit.SECONDS)
            .writeTimeout(3L, TimeUnit.SECONDS)
            .build()
    }

    private val cacheMutex = Mutex()
    private val memoryCache = ConcurrentHashMap<String, List<String>>()

    suspend fun getSequentialRemoteAssets(baseUrl: String, extensions: List<String>): ArrayList<String> {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val normalizedExtensions = extensions.map { it.trimStart('.').lowercase() }.distinct()
        val cacheKey = "$normalizedBaseUrl|${normalizedExtensions.joinToString(",")}"

        memoryCache[cacheKey]?.let { return ArrayList(it) }

        return cacheMutex.withLock {
            memoryCache[cacheKey]?.let { return@withLock ArrayList(it) }

            val discovered = probeSequentialRemoteAssets(normalizedBaseUrl, normalizedExtensions)
            if (discovered.isNotEmpty()) {
                memoryCache[cacheKey] = ArrayList(discovered)
            }
            ArrayList(discovered)
        }
    }

    suspend fun getCategoryRemoteAssets(
        jsonUrl: String,
        rootUrl: String,
        groupName: String,
        extensions: List<String> = listOf("png")
    ): ArrayList<AddCharacterCategoryModel> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(jsonUrl).get().build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext arrayListOf()

                val json = JSONObject(response.body?.string().orEmpty())
                val groupArray = json.optJSONArray(groupName) ?: return@withContext arrayListOf()
                val extension = extensions.firstOrNull()?.trimStart('.') ?: "png"
                val categories = arrayListOf<AddCharacterCategoryModel>()

                for (index in 0 until groupArray.length()) {
                    val categoryJson = groupArray.optJSONObject(index) ?: continue
                    val categoryName = categoryJson.optString("category").trim()
                    val quantity = categoryJson.optInt("quantity", 0)
                    if (quantity <= 0) continue

                    val categoryRoot = if (categoryName.isBlank()) {
                        "${rootUrl.trimEnd('/')}/$groupName"
                    } else {
                        "${rootUrl.trimEnd('/')}/$groupName/$categoryName"
                    }

                    val categoryItems = (1..quantity).mapTo(arrayListOf()) { itemIndex ->
                        SelectedModel(
                            path = "$categoryRoot/$itemIndex.$extension"
                        )
                    }
                    categories.add(
                        AddCharacterCategoryModel(
                            name = categoryName.ifBlank {
                                groupName.replaceFirstChar { it.uppercase() }
                            },
                            items = categoryItems,
                            isSelected = categories.isEmpty()
                        )
                    )
                }
                categories
            }
        } catch (exception: Exception) {
            Log.d(TAG, "Category json load failed for $groupName: ${exception.message}")
            arrayListOf()
        }
    }

    private suspend fun probeSequentialRemoteAssets(baseUrl: String, extensions: List<String>): ArrayList<String> =
        withContext(Dispatchers.IO) {
            val discovered = ArrayList<String>()
            var nextIndex = 1

            while (true) {
                val batchResults = coroutineScope {
                    (nextIndex until nextIndex + BATCH_SIZE).map { index ->
                        async {
                            resolveRemoteUrl(baseUrl, index, extensions)
                        }
                    }.awaitAll()
                }

                val firstMissingIndex = batchResults.indexOfFirst { it == null }
                if (firstMissingIndex == -1) {
                    discovered.addAll(batchResults.filterNotNull())
                    nextIndex += BATCH_SIZE
                    continue
                }

                discovered.addAll(batchResults.take(firstMissingIndex).filterNotNull())
                break
            }

            Log.d(TAG, "Discovered ${discovered.size} assets from $baseUrl")
            discovered
        }

    private fun resolveRemoteUrl(baseUrl: String, index: Int, extensions: List<String>): String? {
        for (extension in extensions) {
            val remoteUrl = "$baseUrl/$index.$extension"
            when (probeRemoteAsset(remoteUrl)) {
                ProbeResult.FOUND -> return remoteUrl
                ProbeResult.MISSING -> continue
                ProbeResult.RETRY_WITH_GET -> {
                    if (probeRemoteAssetWithRangeGet(remoteUrl)) {
                        return remoteUrl
                    }
                }
            }
        }
        return null
    }

    private fun probeRemoteAsset(url: String): ProbeResult {
        val request = Request.Builder()
            .url(url)
            .head()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> ProbeResult.FOUND
                    response.code in arrayOf(403, 405) -> ProbeResult.RETRY_WITH_GET
                    else -> ProbeResult.MISSING
                }
            }
        } catch (exception: IOException) {
            Log.d(TAG, "HEAD probe failed for $url: ${exception.message}")
            ProbeResult.MISSING
        }
    }

    private fun probeRemoteAssetWithRangeGet(url: String): Boolean {
        val request = Request.Builder()
            .url(url)
            .addHeader("Range", "bytes=0-0")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (exception: IOException) {
            Log.d(TAG, "Range probe failed for $url: ${exception.message}")
            false
        }
    }

    private enum class ProbeResult {
        FOUND,
        MISSING,
        RETRY_WITH_GET
    }
}
