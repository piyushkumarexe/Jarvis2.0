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
    override fun onCreate() { super.onCreate(); createChannel(); startForeground(42, notification()); startListening() }
    private fun startListening() { recognizer?.destroy(); recognizer=SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this); it.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) }) } }
    override fun onResults(results: Bundle?) { val text=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(); if(text.lowercase().contains("jarvis")){ val command=text.replace(Regex("(?i)\\bjarvis\\b"),"").trim(); startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("wake_command",command)) }; startListening() }
    override fun onError(error: Int) { android.os.Handler(mainLooper).postDelayed({startListening()},800) }
    override fun onDestroy() { recognizer?.destroy(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder?=null
    override fun onReadyForSpeech(p: Bundle?){}; override fun onBeginningOfSpeech(){}; override fun onRmsChanged(r:Float){}; override fun onBufferReceived(b:ByteArray?){}; override fun onEndOfSpeech(){}; override fun onPartialResults(b:Bundle?){}; override fun onEvent(t:Int,b:Bundle?){}
    private fun createChannel(){getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("jarvis_wake","JARVIS wake word",NotificationManager.IMPORTANCE_LOW))}
    private fun notification(): Notification { val pi=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT); return NotificationCompat.Builder(this,"jarvis_wake").setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("JARVIS is listening").setContentText("Say “Jarvis” to activate").setContentIntent(pi).setOngoing(true).build() }
}
