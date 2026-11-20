package com.music.innertube.models.body

import com.music.innertube.models.Context
import kotlinx.serialization.Serializable

/** Body of `visitor_id`: just a context, with no visitorData, so YouTube issues a brand new visitor. */
@Serializable
data class VisitorIdBody(
    val context: Context,
)
