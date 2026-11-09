package dev.neolauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class AppEntry(
    val label: String,
    val component: ComponentName,
    val icon: ImageBitmap,
    /** Ícone monocromático: a UI aplica a cor do tema. */
    val tintable: Boolean = false,
) {
    /** Identificador estável (pacote + atividade), usado para salvar favoritos. */
    val key: String get() = component.flattenToString()
}

class AppRepository(private val context: Context) {

    private val pm = context.packageManager

    /** Apps com atividade de entrada (os mesmos que aparecem em qualquer gaveta de apps). */
    suspend fun loadApps(style: IconStyle): List<AppEntry> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                val icon = IconRenderer.render(info.loadIcon(pm), style, ICON_PX)
                AppEntry(
                    label = info.loadLabel(pm).toString(),
                    component = ComponentName(info.activityInfo.packageName, info.activityInfo.name),
                    icon = icon.bitmap.asImageBitmap(),
                    tintable = icon.tintable,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun launch(app: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
    }

    fun openAppInfo(app: AppEntry) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", app.component.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun uninstall(app: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.component.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private companion object {
        const val ICON_PX = 144
    }
}
