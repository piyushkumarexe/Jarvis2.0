package ai.jarvis.assistant

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import ai.jarvis.assistant.agent.AgentAction

object ConfirmationOverlay {
    private var current: android.view.View?=null
    fun show(context: Context, action: AgentAction, confirm:()->Unit, cancel:()->Unit) {
        if(!Settings.canDrawOverlays(context)) return
        val wm=context.getSystemService(WindowManager::class.java); current?.let{runCatching{wm.removeView(it)}}
        val box=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(48,36,48,28);background=GradientDrawable().apply{setColor(Color.rgb(28,30,38));cornerRadius=40f}}
        val title=TextView(context).apply{text="JARVIS confirmation";textSize=22f;setTextColor(Color.WHITE)}
        val body=TextView(context).apply{text="Allow ${action.type}: ${action.value ?: action.target ?: "action"}?";textSize=16f;setTextColor(Color.LTGRAY);setPadding(0,20,0,20)}
        val buttons=LinearLayout(context).apply{gravity=Gravity.RIGHT}; val no=Button(context).apply{text="CANCEL"}; val yes=Button(context).apply{text="CONFIRM";setTextColor(Color.rgb(116,246,210))}; buttons.addView(no);buttons.addView(yes);box.addView(title);box.addView(body);box.addView(buttons)
        val params=WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_DIM_BEHIND or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,-3).apply{gravity=Gravity.CENTER;dimAmount=.55f}
        no.setOnClickListener{wm.removeView(box);current=null;cancel()};yes.setOnClickListener{wm.removeView(box);current=null;confirm()};wm.addView(box,params);current=box
    }
}
