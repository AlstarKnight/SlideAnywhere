package io.github.alstarknight.slidex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

/** One sentence from hitokoto.cn, with the work and the person it is from when they are known. */
@Serializable
data class Hitokoto(
    val hitokoto: String,
    val from: String? = null,
    @SerialName("from_who") val fromWho: String? = null,
)

private val hitokotoJson = Json { ignoreUnknownKeys = true }

/** Reads one answer of the service; throws [IllegalArgumentException] when it is not one. */
fun parseHitokoto(text: String): Hitokoto = hitokotoJson.decodeFromString<Hitokoto>(text)

/** Where the sentence is from, the way hitokoto.cn shows it: "—— who「work」"; null when neither is known. */
fun Hitokoto.attribution(): String? {
    val who = fromWho?.trim().orEmpty()
    val work = from?.trim()?.takeIf { it.isNotEmpty() }?.let { "「$it」" }.orEmpty()
    if (who.isEmpty() && work.isEmpty()) return null
    return if (who.isEmpty()) "——$work" else "—— $who$work"
}

private const val HITOKOTO_URL = "https://v1.hitokoto.cn/?encode=json"
private const val TIMEOUT_MILLIS = 5000

/** Asks hitokoto.cn for a new sentence; null without network or when the answer cannot be read. Blocks. */
fun fetchHitokoto(): Hitokoto? {
    val connection = URI.create(HITOKOTO_URL).toURL().openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
        parseHitokoto(connection.inputStream.use { it.readBytes().decodeToString() })
    } catch (e: IOException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    } finally {
        connection.disconnect()
    }
}

/** The last sentence fetched while the app runs, so a card can show it at once and not ask again too often. */
object HitokotoCache {
    /** A card shown again within this long keeps the sentence it had. */
    private const val KEEP_MILLIS = 60_000L

    var last: Hitokoto? = null
        private set
    private var fetchedAt = 0L

    fun put(sentence: Hitokoto, now: Long) {
        last = sentence
        fetchedAt = now
    }

    /** Whether a card shown at [now] should fetch a new sentence. */
    fun isStale(now: Long): Boolean = last == null || now - fetchedAt >= KEEP_MILLIS
}
