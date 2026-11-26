package com.csymvoul.expenses.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.csymvoul.expenses.MainActivity
import com.csymvoul.expenses.R
import com.csymvoul.expenses.data.AppSettings
import com.csymvoul.expenses.data.ExpenseStore
import java.io.File
import java.time.YearMonth

/** Home-screen widget: this month's total, the trip on today, up to three goal bars (each with its painted emblem), and Quick add. */
class LedgerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        manager.updateAppWidget(ids, build(context))
    }

    private data class Ward(val row: Int, val icon: Int, val text: Int, val bar: Int)

    companion object {
        private val WARDS = listOf(
            Ward(R.id.widget_ward1, R.id.widget_ward1_icon, R.id.widget_ward1_text, R.id.widget_ward1_bar),
            Ward(R.id.widget_ward2, R.id.widget_ward2_icon, R.id.widget_ward2_text, R.id.widget_ward2_bar),
            Ward(R.id.widget_ward3, R.id.widget_ward3_icon, R.id.widget_ward3_text, R.id.widget_ward3_bar),
        )

        /** Redraws every placed widget, e.g. after an expense or a setting changed. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LedgerWidget::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, build(context))
        }

        private fun build(context: Context): RemoteViews {
            val settings = AppSettings.get(context) // also applies the number style
            val data = ExpenseStore(File(context.filesDir, "expenses.json")).load()
            val content = WidgetContent.of(data, YearMonth.now(), settings.words)
            val views = RemoteViews(context.packageName, R.layout.widget_ledger)
            views.setTextViewText(R.id.widget_heading, content.heading)
            views.setTextViewText(R.id.widget_total, content.total)
            views.setTextViewText(R.id.widget_label, content.label)
            views.setViewVisibility(R.id.widget_trip_row, if (content.trip == null) View.GONE else View.VISIBLE)
            views.setTextViewText(R.id.widget_trip, content.trip ?: "")
            WARDS.forEachIndexed { i, (row, icon, text, bar) ->
                val ward = content.wards.getOrNull(i)
                views.setViewVisibility(row, if (ward == null) View.GONE else View.VISIBLE)
                if (ward != null) {
                    views.setImageViewResource(icon, ward.icon)
                    views.setTextViewText(text, ward.text)
                    // Over its goal: the line turns burgundy (gold by night), like the app's broken ward.
                    views.setTextColor(text, context.getColor(if (ward.isOver) R.color.widget_heading else R.color.widget_ink))
                    views.setProgressBar(bar, 100, ward.percent, false)
                }
            }
            views.setViewVisibility(R.id.widget_empty, if (content.emptyNote == null) View.GONE else View.VISIBLE)
            views.setTextViewText(R.id.widget_empty, content.emptyNote ?: "")
            views.setOnClickPendingIntent(R.id.widget_root, open(context, 3, quickAdd = false))
            views.setOnClickPendingIntent(R.id.widget_add, open(context, 4, quickAdd = true))
            return views
        }

        private fun open(context: Context, requestCode: Int, quickAdd: Boolean): PendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_RECORD, quickAdd)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
