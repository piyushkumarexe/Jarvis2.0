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
    private val planner: AiPlanner = NvidiaPlanner(); private var tts: TextToSpeech = TextToSpeech(app, this)
    private val _state = MutableStateFlow(RunState.IDLE); val state: StateFlow<RunState> = _state
    private val _transcript = MutableStateFlow(""); val transcript: StateFlow<String> = _transcript
    private val _events = MutableStateFlow<List<TaskEvent>>(emptyList()); val events: StateFlow<List<TaskEvent>> = _events
    private val _pending = MutableStateFlow<AgentAction?>(null); val pending: StateFlow<AgentAction?> = _pending
    fun beginListening(context: Context) { _state.value=RunState.LISTENING; context.startActivity(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_PROMPT,"Tell JARVIS what to do"); putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) }) }
    fun submit(text: String) { if (text.isBlank()) return; _transcript.value=text; execute(text) }
    fun acceptConfirmation() { _pending.value?.let { action -> _pending.value=null; viewModelScope.launch { runAction(action); if (_state.value==RunState.RUNNING) { _state.value=RunState.SUCCESS; speak("Action completed and verified.") } } } }
    fun rejectConfirmation() { _pending.value=null; _state.value=RunState.STOPPED; speak("Cancelled.") }
    fun stop() { _state.value=RunState.STOPPED; _pending.value=null }
    private fun execute(text: String) = viewModelScope.launch { _state.value=RunState.PLANNING; val plan=planner.plan(text); for (a in plan.actions) { if (_state.value==RunState.STOPPED) return@launch; if(a.requiresConfirmation){_pending.value=a;_state.value=RunState.WAITING_CONFIRMATION; speak("This action needs your confirmation.");return@launch}; runAction(a) }; if(_state.value!=RunState.STOPPED){_state.value=RunState.SUCCESS;speak("Task completed and verified.")} }
    private suspend fun runAction(a: AgentAction) { _state.value=RunState.RUNNING; add(a.value ?: a.type.name, EventState.ACTIVE); val service=JarvisAccessibilityService.get(); when(a.type){ ActionType.OPEN_APP -> { val ok=service?.launch(a.value!!) == true; delay(900); val verified=JarvisAccessibilityService.foreground.value==a.value; update(if(ok&&verified)EventState.COMPLETE else EventState.FAILED); if(!verified){_state.value=RunState.ERROR;speak("I could not verify that the app opened.");return} }; ActionType.SEARCH -> { val q=a.value!!; val ok=service?.clickAny("search","Search") == true; delay(300); val typed=service?.type(q)==true; update(if(ok&&typed)EventState.COMPLETE else EventState.FAILED); }; ActionType.SEND_MESSAGE -> { val recipient=service?.click(a.target ?: "") == true; delay(500); val field=service?.clickAny("message","Type a message","Message") == true; val typed=service?.type(a.value ?: "") == true; val sent=service?.clickAny("send","Send") == true; update(if(recipient&&field&&typed&&sent)EventState.COMPLETE else EventState.FAILED); if(!(recipient&&field&&typed&&sent)){_state.value=RunState.ERROR;speak("I could not verify the message was sent.");return} }; ActionType.READ_SCREEN -> { service?.readScreen(); update(EventState.COMPLETE) }; ActionType.BACK -> { service?.back(); update(EventState.COMPLETE) }; ActionType.FINISH -> update(EventState.COMPLETE); else -> { update(EventState.FAILED); _state.value=RunState.ERROR; speak("I cannot safely perform that action yet."); return } } }
    private fun add(label:String,state:EventState){_events.value=_events.value+TaskEvent(label,state)}; private fun update(s:EventState){_events.value=_events.value.dropLast(1)+_events.value.last().copy(state=s)}
    private fun speak(text:String){_state.value=RunState.SPEAKING;tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"jarvis");viewModelScope.launch{delay(1800);if(_state.value==RunState.SPEAKING)_state.value=RunState.SUCCESS}}
    override fun onInit(status:Int){if(status==TextToSpeech.SUCCESS)tts.language=Locale.getDefault()}; override fun onCleared(){tts.shutdown();super.onCleared()}
}
