package com.rachadetox.app

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
        if (pkg in ignored) return
        current = pkg
        check(pkg)
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
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: BlockerService? = null

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
