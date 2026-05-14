package com.duckblast.game.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.duckblast.game.data.model.Profile
import com.duckblast.game.data.model.Profile_
import io.objectbox.Box
import io.objectbox.kotlin.equal
import io.objectbox.query.QueryBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProfileRepository(
    private val context: Context,
    private val box: Box<Profile>
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAllProfiles(): List<Profile> =
        box.query().order(Profile_.lastPlayedAt, QueryBuilder.DESCENDING).build()
            .use { it.find() }

    fun getProfileById(id: Long): Profile? = box.get(id)

    fun saveProfile(profile: Profile): Long {
        if (profile.id == 0L && profile.createdAt == 0L) {
            profile.createdAt = System.currentTimeMillis()
        }
        return box.put(profile)
    }

    fun deleteProfile(id: Long) {
        box.remove(id)
        if (getSelectedProfileId() == id) clearSelectedProfileId()
    }

    fun updateHighScore(id: Long, score: Long) {
        val profile = box.get(id) ?: return
        if (score > profile.highScore) {
            profile.highScore = score
            box.put(profile)
        }
    }

    fun updateStats(id: Long, ducksHit: Int, shotsFired: Int, levelReached: Int) {
        val profile = box.get(id) ?: return
        profile.totalDucksHit += ducksHit
        profile.totalShotsFired += shotsFired
        profile.totalGamesPlayed += 1
        profile.lastPlayedAt = System.currentTimeMillis()
        if (levelReached > profile.highestLevelReached) {
            profile.highestLevelReached = levelReached
        }
        box.put(profile)
    }

    fun getSelectedProfileId(): Long = prefs.getLong(KEY_SELECTED, 0L)

    fun setSelectedProfileId(id: Long) {
        prefs.edit().putLong(KEY_SELECTED, id).apply()
    }

    fun clearSelectedProfileId() {
        prefs.edit().remove(KEY_SELECTED).apply()
    }

    fun getSelectedProfile(): Profile? {
        val id = getSelectedProfileId()
        return if (id == 0L) null else box.get(id)
    }

    fun findByName(name: String): Profile? {
        return box.query(
            Profile_.name.equal(name, QueryBuilder.StringOrder.CASE_INSENSITIVE)
        ).build().use { it.findFirst() }
    }

    fun updateStreak(id: Long) {
        val profile = box.get(id) ?: return
        val today = todayDate()
        val yesterday = yesterdayDate()
        profile.streakDays = when (profile.lastPlayedDate) {
            today -> profile.streakDays.coerceAtLeast(1)
            yesterday -> profile.streakDays + 1
            else -> 1
        }
        profile.lastPlayedDate = today
        profile.lastPlayedAt = System.currentTimeMillis()
        box.put(profile)
    }

    private fun todayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun yesterdayDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    companion object {
        private const val PREFS_NAME = "duckblast_prefs"
        private const val KEY_SELECTED = "selected_profile_id"
    }
}
