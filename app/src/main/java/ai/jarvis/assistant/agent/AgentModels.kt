package ai.jarvis.assistant.agent

import android.graphics.Rect

enum class ActionType { OPEN_APP, OPEN_URL, CLICK, LONG_PRESS, TYPE_TEXT, CLEAR_TEXT, SCROLL, SWIPE, BACK, HOME, WAIT, READ_SCREEN, FIND_ELEMENT, SEARCH, SELECT, PLAY, ADD_TO_CART, CALL, SEND_MESSAGE, SET_VOLUME, SET_BRIGHTNESS, CONFIRM, CANCEL, FINISH }
data class AgentAction(val type: ActionType, val value: String? = null, val target: String? = null, val requiresConfirmation: Boolean = false)
data class UiNode(val text: String?, val description: String?, val resourceId: String?, val className: String?, val clickable: Boolean, val bounds: Rect)
data class Observation(val packageName: String, val nodes: List<UiNode>, val timestamp: Long = System.currentTimeMillis())
data class TaskPlan(val goal: String, val actions: List<AgentAction>)
enum class RunState { IDLE, LISTENING, PLANNING, RUNNING, WAITING_CONFIRMATION, SPEAKING, SUCCESS, ERROR, STOPPED }
data class TaskEvent(val label: String, val state: EventState)
enum class EventState { COMPLETE, ACTIVE, PENDING, FAILED }
