package io.unthrottled.doki.legacy

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.installAndEnable
import io.unthrottled.doki.config.ThemeConfig
import io.unthrottled.doki.notification.UpdateNotification
import io.unthrottled.doki.promotions.MessageBundle
import io.unthrottled.doki.service.DOKI_ICONS_PLUGIN_ID
import io.unthrottled.doki.service.PluginService
import io.unthrottled.doki.util.BalloonPosition

object LegacyMigration {
  fun migrateIfNecessary() {
    clearMaterialIconsFlags()
  }

  fun newVersionMigration(project: Project) {
    if (PluginService.areIconsInstalled().not()) {
      ApplicationManager.getApplication().invokeLater {
        showMaterialMigrationMessage(project)
      }
    }
  }

  private fun clearMaterialIconsFlags() {
    val config = ThemeConfig.instance
    if (config.isMaterialFiles || config.isMaterialDirectories || config.isMaterialPSIIcons) {
      config.isMaterialFiles = false
      config.isMaterialDirectories = false
      config.isMaterialPSIIcons = false
    }
  }

  private fun showMaterialMigrationMessage(project: Project) {
    val installAction =
      object : NotificationAction(MessageBundle.message("promotion.action.ok")) {
        override fun actionPerformed(
          e: AnActionEvent,
          notification: Notification,
        ) {
          installAndEnable(
            project,
            setOf(PluginId.getId(DOKI_ICONS_PLUGIN_ID)),
          ) {
          }
          notification.expire()
        }
      }

    UpdateNotification.showStickyDokiNotification(
      MessageBundle.message("migration.material.title"),
      MessageBundle.message("migration.material.body"),
      actions = listOf(installAction),
      balloonPosition = BalloonPosition.LEFT,
      project = project,
    )
 }
}
