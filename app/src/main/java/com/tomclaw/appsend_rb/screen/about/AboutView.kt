package com.tomclaw.appsend_rb.screen.about

import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.util.AppIcons
import com.tomclaw.knopochki.core.Alignment
import com.tomclaw.knopochki.core.Box
import com.tomclaw.knopochki.core.Column
import com.tomclaw.knopochki.core.Frame
import com.tomclaw.knopochki.core.Insets
import com.tomclaw.knopochki.core.KnopochkiView
import com.tomclaw.knopochki.draw.Icons
import com.tomclaw.knopochki.theme.Role
import com.tomclaw.knopochki.widget.Button
import com.tomclaw.knopochki.widget.ButtonStyle
import com.tomclaw.knopochki.widget.Image
import com.tomclaw.knopochki.widget.ImageScale
import com.tomclaw.knopochki.widget.Text
import com.tomclaw.knopochki.widget.TextAlign
import com.tomclaw.knopochki.widget.TextStyle
import com.tomclaw.knopochki.widget.TopAppBar
import io.reactivex.rxjava3.core.Observable

interface AboutView {

    fun navigationClicks(): Observable<Unit>

    fun rateClicks(): Observable<Unit>

    fun projectsClicks(): Observable<Unit>

    fun setVersion(version: String)

}

class AboutViewImpl(knopochkiView: KnopochkiView) : AboutView {

    private val context = knopochkiView.context
    private val resources = context.resources

    private val navigationRelay = PublishRelay.create<Unit>()
    private val rateRelay = PublishRelay.create<Unit>()
    private val projectsRelay = PublishRelay.create<Unit>()

    private val versionText = Text(
        "",
        style = TextStyle.BODY_MEDIUM,
        role = Role.ON_SURFACE_VARIANT,
        align = TextAlign.CENTER,
    )

    init {
        val bar = TopAppBar(
            title = resources.getString(R.string.info),
            navigationIcon = Icons.ARROW_BACK,
            onNavigationClick = { navigationRelay.accept(Unit) },
        )
        val icon = Image(scale = ImageScale.FIT)
        val sizePx = (ICON_DP * resources.displayMetrics.density).toInt()
        AppIcons.load(context, context.packageName, sizePx) { _, bitmap -> icon.bitmap = bitmap }

        val info = Column(spacingDp = 4f, paddingDp = 24f)
            .add(Frame(icon, widthDp = ICON_DP, heightDp = ICON_DP), align = Alignment.CENTER)
            .add(
                Text(
                    resources.getString(R.string.app_name),
                    style = TextStyle.TITLE_LARGE,
                    role = Role.ON_SURFACE,
                    align = TextAlign.CENTER,
                ),
                align = Alignment.CENTER,
            )
            .add(versionText, align = Alignment.CENTER)
            .add(
                Text(
                    resources.getString(R.string.tomclaw_software),
                    style = TextStyle.BODY_LARGE,
                    role = Role.ON_SURFACE_VARIANT,
                    align = TextAlign.CENTER,
                ),
                align = Alignment.CENTER,
            )

        val buttons = Column(spacingDp = 8f, paddingDp = 16f)
            .add(
                Button(resources.getString(R.string.rate_app), ButtonStyle.FILLED) {
                    rateRelay.accept(Unit)
                }
            )
            .add(
                Button(resources.getString(R.string.all_apps), ButtonStyle.TONAL) {
                    projectsRelay.accept(Unit)
                }
            )

        knopochkiView.root = Column()
            .add(bar)
            .addFilling(Box().add(info, horizontal = Alignment.CENTER, vertical = Alignment.CENTER))
            .add(Insets(buttons, applyBottom = true, applySides = true))
    }

    override fun navigationClicks(): Observable<Unit> = navigationRelay

    override fun rateClicks(): Observable<Unit> = rateRelay

    override fun projectsClicks(): Observable<Unit> = projectsRelay

    override fun setVersion(version: String) {
        versionText.text = version
    }

    private companion object {
        const val ICON_DP = 72f
    }
}
