package com.therian.oc.aaa.data.model.custom

data class NavigationModel(
    val imageNavigation: String,
    var isSelected: Boolean = false,
    val layerIndex: Int = 0  // index vào itemNavList / layerList
)

