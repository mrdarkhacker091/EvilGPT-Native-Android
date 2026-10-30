package com.evilgpt.app.data

import android.content.Context
import android.webkit.MimeTypeMap
import com.evilgpt.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

class EvilGptApi(context: Context) {
    private val prefs = context.getSharedPreferences("evilgpt_session", Context.MODE_PRIVATE)
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun currentSession(): UserSession? {
        val token = prefs.getString("access_token", null) ?: return null
        val userId = prefs.getString("user_id", null) ?: return null
        return UserSession(
            accessToken = token,
            refreshToken = prefs.getString("refresh_token", "") ?: "",
            userId = userId,
            email = prefs.getString("email", "") ?: "",
            expiresAt = prefs.getLong("expires_at", 0L)
        )
    }

    fun signOut() { prefs.edit().clear().apply() }

    suspend fun signIn(email: String, password: String): UserSession = withContext(Dispatchers.IO) {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val result = requestJson("POST", "/auth/v1/token?grant_type=password", body, authenticated = false)
        parseAndSaveSession(result)
    }

    suspend fun signUp(email: String, password: String): UserSession? = withContext(Dispatchers.IO) {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val result = requestJson("POST", "/auth/v1/signup", body, authenticated = false)
        if (result.has("access_token")) parseAndSaveSession(result) else null
    }

    private fun parseAndSaveSession(json: JSONObject): UserSession {
        val user = json.optJSONObject("user") ?: throw IOException("Authentication response did not contain a user.")
        val access = json.optString("access_token")
        if (access.isBlank()) throw IOException("Sign-up succeeded. Check your email to confirm your account, then sign in.")
        val refresh = json.optString("refresh_token")
        val userId = user.optString("id")
        val email = user.optString("email")
        val expires = System.currentTimeMillis() / 1000L + json.optLong("expires_in", 3600L)
        prefs.edit()
            .putString("access_token", access)
            .putString("refresh_token", refresh)
            .putString("user_id", userId)
            .putString("email", email)
            .putLong("expires_at", expires)
            .apply()
        return UserSession(access, refresh, userId, email, expires)
    }

    private fun ensureFreshSession() {
        val session = currentSession() ?: return
        if (session.expiresAt > System.currentTimeMillis() / 1000L + 90L || session.refreshToken.isBlank()) return
        val body = JSONObject().put("refresh_token", session.refreshToken)
        val builder = Request.Builder().url("$baseUrl/auth/v1/token?grant_type=refresh_token")
            .header("apikey", anonKey).header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonType))
        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (response.isSuccessful) parseAndSaveSession(JSONObject(raw))
            else throw apiError(response.code, raw)
        }
    }

    private fun authHeaders(token: String? = currentSession()?.accessToken): Map<String, String> = buildMap {
        put("apikey", anonKey)
        put("Content-Type", "application/json")
        if (!token.isNullOrBlank()) put("Authorization", "Bearer $token")
    }

    private fun requestJson(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = true,
        prefer: String? = null
    ): JSONObject {
        if (authenticated) ensureFreshSession()
        val builder = Request.Builder().url(baseUrl + path)
        authHeaders(if (authenticated) currentSession()?.accessToken else null).forEach { (k, v) -> builder.header(k, v) }
        if (prefer != null) builder.header("Prefer", prefer)
        val requestBody = body?.toString()?.toRequestBody(jsonType)
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(requestBody ?: "{}".toRequestBody(jsonType))
            "PATCH" -> builder.patch(requestBody ?: "{}".toRequestBody(jsonType))
            "DELETE" -> builder.delete(requestBody)
            else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
        }
        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw apiError(response.code, raw)
            return if (raw.isBlank()) JSONObject() else if (raw.trimStart().startsWith("[")) (JSONArray(raw).optJSONObject(0) ?: JSONObject()) else JSONObject(raw)
        }
    }

    private fun requestArray(path: String): JSONArray {
        ensureFreshSession()
        val builder = Request.Builder().url(baseUrl + path).get()
        authHeaders().forEach { (k, v) -> builder.header(k, v) }
        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw apiError(response.code, raw)
            return JSONArray(raw)
        }
    }

    private fun apiError(code: Int, raw: String): IOException {
        val message = try {
            val json = JSONObject(raw)
            json.optString("msg").ifBlank { json.optString("message") }.ifBlank { json.optString("error_description") }.ifBlank { json.optString("error") }
        } catch (_: Exception) { raw.take(240) }
        return IOException(message.ifBlank { "Request failed (HTTP $code)." })
    }

    suspend fun loadModels(): List<AiModel> = withContext(Dispatchers.IO) {
        val config = requestJson("POST", "/rest/v1/rpc/get_public_ai_config", JSONObject())
        val categories = config.optJSONObject("model_categories") ?: return@withContext EvilGptModels.fallback
        val enabled = config.optJSONArray("enabled_models")?.toStringSet().orEmpty()
        val names = config.optJSONObject("model_custom_names")
        val resultModels = mutableListOf<AiModel>()
        listOf("free", "uncensored", "normal").forEach { category ->
            val ids = categories.optJSONArray(category) ?: return@forEach
            for (i in 0 until ids.length()) {
                val id = ids.optString(i)
                if (enabled.isNotEmpty() && id !in enabled) continue
                val fallback = EvilGptModels.fallback.firstOrNull { it.id == id }
                resultModels += AiModel(
                    id = id,
                    label = names?.optString(id)?.takeIf { !it.isNullOrBlank() } ?: fallback?.label ?: id,
                    description = fallback?.description ?: "EvilGPT AI model",
                    category = category
                )
            }
        }
        resultModels.ifEmpty { EvilGptModels.fallback }
    }

    suspend fun loadSessions(): List<ChatSession> = withContext(Dispatchers.IO) {
        val rows = requestArray("/rest/v1/chat_sessions?select=*&order=updated_at.desc&limit=100")
        (0 until rows.length()).mapNotNull { i ->
            val row = rows.optJSONObject(i) ?: return@mapNotNull null
            ChatSession(
                id = row.optString("id"),
                title = row.optString("title", "New Chat"),
                customTitle = row.optString("custom_title").takeIf { it.isNotBlank() && it != "null" },
                starred = row.optBoolean("starred"),
                updatedAt = row.optString("updated_at")
            )
        }
    }

    suspend fun createSession(): ChatSession = withContext(Dispatchers.IO) {
        val data = requestJson("POST", "/rest/v1/chat_sessions?select=*", JSONObject().put("user_id", currentSession()?.userId), prefer = "return=representation")
        ChatSession(data.optString("id"), data.optString("title", "New Chat"), data.optString("custom_title").takeIf { it.isNotBlank() && it != "null" }, data.optBoolean("starred"), data.optString("updated_at"))
    }

    suspend fun loadMessages(sessionId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        val id = enc(sessionId)
        val rows = requestArray("/rest/v1/chat_messages?select=id,role,content,created_at&session_id=eq.$id&order=created_at.asc")
        (0 until rows.length()).mapNotNull { i ->
            val row = rows.optJSONObject(i) ?: return@mapNotNull null
            ChatMessage(row.optString("id"), row.optString("role"), row.optString("content"), row.optString("created_at"))
        }
    }

    suspend fun saveMessage(sessionId: String, role: String, content: String, images: List<ChatAttachment> = emptyList()) = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("session_id", sessionId)
            .put("user_id", currentSession()?.userId)
            .put("role", role)
            .put("content", content)
        if (images.isNotEmpty()) body.put("images", attachmentsJson(images))
        requestJson("POST", "/rest/v1/chat_messages", body, prefer = "return=minimal")
    }

    suspend fun uploadImage(sessionId: String, fileName: String, mimeType: String, bytes: ByteArray): ChatAttachment = withContext(Dispatchers.IO) {
        ensureFreshSession()
        val userId = currentSession()?.userId ?: throw IOException("Please sign in before uploading an image.")
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        val safeName = fileName.substringAfterLast('/').substringAfterLast('\\').ifBlank { "image.$extension" }
        val path = "$userId/$sessionId/${UUID.randomUUID()}.$extension"
        val encodedPath = path.split('/').joinToString("/") { enc(it) }
        val request = Request.Builder()
            .url("$baseUrl/storage/v1/object/chat-images/$encodedPath")
            .header("apikey", anonKey)
            .header("Authorization", "Bearer ${currentSession()?.accessToken.orEmpty()}")
            .header("Content-Type", mimeType)
            .header("x-upsert", "false")
            .post(bytes.toRequestBody(mimeType.toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw apiError(response.code, raw)
        }
        ChatAttachment(safeName, path, mimeType, bytes.size.toLong())
    }

    private fun attachmentsJson(images: List<ChatAttachment>) = JSONArray().apply {
        images.forEach { image ->
            put(JSONObject().put("name", image.name).put("path", image.path).put("mimeType", image.mimeType).put("size", image.size))
        }
    }

    suspend fun renameSession(sessionId: String, title: String) = withContext(Dispatchers.IO) {
        requestJson("PATCH", "/rest/v1/chat_sessions?id=eq.${enc(sessionId)}", JSONObject().put("custom_title", title), prefer = "return=minimal")
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        requestJson("DELETE", "/rest/v1/chat_messages?session_id=eq.${enc(sessionId)}", null, prefer = "return=minimal")
        requestJson("DELETE", "/rest/v1/chat_sessions?id=eq.${enc(sessionId)}", null, prefer = "return=minimal")
    }

    suspend fun toggleStar(sessionId: String, starred: Boolean) = withContext(Dispatchers.IO) {
        requestJson("PATCH", "/rest/v1/chat_sessions?id=eq.${enc(sessionId)}", JSONObject().put("starred", starred), prefer = "return=minimal")
    }

    suspend fun streamChat(
        messages: List<ChatMessage>,
        modelId: String,
        webSearch: Boolean,
        deepThink: Boolean,
        images: List<ChatAttachment> = emptyList(),
        onDelta: (String) -> Unit,
        onStatus: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        ensureFreshSession()
        val payload = JSONObject()
            .put("messages", JSONArray().apply {
                messages.takeLast(100).forEach { put(JSONObject().put("role", it.role).put("content", it.content.take(120_000))) }
            })
            .put("modelId", modelId)
            .put("webSearch", webSearch)
        if (deepThink) payload.put("deepThink", true)
        if (images.isNotEmpty()) payload.put("images", attachmentsJson(images))
        val builder = Request.Builder()
            .url("$baseUrl/functions/v1/evil-chat")
            .header("Authorization", "Bearer ${currentSession()?.accessToken.orEmpty()}")
            .header("apikey", anonKey)
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonType))
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                val raw = response.body?.string().orEmpty()
                throw apiError(response.code, raw)
            }
            val source = response.body?.source() ?: throw IOException("The AI server returned an empty response stream.")
            val output = StringBuilder()
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                val event = try { JSONObject(data) } catch (_: Exception) { continue }
                if (event.has("error")) {
                    val err = event.opt("error")
                    throw IOException(if (err is JSONObject) err.optString("message", "AI service error.") else err.toString())
                }
                if (event.has("status")) onStatus(event.optString("detail").ifBlank { event.optString("status") })
                val delta = event.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")?.optString("content")
                if (!delta.isNullOrEmpty()) {
                    output.append(delta)
                    onDelta(delta)
                }
            }
            output.toString().trim()
        }
    }

    private fun JSONArray.toStringSet(): Set<String> = buildSet { for (i in 0 until length()) add(optString(i)) }
    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
}
