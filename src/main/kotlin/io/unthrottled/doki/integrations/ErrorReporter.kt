package io.unthrottled.doki.integrations

import com.google.gson.Gson
import com.intellij.ide.IdeBundle
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import io.unthrottled.doki.config.ThemeConfig
import io.unthrottled.doki.util.runSafely
import io.unthrottled.doki.util.runSafelyWithResult
import java.lang.management.ManagementFactory
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.stream.Collectors
import java.awt.Component

class ErrorReporter : ErrorReportSubmitter() {
  private val log = Logger.getInstance(ErrorReporter::class.java)

  override fun getReportActionText(): String = "Report Anonymously"

  override fun submit(
    events: Array<out IdeaLoggingEvent>,
    additionalInfo: String?,
    parentComponent: Component,
    consumer: Consumer<in SubmittedReportInfo>,
  ): Boolean {
    return runSafelyWithResult({
      events.forEach { event ->
        val (appInfo, appName) = getAppName()
        val properties = System.getProperties()
        log.info(
          "Doki error report [$appName]: ${event.throwableText.take(500)} | " +
            "Build: ${getBuildInfo(appInfo)} | " +
            "JRE: ${getJRE(properties)} | " +
            "VM: ${getVM(properties)} | " +
            "OS: ${SystemInfo.OS_NAME} ${SystemInfo.OS_VERSION} | " +
            "GC: ${getGC()} | " +
            "Memory: ${Runtime.getRuntime().maxMemory() / (1024 * 1024)}MB | " +
            "Cores: ${Runtime.getRuntime().availableProcessors()} | " +
            "LAF: ${LafManager.getInstance()?.getCurrentUIThemeLookAndFeel()?.name ?: ""} | " +
            "Doki: ${ThemeConfig.instance.version} | " +
            "Config: ${Gson().toJson(ThemeConfig.instance)}" +
            (if (additionalInfo != null) " | Extra: $additionalInfo" else "")
        )
      }
      true
    }) {
      false
    }
  }

  private fun getJRE(properties: Properties): String {
    val javaVersion = properties.getProperty("java.runtime.version", properties.getProperty("java.version", "unknown"))
    val arch = properties.getProperty("os.arch", "")
    return IdeBundle.message("about.box.jre", javaVersion, arch)
  }

  private fun getVM(properties: Properties): String {
    val vmVersion = properties.getProperty("java.vm.name", "unknown")
    val vmVendor = properties.getProperty("java.vendor", "unknown")
    return IdeBundle.message("about.box.vm", vmVersion, vmVendor)
  }

  private fun getGC() =
    ManagementFactory.getGarbageCollectorMXBeans().stream()
      .map { it.name }.collect(Collectors.joining(","))

  private fun getBuildInfo(appInfo: ApplicationInfo): String {
    var buildInfo = IdeBundle.message("about.box.build.number", appInfo.build.asString())
    val cal = appInfo.buildDate
    var buildDate = ""
    if (appInfo.build.isSnapshot) {
      buildDate = SimpleDateFormat("HH:mm, ").format(cal.time)
    }
    buildDate += Instant.ofEpochMilli(cal.timeInMillis)
      .atZone(ZoneId.systemDefault())
      .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    buildInfo += IdeBundle.message("about.box.build.date", buildDate)
    return buildInfo
  }

  private fun getAppName(): Pair<ApplicationInfo, String> {
    val appInfo = ApplicationInfoEx.getInstanceEx() as ApplicationInfo
    var appName = appInfo.fullApplicationName
    val edition = ApplicationNamesInfo.getInstance().editionName
    if (edition != null) appName += " ($edition)"
    return Pair(appInfo, appName)
  }
}
