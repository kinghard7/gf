package com.king.luna.domain.model

enum class Mood(val emoji: String, val label: String) {
    CALM("😌", "平静"),
    HAPPY("😊", "开心"),
    TIRED("😴", "疲惫"),
    IRRITABLE("😤", "烦躁"),
    ANXIOUS("😰", "焦虑"),
    SAD("😢", "低落")
}
