package lavalink.server.player.filters.configs

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import me.rohank05.echo.EchoPcmAudioFilter
import org.json.JSONObject

class EchoConfig(
    private val delay: Double,
    private val decay: Float
) : FilterConfig() {
    override fun build(format: AudioDataFormat, output: FloatPcmAudioFilter): FloatPcmAudioFilter? {
        return EchoPcmAudioFilter(output, format.channelCount, format.sampleRate)
            .setDelay(delay)
            .setDecay(decay)
    }

    override fun isEnabled(): Boolean {
        return isSet(delay.toFloat(), 0.0f) || isSet(decay, 0.0f)
    }

    override fun json(): JSONObject {
        return JSONObject().put("delay", delay).put("decay", decay).put("enabled", isEnabled())
    }

    override fun name(): String {
        return "echo"
    }
}