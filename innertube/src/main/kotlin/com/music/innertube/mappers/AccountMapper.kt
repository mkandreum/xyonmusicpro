package com.music.innertube.mappers

import com.music.innertube.InnerTube
import com.music.innertube.models.AccountInfo
import com.music.innertube.models.MediaInfo
import com.music.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.music.innertube.models.response.AccountMenuResponse
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object AccountMapper {
    suspend fun visitorData(innerTube: InnerTube): Result<String> = runCatching {
        // The short, correct visitorData first; the 558-char sw.js_data one only if that endpoint fails.
        runCatching {
            val text = innerTube.visitorId().bodyAsText()
            Json.parseToJsonElement(text).jsonObject["responseContext"]?.jsonObject?.get("visitorData")
                ?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        }.getOrNull()?.let { return@runCatching it }
        val response = innerTube.getSwJsData().bodyAsText()
        val root = Json.parseToJsonElement(response.substring(5)).jsonArray
        // YouTube moves the value between nesting levels from time to time; search the whole tree.
        findVisitorData(root[0].jsonArray[2])
            ?: throw IllegalStateException("visitorData not found in sw.js_data")
    }

    private fun findVisitorData(element: kotlinx.serialization.json.JsonElement): String? = when (element) {
        is JsonPrimitive -> element.contentOrNull?.takeIf { VISITOR_DATA_REGEX.containsMatchIn(it) }
        is kotlinx.serialization.json.JsonArray -> element.firstNotNullOfOrNull(::findVisitorData)
        else -> null
    }

    suspend fun accountInfo(innerTube: InnerTube): Result<AccountInfo> = runCatching {
        val response = innerTube.accountMenu(WEB_REMIX).body<AccountMenuResponse>()
        val renderer = response.actions.getOrNull(0)?.openPopupAction?.popup?.multiPageMenuRenderer
            ?: throw IllegalStateException("Account menu renderer not found")

        renderer.header?.activeAccountHeaderRenderer?.toAccountInfo()
            ?: throw IllegalStateException("Active account info not found in header")
    }

    /** Lists the channels available through the current account cookie. */
    suspend fun getAccountChannels(innerTube: InnerTube): Result<List<AccountInfo>> = runCatching {
        val raw = innerTube.getAccountSwitcherEndpoint(WEB_REMIX).bodyAsText()
            .removePrefix(")]}'\n")
        val root = Json.parseToJsonElement(raw).jsonObject
        if (root["code"]?.jsonPrimitive?.contentOrNull != "SUCCESS") return@runCatching emptyList()

        val items = root["data"]?.jsonObject
            ?.get("contents")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("accountSectionListRenderer")?.jsonObject
            ?.get("contents")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("accountItemSectionRenderer")?.jsonObject
            ?.get("contents")?.jsonArray.orEmpty()

        items.mapNotNull { itemEl ->
            val item = itemEl.jsonObject["accountItem"]?.jsonObject ?: return@mapNotNull null
            val name = item["accountName"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null
            val dataSyncId = item["serviceEndpoint"]?.jsonObject
                ?.get("selectActiveIdentityEndpoint")?.jsonObject
                ?.get("supportedTokens")?.jsonArray
                ?.firstNotNullOfOrNull {
                    it.jsonObject["datasyncIdToken"]?.jsonObject
                        ?.get("datasyncIdToken")?.jsonPrimitive?.contentOrNull
                }
            AccountInfo(
                name = name,
                email = null,
                channelHandle = item["channelHandle"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.contentOrNull,
                thumbnailUrl = item["accountPhoto"]?.jsonObject?.get("thumbnails")?.jsonArray
                    ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull,
                dataSyncId = dataSyncId,
            )
        }
    }

    suspend fun getMediaInfo(innerTube: InnerTube, videoId: String): Result<MediaInfo> =
        innerTube.getMediaInfo(videoId)

    private val VISITOR_DATA_REGEX = Regex("^Cg[t|s]")
}
