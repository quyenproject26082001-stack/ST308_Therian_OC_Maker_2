package com.therian.oc.aaa.data.model.custom

data class LayerListModel(
    var positionCustom: Int = 0,
    var positionNavigation: Int = 0,
    var imageNavigation: String = "",
    var layer: ArrayList<LayerModel> = arrayListOf(),
    var type: Int = 0, // 0 = no type, 1 = man, 2 = woman
)
