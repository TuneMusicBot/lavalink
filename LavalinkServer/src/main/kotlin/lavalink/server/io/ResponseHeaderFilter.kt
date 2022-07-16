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

import com.github.natanbc.lavadsp.DspInfo
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary
import lavalink.server.info.AppInfo
import lavalink.server.info.GitRepoState
import org.springframework.boot.SpringBootVersion
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.swing.Spring

@Component
class ResponseHeaderFilter(private val appInfo: AppInfo, private val gitRepoState: GitRepoState) : OncePerRequestFilter() {
  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    response.addHeader("Lavalink-Api-Version", "3")
    response.addHeader("Lavalink-Version", appInfo.getVersionBuild())
    response.addHeader("Lavalink-Spring-Version", SpringBootVersion.getVersion())
    response.addHeader("Lavalink-Build", appInfo.buildNumber.takeUnless { it.startsWith("@") } ?: "Unofficial")
    response.addHeader("Lavalink-Lavaplayer-Version", PlayerLibrary.VERSION)
    response.addHeader("Lavalink-Lavadsp-Version", DspInfo.VERSION)
    response.addHeader("Lavalink-Java-Version", System.getProperty("java.version"))
    response.addHeader("Lavalink-Kotlin-Version", KotlinVersion.CURRENT.toString())
    response.addHeader("Lavalink-Build-Time", appInfo.buildTime.toString())
    response.addHeader("Lavalink-Gitloaded", gitRepoState.loaded.toString())
    if (gitRepoState.loaded) {
      response.addHeader("Lavalink-Commit", gitRepoState.commitIdAbbrev)
      response.addHeader("Lavalink-Commit-Time", gitRepoState.commitTime.toString())
      response.addHeader("Lavalink-Branch", gitRepoState.branch)
    }
    filterChain.doFilter(request, response)
  }
}
