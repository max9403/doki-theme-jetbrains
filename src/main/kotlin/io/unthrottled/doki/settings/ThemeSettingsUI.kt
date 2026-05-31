package io.unthrottled.doki.settings

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.FontComboBox
import com.intellij.ui.components.ActionLink
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.FontInfo
import com.intellij.util.ui.UIUtil
import io.unthrottled.doki.icon.DokiIcons
import io.unthrottled.doki.promotions.MessageBundle
import io.unthrottled.doki.service.PluginService
import io.unthrottled.doki.settings.actors.ThemeActor
import io.unthrottled.doki.stickers.CurrentSticker
import io.unthrottled.doki.stickers.StickerPaneService
import io.unthrottled.doki.themes.ThemeManager
import javax.swing.JComponent
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class ThemeSettingsUI : SearchableConfigurable, DumbAware {

  private val themeSettingsModel: ThemeSettingsModel = ThemeSettings.createThemeSettingsModel()
  private var initialThemeSettingsModel: ThemeSettingsModel = ThemeSettings.createThemeSettingsModel()

  private lateinit var themeCombo: javax.swing.JComboBox<String>
  private lateinit var consoleFontCombo: FontComboBox
  private lateinit var hideDelaySpinner: JSpinner
  private lateinit var maxHeightSpinner: JSpinner
  private lateinit var maxWidthSpinner: JSpinner
  private lateinit var smolMaxHeightSpinner: JSpinner
  private lateinit var smolMaxWidthSpinner: JSpinner
  private lateinit var customFontSizeSpinner: JSpinner
  private lateinit var notificationOpacitySlider: javax.swing.JSlider

  override fun getId(): String = ThemeSettings.SETTINGS_ID

  override fun getDisplayName(): String = ThemeSettings.THEME_SETTINGS_DISPLAY_NAME

  override fun createComponent(): JComponent? = panel {

    // === Theme Row ===
    row {
      label(MessageBundle.message("settings.general.current.theme"))
      themeCombo = comboBox(ThemeManager.instance.allThemes.sortedBy { it.name }.map { it.name })
        .component as javax.swing.JComboBox<String>
      themeCombo.selectedItem = initialThemeSettingsModel.currentTheme
      themeCombo.addActionListener {
        themeSettingsModel.currentTheme = themeCombo.selectedItem as String
      }

      cell(ActionLink(MessageBundle.message("settings.general.random.theme")) { _ ->
        val themes = ThemeManager.instance.allThemes
        if (themes.isNotEmpty()) {
          val randomTheme = themes.random()
          ThemeActor.applyTheme(randomTheme.name)
          themeSettingsModel.currentTheme = randomTheme.name
          themeCombo.selectedItem = randomTheme.name
        }
      }.apply { icon = AllIcons.Actions.Refresh })

      checkBox(MessageBundle.message("settings.general.content.discreet-mode")).applyToComponent {
        isSelected = initialThemeSettingsModel.discreetMode
        toolTipText = MessageBundle.message("settings.general.content.discreet-mode.tooltip")
        addActionListener {
          themeSettingsModel.discreetMode = isSelected
          toggleDiscreetMode(isSelected)
        }
      }
    }

    // === Content Settings Group ===
    group(MessageBundle.message("settings.general.weeb.stuff.title")) {

      // -- Sticker controls (left) + Content type (right) --
      row {
        panel {
          row {
            checkBox(MessageBundle.message("settings.general.weeb.sticker.enabled")).applyToComponent {
              isSelected = initialThemeSettingsModel.areStickersEnabled
              addActionListener { themeSettingsModel.areStickersEnabled = isSelected }
            }
          }
          row {
            checkBox(MessageBundle.message("settings.general.smol.stickers.enable")).applyToComponent {
              isSelected = initialThemeSettingsModel.showSmallStickers
              addActionListener {
                themeSettingsModel.showSmallStickers = isSelected
                smolMaxHeightSpinner.isEnabled = isSelected && themeSettingsModel.smallMaxStickerHeight >= 0
                smolMaxWidthSpinner.isEnabled = isSelected && themeSettingsModel.smallMaxStickerWidth >= 0
              }
            }
          }
          row {
            checkBox(MessageBundle.message("settings.general.weeb.sticker.positioning")).applyToComponent {
              isSelected = initialThemeSettingsModel.isMoveableStickers
              addActionListener { themeSettingsModel.isMoveableStickers = isSelected }
            }
          }
        }.align(AlignY.TOP)

        panel {
          row { label(MessageBundle.message("settings.general.weeb.sticker.type.title")) }
          buttonsGroup(null) {
            row {
              radioButton(MessageBundle.message("settings.general.weeb.sticker.type.primary")).applyToComponent {
                isSelected = !initialThemeSettingsModel.isCustomSticker && initialThemeSettingsModel.currentSticker == CurrentSticker.DEFAULT
                addActionListener {
                  if (isSelected) {
                    themeSettingsModel.currentSticker = CurrentSticker.DEFAULT
                    themeSettingsModel.isCustomSticker = false
                  }
                }
              }
            }
            row {
              radioButton(MessageBundle.message("settings.general.weeb.sticker.type.secondary")).applyToComponent {
                isSelected = !initialThemeSettingsModel.isCustomSticker && initialThemeSettingsModel.currentSticker == CurrentSticker.SECONDARY
                addActionListener {
                  if (isSelected) {
                    themeSettingsModel.currentSticker = CurrentSticker.SECONDARY
                    themeSettingsModel.isCustomSticker = false
                  }
                }
              }
            }
            row {
              radioButton(MessageBundle.message("settings.general.weeb.sticker.type.custom")).applyToComponent {
                isSelected = initialThemeSettingsModel.isCustomSticker
                addActionListener {
                  if (isSelected) {
                    themeSettingsModel.isCustomSticker = true
                  }
                }
              }
              button(MessageBundle.message("settings.general.content.sticker.custom.choose")) {
                val project = ProjectManager.getInstance().openProjects.firstOrNull()
                  ?: ProjectManager.getInstance().defaultProject
                val dialog = CustomStickerChooser(project, themeSettingsModel.customStickerPath)
                if (dialog.showAndGet() && dialog.exitCode == DialogWrapper.OK_EXIT_CODE) {
                  themeSettingsModel.customStickerPath = dialog.path
                  themeSettingsModel.isCustomSticker = true
                }
              }
            }
          }
        }.align(AlignY.TOP)
      }

      // -- Custom sticker path row --
      row {
        label("").applyToComponent {
          text = if (initialThemeSettingsModel.isCustomSticker) {
            val path = initialThemeSettingsModel.customStickerPath
            if (path.isNotBlank()) path else MessageBundle.message("settings.general.weeb.sticker.type.custom.path.empty")
          } else ""
          font = font.deriveFont(font.size - 1f)
          foreground = UIUtil.getContextHelpForeground()
        }
      }

      // -- Hide on hover + delay --
      row {
        checkBox(MessageBundle.message("settings.general.sticker.hide-on-hover")).applyToComponent {
          isSelected = initialThemeSettingsModel.hideOnHover
          addActionListener {
            themeSettingsModel.hideOnHover = isSelected
            hideDelaySpinner.isEnabled = isSelected
          }
        }
        hideDelaySpinner = cell(JSpinner(SpinnerNumberModel(initialThemeSettingsModel.hideDelayMS, 10, Int.MAX_VALUE, 1)))
            .enabled(initialThemeSettingsModel.hideOnHover)
            .applyToComponent {
                addChangeListener { themeSettingsModel.hideDelayMS = (it.source as JSpinner).model.value as Int }
            }.component
        label(MessageBundle.message("settings.general.sticker.hide.delay"))
      }

      // -- Reset margins + info --
      row {
        button(MessageBundle.message("settings.general.sticker.reset.margins")) {
          StickerPaneService.instance.resetMargins()
        }
        label(MessageBundle.message("settings.general.content.sticker.margin.info")).applyToComponent {
          icon = AllIcons.Actions.Help
          foreground = UIUtil.getContextHelpForeground()
        }
      }
    }

    // === Sizing Groups (side by side) ===
    row {
      // -- Primary Sticker Dimension Cap --
      panel {
        group(MessageBundle.message("settings.general.content.dimension.cap.title")) {
          row {
            maxHeightSpinner = cell(JSpinner(SpinnerNumberModel(initialThemeSettingsModel.maxStickerHeight.let { if (it < 0) 200 else it }, 1, Int.MAX_VALUE, 1)))
                .enabled(initialThemeSettingsModel.maxStickerHeight >= 0)
                .applyToComponent {
                    addChangeListener { themeSettingsModel.maxStickerHeight = (it.source as JSpinner).model.value as Int }
                }.component
            label(MessageBundle.message("settings.general.content.dimension.cap.max.height"))
            checkBox(MessageBundle.message("settings.general.content.dimension.no.limit")).applyToComponent {
              isSelected = initialThemeSettingsModel.maxStickerHeight < 0
              addActionListener {
                themeSettingsModel.maxStickerHeight = if (isSelected) -1 else 200
                maxHeightSpinner.isEnabled = !isSelected
                if (!isSelected) (maxHeightSpinner.model as SpinnerNumberModel).value = 200
              }
            }
          }
          row {
            maxWidthSpinner = cell(JSpinner(SpinnerNumberModel(initialThemeSettingsModel.maxStickerWidth.let { if (it < 0) 200 else it }, 1, Int.MAX_VALUE, 1)))
                .enabled(initialThemeSettingsModel.maxStickerWidth >= 0)
                .applyToComponent {
                    addChangeListener { themeSettingsModel.maxStickerWidth = (it.source as JSpinner).model.value as Int }
                }.component
            label(MessageBundle.message("settings.general.content.dimension.cap.max.width"))
            checkBox(MessageBundle.message("settings.general.content.dimension.no.limit")).applyToComponent {
              isSelected = initialThemeSettingsModel.maxStickerWidth < 0
              addActionListener {
                themeSettingsModel.maxStickerWidth = if (isSelected) -1 else 200
                maxWidthSpinner.isEnabled = !isSelected
                if (!isSelected) (maxWidthSpinner.model as SpinnerNumberModel).value = 200
              }
            }
          }
        }
      }

      // -- Small Stickers --
      panel {
        group(MessageBundle.message("settings.general.smol.stickers.title")) {
          row {
            smolMaxHeightSpinner = cell(JSpinner(SpinnerNumberModel(initialThemeSettingsModel.smallMaxStickerHeight.let { if (it < 0) 100 else it }, 1, Int.MAX_VALUE, 1)))
                .enabled(initialThemeSettingsModel.showSmallStickers && initialThemeSettingsModel.smallMaxStickerHeight >= 0)
                .applyToComponent {
                    addChangeListener { themeSettingsModel.smallMaxStickerHeight = (it.source as JSpinner).model.value as Int }
                }.component
            label(MessageBundle.message("settings.general.content.dimension.cap.max.height"))
            checkBox(MessageBundle.message("settings.general.content.dimension.no.limit")).applyToComponent {
              isSelected = initialThemeSettingsModel.smallMaxStickerHeight < 0
              addActionListener {
                themeSettingsModel.smallMaxStickerHeight = if (isSelected) -1 else 100
                smolMaxHeightSpinner.isEnabled = !isSelected && initialThemeSettingsModel.showSmallStickers
                if (!isSelected) (smolMaxHeightSpinner.model as SpinnerNumberModel).value = 100
              }
            }
          }
          row {
            smolMaxWidthSpinner = cell(JSpinner(SpinnerNumberModel(initialThemeSettingsModel.smallMaxStickerWidth.let { if (it < 0) 100 else it }, 1, Int.MAX_VALUE, 1)))
                .enabled(initialThemeSettingsModel.showSmallStickers && initialThemeSettingsModel.smallMaxStickerWidth >= 0)
                .applyToComponent {
                    addChangeListener { themeSettingsModel.smallMaxStickerWidth = (it.source as JSpinner).model.value as Int }
                }.component
            label(MessageBundle.message("settings.general.content.dimension.cap.max.width"))
            checkBox(MessageBundle.message("settings.general.content.dimension.no.limit")).applyToComponent {
              isSelected = initialThemeSettingsModel.smallMaxStickerWidth < 0
              addActionListener {
                themeSettingsModel.smallMaxStickerWidth = if (isSelected) -1 else 100
                smolMaxWidthSpinner.isEnabled = !isSelected && initialThemeSettingsModel.showSmallStickers
                if (!isSelected) (smolMaxWidthSpinner.model as SpinnerNumberModel).value = 100
              }
            }
          }
        }
      }
    }

    // === Background Images Group ===
    group(MessageBundle.message("settings.general.weeb.background.title")) {
      row {
        checkBox(MessageBundle.message("settings.general.weeb.background.editor")).applyToComponent {
          isSelected = initialThemeSettingsModel.isDokiBackground
          addActionListener { themeSettingsModel.isDokiBackground = isSelected }
        }
        checkBox(MessageBundle.message("settings.general.weeb.background.frame")).applyToComponent {
          isSelected = initialThemeSettingsModel.isEmptyFrameBackground
          addActionListener { themeSettingsModel.isEmptyFrameBackground = isSelected }
        }
      }
      row {
        label(MessageBundle.message("settings.general.weeb.background.editor.info")).applyToComponent {
          icon = AllIcons.General.Warning
          foreground = UIUtil.getContextHelpForeground()
        }
      }
    }

    // === Other Settings Group ===
    group(MessageBundle.message("settings.general.other")) {

      row {
        checkBox(MessageBundle.message("settings.general.weeb.statusbar")).applyToComponent {
          isSelected = initialThemeSettingsModel.showThemeStatusBar
          addActionListener { themeSettingsModel.showThemeStatusBar = isSelected }
        }
      }

      // -- Notification Opacity --
      row {
        label(MessageBundle.message("settings.general.other.glass-notification.title"))
        notificationOpacitySlider = slider(0, 100, 1, 10).component
        notificationOpacitySlider.value = initialThemeSettingsModel.notificationOpacity
        notificationOpacitySlider.addChangeListener { e -> themeSettingsModel.notificationOpacity = (e.source as javax.swing.JSlider).value }
        notificationOpacitySlider.foreground = UIUtil.getContextHelpForeground()
      }

      row {
        checkBox(MessageBundle.message("settings.general.other.animation.label")).applyToComponent {
          isSelected = initialThemeSettingsModel.isLafAnimation
          addActionListener { themeSettingsModel.isLafAnimation = isSelected }
        }
      }
      row {
        checkBox(MessageBundle.message("doki.settings.allow.promotion")).applyToComponent {
          isSelected = initialThemeSettingsModel.allowPromotionalContent
          addActionListener { themeSettingsModel.allowPromotionalContent = isSelected }
        }
      }
    }

    // === Fonts Group ===
    group(MessageBundle.message("settings.fonts.title")) {
      row {
        label(MessageBundle.message("settings.general.other.global.font.label"))
        customFontSizeSpinner = cell(JSpinner(SpinnerNumberModel(initialThemeSettingsModel.customFontSizeValue, 1, Int.MAX_VALUE, 1)))
            .applyToComponent {
                addChangeListener { themeSettingsModel.customFontSizeValue = (it.source as JSpinner).model.value as Int }
            }.component

        checkBox(MessageBundle.message("settings.general.other.global.font.size")).applyToComponent {
          isSelected = initialThemeSettingsModel.isCustomFontSize
          addActionListener { themeSettingsModel.isCustomFontSize = isSelected }
        }
      }

      row {
        label(MessageBundle.message("settings.console.font"))
        consoleFontCombo = FontComboBox().also { combo ->
          ApplicationManager.getApplication().executeOnPooledThread {
            val initialFont = FontInfo.get(initialThemeSettingsModel.consoleFontValue)
            combo.selectedItem = initialFont
          }
          combo.addActionListener {
            val fontInfo = combo.selectedItem as? FontInfo
            if (fontInfo != null) themeSettingsModel.consoleFontValue = fontInfo.font.name
          }
        }

        checkBox(MessageBundle.message("settings.fonts.console.override")).applyToComponent {
          isSelected = initialThemeSettingsModel.isOverrideConsoleFont
          addActionListener { themeSettingsModel.isOverrideConsoleFont = isSelected }
        }
      }
    }

    // === Links Row ===
    row {
      cell(ActionLink(MessageBundle.message("settings.general.documentation")) { _ ->
        BrowserUtil.browse("https://github.com/doki-theme/doki-theme-jetbrains#documentation")
      }.apply { icon = AllIcons.Actions.Help })

      cell(ActionLink(MessageBundle.message("settings.general.changelog")) { _ ->
        BrowserUtil.browse("https://github.com/doki-theme/doki-theme-jetbrains/blob/main/changelog/CHANGELOG.md")
      }.apply { icon = AllIcons.Actions.Refresh })

      cell(ActionLink(MessageBundle.message("settings.general.report.issue")) { _ ->
        BrowserUtil.browse("https://github.com/doki-theme/doki-theme-jetbrains/issues")
      }.apply { icon = AllIcons.General.BalloonError })
    }
  }.also { toggleDiscreetMode(initialThemeSettingsModel.discreetMode) }

  private fun toggleDiscreetMode(discreet: Boolean) {
    listOf(hideDelaySpinner, maxHeightSpinner, maxWidthSpinner, smolMaxHeightSpinner, smolMaxWidthSpinner, notificationOpacitySlider)
      .forEach { it.isEnabled = !discreet }
  }

  override fun isModified(): Boolean = !initialThemeSettingsModel.equals(themeSettingsModel)

  override fun apply() {
    ThemeSettings.apply(themeSettingsModel)
    initialThemeSettingsModel = themeSettingsModel.duplicate()
  }
}
