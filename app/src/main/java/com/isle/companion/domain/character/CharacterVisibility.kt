package com.isle.companion.domain.character

import com.isle.companion.data.local.entity.CharacterEntity

/**
 * 角色可见性：「恋人」定位已下线，历史角色数据保留但不再展示。
 * 年龄门移除后，不再按年龄段过滤角色。
 */
private val ALWAYS_HIDDEN_RELATIONS = setOf("love")

fun List<CharacterEntity>.visibleFor(): List<CharacterEntity> =
    filterNot { it.relationType in ALWAYS_HIDDEN_RELATIONS }
