package io.unthrottled.doki.promotions

import com.intellij.ide.IdleTracker
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import io.unthrottled.doki.themes.ThemeManager
import io.unthrottled.doki.util.doOrElse
import io.unthrottled.doki.util.toOptional
import java.util.Optional
import kotlin.random.Random

enum class PromotionStatus {
  ACCEPTED,
  REJECTED,
  BLOCKED,
  UNKNOWN,
}

data class PromotionResults(
  val status: PromotionStatus,
)

object AniMemePromotionService {
  fun runPromotion(
    onPromotion: (PromotionResults) -> Unit,
    onReject: () -> Unit,
  ) {
    AniMemePluginPromotionRunner(onPromotion, onReject)
  }
}

class AniMemePluginPromotionRunner(
  private val onPromotion: (PromotionResults) -> Unit,
  private val onReject: () -> Unit,
) : Runnable {
  init {
    val timeoutMs = 5 * 60 * 1000 + Random(System.currentTimeMillis()).nextInt(0, 2000)
    IdleTracker.getInstance().addIdleListener(timeoutMs, this)
  }

  override fun run() {
    AniMemePluginPromotion.runPromotion(onPromotion, onReject)
  }
}

object AniMemePluginPromotion {
  fun runPromotion(
    onPromotion: (PromotionResults) -> Unit,
    onReject: () -> Unit,
  ) {
    ApplicationManager.getApplication().executeOnPooledThread {
      ThemeManager.instance.currentTheme.ifPresent { dokiTheme ->
        val promotionAssets = PromotionAssets(dokiTheme)
        getFirstProject()
          .doOrElse(
            { project ->
              ApplicationManager.getApplication().invokeLater {
                AniMemePromotionDialog(
                  promotionAssets,
                  project,
                  onPromotion,
                ).show()
              }
            },
            onReject,
          )
      }
    }
  }
}

fun getFirstProject(): Optional<Project> =
  ProjectManager.getInstance().openProjects
    .toOptional()
    .filter { it.isNotEmpty() }
    .map { it.first() }
