package io.unthrottled.doki.hax

import com.intellij.execution.runners.ProcessProxy
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.impl.EditorComposite
import io.unthrottled.doki.stickers.DOKI_BACKGROUND_PROP
import io.unthrottled.doki.util.runSafely
import javassist.CannotCompileException
import javassist.ClassClassPath
import javassist.ClassPool
import javassist.CtClass
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import javassist.expr.NewExpr

object HackComponent : Disposable {
  private val log = Logger.getInstance(javaClass)

  init {
    enableDisposableBackground()
    hackReformatHintInfoForeground()
    hackLiveIndicator()
  }

  /** Patches file reformat hint to use theme-aware context help foreground. */
  private fun hackReformatHintInfoForeground() {
    runSafely({
      val cp = ClassPool(true)
      cp.insertClassPath(ClassClassPath(Class.forName("com.intellij.codeInsight.actions.DirectoryFormattingOptions")))
      val ctClass = cp.get("com.intellij.codeInsight.actions.FileInEditorProcessor\$FormattedMessageBuilder")
      val init = ctClass.getDeclaredMethod("getMessage")
      init.instrument(
        object : ExprEditor() {
          override fun edit(e: MethodCall?) {
            if (e?.methodName == "toHtmlColor") {
              e.replace($$"{ $1 = com.intellij.util.ui.UIUtil.getContextHelpForeground();  $_ = $proceed($$); }")
            }
          }
        },
      )
      ctClass.toClass()
    }) {
      log.warn("Unable to hackReformatHintInfoForeground for reasons")
    }
  }

  /** Patches run configuration live indicator to use Doki accent color instead of hardcoded green. */
    private fun hackLiveIndicator() {
    runSafely({
      val cp = ClassPool(true)
      cp.insertClassPath(ClassClassPath(ProcessProxy::class.java))
      val ctClass = cp.get("com.intellij.execution.runners.ExecutionUtil")
      val init = ctClass.getDeclaredMethods("getLiveIndicator")[1]
      init.instrument(
        object : ExprEditor() {
          override fun edit(e: MethodCall?) {
            if (e?.methodName == "getIndicator") {
              e.replace(
                $$"{ $4 = com.intellij.ui.JBColor.namedColor(\"Doki.Accent.color\", java.awt.Color.GREEN); $_ = $proceed($$); }",
              )
            }
          }
        },
      )
      ctClass.toClass()
   }) {
      log.warn("Unable to hackLiveIndicator for reasons.")
    }
  }

  private fun enableDisposableBackground() {
    hackBackgroundPaintingComponent()
  }

  /**
   * Enables the ability to use the frame property
   * but also allows prevents the background image from staying after plugin removal.
   */
  private fun hackBackgroundPaintingComponent() {
    runSafely({
      val cp = ClassPool(true)
      cp.insertClassPath(ClassClassPath(EditorComposite::class.java))
      val ctClass2 = cp.get("com.intellij.openapi.wm.impl.IdeBackgroundUtil")

      // enable themed startup
      val backgroundMethod = ctClass2.getDeclaredMethod("getIdeBackgroundColor")
      backgroundMethod.instrument(
        object : ExprEditor() {
          override fun edit(e: NewExpr?) {
            e?.replace($$"{ $_ = com.intellij.util.ui.UIUtil.getPanelBackground(); }")
          }

          override fun edit(m: MethodCall?) {
            m?.replace($$"{ $_ = com.intellij.util.ui.UIUtil.getPanelBackground(); }")
          }
        },
      )

      // enable disposable stickers
      val method = ctClass2.getDeclaredMethod("withFrameBackground")
      method.instrument(
        object : ExprEditor() {
          @Throws(CannotCompileException::class)
          override fun edit(m: MethodCall?) {
            if (m!!.methodName == "withNamedPainters") {
              m.replace($$"{ $2 = \"$$DOKI_BACKGROUND_PROP\"; $_ = $proceed($$); }")
            }
          }
        },
      )

      val initFramePaintersMethod = ctClass2.getDeclaredMethod("initFramePainters")
      initFramePaintersMethod.instrument(
        object : ExprEditor() {
          @Throws(CannotCompileException::class)
          override fun edit(m: MethodCall?) {
            if (m!!.methodName == "initWallpaperPainter") {
              m.replace($$"{ $1 = \"$$DOKI_BACKGROUND_PROP\"; $_ = $proceed($$); }")
            } else if (m.methodName == "getNamedPainters") {
              m.replace($$"{ $1 = \"$$DOKI_BACKGROUND_PROP\"; $_ = $proceed($$); }")
            }
          }
        },
      )

      ctClass2.toClass()
    }) {
      log.warn("Unable to hackBackgroundPaintingComponent for reasons.")
    }
  }

  override fun dispose() {
  }
}
