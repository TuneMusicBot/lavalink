package lavalink.server.io

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary
import lavalink.server.info.AppInfo
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringBootVersion
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
class VersionRestHandler {
  val appInfo = AppInfo()

  companion object {
    private val log = LoggerFactory.getLogger(VersionRestHandler::class.java)
  }

  private fun log(request: HttpServletRequest) {
    val path = request.servletPath
    log.info("GET $path")
  }

  @GetMapping(value = ["/version", "/versions"], produces = ["application/json"])
  @ResponseBody
  fun getVersions(request: HttpServletRequest): ResponseEntity<String> {
    log(request)

    val versions = JSONObject()
      .put("Spring", SpringBootVersion.getVersion())
      .put("Build", appInfo.buildNumber.takeUnless { it.startsWith("@") } ?: "Unofficial")
      .put("Lavaplayer", PlayerLibrary.VERSION)
      .put("JVM", System.getProperty("java.version"))
      .put("Kotlin", KotlinVersion.CURRENT)
      .put("BuildTime", appInfo.buildTime)

    return ResponseEntity(versions.toString(), HttpStatus.OK)
  }
}