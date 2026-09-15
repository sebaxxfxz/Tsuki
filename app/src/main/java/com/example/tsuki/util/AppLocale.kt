package com.example.tsuki.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object AppLocale {
    const val SYSTEM = ""
    const val SPANISH = "es"
    const val ENGLISH = "en"

    fun resolveTag(stored: String?): String {
        if (stored.isNullOrBlank()) {
            val sysLocales = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Resources.getSystem().configuration.locales
            } else {
                null
            }
            val sysLocale = if (sysLocales != null && !sysLocales.isEmpty) {
                sysLocales[0]
            } else {
                @Suppress("DEPRECATION")
                Resources.getSystem().configuration.locale
            }
            val sys = (sysLocale?.language ?: "es").lowercase()
            return if (sys.startsWith("es")) SPANISH else ENGLISH
        }
        return stored
    }

    fun readStored(context: Context): String {
        return context.getSharedPreferences("tsuki_prefs", Context.MODE_PRIVATE).getString("app_locale_tag", "") ?: ""
    }

    fun saveStored(context: Context, tag: String) {
        context.getSharedPreferences("tsuki_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("app_locale_tag", tag)
            .commit()
    }

    fun wrap(base: Context, storedTag: String?): Context {
        val tag = resolveTag(storedTag)
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        }
        return base.createConfigurationContext(config)
    }

    fun applyAndRecreate(activity: Activity, tag: String) {
        saveStored(activity, tag)
        val resolvedTag = resolveTag(tag)
        try {
            java.io.File(activity.cacheDir, "tsuki_home_feed_cache.json").delete()
        } catch (_: Throwable) {}
        val locale = Locale.forLanguageTag(resolvedTag)
        Locale.setDefault(locale)
        val config = Configuration(activity.resources.configuration)
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        }
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
        @Suppress("DEPRECATION")
        activity.applicationContext.resources.updateConfiguration(config, activity.applicationContext.resources.displayMetrics)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = activity.getSystemService(android.app.LocaleManager::class.java)
                val localeList = if (tag.isBlank()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(resolvedTag)
                localeManager?.applicationLocales = localeList
            } catch (_: Throwable) {
            }
        }
        activity.recreate()
    }
}
