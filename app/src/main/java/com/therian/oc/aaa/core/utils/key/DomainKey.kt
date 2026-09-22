package com.therian.oc.aaa.core.utils.key

object DomainKey {
    const val BASE_URL = "https://lvtglobal.site"
    const val BASE_URL_PREVENTIVE = "https://lvt-api-site.io.vn"
    const val SUB_DOMAIN = "/public/app/ST308_TherianOCMaker2"

    private const val SUB_DOMAIN_BG = "/public/app/ST308_TherianOCMaker2"

    const val ADD_CHARACTER_CATEGORY_ROOT =
        "https://lvtglobal.tech/public/app/ST301_FantasyAvatarOCMaker/bg"
    const val ADD_CHARACTER_CATEGORY_JSON = "$ADD_CHARACTER_CATEGORY_ROOT/bg.json"


    const val HTTP = "https://"

    const val AVATAR_CHARACTER_API = "avatar.png"
    const val LAYER_EXTENSION = ".png"
    const val IMAGE_NAVIGATION = "nav.png"

    fun getAddCharacterAssetUrl(folder: String): String {
        return "$BASE_URL$SUB_DOMAIN_BG/bg/$folder"
    }
}
