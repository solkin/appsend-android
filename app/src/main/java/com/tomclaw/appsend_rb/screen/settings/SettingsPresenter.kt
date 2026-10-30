package com.tomclaw.appsend_rb.screen.settings

import com.tomclaw.appsend_rb.domain.AppPreferences
import com.tomclaw.appsend_rb.domain.ExportFormat
import com.tomclaw.appsend_rb.domain.ExportStorage
import com.tomclaw.appsend_rb.domain.ThemeMode
import com.tomclaw.appsend_rb.domain.Analytics
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

data class SettingsViewModel(
    val themeMode: ThemeMode,
    val dynamicColorsAvailable: Boolean,
    val dynamicColors: Boolean,
    val analytics: Boolean,
    val exportFormat: ExportFormat,
    val folder: String,
    val template: String,
    val version: String,
)

/** What the presenter needs from the platform, none of it worth a repository. */
interface SettingsEnvironment {

    fun applyTheme(mode: ThemeMode)

    fun dynamicColorsAvailable(): Boolean

    fun version(): String

}

interface SettingsInteractor {

    fun themeMode(): ThemeMode

    fun setThemeMode(mode: ThemeMode)

    fun dynamicColorsAvailable(): Boolean

    fun dynamicColors(): Boolean

    fun setDynamicColors(enabled: Boolean)

    fun analytics(): Boolean

    fun setAnalytics(enabled: Boolean)

    fun exportFormat(): ExportFormat

    fun setExportFormat(format: ExportFormat)

    fun template(): String

    fun setTemplate(template: String)

    fun folder(): String

    fun setFolder(uri: String)

    fun version(): String

}

class SettingsInteractorImpl(
    private val preferences: AppPreferences,
    private val storage: ExportStorage,
    private val environment: SettingsEnvironment,
) : SettingsInteractor {

    override fun themeMode(): ThemeMode = preferences.themeMode

    override fun setThemeMode(mode: ThemeMode) {
        preferences.themeMode = mode
        environment.applyTheme(mode)
    }

    override fun dynamicColorsAvailable(): Boolean = environment.dynamicColorsAvailable()

    override fun dynamicColors(): Boolean = preferences.dynamicColors

    override fun setDynamicColors(enabled: Boolean) {
        preferences.dynamicColors = enabled
    }

    override fun analytics(): Boolean = preferences.analyticsEnabled

    override fun setAnalytics(enabled: Boolean) {
        preferences.analyticsEnabled = enabled
    }

    override fun exportFormat(): ExportFormat = preferences.exportFormat

    override fun setExportFormat(format: ExportFormat) {
        preferences.exportFormat = format
    }

    override fun template(): String = preferences.fileNameTemplate

    override fun setTemplate(template: String) {
        preferences.fileNameTemplate = template
    }

    override fun folder(): String = storage.location()

    override fun setFolder(uri: String) {
        preferences.exportFolder = uri
    }

    override fun version(): String = environment.version()

}

interface SettingsRouter {

    fun pickFolder()

    fun openRate()

    fun openDeveloperApps()

    fun restart()

}

interface SettingsPresenter {

    fun attachView(view: SettingsView)

    fun detachView()

    fun attachRouter(router: SettingsRouter)

    fun detachRouter()

    fun onFolderPicked(uri: String)

    fun onShown()

}

class SettingsPresenterImpl(
    private val interactor: SettingsInteractor,
    private val analytics: Analytics,
) : SettingsPresenter {

    private var view: SettingsView? = null
    private var router: SettingsRouter? = null
    private val subscriptions = CompositeDisposable()

    override fun attachView(view: SettingsView) {
        this.view = view
        analytics.trackEvent("screen_open", mapOf("screen" to "settings"))
        subscriptions += view.themeChanges().subscribe {
            interactor.setThemeMode(it)
            changed("theme", it.name)
        }
        subscriptions += view.dynamicColorChanges().subscribe {
            interactor.setDynamicColors(it)
            changed("dynamic_colors", it.toString())
            router?.restart()
        }
        subscriptions += view.analyticsChanges().subscribe {
            // Report the opt-out itself while reporting is still on.
            changed("analytics", it.toString())
            interactor.setAnalytics(it)
        }
        subscriptions += view.formatChanges().subscribe {
            interactor.setExportFormat(it)
            changed("export_format", it.name)
        }
        subscriptions += view.templateChanges().subscribe {
            interactor.setTemplate(it)
            changed("file_name_template", "custom")
        }
        subscriptions += view.folderClicks().subscribe { router?.pickFolder() }
        subscriptions += view.rateClicks().subscribe { router?.openRate() }
        subscriptions += view.developerAppsClicks().subscribe { router?.openDeveloperApps() }
        render()
    }

    override fun detachView() {
        subscriptions.clear()
        view = null
    }

    override fun attachRouter(router: SettingsRouter) {
        this.router = router
    }

    override fun detachRouter() {
        router = null
    }

    /** Other screens change the export settings too, while this tab stays alive. */
    override fun onShown() = render()

    override fun onFolderPicked(uri: String) {
        interactor.setFolder(uri)
        changed("export_folder", "custom")
    }

    private fun changed(setting: String, value: String) {
        analytics.trackEvent("settings_changed", mapOf("setting" to setting, "value" to value))
        render()
    }

    private fun render() {
        view?.render(
            SettingsViewModel(
                themeMode = interactor.themeMode(),
                dynamicColorsAvailable = interactor.dynamicColorsAvailable(),
                dynamicColors = interactor.dynamicColors(),
                analytics = interactor.analytics(),
                exportFormat = interactor.exportFormat(),
                folder = interactor.folder(),
                template = interactor.template(),
                version = interactor.version(),
            )
        )
    }

}
