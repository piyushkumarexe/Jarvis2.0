package ai.jarvis.assistant.agent

import ai.jarvis.assistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** NVIDIA's OpenAI-compatible endpoint. Falls back safely when no key is configured or the network fails. */
class NvidiaPlanner(private val fallback: AiPlanner = SafeCommandPlanner()) : AiPlanner {
    override suspend fun plan(request: String): TaskPlan = withContext(Dispatchers.IO) {
        if (BuildConfig.NVIDIA_API_KEY.isBlank()) return@withContext fallback.plan(request)
        try {
            val connection = (URL("${BuildConfig.NVIDIA_BASE_URL}/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 15_000; readTimeout = 45_000; doOutput = true
                setRequestProperty("Authorization", "Bearer ${BuildConfig.NVIDIA_API_KEY}")
                setRequestProperty("Content-Type", "application/json")
            }
            val body = JSONObject().apply {
                put("model", BuildConfig.NVIDIA_MODEL); put("temperature", 0.1); put("max_tokens", 1200)
                put("messages", JSONArray().put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT)).put(JSONObject().put("role", "user").put("content", request)))
            }
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            if (connection.responseCode !in 200..299) return@withContext fallback.plan(request)
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            parsePlan(JSONObject(response).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"), request) ?: fallback.plan(request)
        } catch (_: Exception) { fallback.plan(request) }
    }

    private fun parsePlan(content: String, goal: String): TaskPlan? = try {
        val jsonText = content.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
        val root = JSONObject(jsonText); val actions = mutableListOf<AgentAction>(); val array = root.optJSONArray("actions") ?: return null
        for (i in 0 until array.length()) { val a=array.getJSONObject(i); val type=runCatching{ActionType.valueOf(a.getString("type").uppercase())}.getOrNull() ?: continue; actions += AgentAction(type,a.optString("value").takeIf{it.isNotBlank()},a.optString("target").takeIf{it.isNotBlank()},a.optBoolean("requiresConfirmation")) }
        if (actions.isEmpty()) null else TaskPlan(goal, actions + AgentAction(ActionType.FINISH))
    } catch (_: Exception) { null }

    companion object { private const val SYSTEM_PROMPT = """
You are JARVIS, an Android UI agent. Understand English, Hindi and Hinglish. Return ONLY JSON: {\"actions\":[{\"type\":\"OPEN_APP|CLICK|TYPE_TEXT|SEARCH|SCROLL|BACK|READ_SCREEN|SELECT|PLAY|ADD_TO_CART|SEND_MESSAGE|CALL|WAIT|FINISH\",\"value\":\"...\",\"target\":\"...\",\"requiresConfirmation\":true|false}]}. Use semantic targets, never coordinates. Mark SEND_MESSAGE, CALL, purchases, deletion and public posting as requiresConfirmation true. Never request passwords, OTPs or security bypasses. If uncertain, use READ_SCREEN.
""" }
}
