package ai.jarvis.assistant

import android.app.*
import android.content.*
import androidx.core.app.NotificationCompat
import ai.jarvis.assistant.agent.AgentAction

object ConfirmationNotifier {
    const val CHANNEL="jarvis_confirmation"
    fun show(context: Context, action: AgentAction) {
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,"JARVIS confirmations",NotificationManager.IMPORTANCE_HIGH).apply { description="Approve high-impact actions without leaving the target app" })
        val yes=PendingIntent.getBroadcast(context,1,Intent(context,ConfirmationReceiver::class.java).setAction("ai.jarvis.CONFIRM"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val no=PendingIntent.getBroadcast(context,2,Intent(context,ConfirmationReceiver::class.java).setAction("ai.jarvis.CANCEL"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(77,NotificationCompat.Builder(context,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("JARVIS needs confirmation").setContentText("${action.type}: ${action.value ?: action.target ?: "action"}").setPriority(NotificationCompat.PRIORITY_MAX).setAutoCancel(true).addAction(0,"CONFIRM",yes).addAction(0,"CANCEL",no).build())
    }
    fun clear(context: Context)=context.getSystemService(NotificationManager::class.java).cancel(77)
}
class ConfirmationReceiver: BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) { if(intent.action=="ai.jarvis.CONFIRM") JarvisViewModel.active?.acceptConfirmation() else JarvisViewModel.active?.rejectConfirmation(); ConfirmationNotifier.clear(context) } }
