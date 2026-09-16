package ai.jarvis.assistant

import android.app.Application
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.jarvis.assistant.agent.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class JarvisViewModel(app: Application) : AndroidViewModel(app), TextToSpeech.OnInitListener {
    companion object { var active: JarvisViewModel? = null }
    private val planner: AiPlanner = NvidiaPlanner(); private var tts: TextToSpeech = TextToSpeech(app, this)
    init { active=this }
    private val _state = MutableStateFlow(RunState.IDLE); val state: StateFlow<RunState> = _state
    private val _transcript = MutableStateFlow(""); val transcript: StateFlow<String> = _transcript
    private val _events = MutableStateFlow<List<TaskEvent>>(emptyList()); val events: StateFlow<List<TaskEvent>> = _events
    private val _pending = MutableStateFlow<AgentAction?>(null); val pending: StateFlow<AgentAction?> = _pending
    private var pendingQueue: List<AgentAction> = emptyList()
    private var pendingApp: String? = null
    fun beginListening(context: Context) { _state.value=RunState.LISTENING; context.startActivity(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_PROMPT,"Tell JARVIS what to do"); putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) }) }
    fun submit(text: String) { if (text.isBlank()) return; _transcript.value=text; _events.value=emptyList(); execute(text) }
    fun acceptConfirmation() { if (_pending.value == null) return; _pending.value=null; viewModelScope.launch { _state.value=RunState.RUNNING; pendingApp?.let { JarvisAccessibilityService.get()?.launch(it); delay(900) }; for (action in pendingQueue) { runAction(action); if (_events.value.lastOrNull()?.state==EventState.FAILED) { _state.value=RunState.ERROR; speak("I could not verify that action.", RunState.ERROR); pendingQueue=emptyList(); return@launch } }; pendingQueue=emptyList(); _state.value=RunState.SUCCESS; speak("Action completed and verified.") } }
    fun rejectConfirmation() { _pending.value=null; _state.value=RunState.STOPPED; speak("Cancelled.") }
    fun stop() { _state.value=RunState.STOPPED; _pending.value=null }
    private fun execute(text: String) = viewModelScope.launch { _state.value=RunState.PLANNING; val observation=JarvisAccessibilityService.get()?.readScreen(); val plan=planner.plan(text, observation); pendingApp=plan.actions.firstOrNull { it.type==ActionType.OPEN_APP }?.value; for ((index,a) in plan.actions.withIndex()) { if (_state.value==RunState.STOPPED) return@launch; if(a.requiresConfirmation){ pendingQueue=plan.actions.drop(index); _pending.value=a; _state.value=RunState.WAITING_CONFIRMATION; ConfirmationNotifier.show(getApplication(),a); ConfirmationOverlay.show(getApplication(),a,{acceptConfirmation()},{rejectConfirmation()}); return@launch }; runAction(a); if (_events.value.lastOrNull()?.state==EventState.FAILED) { _state.value=RunState.ERROR; speak("I could not verify that step, so I stopped safely.", RunState.ERROR); return@launch } }; if(_state.value!=RunState.STOPPED){_state.value=RunState.SUCCESS;speak("Task completed and verified.")} }
    private suspend fun runAction(a: AgentAction) { _state.value=RunState.RUNNING; add(a.value ?: a.type.name, EventState.ACTIVE); val service=JarvisAccessibilityService.get(); when(a.type){ ActionType.OPEN_APP -> { val ok=service?.launch(a.value!!) == true; delay(900); val verified=JarvisAccessibilityService.foreground.value==a.value; update(if(ok&&verified)EventState.COMPLETE else EventState.FAILED); if(!verified){_state.value=RunState.ERROR;speak("I could not verify that the app opened.");return} }; ActionType.SEARCH -> { val q=a.value!!; val opened=service?.clickAny("search","Search") == true; delay(300); val typed=service?.type(q)==true; delay(150); val submitted=service?.pressEnter()==true || service?.clickAny("search","Search","go") == true; delay(800); update(if(opened&&typed&&submitted)EventState.COMPLETE else EventState.FAILED); }; ActionType.CLICK, ActionType.SELECT -> { val ok=service?.click(a.target ?: a.value ?: "") == true; update(if(ok)EventState.COMPLETE else EventState.FAILED); }; ActionType.TYPE_TEXT -> { val ok=service?.type(a.value ?: "") == true; update(if(ok)EventState.COMPLETE else EventState.FAILED) }; ActionType.PLAY -> { val ok=service?.clickAny(a.target ?: "play","Play","Watch","Watch now","Open") == true; update(if(ok)EventState.COMPLETE else EventState.FAILED) }; ActionType.ADD_TO_CART -> { val ok=service?.clickAny("Add to cart","Add to Cart","Add","Buy now") == true; update(if(ok)EventState.COMPLETE else EventState.FAILED) }; ActionType.INSTALL_APP -> { val ok=service?.clickAny("Install","Get","Install now") == true; update(if(ok)EventState.COMPLETE else EventState.FAILED) }; ActionType.SCROLL -> { val ok=service?.scroll(a.value?.lowercase() != "up") == true; update(if(ok)EventState.COMPLETE else EventState.FAILED) }; ActionType.WAIT -> { delay((a.value?.toLongOrNull() ?: 1000L).coerceAtMost(10000)); update(EventState.COMPLETE) }; ActionType.SEND_MESSAGE -> { var recipient=service?.click(a.target ?: "") == true || service?.hasVisibleText(a.target ?: "") == true; if(!recipient){ if(service?.clickContactSearch()!=true) service?.clickAny("Search"); delay(250); service?.type(a.target ?: ""); service?.pressEnter(); delay(1200); recipient=service?.click(a.target ?: "") == true || service?.hasVisibleText(a.target ?: "") == true }; delay(600); val field=service?.focusMessage() == true; val typed=field && service?.type(a.value ?: "") == true; delay(250); val sent=typed && (service?.clickAny("Send","send message") == true); update(if(recipient&&field&&typed&&sent)EventState.COMPLETE else EventState.FAILED); if(!(recipient&&field&&typed&&sent)){_state.value=RunState.ERROR;speak("I could not verify the message was sent.",RunState.ERROR);return} }; ActionType.READ_SCREEN -> { service?.readScreen(); update(EventState.COMPLETE) }; ActionType.BACK -> { service?.back(); update(EventState.COMPLETE) }; ActionType.FINISH -> update(EventState.COMPLETE); else -> { update(EventState.FAILED); _state.value=RunState.ERROR; speak("I cannot safely perform that action yet."); return } } }
    private fun add(label:String,state:EventState){_events.value=_events.value+TaskEvent(label,state)}; private fun update(s:EventState){_events.value=_events.value.dropLast(1)+_events.value.last().copy(state=s)}
    private fun speak(text:String, endState:RunState=RunState.SUCCESS){_state.value=RunState.SPEAKING;tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"jarvis");viewModelScope.launch{delay(1800);if(_state.value==RunState.SPEAKING)_state.value=endState}}
    override fun onInit(status:Int){if(status==TextToSpeech.SUCCESS)tts.language=Locale.getDefault()}; override fun onCleared(){tts.shutdown();super.onCleared()}
}
