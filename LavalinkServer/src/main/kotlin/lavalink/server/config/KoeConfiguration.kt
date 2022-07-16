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

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollDatagramChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueDatagramChannel
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.socket.nio.NioSocketChannel
import moe.kyokobot.koe.KoeOptions
import moe.kyokobot.koe.codec.udpqueue.UdpQueueFramePollerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KoeConfiguration(val configProperties: KoeConfigProperties) {
  companion object {
    var udpQueueBufferDuration: Int? = null
  }

  private val log: Logger = LoggerFactory.getLogger(KoeConfiguration::class.java)

  @Bean
  fun koeOptions(): KoeOptions = KoeOptions.builder().apply {
    log.info("OS: " + System.getProperty("os.name") + ", Arch: " + System.getProperty("os.arch"))
    val os = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")

    setHighPacketPriority(configProperties.highPacketPriority)

    /* JDA-NAS */
    val nasSupported =
      (os.contains("linux", true)
              && (arch.equals("amd64", true) || arch.equals("x86", true) || arch.equals("aarch64", true)))
              || os.contains("mac", true)
              || (os.contains("win", true) && arch.equals("amd64") || arch.equals("x86"))

    if (nasSupported && configProperties.useNAS) {
      log.info("Enabling JDA-NAS")

      var bufferSize = configProperties.bufferDurationMs ?: UdpQueueFramePollerFactory.DEFAULT_BUFFER_DURATION
      if (bufferSize <= 60) {
        log.warn("Buffer size of ${bufferSize}ms is illegal. Defaulting to ${UdpQueueFramePollerFactory.DEFAULT_BUFFER_DURATION}")
        bufferSize = UdpQueueFramePollerFactory.DEFAULT_BUFFER_DURATION
      }

      udpQueueBufferDuration = bufferSize
      setFramePollerFactory(UdpQueueFramePollerFactory(bufferSize, Runtime.getRuntime().availableProcessors()))
    } else {
      log.warn("This system and architecture appears to not support native audio sending! "
              + "GC pauses may cause your bot to stutter during playback.")
    }

    if (configProperties.useEpoll && Epoll.isAvailable()) {
      /* Epoll Transport */
      log.info("Using Epoll Transport.")
      setEventLoopGroup(EpollEventLoopGroup())
      setDatagramChannelClass(EpollDatagramChannel::class.java)
      setSocketChannelClass(EpollSocketChannel::class.java)
    } else if (configProperties.useKQueue && KQueue.isAvailable()) {
      /* KQueue Transport */
      log.info("Using KQueue Transport.")
      setEventLoopGroup(KQueueEventLoopGroup())
      setDatagramChannelClass(KQueueDatagramChannel::class.java)
      setSocketChannelClass(KQueueSocketChannel::class.java)
    } else {
      /* Nio Transport */
      log.info("Using Nio Transport.")
      setEventLoopGroup(NioEventLoopGroup())
      setDatagramChannelClass(NioDatagramChannel::class.java)
      setSocketChannelClass(NioSocketChannel::class.java)
    }

    /* Byte Buf Allocator */
    var custom = true
    when (configProperties.byteBufAllocator) {
      "netty-default" -> setByteBufAllocator(ByteBufAllocator.DEFAULT)
      "default", "pooled" -> setByteBufAllocator(PooledByteBufAllocator.DEFAULT)
      "unpooled" -> setByteBufAllocator(UnpooledByteBufAllocator.DEFAULT)
      else -> {
        log.warn("Invalid byte buf allocator \"${configProperties.byteBufAllocator}\", defaulting to the 'pooled' byte buf allocator.")
        custom = false
      }
    }

    if (custom) {
      log.info("Using the '${configProperties.byteBufAllocator}' byte buf allocator")
    }
  }.create()
}