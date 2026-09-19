package com.therian.oc.aaa.core.utils.key

object DomainKey {
    const val BASE_URL = "https://lvtglobal.site"
    const val BASE_URL_PREVENTIVE = "https://lvt-api-site.io.vn"
    const val SUB_DOMAIN = "/public/app/ST300_TherianOCMaker"

    private const val SUB_DOMAIN_BG = "/public/app/ST300_TherianOCMaker"


    const val HTTP = "https://"

    const val AVATAR_CHARACTER_API = "avatar.png"
    const val LAYER_EXTENSION = ".png"
    const val IMAGE_NAVIGATION = "nav.png"

    fun getAddCharacterAssetUrl(folder: String): String {
        return "$BASE_URL$SUB_DOMAIN_BG/bg/$folder"
    }
}