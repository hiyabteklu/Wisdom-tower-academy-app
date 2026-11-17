package com.wisdomtower.academy.data.reminders

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.wisdomtower.academy.ui.study.StudyBlock
import java.util.Calendar

/**
 * Reminders for the NATIVE Study Planner (owner decision 2026-10-12: the native planner must send reminder
 * notifications; no sharing with the website planner).
 *
 * How it works: every block of the weekly timetable gets ONE system alarm for its next start time (its weekday and
 * "HH:MM"). When the alarm fires, [PlannerReminderReceiver] posts the existing "Planner Reminder" notification
 * (`AcademyNotificationManager.notifyPlannerTaskDue`, which respects the Settings switch "Planner & Deadlines") and sets
 * the alarm for the same weekday next week. Alarms work while the app is closed and offline (the blocks are read from
 * the phone copy). [sync] is called after every change of the timetable; [PlannerBootReceiver] calls it again after a
 * reboot, an app update, a clock change, a time zone change, or when the student switches "Alarms & reminders" on/off
 * (alarms do not survive a reboot).
 *
 * Exactness (2026-11-17, reverses S1 of 2026-10-17): class-schedule and academic-calendar reminders must arrive at the
 * minute (inexact alarms were arriving 30-50 minutes late). The app declares `SCHEDULE_EXACT_ALARM` (the user-granted
 * one, NOT `USE_EXACT_ALARM`) and uses [AlarmManager.setExactAndAllowWhileIdle] when [canUseExact] is true. When the
 * student has not allowed "Alarms & reminders" it falls back to [AlarmManager.setAndAllowWhileIdle] (may be late).
 * [askExactAlarmOnce] explains and opens that system screen once. Play Console: the exact-alarm declaration must say
 * the core function is timetable / calendar reminders at the exact start time of each block.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object PlannerReminderScheduler {
    private const val PREFS = "wta_planner_reminders"
    private const val KEY_SCHEDULED = "scheduled_ids"
    private const val KEY_ASKED_NOTIFICATIONS = "asked_notification_permission"
    private const val KEY_ASKED_EXACT = "asked_exact_alarm_permission"
    private const val NOTIFICATION_PERMISSION_REQUEST = 7301

    const val ACTION_FIRE = "com.wisdomtower.academy.PLANNER_REMINDER"
    const val EXTRA_BLOCK_ID = "planner_block_id"

    /** The page a tapped reminder opens (the native planner once patch P10 is applied). */
    const val TARGET_URL = "https://www.wisdom-tower-academy.live/learning?tool=planner"

    /**
     * First moment after [after] (milliseconds) that is [day] (0 = Monday ... 6 = Sunday) at [startMinutes] after midnight,
     * in the phone's time zone. Uses calendar days, so summer/winter time changes do not move the reminder.
     */
    fun nextTriggerMillis(day: Int, startMinutes: Int, after: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = after
        val targetDow = if (day == 6) Calendar.SUNDAY else day + 2 // Calendar.MONDAY is 2 ... SATURDAY is 7
        cal.set(Calendar.HOUR_OF_DAY, startMinutes / 60)
        cal.set(Calendar.MINUTE, startMinutes % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val diff = (targetDow - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
        cal.add(Calendar.DAY_OF_YEAR, diff)
        if (cal.timeInMillis <= after) cal.add(Calendar.DAY_OF_YEAR, 7)
        return cal.timeInMillis
    }

    /** Replaces all planner alarms by one alarm per block of [blocks]. Safe to call as often as needed. */
    fun sync(context: Context, blocks: List<StudyBlock>) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = prefs.getStringSet(KEY_SCHEDULED, emptySet()) ?: emptySet()
        old.forEach { cancel(app, it) }
        val now = System.currentTimeMillis()
        val ids = HashSet<String>()
        blocks.forEach { b ->
            schedule(app, b, now)
            ids.add(b.id)
        }
        prefs.edit().putStringSet(KEY_SCHEDULED, ids).apply()
    }

    /**
     * True when the phone lets this app set exact alarms (Android 12+: the student switched on "Alarms & reminders";
     * older Android: always).
     */
    fun canUseExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.applicationContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return am.canScheduleExactAlarms()
    }

    /** Sets the alarm of one block for its first start time after [after]: exact when allowed, otherwise approximate. */
    fun schedule(context: Context, block: StudyBlock, after: Long) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val parts = block.startTime.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val trigger = nextTriggerMillis(block.day, h * 60 + m, after)
        val pi = pendingIntent(app, block.id)
        if (canUseExact(app)) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                return
            } catch (e: SecurityException) {
                // The student switched the permission off a moment ago: fall through to the approximate alarm.
            }
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
    }

    private fun cancel(app: Context, blockId: String) {
        val am = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        am.cancel(pendingIntent(app, blockId))
    }

    private fun pendingIntent(app: Context, blockId: String): PendingIntent {
        val intent = Intent(app, PlannerReminderReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_BLOCK_ID, blockId)
        return PendingIntent.getBroadcast(
            app,
            blockId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun findActivity(context: Context?): Activity? {
        var c: Context? = context
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }

    /**
     * Android 13+ shows reminders only after the student allows notifications. Asks once (the first time a block is
     * added), only when [context] is (or wraps) an Activity and the permission is not granted yet. The answer is not
     * needed here: the system remembers it.
     */
    fun askNotificationPermissionOnce(context: Context?) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val app = context.applicationContext
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false)) return
        val activity = findActivity(context) ?: return
        prefs.edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply()
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
    }

    /**
     * Explains once, in plain words, why "Alarms & reminders" should be switched on, and opens that system screen.
     * Does nothing when exact alarms are already allowed, when [context] is not an Activity, when the question was
     * already asked, or while the notification permission is still missing (that question comes first; this one
     * follows the next time a block is added). Reminders keep working (approximately) if the student says "Not now".
     * When the student switches the permission on, the system tells [PlannerBootReceiver], which rebuilds the alarms exact.
     */
    fun askExactAlarmOnce(context: Context?) {
        if (context == null || canUseExact(context)) return
        val app = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED_EXACT, false)) return
        val activity = findActivity(context) ?: return
        prefs.edit().putBoolean(KEY_ASKED_EXACT, true).apply()
        activity.runOnUiThread {
            if (activity.isFinishing) return@runOnUiThread
            AlertDialog.Builder(activity)
                .setTitle("Get reminders on time")
                .setMessage(
                    "To remind you at the exact minute each class or study block starts, allow \"Alarms & reminders\" " +
                        "for this app on the next screen. If you skip this, reminders can arrive several minutes late."
                )
                .setPositiveButton("Open setting") { _, _ -> openExactAlarmSettings(activity) }
                .setNegativeButton("Not now", null)
                .show()
        }
    }

    /** Opens the system "Alarms & reminders" screen of this app (Android 12+). Can also be used by a Settings row later. */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // A phone without this screen: nothing to open, reminders stay approximate.
        }
    }
}
