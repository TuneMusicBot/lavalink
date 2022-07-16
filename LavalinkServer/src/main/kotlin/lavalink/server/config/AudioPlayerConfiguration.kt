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

package lavalink.server.config

import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.audiomack.AudiomackAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.bandlab.BandlabAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.bilibili.BilibiliAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.clyp.ClypAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.iheart.iHeartAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.jamendo.JamendoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.mixcloud.MixcloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.ocremix.OcremixAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.odysee.OdyseeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.reddit.RedditAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.reverbnation.ReverbnationAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.rumble.RumbleAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundgasm.SoundgasmAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.streamable.StreamableAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.tiktok.TiktokAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.tunein.TuneinAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitter.TwitterAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup
import com.sedmelluq.lava.extensions.youtuberotator.planner.*
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv4Block
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block
import com.sedmelluq.lavaplayer.extensions.format.xm.XmContainerProbe
import com.sedmelluq.lavaplayer.extensions.thirdpartysources.applemusic.AppleMusicAudioSourceManager
import com.sedmelluq.lavaplayer.extensions.thirdpartysources.deezer.DeezerAudioSourceManager
import com.sedmelluq.lavaplayer.extensions.thirdpartysources.napster.NapsterAudioSourceManager
import com.sedmelluq.lavaplayer.extensions.thirdpartysources.spotify.SpotifyAudioSourceManager
import com.sedmelluq.lavaplayer.extensions.thirdpartysources.tidal.TidalAudioSourceManager
import com.sedmelluq.lavaplayer.extensions.thirdpartysources.yamusic.YandexHttpContextFilter
import com.sedmelluq.lavaplayer.extensions.thirdpartysources.yamusic.YandexMusicAudioSourceManager
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.InetAddress
import java.util.function.Predicate

/**
 * Created by napster on 05.03.18.
 */
@Configuration
class AudioPlayerConfiguration {

  private val log = LoggerFactory.getLogger(AudioPlayerConfiguration::class.java)

  @Bean
  fun audioPlayerManagerSupplier(
    lavaplayerProps: LavaplayerConfigProperties,
    routePlanner: AbstractRoutePlanner?
  ): AudioPlayerManager {
    val audioPlayerManager = DefaultAudioPlayerManager()
    val mediaContainer = MediaContainerRegistry.extended(XmContainerProbe())
    audioPlayerManager.enableGcMonitoring()


    val youtube = YoutubeAudioSourceManager(
        true,
        lavaplayerProps.youtubeConfig?.email,
        lavaplayerProps.youtubeConfig?.password
    )
    if (routePlanner != null) {
      val retryLimit = lavaplayerProps.ratelimit?.retryLimit ?: -1
      when {
        retryLimit < 0 -> YoutubeIpRotatorSetup(routePlanner).forSource(youtube).setup()
        retryLimit == 0 -> YoutubeIpRotatorSetup(routePlanner).forSource(youtube).withRetryLimit(Int.MAX_VALUE)
          .setup()
        else -> YoutubeIpRotatorSetup(routePlanner).forSource(youtube).withRetryLimit(retryLimit).setup()
      }
    }


    audioPlayerManager.registerSourceManager(youtube)

    audioPlayerManager.registerSourceManager(YandexMusicAudioSourceManager(true, audioPlayerManager))
    YandexHttpContextFilter.setOAuthToken(lavaplayerProps.yandexOAuthToken)

    audioPlayerManager.registerSourceManager(SpotifyAudioSourceManager(true, audioPlayerManager))
    audioPlayerManager.registerSourceManager(DeezerAudioSourceManager(true, audioPlayerManager))
    audioPlayerManager.registerSourceManager(TidalAudioSourceManager(true, audioPlayerManager))
    audioPlayerManager.registerSourceManager(AppleMusicAudioSourceManager(true, audioPlayerManager))
    audioPlayerManager.registerSourceManager(NapsterAudioSourceManager(true, audioPlayerManager))

    audioPlayerManager.registerSourceManager(AudiomackAudioSourceManager())
    audioPlayerManager.registerSourceManager(BandlabAudioSourceManager())
    audioPlayerManager.registerSourceManager(BilibiliAudioSourceManager(true))
    audioPlayerManager.registerSourceManager(ClypAudioSourceManager())
    audioPlayerManager.registerSourceManager(JamendoAudioSourceManager(true))
    audioPlayerManager.registerSourceManager(MixcloudAudioSourceManager(true))
    audioPlayerManager.registerSourceManager(OcremixAudioSourceManager())
    audioPlayerManager.registerSourceManager(ReverbnationAudioSourceManager())
    audioPlayerManager.registerSourceManager(SoundgasmAudioSourceManager())
    audioPlayerManager.registerSourceManager(StreamableAudioSourceManager())
    audioPlayerManager.registerSourceManager(TuneinAudioSourceManager())
    audioPlayerManager.registerSourceManager(TwitterAudioSourceManager())
    audioPlayerManager.registerSourceManager(iHeartAudioSourceManager(true, mediaContainer))
    audioPlayerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault())
    audioPlayerManager.registerSourceManager(BandcampAudioSourceManager())
    audioPlayerManager.registerSourceManager(TiktokAudioSourceManager())
    audioPlayerManager.registerSourceManager(TwitchStreamAudioSourceManager())
    audioPlayerManager.registerSourceManager(VimeoAudioSourceManager(true))
    audioPlayerManager.registerSourceManager(RedditAudioSourceManager())
    audioPlayerManager.registerSourceManager(RumbleAudioSourceManager())
    audioPlayerManager.registerSourceManager(OdyseeAudioSourceManager(true))
    audioPlayerManager.registerSourceManager(GetyarnAudioSourceManager())
    audioPlayerManager.registerSourceManager(LocalAudioSourceManager(mediaContainer))
    audioPlayerManager.registerSourceManager(HttpAudioSourceManager(mediaContainer))

    audioPlayerManager.configuration.isFilterHotSwapEnabled = true
    audioPlayerManager.frameBufferDuration = lavaplayerProps.frameBufferDuration
    log.info("Using the non-allocating frame buffer.")
    audioPlayerManager.configuration.setFrameBufferFactory(::NonAllocatingAudioFrameBuffer)

    return audioPlayerManager
  }

  @Bean
  fun routePlanner(lavaplayerProps: LavaplayerConfigProperties): AbstractRoutePlanner? {
    val rateLimitConfig = lavaplayerProps.ratelimit
    if (rateLimitConfig == null) {
      log.debug("No rate limit config block found, skipping setup of route planner")
      return null
    }

    val ipBlockList = rateLimitConfig.ipBlocks
    if (ipBlockList.isEmpty()) {
      log.info("List of ip blocks is empty, skipping setup of route planner")
      return null
    }

    val blacklisted = rateLimitConfig.excludedIps.map { InetAddress.getByName(it) }
    val filter = Predicate<InetAddress> { !blacklisted.contains(it) }
    val ipBlocks = ipBlockList.map {
      when {
        Ipv4Block.isIpv4CidrBlock(it) -> Ipv4Block(it)
        Ipv6Block.isIpv6CidrBlock(it) -> Ipv6Block(it)
        else -> throw RuntimeException("Invalid IP Block '$it', make sure to provide a valid CIDR notation")
      }
    }

    return when (rateLimitConfig.strategy.lowercase().trim()) {
      "rotateonban" -> RotatingIpRoutePlanner(ipBlocks, filter, rateLimitConfig.searchTriggersFail)
      "loadbalance" -> BalancingIpRoutePlanner(ipBlocks, filter, rateLimitConfig.searchTriggersFail)
      "nanoswitch" -> NanoIpRoutePlanner(ipBlocks, rateLimitConfig.searchTriggersFail)
      "rotatingnanoswitch" -> RotatingNanoIpRoutePlanner(ipBlocks, filter, rateLimitConfig.searchTriggersFail)
      else -> throw RuntimeException("Unknown strategy!")
    }
  }

}
