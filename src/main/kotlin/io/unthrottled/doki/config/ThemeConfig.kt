package io.unthrottled.doki.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil.createCopy
import io.unthrottled.doki.stickers.CurrentSticker
import io.unthrottled.doki.stickers.StickerLevel
import java.util.Locale

@OptIn(ExperimentalStdlibApi::class)
@State(
  name = "DokiDokiThemeConfig",
  storages = [Storage("doki_doki_theme.xml")],
)
class ThemeConfig : BaseState(), PersistentStateComponent<ThemeConfig>, Cloneable {
  companion object {
    val instance: ThemeConfig
      get() = ApplicationManager.getApplication().getService(ThemeConfig::class.java)
  }

  var hideOnHover: Boolean by property(false)
  var isLafAnimation: Boolean by property(false)
  var isMoveableStickers: Boolean by property(false)
  var isNotShowReadmeAtStartup: Boolean by property(false)
  var isFirstTime: Boolean by property(true)
  var isDokiBackground: Boolean by property(false)
  var isEmptyFrameBackground: Boolean by property(true)
  var showThemeStatusBar: Boolean by property(true)
  var allowPromotions: Boolean by property(true)
  var isGlobalFontSize: Boolean by property(false)
  var isOverrideConsoleFont: Boolean by property(false)

  // todo remove this after release has cooked
  var isMaterialDirectories: Boolean by property(false)
  var isMaterialFiles: Boolean by property(false)
  var isMaterialPSIIcons: Boolean by property(false)

  var capStickerDimensions: Boolean by property(false)
  var showSmallStickers: Boolean by property(false)
  var discreetMode: Boolean by property(false)

  var hideDelayMS: Int by property(750)
  var customFontSize: Int by property(13)
  var maxStickerWidth: Int by property(-1)
  var maxStickerHeight: Int by property(-1)
  var smallMaxStickerWidth: Int by property(100)
  var smallMaxStickerHeight: Int by property(100)
  var notificationOpacity: Int by property(100)

  private val _savedMargins = string("{}")
  var savedMargins: String
    get() = _savedMargins.getValue(this) ?: "{}"
    set(value) { _savedMargins.setValue(this, value) }

  private val _userId = string("")
  var userId: String
    get() = _userId.getValue(this) ?: ""
    set(value) { _userId.setValue(this, value) }

  private val _version = string("0.0.0")
  var version: String
    get() = _version.getValue(this) ?: "0.0.0"
    set(value) { _version.setValue(this, value) }

  private val _stickerLevel = string(StickerLevel.ON.name)
  var stickerLevel: String
    get() = _stickerLevel.getValue(this) ?: StickerLevel.ON.name
    set(value) { _stickerLevel.setValue(this, value) }

  private val _currentStickerName = string(CurrentSticker.DEFAULT.name)
  private var currentStickerName: String
    get() = _currentStickerName.getValue(this) ?: CurrentSticker.DEFAULT.name
    set(value) { _currentStickerName.setValue(this, value) }

  private val _consoleFontName = string("JetBrains Mono")
  var consoleFontName: String
    get() = _consoleFontName.getValue(this) ?: "JetBrains Mono"
    set(value) { _consoleFontName.setValue(this, value) }

  private val _discreetModeConfig = string("{}")
  var discreetModeConfig: String
    get() = _discreetModeConfig.getValue(this) ?: "{}"
    set(value) { _discreetModeConfig.setValue(this, value) }

  override fun getState(): ThemeConfig? = createCopy(this)

  override fun loadState(state: ThemeConfig) {
    copyFrom(state)
  }

  var currentSticker: CurrentSticker
    get() {
      val stickerNameType = currentStickerName.uppercase(Locale.getDefault())
      return if (CurrentSticker.values().none { it.name == stickerNameType }) {
        val defaultSticker = CurrentSticker.DEFAULT
        currentSticker = defaultSticker
        defaultSticker
      } else {
        CurrentSticker.valueOf(stickerNameType)
      }
    }
    set(value) {
      currentStickerName = value.name
    }

  val currentStickerLevel: StickerLevel
    get() {
      val theStickerLevel = stickerLevel.uppercase(Locale.getDefault())
      return if (StickerLevel.values().none { it.name == theStickerLevel }) {
        val defaultStickerLevel = StickerLevel.ON
        stickerLevel = defaultStickerLevel.name
        defaultStickerLevel
      } else {
        StickerLevel.valueOf(theStickerLevel)
      }
    }
}
