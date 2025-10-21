class TransferEngine(
    private val ctx: Context,
    private val sendBinary: (ByteArray, Int, Int) -> Unit,
    private val sendJson: (Any) -> Unit
) {
    private val buf = ByteArray(256 * 1024) // 256 KiB chunks

    suspend fun streamChunk(cmd: Protocol.ReadChunk) {
        val uri = cmd.uriOrPath.toUriOrPathToUri()
        ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            FileInputStream(afd.fileDescriptor).use { input ->
                if (cmd.offset > 0) input.skip(cmd.offset)
                val n = input.read(buf, 0, min(cmd.length, buf.size))
                val eof = (n <= 0) || (afd.length > 0 && cmd.offset + n >= afd.length)
                sendJson(Protocol.ReadChunkMeta(cmd.reqId, cmd.offset, max(0, n), eof))
                if (n > 0) sendBinary(buf, 0, n)
            }
        }
    }

    suspend fun sendThumbnail(cmd: Protocol.Thumb) {
        val uri = cmd.uriOrPath.toUriOrPathToUri()
        val bmp = if (Build.VERSION.SDK_INT >= 29) {
            ThumbLoader.load(ctx.contentResolver, uri, cmd.maxW, cmd.maxH)
        } else null
        if (bmp != null) {
            val bytes = ThumbLoader.encodeJpeg(bmp)
            sendJson(Protocol.ThumbMeta(cmd.reqId, bytes.size))
            sendBinary(bytes)
        } else {
            sendJson(Protocol.Error(cmd.reqId, "no_thumb"))
        }
    }

    suspend fun computeHash(cmd: Protocol.Hash): String? =
        hashUri(cmd.uriOrPath.toUriOrPathToUri(), "SHA-256")

    private fun hashUri(uri: Uri, algo: String): String? {
        val md = MessageDigest.getInstance(algo)
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            val buf = ByteArray(512 * 1024)
            while (true) {
                val n = input.read(buf); if (n <= 0) break
                md.update(buf, 0, n)
            }
        } ?: return null
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
