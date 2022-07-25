package lavalink.server.player

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.track.TrackMarker
import lavalink.server.io.SocketContext
import lavalink.server.io.SocketServer
import lavalink.server.player.filters.FilterChain
import lavalink.server.recorder.AudioReceiver
import lavalink.server.util.Util
import moe.kyokobot.koe.VoiceServerInfo
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@RestController
class PlayerRestHandler (
    private val socketServer: SocketServer
        ) {

    companion object {
        private val log = LoggerFactory.getLogger(AudioLoaderRestHandler::class.java)
    }

    private fun log(request: HttpServletRequest) {
        log.info("${request.method} ${request.servletPath}")
    }

    @GetMapping(value = ["/players/{guildId}"], produces = ["application/json"])
    fun getPlayer(request: HttpServletRequest, @PathVariable("guildId") guildId: String): ResponseEntity<String> {
        log(request);
        val userId = request.getHeader("user-id")
        if (userId == null) return ResponseEntity.badRequest().build()
        val context = socketServer.contextMap.values.find { it.players.containsKey(guildId) && it.koe.clientId == userId.toLong() }
        if (context == null) return ResponseEntity.notFound().build()
        val player = context.getPlayer(guildId)
        return ResponseEntity.ok(player.getState().toString())
    }

    @PostMapping(value = ["/players/{guildId}"], produces = ["application/json"], consumes = ["application/json"])
    @PatchMapping(value = ["/players/{guildId}"], produces = ["application/json"], consumes = ["application/json"])
    fun patchPlayer(request: HttpServletRequest, @PathVariable("guildId") guildId: String, @RequestBody body: String): ResponseEntity<String> {
        log(request)
        val userId = request.getHeader("user-id")
        if (userId == null) return ResponseEntity.badRequest().build()
        val context = socketServer.contextMap.values.find { it.koe.clientId == userId.toLong() }
        if (context == null) return ResponseEntity.notFound().build()
        val player = context.getPlayer(guildId)
        applySettingToPlayer(player, JSONObject(body), context)
        return ResponseEntity.ok(player.getState().toString())
    }

    @PostMapping(value = ["/players/{guildId}/voice"], produces = ["application/json"], consumes = ["application/json"])
    @PatchMapping(value = ["/players/{guildId}/voice"], produces = ["application/json"], consumes = ["application/json"])
    fun voiceUpdate(request: HttpServletRequest, @PathVariable("guildId") guildId: String, @RequestBody body: String): ResponseEntity<String> {
        log(request)
        val userId = request.getHeader("user-id")
        if (userId == null) return ResponseEntity.badRequest().build()
        val context = socketServer.contextMap.values.find { it.koe.clientId == userId.toLong() }
        if (context == null) return ResponseEntity.notFound().build()
        val player = context.getPlayer(guildId)
        applySettingToPlayer(player, JSONObject().put("voiceUpdate", JSONObject(body)), context)
        return ResponseEntity.ok(player.getState().toString())
    }

    @PostMapping(value = ["/players/{guildId}/play"], produces = ["application/json"], consumes = ["application/json"])
    @PatchMapping(value = ["/players/{guildId}/play"], produces = ["application/json"], consumes = ["application/json"])
    fun playSong(request: HttpServletRequest, @PathVariable("guildId") guildId: String, @RequestBody body: String): ResponseEntity<String> {
        log(request)
        val userId = request.getHeader("user-id")
        if (userId == null) return ResponseEntity.badRequest().build()
        val context = socketServer.contextMap.values.find { it.koe.clientId == userId.toLong() }
        if (context == null) return ResponseEntity.notFound().build()
        val player = context.getPlayer(guildId)
        applySettingToPlayer(player, JSONObject().put("play", JSONObject(body)), context)
        return ResponseEntity.ok(player.getState().toString())
    }

    @PostMapping(value = ["/players/{guildId}/pause"], produces = ["application/json"])
    fun pauseSong(request: HttpServletRequest, @PathVariable("guildId") guildId: String): ResponseEntity<String> {
        log(request)
        val userId = request.getHeader("user-id")
        if (userId == null) return ResponseEntity.badRequest().build()
        val context = socketServer.contextMap.values.find { it.koe.clientId == userId.toLong() }
        if (context == null) return ResponseEntity.notFound().build()
        val player = context.getPlayer(guildId)
        player.setPause(true)
        return ResponseEntity.ok(player.getState().toString())
    }

    @PostMapping(value = ["/players/{guildId}/resume"], produces = ["application/json"])
    fun resumeSong(request: HttpServletRequest, @PathVariable("guildId") guildId: String): ResponseEntity<String> {
        log(request)
        val userId = request.getHeader("user-id")
        if (userId == null) return ResponseEntity.badRequest().build()
        val context = socketServer.contextMap.values.find { it.koe.clientId == userId.toLong() }
        if (context == null) return ResponseEntity.notFound().build()
        val player = context.getPlayer(guildId)
        player.setPause(false)
        return ResponseEntity.ok(player.getState().toString())
    }

    @PostMapping(value = ["/players/{guildId}/stop"], produces = ["application/json"])
    fun stopSong(request: HttpServletRequest, @PathVariable("guildId") guildId: String): ResponseEntity<String> {
        log(request)
        val userId = request.getHeader("user-id")
        if (userId == null) return ResponseEntity.badRequest().build()
        val context = socketServer.contextMap.values.find { it.koe.clientId == userId.toLong() }
        if (context == null) return ResponseEntity.notFound().build()
        val player = context.getPlayer(guildId)
        player.stop()
        return ResponseEntity.ok(player.getState().toString())
    }

    @PostMapping(value = ["/players/{guildId}/seek"], produces = ["application/json"])
    @PatchMapping(value = ["/players/{guildId}/seek"], produces = ["application/json"], consumes = ["application/json"])
    fun seekSong(request: HttpServletRequest, @PathVariable("guildId") guildId: String, @RequestBody body: String): ResponseEntity<String> {
        log(request)
        val userId = request.getHeader("user-id")
        if (userId == null) return ResponseEntity.badRequest().build()
        val context = socketServer.contextMap.values.find { it.koe.clientId == userId.toLong() }
        if (context == null) return ResponseEntity.notFound().build()
        val player = context.getPlayer(guildId)
        player.seekTo(JSONObject(body).getLong("position"))
        return ResponseEntity.ok(player.getState().toString())
    }

    @PostMapping(value = ["/players/{guildId}/volume"], produces = ["application/json"])
    @PatchMapping(value = ["/players/{guildId}/volume"], produces = ["application/json"], consumes = ["application/json"])
    fun changeVolume(request: HttpServletRequest, @PathVariable("guildId") guildId: String, @RequestBody body: String): ResponseEntity<String> {
        log(request)
        val userId = request.getHeader("user-id")
        if (userId == null) return ResponseEntity.badRequest().build()
        val context = socketServer.contextMap.values.find { it.koe.clientId == userId.toLong() }
        if (context == null) return ResponseEntity.notFound().build()
        val player = context.getPlayer(guildId)
        player.setVolume(JSONObject(body).getInt("position"))
        return ResponseEntity.ok(player.getState().toString())
    }

    @PostMapping(value = ["/players/{guildId}/filters"], produces = ["application/json"])
    @PatchMapping(value = ["/players/{guildId}/filters"], produces = ["application/json"], consumes = ["application/json"])
    fun changeFilters(request: HttpServletRequest, @PathVariable("guildId") guildId: String, @RequestBody body: String): ResponseEntity<String> {
        log(request)
        val userId = request.getHeader("user-id")
        if (userId == null) return ResponseEntity.badRequest().build()
        val context = socketServer.contextMap.values.find { it.koe.clientId == userId.toLong() }
        if (context == null) return ResponseEntity.notFound().build()
        val player = context.getPlayer(guildId)
        player.filters = FilterChain.parse(body)
        return ResponseEntity.ok(player.getState().toString())
    }

    @PostMapping(value = ["/records/{guildId}/start"], produces = ["application/json"])
    fun startRecord(request: HttpServletRequest, @PathVariable("guildId") guildId: String, @RequestBody body: String): ResponseEntity<String> {
        log(request)
        val userId = request.getHeader("user-id")
        if (userId == null) return ResponseEntity.badRequest().build()
        val context = socketServer.contextMap.values.find { it.koe.clientId == userId.toLong() }
        if (context == null) return ResponseEntity.notFound().build()
        val player = context.getPlayer(guildId)
        startRecording(context, JSONObject(body), player)
        return ResponseEntity.ok(player.getState().toString())
    }

    @PostMapping(value = ["/records/{guildId}/stop"], produces = ["application/json"])
    fun stopRecord(request: HttpServletRequest, @PathVariable("guildId") guildId: String, @RequestBody body: String): ResponseEntity<String> {
        log(request)
        val userId = request.getHeader("user-id")
        if (userId == null) return ResponseEntity.badRequest().build()
        val context = socketServer.contextMap.values.find { it.koe.clientId == userId.toLong() }
        if (context == null) return ResponseEntity.notFound().build()
        val player = context.getPlayer(guildId)
        if (player.receiver == null) return ResponseEntity.notFound().build()
        stopRecording(context, player)
        return ResponseEntity.ok(player.getState().toString())
    }

    private fun applySettingToPlayer(player: Player, config: JSONObject, context: SocketContext) {
        if (config.has("pause")) player.setPause(config.getBoolean("pause"))
        if (config.has("filters")) player.filters = FilterChain.parse(config.getJSONObject("filters").toString())
        if (config.has("position")) player.seekTo(config.getLong("position"))
        if (config.has("song")) {
            val json = config.getJSONObject("song")
            val track = Util.decodeAudioTrack(context.audioPlayerManager, json.getString("track"))
            if (json.has("startTime")) track.position = json.getLong("startTime")
            if (json.has("endTime")) {
                val stopTime = json.getLong("endTime")
                if (stopTime > 0) {
                    val handler = TrackEndMarkerHandler(player)
                    val marker = TrackMarker(stopTime, handler)
                    track.setMarker(marker)
                }
            }
            player.play(track)
        }
        if (config.has("voiceUpdate")) {
            val json = config.getJSONObject("voiceUpdate")
            val sessionId = json.getString("sessionId")
            val event = json.getJSONObject("event")
            val endpoint: String? = event.optString("endpoint")
            if (endpoint != null) {
                val token: String = event.getString("token")
                val conn = context.getMediaConnection(player)
                conn.connect(VoiceServerInfo(sessionId, endpoint, token)).whenComplete { _, _ ->
                    player.provideTo(conn)
                    player.receiver?.start()
                }
            }
        }
        if (config.has("record")) this.startRecording(context, config.getJSONObject("record"), player)
    }

    private fun stopRecording(context: SocketContext, player: Player) {
        if (player.receiver != null) {
            player.receiver?.close()
            val conn = context.getMediaConnection(player)
            conn.receiveHandler = null
            context.send(JSONObject().put("op", "event").put("type", "RecordStop").put("guildId", player.guildId).put("id", player.receiver?.id))
            player.receiver = null
        }
    }

    private fun startRecording(context: SocketContext, json: JSONObject, player: Player) {
        val conn = context.getMediaConnection(player)
        stopRecording(context, player)
        val id = json.getString("id")
        val users = try { json.getJSONArray("users").mapTo(HashSet()) { it.toString() } } catch (e: Exception) { null }
        val bitrate = json.optInt("bitrate", 64000)
        val channels = json.optInt("channels", 2)
        val duration = json.optInt("duration")
        val mp3 = json.optBoolean("mp3", true)
        val receiver = AudioReceiver(player.guildId, id, users, channels, bitrate, mp3, duration, context)
        conn.receiveHandler = receiver
        player.receiver = receiver
        if (conn.gatewayConnection?.isOpen == true) {
            receiver.start()
        }
        context.send(
            JSONObject()
                .put("op", "event")
                .put("type", "RecordStart")
                .put("guildId", player.guildId)
                .put("id", player.receiver?.id)
                .put("bitrate", bitrate)
                .put("channels", channels)
                .put("users", users ?: JSONArray())
        )
    }
}