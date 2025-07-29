package com.strivacity.android.native_sdk.render.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable sealed class Layout

@Serializable
@SerialName("widget")
data class WidgetLayout(val formId: String, val widgetId: String) : Layout()

@Serializable
@SerialName("horizontal")
data class HorizontalLayout(val items: List<Layout>) : Layout()

@Serializable @SerialName("vertical") data class VerticalLayout(val items: List<Layout>) : Layout()
