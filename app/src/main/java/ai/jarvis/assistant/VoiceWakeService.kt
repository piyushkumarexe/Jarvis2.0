package ai.jarvis.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import java.util.Locale

/** Android requires a visible foreground notification for continuous microphone access. */
class VoiceWakeService : Service(), RecognitionListener {
    private var recognizer: SpeechRecognizer? = null
    private val handler=android.os.Handler(mainLooper)
    private var stoppedForCommand=false
    private var restartScheduled=false
    override fun onCreate() { super.onCreate(); try { createChannel(); startForeground(42, notification()); startListening() } catch (_: Exception) { stopSelf() } }
    private fun startListening() { if(stoppedForCommand) return; restartScheduled=false; recognizer?.destroy(); recognizer=runCatching { SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this); it.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true); putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500) }) } }.getOrNull() }
    private fun scheduleRestart(delay:Long=2500) { if(stoppedForCommand || restartScheduled) return; restartScheduled=true; handler.postDelayed({ startListening() },delay) }
    override fun onResults(results: Bundle?) { if(stoppedForCommand)return; val text=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(); if(text.lowercase().contains("jarvis")){ stoppedForCommand=true; val command=text.replace(Regex("(?i)\\bjarvis\\b"),"").trim(); recognizer?.stopListening(); WakeBridge.commands.tryEmit(command); startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)); handler.postDelayed({ stoppedForCommand=false; scheduleRestart(200) },2500) } else scheduleRestart(200) }
    override fun onError(error: Int) { scheduleRestart() }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); recognizer?.destroy(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder?=null
    override fun onReadyForSpeech(p: Bundle?){}; override fun onBeginningOfSpeech(){}; override fun onRmsChanged(r:Float){}; override fun onBufferReceived(b:ByteArray?){}; override fun onEndOfSpeech(){}; override fun onPartialResults(b:Bundle?){}; override fun onEvent(t:Int,b:Bundle?){}
    private fun createChannel(){getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("jarvis_wake","JARVIS wake word",NotificationManager.IMPORTANCE_LOW))}
    private fun notification(): Notification { val pi=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT); return NotificationCompat.Builder(this,"jarvis_wake").setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("JARVIS is listening").setContentText("Say “Jarvis” to activate").setContentIntent(pi).setOngoing(true).build() }
}
