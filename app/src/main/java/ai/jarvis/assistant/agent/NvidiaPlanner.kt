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
    override suspend fun plan(request: String, observation: Observation?): TaskPlan = withContext(Dispatchers.IO) {
        if (BuildConfig.NVIDIA_API_KEY.isBlank()) return@withContext enforceMessage(enforceIntents(fallback.plan(request), request), request)
        try {
            val connection = (URL("${BuildConfig.NVIDIA_BASE_URL}/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 15_000; readTimeout = 45_000; doOutput = true
                setRequestProperty("Authorization", "Bearer ${BuildConfig.NVIDIA_API_KEY}")
                setRequestProperty("Content-Type", "application/json")
            }
            val body = JSONObject().apply {
                put("model", BuildConfig.NVIDIA_MODEL); put("temperature", 0.1); put("max_tokens", 1200)
                val ui=observation?.nodes?.take(180)?.joinToString("\n") { listOfNotNull(it.text,it.description,it.resourceId).joinToString(" | ") }.take(12000)
                put("messages", JSONArray().put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT)).put(JSONObject().put("role", "user").put("content", "USER TASK:\n$request\n\nCURRENT ACCESSIBILITY UI (semantic, not coordinates):\n$ui")))
            }
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            if (connection.responseCode !in 200..299) return@withContext enforceMessage(enforceIntents(fallback.plan(request), request), request)
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            enforceMessage(enforceIntents(parsePlan(JSONObject(response).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"), request) ?: fallback.plan(request), request), request)
        } catch (_: Exception) { enforceMessage(enforceIntents(fallback.plan(request), request), request) }
    }

    private fun parsePlan(content: String, goal: String): TaskPlan? = try {
        val jsonText = content.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
        val root = JSONObject(jsonText); val actions = mutableListOf<AgentAction>(); val array = root.optJSONArray("actions") ?: return null
        for (i in 0 until array.length()) { val a=array.getJSONObject(i); val type=runCatching{ActionType.valueOf(a.getString("type").uppercase())}.getOrNull() ?: continue; actions += AgentAction(type,a.optString("value").takeIf{it.isNotBlank()},a.optString("target").takeIf{it.isNotBlank()},a.optBoolean("requiresConfirmation")) }
        if (actions.isEmpty()) null else TaskPlan(goal, actions + AgentAction(ActionType.FINISH))
    } catch (_: Exception) { null }

    private fun enforceIntents(base: TaskPlan, request: String): TaskPlan {
        val lower=request.lowercase(); val result=base.actions.filterNot { it.type==ActionType.FINISH }.toMutableList()
        val app=when { "instagram" in lower || "insta" in lower -> "com.instagram.android"; "whatsapp" in lower -> "com.whatsapp"; "youtube" in lower -> "com.google.android.youtube"; "flipkart" in lower -> "com.flipkart.android"; "amazon" in lower -> "in.amazon.mShop.android.shopping"; "play store" in lower || "playstore" in lower -> "com.android.vending"; else -> null }
        if (app!=null) { result.removeAll { it.type==ActionType.OPEN_APP }; result.add(0,AgentAction(ActionType.OPEN_APP,app)) }
        val searchMatch=Regex("(?:search|find|ढूंढ|खोज)\\s+(?:for\\s+)?(.+?)(?:(?:\\s+and\\s+play)|$)",RegexOption.IGNORE_CASE).find(request)
        val query=searchMatch?.groupValues?.getOrNull(1)?.trim()?.trimEnd('.') ?: Regex("(?i)install\\s+(.+?)\\s*$").find(request)?.groupValues?.getOrNull(1)?.trim()
        if (query!=null && query.isNotBlank()) { result.removeAll { it.type==ActionType.SEARCH }; result.add(AgentAction(ActionType.SEARCH,query)) }
        if ((("play" in lower && "play store" !in lower) || "चलाओ" in lower || "chalao" in lower)) { result.removeAll { it.type==ActionType.PLAY }; result.add(AgentAction(ActionType.PLAY,target="Play")) }
        if (("add to cart" in lower || "cart me" in lower || "कार्ट" in lower) && result.none { it.type==ActionType.ADD_TO_CART }) result.add(AgentAction(ActionType.ADD_TO_CART,target="Add to Cart"))
        if ("install" in lower && result.none { it.type==ActionType.INSTALL_APP }) result.add(AgentAction(ActionType.INSTALL_APP, query, "Install", true))
        return TaskPlan(base.goal,result+AgentAction(ActionType.FINISH))
    }

    private fun enforceMessage(base: TaskPlan, request: String): TaskPlan {
        val lower=request.lowercase(); if (!(lower.contains("whatsapp") || lower.contains("message") || lower.contains("send"))) return base
        val match=Regex("(?i)(?:send|message|text)\\s+(.+?)\\s+(?:to|ko)\\s+([A-Za-z][A-Za-z .]+)$").find(request) ?: return base
        var body=match.groupValues[1].trim(); val target=match.groupValues[2].trim(); body=body.replace(Regex("(?i)\\s+message$"),"").trim()
        if(body.isBlank() || target.isBlank()) return base
        val result=base.actions.filterNot { it.type==ActionType.SEND_MESSAGE }.toMutableList(); result.add(AgentAction(ActionType.SEND_MESSAGE,body,target,true)); return TaskPlan(base.goal,result.filterNot { it.type==ActionType.FINISH }+AgentAction(ActionType.FINISH))
    }

    companion object { private const val SYSTEM_PROMPT = """
You are JARVIS, an Android UI agent. Understand English, Hindi and Hinglish. Return ONLY JSON: {\"actions\":[{\"type\":\"OPEN_APP|CLICK|TYPE_TEXT|SEARCH|SCROLL|BACK|READ_SCREEN|SELECT|PLAY|ADD_TO_CART|INSTALL_APP|SEND_MESSAGE|CALL|WAIT|FINISH\",\"value\":\"...\",\"target\":\"...\",\"requiresConfirmation\":true|false}]}. Use semantic targets, never coordinates. Mark SEND_MESSAGE, CALL, purchases, deletion and public posting as requiresConfirmation true. Never request passwords, OTPs or security bypasses. If uncertain, use READ_SCREEN.
""" }
}
