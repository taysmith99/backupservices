object ThumbLoader {
    @RequiresApi(29)
    fun load(resolver: ContentResolver, uri: Uri, w: Int, h: Int): Bitmap? =
        resolver.loadThumbnail(uri, android.util.Size(w, h), null)

    fun encodeJpeg(bmp: Bitmap, quality: Int = 80): ByteArray =
        ByteArrayOutputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
}
