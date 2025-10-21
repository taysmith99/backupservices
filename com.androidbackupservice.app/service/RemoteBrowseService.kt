@AndroidEntryPoint // if using Hilt (optional)
class RemoteBrowseService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var ws: WsClient
    private lateinit var scanner: MediaScanner
    private lateinit var transfer: TransferEngine

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.startForeground(this, channelId = "backup", id = 1001,
            title = "Backup ready", text = "Waiting for remote commands")
        ws = WsClient(this) { onWsEvent(it) }
        scanner = MediaScanner(this)
        transfer = TransferEngine(this, ws::sendBinary, ws::sendJson)

        NetWatcher(this) { isUp -> if (isUp) ws.ensureConnected() else ws.close() }
        ws.ensureConnected() // connect to Cloudflare Worker
    }

    override fun onStartCommand(i: Intent?, f: Int, sId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?) = null

    private fun onWsEvent(evt: WsEvent) = when (evt) {
        is WsEvent.Open -> ws.sendJson(Protocol.helloPayload())
        is WsEvent.Text -> scope.launch { handleCommand(evt.text) }
        is WsEvent.Failure, is WsEvent.Closed -> { /* backoff reconnect happens inside WsClient */ }
        else -> {}
    }

    private suspend fun handleCommand(text: String) {
        val cmd = Protocol.decode(text)
        when (cmd) {
            is Protocol.ListMedia -> {
                val page = scanner.list(cmd.kind, cmd.afterModified, cmd.limit)
                ws.sendJson(Protocol.ListMediaResult(page))
            }
            is Protocol.StatItem -> {
                val meta = scanner.stat(cmd.uriOrPath)
                ws.sendJson(Protocol.StatResult(meta))
            }
            is Protocol.ReadChunk -> {
                transfer.streamChunk(cmd)  // sends meta + binary frames
            }
            is Protocol.Thumb -> {
                transfer.sendThumbnail(cmd) // binary frame (PNG/JPEG/WebP)
            }
            is Protocol.FindNewSince -> {
                val results = scanner.findNewSince(cmd.sinceEpochMs)
                ws.sendJson(Protocol.FindNewSinceResult(results))
            }
            is Protocol.Hash -> {
                val h = transfer.computeHash(cmd)
                ws.sendJson(Protocol.HashResult(cmd.uriOrPath, h))
            }
        }
    }
}
