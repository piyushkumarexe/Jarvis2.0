package ai.jarvis.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import ai.jarvis.assistant.agent.Observation
import ai.jarvis.assistant.agent.UiNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class JarvisAccessibilityService : AccessibilityService() {
    companion object { private var instance: JarvisAccessibilityService? = null; val connected = MutableStateFlow(false); val foreground = MutableStateFlow(""); val screen = MutableStateFlow<Observation?>(null); fun get() = instance }
    override fun onServiceConnected() { instance = this; connected.value = true }
    override fun onDestroy() { connected.value = false; instance = null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { foreground.value = event?.packageName?.toString().orEmpty(); if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || rootInActiveWindow != null) screen.value = readScreen() }
    override fun onInterrupt() {}
    fun readScreen(): Observation { val root = rootInActiveWindow; val list = mutableListOf<UiNode>(); if (root != null) walk(root, list); return Observation(foreground.value, list) }
    private fun walk(node: AccessibilityNodeInfo, out: MutableList<UiNode>) { val r = android.graphics.Rect(); node.getBoundsInScreen(r); out += UiNode(node.text?.toString(), node.contentDescription?.toString(), node.viewIdResourceName, node.className?.toString(), node.isClickable, r); for (i in 0 until node.childCount) node.getChild(i)?.let { walk(it, out); it.recycle() } }
    fun find(query: String): AccessibilityNodeInfo? { fun search(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? { if (n == null) return null; val q = query.lowercase(); if ((n.text?.toString()?.lowercase()?.contains(q) == true || n.contentDescription?.toString()?.lowercase()?.contains(q) == true || n.viewIdResourceName?.contains(q) == true) && (n.isClickable || n.isFocusable || n.className?.toString()?.contains("EditText") == true)) return n; for (i in 0 until n.childCount) search(n.getChild(i))?.let { return it }; return null }; return search(rootInActiveWindow) }
    fun click(query: String): Boolean = find(query)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
    fun clickAny(vararg queries: String): Boolean = queries.any { click(it) }
    fun hasVisibleText(query:String): Boolean { fun walk(n:AccessibilityNodeInfo?):Boolean { if(n==null)return false; if(n.text?.toString()?.contains(query,true)==true || n.contentDescription?.toString()?.contains(query,true)==true)return true; for(i in 0 until n.childCount)if(walk(n.getChild(i)))return true; return false }; return walk(rootInActiveWindow) }
    fun focusMessage(): Boolean { fun walk(n:AccessibilityNodeInfo?):Boolean { if(n==null)return false; val label=(n.text?.toString()+" "+n.hintText?.toString()+" "+n.contentDescription?.toString()).lowercase(); if(n.isEditable && (label.contains("message") || label.contains("type a message"))){ n.performAction(AccessibilityNodeInfo.ACTION_FOCUS); n.performAction(AccessibilityNodeInfo.ACTION_CLICK); return true }; for(i in 0 until n.childCount)if(walk(n.getChild(i)))return true; return false }; return walk(rootInActiveWindow) }
    fun clickContactSearch(): Boolean { val n=findExact("Search") ?: findExact("Contacts") ?: findExact("Search contacts") ?: return false; return n.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
    private fun findExact(query:String): AccessibilityNodeInfo? { fun search(n:AccessibilityNodeInfo?):AccessibilityNodeInfo? { if(n==null)return null; val t=n.text?.toString()?.trim(); val d=n.contentDescription?.toString()?.trim(); if((t==query || d==query) && (n.isClickable || n.isFocusable || n.isEditable)) return n; for(i in 0 until n.childCount) search(n.getChild(i))?.let{return it}; return null }; return search(rootInActiveWindow) }

    fun type(text: String): Boolean { val focused=rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT); val n=focused ?: findEditable(rootInActiveWindow) ?: return false; val b=Bundle(); b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text); return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b) }
    private fun findEditable(n:AccessibilityNodeInfo?):AccessibilityNodeInfo? { if(n==null)return null; val label=(n.text?.toString()+" "+n.hintText?.toString()+" "+n.contentDescription?.toString()).lowercase(); if(n.isEditable && !label.contains("meta ai") && !label.contains("ask meta")) return n; for(i in 0 until n.childCount) findEditable(n.getChild(i))?.let{return it}; return null }

    fun pressEnter(): Boolean { val n = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false; return if (android.os.Build.VERSION.SDK_INT >= 30) n.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id) else false }
    fun back() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun home() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun scroll(forward: Boolean) = findScrollable(rootInActiveWindow)?.performAction(if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) == true
    private fun findScrollable(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? { if (n == null) return null; if (n.isScrollable) return n; for(i in 0 until n.childCount) findScrollable(n.getChild(i))?.let{return it}; return null }
    fun swipe(x1: Float,y1: Float,x2: Float,y2: Float): Boolean { val p=Path().apply{moveTo(x1,y1);lineTo(x2,y2)}; return dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p,0,400)).build(),null,null) }
    fun launch(packageName: String): Boolean = try { val intent = packageManager.getLaunchIntentForPackage(packageName); if (intent == null) false else { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent); true } } catch(_: Exception) { false }
}
