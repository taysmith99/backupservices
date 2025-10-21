data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String?,
    val mime: String?,
    val size: Long,
    val dateModified: Long, // seconds
    val width: Int?,
    val height: Int?,
    val isVideo: Boolean
)

class MediaScanner(private val ctx: Context) {

    fun list(kind: Protocol.Kind, afterModified: Long?, limit: Int): List<MediaItem> {
        val (collection, projection) = when (kind) {
            Protocol.Kind.IMAGES -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI to PROJECTION
            Protocol.Kind.VIDEOS -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI to PROJECTION
        }

        val sel = buildString {
            append("${MediaStore.MediaColumns.SIZE} > 0")
            if (afterModified != null) append(" AND ${MediaStore.MediaColumns.DATE_MODIFIED} > ?")
        }
        val args = if (afterModified != null) arrayOf(afterModified.toString()) else null
        val sort = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC LIMIT $limit"

        ctx.contentResolver.query(collection, projection, sel, args, sort)?.use { c ->
            val out = ArrayList<MediaItem>(c.count)
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val uri = ContentUris.withAppendedId(collection, id)
                out += MediaItem(
                    id = id,
                    uri = uri,
                    displayName = c.getString(1),
                    mime = c.getString(2),
                    size = c.getLong(3),
                    dateModified = c.getLong(4),
                    width = c.getIntOrNull(5),
                    height = c.getIntOrNull(6),
                    isVideo = (kind == Protocol.Kind.VIDEOS)
                )
            }
            return out
        }
        return emptyList()
    }

    fun stat(uriOrPath: String): MediaItem? {
        val uri = uriOrPath.toUriOrPathToUri()
        // Query by uri; left as an exercise (similar to list).
        // Fallback: File(path) for paths you decide to support.
        return null
    }

    companion object {
        private val PROJECTION = arrayOf(
            MediaStore.MediaColumns._ID,             // 0
            MediaStore.MediaColumns.DISPLAY_NAME,    // 1
            MediaStore.MediaColumns.MIME_TYPE,       // 2
            MediaStore.MediaColumns.SIZE,            // 3
            MediaStore.MediaColumns.DATE_MODIFIED,   // 4 (seconds)
            MediaStore.MediaColumns.WIDTH,           // 5
            MediaStore.MediaColumns.HEIGHT           // 6
        )
    }
}

private fun Cursor.getIntOrNull(idx: Int): Int? =
    if (isNull(idx)) null else getInt(idx)

private fun String.toUriOrPathToUri(): Uri =
    if (startsWith("content://")) Uri.parse(this) else Uri.fromFile(File(this))
