package com.isle.companion.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "isle_profile")

/**
 * 用户资料：全局共享，所有角色都能立刻了解你。
 */
data class UserProfile(
    /** 希望被怎么称呼 */
    val name: String = "",
    /** 性格自述 */
    val personality: String = "",
    /** 自我介绍 / 想说的话 */
    val intro: String = "",
    /** 年龄段："" 未设置 / "adult" 已满18 / "minor" 未满18 */
    val ageGroup: String = ""
) {
    /** 是否为未成年人（青少年模式） */
    val isMinor: Boolean get() = ageGroup == "minor"
    /** 是否已完成年龄确认 */
    val isAgeConfirmed: Boolean get() = ageGroup.isNotEmpty()
    val isFilled: Boolean
        get() = name.isNotBlank() || personality.isNotBlank() || intro.isNotBlank()
}

class UserProfileRepository(private val context: Context) {

    private object Keys {
        val NAME = stringPreferencesKey("user_name")
        val PERSONALITY = stringPreferencesKey("user_personality")
        val INTRO = stringPreferencesKey("user_intro")
        val AGE_GROUP = stringPreferencesKey("age_group")
    }

    val profile: Flow<UserProfile> = context.profileDataStore.data.map { prefs ->
        UserProfile(
            name = prefs[Keys.NAME] ?: "",
            personality = prefs[Keys.PERSONALITY] ?: "",
            intro = prefs[Keys.INTRO] ?: "",
            ageGroup = prefs[Keys.AGE_GROUP] ?: ""
        )
    }

    suspend fun updateName(value: String) = context.profileDataStore.edit { it[Keys.NAME] = value.trim() }

    suspend fun updatePersonality(value: String) =
        context.profileDataStore.edit { it[Keys.PERSONALITY] = value.trim() }

    suspend fun updateIntro(value: String) =
        context.profileDataStore.edit { it[Keys.INTRO] = value.trim() }

    /** 设置年龄段（adult / minor） */
    suspend fun setAgeGroup(value: String) =
        context.profileDataStore.edit { it[Keys.AGE_GROUP] = value }

    /** 清除年龄确认（用于重新选择） */
    suspend fun clearAgeGroup() =
        context.profileDataStore.edit { it.remove(Keys.AGE_GROUP) }
}
