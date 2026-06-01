package io.unthrottled.doki.integrations

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.DynamicPluginVetoer
import com.intellij.ide.plugins.IdeaPluginDescriptor
import io.unthrottled.doki.icon.IconPathReplacementComponent
import io.unthrottled.doki.laf.LookAndFeelInstaller
import io.unthrottled.doki.service.DOKI_ICONS_PLUGIN_ID
import io.unthrottled.doki.themes.impl.ThemeManagerImpl
import io.unthrottled.doki.util.Logging

private const val DOKI_THEME_COMMUNITY_PLUGIN_ID = "io.acari.DDLCTheme"
private const val DOKI_THEME_ULTIMATE_PLUGIN_ID = "io.unthrottled.DokiTheme"

class IDEPluginInstallListener : DynamicPluginListener, DynamicPluginVetoer, Logging {
  override fun beforePluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
  }

  override fun beforePluginUnload(
    pluginDescriptor: IdeaPluginDescriptor,
    isUpdate: Boolean,
  ) {
  }

  override fun vetoPluginUnload(pluginDescriptor: IdeaPluginDescriptor): String? = null

  override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
    if (isDokiIconPlugin(pluginDescriptor)) {
      IconPathReplacementComponent.removePatchers()
      LookAndFeelInstaller.installAllUIComponents()
    }

    if (isDokiThemePlugin(pluginDescriptor)) {
      ThemeManagerImpl.refreshThemes()
      LookAndFeelInstaller.installAllUIComponents()
    }
  }

  private fun isDokiIconPlugin(intellijIdeaPluginDescriptorParameterReferenceVariable: IdeaPluginDescriptor) =
    intellijIdeaPluginDescriptorParameterReferenceVariable.pluginId.idString == DOKI_ICONS_PLUGIN_ID

  private fun isDokiThemePlugin(pluginDescriptor: IdeaPluginDescriptor) =
    pluginDescriptor.pluginId.idString == DOKI_THEME_COMMUNITY_PLUGIN_ID ||
      pluginDescriptor.pluginId.idString == DOKI_THEME_ULTIMATE_PLUGIN_ID

  override fun pluginUnloaded(
    pluginDescriptor: IdeaPluginDescriptor,
    isUpdate: Boolean,
  ) {
    if (isDokiIconPlugin(pluginDescriptor)) {
      IconPathReplacementComponent.installPatchers()
      LookAndFeelInstaller.installAllUIComponents()
    }
  }
}
