package com.rachadetox.app

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

/**
 * Cierra las apps bloqueadas hoy.
 * Solo recibe el nombre de la app que pasa a primer plano: está configurado
 * para NO leer el contenido de la pantalla (canRetrieveWindowContent = false).
 */
class BlockerService : AccessibilityService() {

    private var current: String? = null
    private var ignored: Set<String> = emptySet()

    // Mientras usas una app con límite, el propio servicio mira cada 15 s si has perdido la
    // racha del todo, para cerrarla aunque Android haya parado el servicio de segundo plano.
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val watch = object : Runnable {
        override fun run() {
            val pkg = current ?: return
            if (Store(this@BlockerService).goals().none { it.pkg == pkg }) return
            worker.execute {
                try {
                    StreakEngine.refreshToday(this@BlockerService)
                } catch (_: Exception) {
                }
                handler.post { current?.let { check(it) } }
            }
            handler.postDelayed(this, WATCH_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val keyboards = try {
            getSystemService(InputMethodManager::class.java)?.enabledInputMethodList
                ?.map { it.packageName }?.toSet() ?: emptySet()
        } catch (_: Exception) {
            emptySet()
        }
        ignored = keyboards + setOf("com.android.systemui", packageName)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) {
            // Estás en Alba: ya no hay otra app delante que cerrar
            current = null
            handler.removeCallbacks(watch)
            return
        }
        if (pkg in ignored) return
        current = pkg
        check(pkg)
        handler.removeCallbacks(watch)
        if (current != null) handler.post(watch)
    }

    /** Vuelve a mirar la app actual (p. ej. justo después de bloquearla). */
    fun enforceNow() {
        current?.let { check(it) }
    }

    private fun check(pkg: String) {
        val store = Store(this)
        // Día de prueba tras perder la racha: antes de abrir la app, un aviso
        if (store.isGuardedToday(pkg)) {
            startActivity(
                Intent(this, GuardActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(GuardActivity.EXTRA_PKG, pkg)
            )
            current = null
            return
        }
        if (!store.isBlockedToday(pkg)) return
        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(
            Intent(this, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(BlockActivity.EXTRA_PKG, pkg)
        )
        current = null
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(watch)
        worker.shutdown()
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        private const val WATCH_MS = 15_000L

        @Volatile
        var instance: BlockerService? = null

        /**
         * El permiso está activado Y el servicio está funcionando de verdad. Tras una
         * actualización, algunos móviles dejan el interruptor encendido pero el servicio parado.
         */
        fun isWorking(context: Context): Boolean = isEnabled(context) && instance != null

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val me = ComponentName(context, BlockerService::class.java)
            return enabled.split(':').any {
                ComponentName.unflattenFromString(it) == me
            }
        }
    }
}
