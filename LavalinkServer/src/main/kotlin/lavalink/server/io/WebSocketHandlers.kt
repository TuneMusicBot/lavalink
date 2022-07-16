/*
 *  Copyright (c) 2021 Freya Arbjerg and contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 */

package lavalink.server.io

import com.sedmelluq.discord.lavaplayer.track.TrackMarker
import lavalink.server.player.AudioLoader
import lavalink.server.player.AudioLoaderRestHandler
import lavalink.server.player.TrackEndMarkerHandler
import lavalink.server.player.filters.configs.Band
import lavalink.server.player.filters.FilterChain
import lavalink.server.recorder.AudioReceiver
import lavalink.server.util.Util
import moe.kyokobot.koe.VoiceServerInfo
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WebSocketHandlers {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(WebSocketHandlers::class.java)
  }

  fun voiceUpdate(context: SocketContext, json: JSONObject) {
    val sessionId = json.getString("sessionId")
    val guildId = json.getLong("guildId")

    val event = json.getJSONObject("event")
    val endpoint: String? = event.optString("endpoint")
    val token: String = event.getString("token")

    //discord sometimes send a partial server update missing the endpoint, which can be ignored.
    endpoint ?: return

    val player = context.getPlayer(guildId)
    val conn = context.getMediaConnection(player)
    conn.connect(VoiceServerInfo(sessionId, endpoint, token)).whenComplete {_, _ ->
      player.provideTo(conn)
      player.receiver?.start()
    }
  }

  fun play(context: SocketContext, json: JSONObject) {
    val player = context.getPlayer(json.getString("guildId"))
    val noReplace = json.optBoolean("noReplace", false)

    if (noReplace && player.playingTrack != null) {
      log.info("Skipping play request because of noReplace")
      return
    }

    val track = Util.decodeAudioTrack(context.audioPlayerManager, json.getString("track"))

    if (json.has("startTime")) {
      track.position = json.getLong("startTime")
    }

    player.setPause(json.optBoolean("pause", false))

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

  fun stop(context: SocketContext, json: JSONObject) {
    val player = context.getPlayer(json.getString("guildId"))
    player.stop()
  }

  fun pause(context: SocketContext, json: JSONObject) {
    val player = context.getPlayer(json.getString("guildId"))
    player.setPause(json.getBoolean("pause"))
    SocketServer.sendPlayerUpdate(context, player)
  }

  fun seek(context: SocketContext, json: JSONObject) {
    val player = context.getPlayer(json.getString("guildId"))
    player.seekTo(json.getLong("position"))
    SocketServer.sendPlayerUpdate(context, player)
  }

  fun volume(context: SocketContext, json: JSONObject) {
    val player = context.getPlayer(json.getString("guildId"))
    player.setVolume(json.getInt("volume"))
  }

  fun destroy(context: SocketContext, json: JSONObject) {
    context.destroy(json.getLong("guildId"))
  }

  fun configureResuming(context: SocketContext, json: JSONObject) {
    context.resumeKey = json.optString("key", null)
    if (json.has("timeout")) context.resumeTimeout = json.getLong("timeout")
  }

  fun filters(context: SocketContext, guildId: String, json: String) {
    val player = context.getPlayer(guildId)

    try {
      val filters = FilterChain.parse(json)
      player.filters = filters
    } catch (ex: Exception) {
      log.error("Error while parsing filters.", ex)
    }
  }

  fun pong(context: SocketContext, json: JSONObject) {
    val payload = JSONObject().put("op", "pong")

    if (json.has("guildId")) {
      val mediaConnection = context.getMediaConnection(context.getPlayer(json.getString("guildId")))

      payload.put("ping", mediaConnection.gatewayConnection?.ping ?: 0)
      payload.put("guildId", mediaConnection.guildId.toString())
    }

    context.send(payload)
  }

  fun loadTracks(context: SocketContext, json: JSONObject) {
    AudioLoader(context.audioPlayerManager)
      .load(json.getString("identifier"))
      .thenApply { AudioLoaderRestHandler.encodeLoadResult(it, context.audioPlayerManager, json.getJSONObject("user")) }
      .thenApply { it.put("nonce", json.getString("nonce")).put("identifier", json.getString("identifier")).put("op", "loadTracks") }
      .thenApply(context::send)
  }

  fun recordStart(context: SocketContext, json: JSONObject) {
    val player = context.getPlayer(json.getString("guildId"))
    val conn = context.getMediaConnection(player)
    if (player.receiver != null) {
      player.receiver?.close()
      conn.receiveHandler = null
      context.send(JSONObject().put("op", "event").put("type", "RecordStop").put("guildId", player.guildId).put("id", player.receiver?.id))
      player.receiver = null
    }
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

  fun recordStop(context: SocketContext, json: JSONObject) {
    val player = context.getPlayer(json.getString("guildId"))
    if (player.receiver != null) {
      player.receiver?.close()
      val conn = context.getMediaConnection(player)
      conn.receiveHandler = null
      context.send(JSONObject().put("op", "event").put("type", "RecordStop").put("guildId", player.guildId).put("id", player.receiver?.id))
      player.receiver = null
    }
  }
}
