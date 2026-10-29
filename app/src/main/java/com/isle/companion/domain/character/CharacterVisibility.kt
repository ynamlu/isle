package com.isle.companion.domain.character

import com.isle.companion.data.local.entity.CharacterEntity

/** 青少年模式下不开放的关系类型；已有角色隐藏但数据保留。 */
private val MINOR_HIDDEN_RELATIONS = setOf("love", "custom")

fun List<CharacterEntity>.visibleFor(isMinor: Boolean): List<CharacterEntity> =
    if (!isMinor) this else filterNot { it.relationType in MINOR_HIDDEN_RELATIONS }
