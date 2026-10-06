package com.wisdomtower.academy

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Native Notification & Study Timer Director for Wisdom Tower Academy.
 *
 * Manages:
 * 1. Rewarding Study Timer completion notifications (~40 warm, inspiring messages, native icons, no emojis).
 * 2. Planner due-time reminders with warm personalized template (real name, task, time).
 * 3. Daily study goal nudges with caring, encouraging Duolingo-like tone.
 * 4. Granular notification category settings with sensible defaults (all on).
 */
object AcademyNotificationManager {

    const val PREFS_NAME = "wta_notification_prefs"
    const val KEY_TIMER_ENABLED = "pref_timer_notifications"
    const val KEY_PLANNER_ENABLED = "pref_planner_notifications"
    const val KEY_GOALS_ENABLED = "pref_goals_notifications"
    const val KEY_UPDATES_ENABLED = "pref_updates_notifications"

    const val CHANNEL_TIMER = "wta_channel_study_timer"
    const val CHANNEL_PLANNER = "wta_channel_planner"
    const val CHANNEL_GOALS = "wta_channel_daily_goals"
    const val CHANNEL_UPDATES = "wisdom_tower_notifications"

    const val EXTRA_TARGET_URL = "extra_target_url"

    // ~40 warm, inspiring, rewarding study timer completion messages (no emojis, native typography)
    val TIMER_REWARDING_MESSAGES = listOf(
        "Great focus today. Every session builds lasting mastery.",
        "Time's up. You honored your commitment to study.",
        "Session complete. Take a deep breath and let the knowledge settle.",
        "Outstanding dedication. Consistency is the secret of high achievers.",
        "You finished your study block. Progress is made one session at a time.",
        "Focused time well spent. Your future self is thanking you right now.",
        "Study goal reached. Step away, stretch, and rest your eyes.",
        "Brilliant effort today. Small daily habits lead to monumental results.",
        "Session concluded. You showed up and put in the work.",
        "Time is up. That was a solid stretch of uninterrupted concentration.",
        "Great work. You turned intention into real progress.",
        "Study block completed. Every minute invested here pays dividends later.",
        "Well done. You proved discipline over distraction today.",
        "Deep work session finished. High performance is built step by step.",
        "You crossed the finish line for this session. Excellent focus.",
        "Session accomplished. Your dedication to your craft shows.",
        "Timer finished. Celebrate this win—consistent practice is rare and valuable.",
        "Another milestone reached. Step away and recharge your mind.",
        "Focused, steady, and completed. You are building real capability.",
        "Session ended with excellence. Be proud of the discipline you demonstrated.",
        "Done and dusted. That was an exceptional study block.",
        "You stayed with it until the last second. Remarkable perseverance.",
        "Focus session ended. Your persistence will turn into distinction.",
        "Outstanding focus. You just conquered another chapter in your journey.",
        "Timer done. Clear your thoughts and enjoy a well-deserved pause.",
        "Another productive block completed. That is how champions study.",
        "You gave this session your full attention. High quality work accomplished.",
        "Time. Take pride in finishing what you started.",
        "Session wrapped up. Consistent effort always outperforms talent alone.",
        "Superb discipline. You stayed present and conquered the material.",
        "Mission accomplished for this study block. Rest now, return sharper.",
        "Timer expired. You invested in your mind today, and it will show.",
        "Great session. You made difficult concepts look manageable.",
        "That's a wrap. Focused hours are the cornerstone of academic success.",
        "You finished strong. True learning takes patience, and you showed it.",
        "Session complete. Steady discipline produces extraordinary intellect.",
        "Focus achieved. Give yourself credit for staying on task.",
        "Timer complete. Step back, hydrate, and enjoy your progress.",
        "Solid session in the books. You are steadily closing the gap to mastery.",
        "Time's up. You honored your study block with great concentration.",
        "Sharp focus and great resolve. You made this study session count.",
        "A victorious focus block. Step back and celebrate your discipline."
    )

    // Daily study goal nudges: caring, Duolingo-like tone, encouraging and gentle
    val DAILY_GOAL_NUDGES = listOf(
        "A little step today goes a long way. 10 minutes could change your whole week.",
        "Your future self will thank you for opening this today. Ready for a quick study session?",
        "No pressure, just checking in on your daily goal. Let's tackle one small topic together.",
        "Hey, even 5 minutes of review keeps the momentum alive. You've got this.",
        "We believe in you. Jump back in whenever you're ready—we saved your spot.",
        "A quick quiz or flashcard review today will keep your streak shining.",
        "Progress isn't about perfection; it's about showing up. Take one small step today.",
        "Your goals are waiting for you. Take a brief break from the noise and learn something fresh."
    )

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // 1. Study Timer Channel
        val timerChannel = NotificationChannel(
            CHANNEL_TIMER,
            "Study Timer Completions",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Rewarding alerts when your study focus blocks wrap up"
            enableVibration(true)
            enableLights(true)
        }
        nm.createNotificationChannel(timerChannel)

        // 2. Planner Reminders Channel
        val plannerChannel = NotificationChannel(
            CHANNEL_PLANNER,
            "Planner & Deadlines",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Due-time task reminders and scheduled study sessions"
            enableVibration(true)
            enableLights(true)
        }
        nm.createNotificationChannel(plannerChannel)

        // 3. Daily Study Goals Channel
        val goalsChannel = NotificationChannel(
            CHANNEL_GOALS,
            "Daily Study Goals",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Caring, gentle reminders when under your daily study target"
            enableVibration(true)
            enableLights(true)
        }
        nm.createNotificationChannel(goalsChannel)

        // 4. Academy Updates Channel
        val updatesChannel = NotificationChannel(
            CHANNEL_UPDATES,
            "Wisdom Tower Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New materials, exam schedules, and academy announcements"
            enableVibration(true)
            enableLights(true)
        }
        nm.createNotificationChannel(updatesChannel)
    }

    // Category Preference Helpers
    fun isTimerEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_TIMER_ENABLED, true)

    fun isPlannerEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_PLANNER_ENABLED, true)

    fun isGoalsEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_GOALS_ENABLED, true)

    fun isUpdatesEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_UPDATES_ENABLED, true)

    fun setSetting(ctx: Context, key: String, enabled: Boolean) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, enabled)
            .apply()
    }

    /**
     * Dispatches a rewarding study timer completion notification.
     * Uses a native notification icon, high priority, sound, and a random rewarding line.
     */
    fun notifyStudyTimerCompleted(ctx: Context, targetUrl: String = "https://www.wisdom-tower-academy.live/learning") {
        if (!isTimerEnabled(ctx)) return
        ensureChannels(ctx)

        val message = TIMER_REWARDING_MESSAGES.random()
        val title = "Study Session Complete"

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_URL, targetUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            ctx,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val accentColor = ContextCompat.getColor(ctx, R.color.brand_accent)

        val builder = NotificationCompat.Builder(ctx, CHANNEL_TIMER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(accentColor)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(1001, builder.build())
    }

    /**
     * Dispatches a planner due-time reminder with warm template:
     * "Hi [Name], it's time for [Task] ([Time]) — let's make some great progress today."
     */
    fun notifyPlannerTaskDue(
        ctx: Context,
        taskName: String,
        dueTimeText: String? = null,
        studentName: String? = null,
        targetUrl: String = "https://www.wisdom-tower-academy.live/learning"
    ) {
        if (!isPlannerEnabled(ctx)) return
        ensureChannels(ctx)

        val timeStr = dueTimeText?.takeIf { it.isNotBlank() }
            ?: SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        val greeting = if (!studentName.isNullOrBlank()) "Hi $studentName, " else ""
        val body = "${greeting}it's time for $taskName ($timeStr) — let's make some great progress today."
        val title = "Planner Reminder"

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_URL, targetUrl)
        }

        val requestCode = (taskName.hashCode() and 0xFFFF) + 2000
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val accentColor = ContextCompat.getColor(ctx, R.color.brand_accent)

        val builder = NotificationCompat.Builder(ctx, CHANNEL_PLANNER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(accentColor)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(requestCode, builder.build())
    }

    /**
     * Dispatches a caring, Duolingo-like daily study goal nudge when under daily target.
     */
    fun notifyDailyGoalNudge(
        ctx: Context,
        targetUrl: String = "https://www.wisdom-tower-academy.live/learning"
    ) {
        if (!isGoalsEnabled(ctx)) return
        ensureChannels(ctx)

        val message = DAILY_GOAL_NUDGES.random()
        val title = "Wisdom Tower Daily Check-in"

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_URL, targetUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            ctx,
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val accentColor = ContextCompat.getColor(ctx, R.color.brand_accent)

        val builder = NotificationCompat.Builder(ctx, CHANNEL_GOALS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(accentColor)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(pendingIntent)

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(3001, builder.build())
    }
}
