package io.unthrottled.doki.settings.actors

import com.intellij.openapi.project.ProjectManager
import io.unthrottled.doki.config.ThemeConfig
import io.unthrottled.doki.notification.UpdateNotification
import io.unthrottled.doki.promotions.MessageBundle
import io.unthrottled.doki.service.GlassNotificationService

object SeeThroughNotificationsActor {
  fun enableSeeThroughNotifications(notificationOpacity: Int) {
    val previousOpacity = ThemeConfig.instance.notificationOpacity
    ThemeConfig.instance.notificationOpacity = notificationOpacity
    GlassNotificationService.makeNotificationSeeThrough()

    if (previousOpacity != notificationOpacity) {
      UpdateNotification.sendMessage(
        MessageBundle.message("notification.glass.notification.title"),
        MessageBundle.message("notification.glass.notification.body"),
        ProjectManager.getInstance().openProjects.firstOrNull(),
      )
    }
  }
}
