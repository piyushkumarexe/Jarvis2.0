package ai.jarvis.assistant.agent

/** Provider boundary: replace this implementation with a local model or an authenticated backend. */
interface AiPlanner { suspend fun plan(request: String, observation: Observation? = null): TaskPlan }

class SafeCommandPlanner : AiPlanner {
    override suspend fun plan(request: String, observation: Observation?): TaskPlan {
        val raw = request.trim(); val lower = raw.lowercase()
        val app = when { "instagram" in lower || "insta" in lower -> "com.instagram.android"; "whatsapp" in lower -> "com.whatsapp"; "youtube" in lower -> "com.google.android.youtube"; "chrome" in lower -> "com.android.chrome"; "maps" in lower -> "com.google.android.apps.maps"; "settings" in lower -> "com.android.settings"; else -> null }
        val actions = mutableListOf<AgentAction>()
        if (app != null) actions += AgentAction(ActionType.OPEN_APP, app)
        val query = when {
            "youtube" in lower -> Regex("(?:search|find|par)\\s+(.+?)(?:\\s+search)?$", RegexOption.IGNORE_CASE).find(raw)?.groupValues?.getOrNull(1)
            "weather" in lower -> "weather"
            else -> null
        }?.trim()?.removeSuffix("search")?.trim()
        if (query != null && query.isNotBlank()) actions += AgentAction(ActionType.SEARCH, query)
        val message = Regex("(?:bolo|send|message|bhej(?:\\s+do)?|bata)\\s+(?:ki\\s+)?(.+)$", RegexOption.IGNORE_CASE).find(raw)?.groupValues?.getOrNull(1)
        val recipient = Regex("(?:to|ko)\\s+([A-Za-z][A-Za-z ]+?)(?:\\s+(?:bolo|send|message|bhej|ko)\\b|\\s*$)", RegexOption.IGNORE_CASE).find(raw)?.groupValues?.getOrNull(1)?.trim()
        if (message != null && recipient != null) actions += AgentAction(ActionType.SEND_MESSAGE, message.trim(), recipient, true)
        if (actions.isEmpty()) actions += AgentAction(ActionType.READ_SCREEN)
        return TaskPlan(raw, actions + AgentAction(ActionType.FINISH))
    }
}
