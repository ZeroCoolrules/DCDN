package com.example.data.model

data class TokenAllocation(
    val category: String,
    val allocationPercent: Double,
    val totalTokens: String,
    val vestingSchedule: String,
    val purpose: String
)
