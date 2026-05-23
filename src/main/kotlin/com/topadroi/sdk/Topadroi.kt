// TopadroiSDK — 輕量事件收集 SDK（Android）
//
// 與 iOS 對稱的公開 API。收集 + 緩衝 + 批次上報事件到 topadroi server 端（POST /v1/mobile/events）。
// MVP scaffold：app-scoped UUID、offline queue（SharedPreferences）、批次 HTTP 上報、event_id UUID。
// production 待硬化：SQLite queue、retry 指數退避、GAID（play-services-ads-identifier）、Install Referrer。

package com.topadroi.sdk

import com.topadroi.sdk.core.TopadroiCore
import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

data class Consent(val adTracking: Boolean = false, val analytics: Boolean = false)
data class Traits(val email: String? = null, val phone: String? = null, val externalId: String? = null)
data class Options(
    val endpoint: String = "https://capi-worker.topadroi.com/v1/mobile/events",
    val flushAt: Int = 50,
    val maxQueue: Int = 1000,
    val debug: Boolean = false,
)

object Topadroi {
    private lateinit var appContext: Context
    private var apiKey = ""
    private var site = ""
    private var options = Options()
    private var consent = Consent()
    private var traits = Traits()
    private val initialized = AtomicBoolean(false)
    private val io = Executors.newSingleThreadExecutor()
    private val lock = Any()
    private val queue = ArrayList<JSONObject>()

    private const val PREFS = "topadroi.prefs"
    private const val KEY_QUEUE = "queue"
    private const val KEY_ANON = "app_scoped_id"

    fun initialize(context: Context, apiKey: String, site: String, options: Options = Options()) {
        this.appContext = context.applicationContext
        this.apiKey = apiKey
        this.site = site
        this.options = options
        loadQueue()
        initialized.set(true)
        log("initialized site=$site")
    }

    fun setConsent(consent: Consent) { this.consent = consent }
    fun identify(traits: Traits) { this.traits = traits }

    fun track(event: String, properties: Map<String, Any?> = emptyMap()) {
        if (!initialized.get()) return
        // 組裝邏輯委派 JVM 已測的 core.TopadroiCore（tested=shipped）；JSONObject 僅作序列化邊界。
        val udMap = TopadroiCore.buildUserData(
            email = traits.email, phone = traits.phone, externalId = traits.externalId,
            adTracking = consent.adTracking, appScopedId = appScopedId(), gaid = gaid(),
        )
        val evMap = TopadroiCore.buildEvent(
            site = site, eventName = event, eventId = UUID.randomUUID().toString(),
            eventTime = System.currentTimeMillis() / 1000,
            properties = properties, userData = udMap, appMetadata = buildAppMetadata(),
            adTracking = consent.adTracking, analytics = consent.analytics,
        )
        val ev = JSONObject(evMap)   // org.json 遞迴 wrap 巢狀 Map/Collection
        val shouldFlush: Boolean
        synchronized(lock) {
            queue.add(ev)
            while (queue.size > options.maxQueue) queue.removeAt(0)
            shouldFlush = queue.size >= options.flushAt
            persistQueue()
        }
        if (shouldFlush) flush()
    }

    fun reset() {
        synchronized(lock) { queue.clear(); persistQueue() }
        traits = Traits()
    }

    fun flush() {
        val batch: List<JSONObject>
        synchronized(lock) {
            if (queue.isEmpty() || apiKey.isEmpty()) return
            batch = ArrayList(queue.take(100))
        }
        io.execute { sendBatch(batch) }
    }

    // app-scoped UUID（取代 GAID 當主匹配鍵）
    private fun appScopedId(): String {
        val sp = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.getString(KEY_ANON, null)?.let { return it }
        val v = UUID.randomUUID().toString()
        sp.edit().putString(KEY_ANON, v).apply()
        return v
    }

    // user_data 組裝已移至 core.TopadroiCore.buildUserData（JVM 單元測試覆蓋）。

    private fun buildAppMetadata(): Map<String, Any?> = linkedMapOf(
        "platform" to "android",
        "os_version" to (Build.VERSION.RELEASE ?: ""),
        "device_model" to (Build.MODEL ?: ""),
        "locale" to java.util.Locale.getDefault().toLanguageTag(),
        "timezone" to java.util.TimeZone.getDefault().id,
    )

    // production hook：接 play-services-ads-identifier。scaffold 回 null（無 GAID 不猜測）。
    private fun gaid(): String? = null

    private fun sendBatch(batch: List<JSONObject>) {
        try {
            val body = JSONObject().put("events", JSONArray(batch)).toString()
            val conn = (URL(options.endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-SDK-Version", "TopadroiSDK-Android/0.1")
                setRequestProperty("X-App-Id", appContext.packageName)  // bundle allowlist 驗證（opt-in）
                doOutput = true
                outputStream.use { it.write(body.toByteArray()) }
            }
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) {
                val sentIds = batch.mapNotNull { it.optString("event_id", null) }.toHashSet()
                synchronized(lock) {
                    queue.removeAll { sentIds.contains(it.optString("event_id", null)) }
                    persistQueue()
                }
                log("flushed ${batch.size} events")
            } else {
                log("flush failed code=$code (retry later)")
            }
        } catch (e: Exception) {
            log("flush error: ${e.message} (retry later)")
        }
    }

    private fun persistQueue() {
        val arr = JSONArray()
        queue.forEach { arr.put(it) }
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_QUEUE, arr.toString()).apply()
    }

    private fun loadQueue() {
        val s = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_QUEUE, null) ?: return
        val arr = JSONArray(s)
        synchronized(lock) {
            queue.clear()
            for (i in 0 until arr.length()) queue.add(arr.getJSONObject(i))
        }
    }

    private fun log(msg: String) { if (options.debug) android.util.Log.d("Topadroi", msg) }
}
