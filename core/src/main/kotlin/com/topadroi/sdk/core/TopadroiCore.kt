// SPDX-License-Identifier: Apache-2.0
// TopadroiCore — Android SDK 的平台無關核心邏輯（無 android.* / org.json 依賴）。
//
// 與 iOS TopadroiCore 對稱：事件組裝、user_data 建構、offline queue 皆為純 Kotlin（Map 結構），
// 可在 JVM 單元測試快速執行。平台層（Topadroi.kt）負責 Context/GAID/HTTP/JSONObject 序列化，
// 並委派此處組裝邏輯，確保「測到的邏輯＝出貨的邏輯」。

package com.topadroi.sdk.core

object TopadroiCore {
    /** 組 user_data。gaid 僅在 adTracking=true 且非空時帶入（隱私預設安全；尊重 limit ad tracking）。 */
    fun buildUserData(
        email: String?,
        phone: String?,
        externalId: String?,
        adTracking: Boolean,
        appScopedId: String,
        gaid: String?,
    ): Map<String, Any?> {
        val ud = LinkedHashMap<String, Any?>()
        ud["anon_id"] = appScopedId
        if (email != null) ud["em"] = email
        if (phone != null) ud["ph"] = phone
        if (externalId != null) ud["external_id"] = externalId
        if (adTracking && !gaid.isNullOrEmpty()) ud["gaid"] = gaid
        return ud
    }

    /** 組單一 event payload（與後端 /v1/mobile/events schema 一致）。 */
    fun buildEvent(
        site: String,
        eventName: String,
        eventId: String,
        eventTime: Long,
        properties: Map<String, Any?>,
        userData: Map<String, Any?>,
        appMetadata: Map<String, Any?>,
        adTracking: Boolean,
        analytics: Boolean,
    ): Map<String, Any?> = linkedMapOf(
        "site_id" to site,
        "event_name" to eventName,
        "event_id" to eventId,
        "event_time" to eventTime,
        "user_data" to userData,
        "event_data" to properties,
        "app_metadata" to appMetadata,
        "consent" to linkedMapOf("ad_tracking" to adTracking, "analytics" to analytics),
    )
}

/** 純邏輯 offline 事件佇列（in-memory；持久化由平台層序列化）。執行緒安全由呼叫端保證。 */
class EventQueue(maxQueue: Int = 1000) {
    private val maxQueue = if (maxQueue < 1) 1 else maxQueue
    private val events = ArrayList<Map<String, Any?>>()

    val size: Int get() = events.size
    fun snapshot(): List<Map<String, Any?>> = events.toList()

    /** 加入事件，超過上限丟最舊（回傳是否有丟棄）。 */
    fun enqueue(event: Map<String, Any?>): Boolean {
        events.add(event)
        if (events.size > maxQueue) {
            while (events.size > maxQueue) events.removeAt(0)
            return true
        }
        return false
    }

    fun head(n: Int): List<Map<String, Any?>> = events.take(n)

    fun removeByIds(ids: Set<String>) {
        events.removeAll { ids.contains(it["event_id"] as? String ?: "") }
    }

    fun clear() = events.clear()

    fun load(list: List<Map<String, Any?>>) {
        events.clear()
        events.addAll(list)
    }
}
