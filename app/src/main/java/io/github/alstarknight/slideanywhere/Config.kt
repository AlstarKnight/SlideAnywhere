package io.github.alstarknight.slideanywhere

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Everything the panel shows: pages of cards. Stored as JSON and used for import/export. */
@Serializable
data class PanelConfig(val version: Int = 1, val pages: List<Page> = listOf(Page()))

@Serializable
data class Page(val cards: List<Card> = emptyList())

@Serializable
sealed interface Card {
    val id: String
    val title: String
}

/** A titled grid of icons (apps, links, shortcuts), like the original app's cards. */
@Serializable
@SerialName("grid")
data class GridCard(override val id: String, override val title: String, val items: List<Item> = emptyList()) : Card

@Serializable
@SerialName("note")
data class NoteCard(override val id: String, override val title: String, val text: String = "") : Card

@Serializable
sealed interface Item {
    val label: String
}

@Serializable
@SerialName("app")
data class AppItem(override val label: String, val packageName: String, val activityName: String) : Item

/** A web address, an app scheme such as `alipays://…`, or an `intent:` URI. */
@Serializable
@SerialName("link")
data class LinkItem(override val label: String, val uri: String) : Item

@Serializable
@SerialName("action")
data class ActionItem(override val label: String, val action: Action) : Item

@Serializable
enum class Action {
    NOTIFICATIONS,
    QUICK_SETTINGS,
    RECENTS,
    HOME,
    BACK,
    LOCK_SCREEN,
    SCREENSHOT,
    SPLIT_SCREEN,
    PLAY_PAUSE,
    NEXT_TRACK,
    PREVIOUS_TRACK,
    VOLUME_UP,
    VOLUME_DOWN,
    FLASHLIGHT,
    VOICE_ASSISTANT,
}

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
    classDiscriminator = "type"
}

fun PanelConfig.toJson(): String = json.encodeToString(this)

/** Parses a config file; throws [IllegalArgumentException] (incl. SerializationException) when it is not valid. */
fun parseConfig(text: String): PanelConfig {
    val config = json.decodeFromString<PanelConfig>(text)
    return if (config.pages.isEmpty()) config.copy(pages = listOf(Page())) else config
}
