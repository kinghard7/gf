package com.king.luna.domain.model

// 流量等级，NONE = 当天不是经期
enum class FlowLevel(val label: String) {
    NONE("无"),
    LIGHT("轻"),
    MEDIUM("中"),
    HEAVY("重")
}
