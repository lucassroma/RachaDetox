package com.rachadetox.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import java.util.Locale

/** Idiomas de Alba. */
enum class AppLang(val code: String, val label: String) {
    ES("es", "Español"),
    EN("en", "English"),
    IT("it", "Italiano");

    val locale: Locale get() = Locale.forLanguageTag(code)
}

/**
 * Idioma elegido por el usuario. Es estado de Compose: al cambiarlo,
 * todas las pantallas se vuelven a dibujar en el idioma nuevo.
 */
object Lang {
    private const val PREFS = "racha_detox"
    private const val KEY = "language"

    private val state = mutableStateOf(AppLang.ES)
    private var chosen = false

    val current: AppLang get() = state.value
    val locale: Locale get() = state.value.locale
    val hasChosen: Boolean get() = chosen

    fun init(context: Context) {
        val code = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
        chosen = code != null
        state.value = AppLang.entries.firstOrNull { it.code == code } ?: systemDefault()
    }

    fun set(context: Context, lang: AppLang) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, lang.code).apply()
        chosen = true
        state.value = lang
    }

    /** Idioma del móvil, si es uno de los tres; si no, español. */
    fun systemDefault(): AppLang {
        val sys = Locale.getDefault().language
        return AppLang.entries.firstOrNull { it.code == sys } ?: AppLang.ES
    }
}

/** Devuelve el texto en el idioma actual. */
fun tr(es: String, en: String, it: String): String = when (Lang.current) {
    AppLang.ES -> es
    AppLang.EN -> en
    AppLang.IT -> it
}

/** Arranca el idioma antes que cualquier pantalla o servicio. */
class AlbaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Lang.init(this)
    }
}
