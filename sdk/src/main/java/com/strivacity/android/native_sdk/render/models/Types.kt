package com.strivacity.android.native_sdk.render.models

import kotlinx.serialization.Serializable

@Serializable
data class Screen(
    val screen: String?,
    val branding: Branding?,
    val hostedUrl: String?,
    val finalizeUrl: String?,
    val forms: List<FormWidget>?,
    val layout: Layout?,
    var messages: Messages?
)

@Serializable
data class Branding(
    val logoUrl: String?,
    val copyright: String?,
    val siteTermUrl: String?,
    val privacyPolicyUrl: String?
)
