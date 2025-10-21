object Protocol {
    enum class Kind { IMAGES, VIDEOS }

    data class Hello(val type: String = "hello", val sdk: Int = Build.VERSION.SDK_INT)
    fun helloPayload() = Hello()

    data class ListMedia(val type: String = "list", val kind: Kind, val afterModified: Long?, val limit: Int)
    data class ListMediaResult(val type: String = "list_result", val items: List<MediaItem>)

    data class StatItem(val type: String = "stat", val uriOrPath: String)
    data class StatResult(val type: String = "stat_result", val item: MediaItem?)

    data class ReadChunk(val type: String = "read", val uriOrPath: String, val offset: Long, val length: Int, val reqId: String)
    data class ReadChunkMeta(val type: String = "read_chunk", val reqId: String, val offset: Long, val len: Int, val eof: Boolean)

    data class Thumb(val type: String = "thumb", val uriOrPath: String, val maxW: Int, val maxH: Int, val reqId: String)
    data class ThumbMeta(val type: String = "thumb_meta", val reqId: String, val len: Int)

    data class FindNewSince(val type: String = "find_new", val sinceEpochMs: Long, val limit: Int)
    data class FindNewSinceResult(val type: String = "find_new_result", val items: List<MediaItem>)

    data class Hash(val type: String = "hash", val uriOrPath: String, val reqId: String)
    data class HashResult(val type: String = "hash_result", val uriOrPath: String, val sha256: String?)

    data class Error(val type: String = "error", val reqId: String?, val code: String, val message: String? = null)

    fun encode(obj: Any): String = JSONObject.wrap(obj).toString() // replace with kotlinx.serialization if you like
    fun decode(json: String): Any { /* parse into one of the above based on 'type' */ return JSONObject(json) /* … */ }
}
