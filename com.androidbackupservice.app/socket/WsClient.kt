class WsClient(
    private val ctx: Context,
    private val listener: (WsEvent) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var ws: WebSocket? = null
    private var attempts = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun ensureConnected() {
        if (ws != null) return
        val req = Request.Builder().url("wss://your-worker.example/ws?device=...").build()
        ws = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                attempts = 0; listener(WsEvent.Open)
            }
            override fun onMessage(webSocket: WebSocket, text: String) { listener(WsEvent.Text(text)) }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) { /* optional */ }
            override fun onFailure(webSocket: WebSocket, t: Throwable, r: Response?) {
                listener(WsEvent.Failure(t)); ws = null; scheduleReconnect()
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener(WsEvent.Closed(code, reason)); ws = null; scheduleReconnect()
            }
        })
    }

    fun close() { ws?.close(1000, "net down"); ws = null }

    private fun scheduleReconnect() {
        val delayMs = (1000 * 2.0.pow(min(6, attempts++))).toLong()
        scope.launch { delay(delayMs); ensureConnected() }
    }

    fun sendJson(obj: Any) { ws?.send(Protocol.encode(obj)) }
    fun sendBinary(bytes: ByteArray, offset: Int = 0, len: Int = bytes.size) {
        ws?.send(ByteString.of(bytes, offset, len))
    }
}

sealed interface WsEvent {
    data object Open : WsEvent
    data class Text(val text: String) : WsEvent
    data class Failure(val t: Throwable) : WsEvent
    data class Closed(val code: Int, val reason: String) : WsEvent
}
