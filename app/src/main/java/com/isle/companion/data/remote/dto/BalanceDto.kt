package com.isle.companion.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BalanceResponse(
    @SerialName("is_available") val isAvailable: Boolean = false,
    @SerialName("balance_infos") val balanceInfos: List<BalanceInfo> = emptyList()
)

@Serializable
data class BalanceInfo(
    val currency: String = "",
    @SerialName("total_balance") val totalBalance: String = "",
    @SerialName("granted_balance") val grantedBalance: String = "",
    @SerialName("topped_up_balance") val toppedUpBalance: String = ""
)
