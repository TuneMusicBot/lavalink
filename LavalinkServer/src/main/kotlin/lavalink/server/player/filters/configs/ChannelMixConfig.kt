/*
 *  Copyright (c) 2021 Dimensional Fun
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

package lavalink.server.player.filters.configs

import com.github.natanbc.lavadsp.channelmix.ChannelMixPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import org.json.JSONObject

class ChannelMixConfig(
  private val leftToRight: Float = 0f,
  private val rightToRight: Float = 1f,
  private val rightToLeft: Float = 0f,
  private val leftToLeft: Float = 1f
) : FilterConfig() {
  override fun build(format: AudioDataFormat, output: FloatPcmAudioFilter): FloatPcmAudioFilter? {
    return ChannelMixPcmAudioFilter(output)
      .setRightToLeft(rightToLeft)
      .setLeftToLeft(leftToLeft)
      .setLeftToRight(leftToRight)
      .setRightToRight(rightToRight)
  }

  override fun isEnabled(): Boolean {
    return isSet(leftToLeft, 1.0f) || isSet(leftToRight, 0.0f)
      || isSet(rightToLeft, 0.0f) || isSet(rightToRight, 1.0f)
  }

  override fun json(): JSONObject {
    return JSONObject().put("leftToRight", leftToRight).put("rightToRight", rightToRight).put("rightToLeft", rightToLeft).put("leftToLeft", leftToLeft).put("enabled", isEnabled())
  }

  override fun name(): String {
    return "channelmix"
  }
}
