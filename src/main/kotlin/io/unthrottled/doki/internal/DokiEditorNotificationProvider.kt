package io.unthrottled.doki.internal

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import io.unthrottled.doki.actions.EditorNotificationManager
import java.util.function.Function

class DokiEditorNotificationProvider : EditorNotificationProvider {
  private fun buildNotification(): EditorNotificationPanel? {
    if (!EditorNotificationManager.shouldShowNotification()) return null

    val panel = EditorNotificationPanel()
    panel.text("This is a test, bro")
    panel.createActionLabel("Link One") {
    }
    panel.createActionLabel("Link Two") {
    }
    return panel
  }

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<FileEditor, EditorNotificationPanel?> =
    Function { _ -> buildNotification() }
}
