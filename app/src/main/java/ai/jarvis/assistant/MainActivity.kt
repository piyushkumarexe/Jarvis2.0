package ai.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableSharedFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.jarvis.assistant.agent.*

private val Navy=Color(0xFF070B14); private val Panel=Color(0xFF101827); private val Cyan=Color(0xFF74F6D2); private val Violet=Color(0xFF8C7CFF)
object WakeBridge { val commands=MutableSharedFlow<String>(replay=1, extraBufferCapacity=4) }
class MainActivity: ComponentActivity(){ override fun onCreate(b:Bundle?){super.onCreate(b);setContent{JarvisScreen()}} }
@Composable fun JarvisScreen(vm:JarvisViewModel=viewModel()) { val context=LocalContext.current; val state by vm.state.collectAsState(); val transcript by vm.transcript.collectAsState(); val events by vm.events.collectAsState(); val pending by vm.pending.collectAsState(); var input by remember{mutableStateOf("")}; val mic=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){ r-> val t=r.data?.getStringArrayListExtra("android.speech.extra.RESULTS")?.firstOrNull(); if(t!=null)vm.submit(t) }; val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ /* microphone is now available for one-shot commands */ }
    LaunchedEffect(Unit) { WakeBridge.commands.collect { command -> if(command.isBlank()) vm.beginListening(context) else vm.submit(command) } }
    MaterialTheme(colorScheme=darkColorScheme(background=Navy,surface=Panel,primary=Cyan)){ Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Navy,Color(0xFF11152A)))).padding(22.dp)){
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
            Column {
                Text("JARVIS", color=Cyan, fontSize=28.sp, fontWeight=FontWeight.Bold)
                Text("ANDROID INTELLIGENCE", color=Color.Gray, fontSize=10.sp, letterSpacing=2.sp)
            }
            IconButton(onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))) }) {
                Icon(Icons.Default.Settings, "Permissions", tint=Color.LightGray)
            }
        }
        Spacer(Modifier.height(22.dp)); Orb(state); Text(if(state==RunState.IDLE)"Ready for your command" else state.name.replace('_',' '),color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Medium,modifier=Modifier.align(Alignment.CenterHorizontally)); Spacer(Modifier.height(18.dp))
        if(transcript.isNotBlank()) Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(18.dp)){Text("“$transcript”",Modifier.padding(16.dp),color=Color.White)}
        Spacer(Modifier.height(12.dp)); Text("LIVE TASK",color=Color.Gray,fontSize=11.sp,letterSpacing=2.sp); events.forEach{e->Row(Modifier.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Text(if(e.state==EventState.COMPLETE)"✓" else if(e.state==EventState.ACTIVE)"●" else "○",color=if(e.state==EventState.FAILED)Color.Red else Cyan,fontSize=18.sp);Text("  ${e.label}",color=Color.White)}}
        Spacer(Modifier.weight(1f)); Row(verticalAlignment=Alignment.CenterVertically){OutlinedTextField(input,{input=it},Modifier.weight(1f),placeholder={Text("Ask anything…")},singleLine=true,shape=RoundedCornerShape(18.dp));IconButton(onClick={vm.submit(input);input=""}){Icon(Icons.Default.Send,"Send",tint=Cyan)}}; Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){Button(onClick={permission.launch(Manifest.permission.RECORD_AUDIO); mic.launch(Intent("android.speech.action.RECOGNIZE_SPEECH").apply{putExtra("android.speech.extra.LANGUAGE_MODEL","free_form")})},shape=CircleShape,colors=ButtonDefaults.buttonColors(containerColor=Cyan),modifier=Modifier.size(76.dp)){Icon(Icons.Default.Mic,"Speak",tint=Navy,modifier=Modifier.size(32.dp))}; if(state in listOf(RunState.RUNNING,RunState.LISTENING,RunState.PLANNING)){Spacer(Modifier.width(18.dp));OutlinedButton(onClick={vm.stop()},colors=ButtonDefaults.outlinedButtonColors(contentColor=Color(0xFFFF7185))){Icon(Icons.Default.Stop,"Stop");Text(" STOP")}}}
        Spacer(Modifier.height(12.dp)); Text("Accessibility: ${if(JarvisAccessibilityService.connected.collectAsState().value)"CONNECTED" else "NOT CONNECTED — tap ⚙"}",color=Color.Gray,fontSize=11.sp,modifier=Modifier.align(Alignment.CenterHorizontally))
    }; if(pending!=null) AlertDialog(onDismissRequest={vm.rejectConfirmation()},title={Text("Confirm action")},text={Text("JARVIS wants to perform a high-impact action. Continue?\n\n${pending!!.type}: ${pending!!.value}")},confirmButton={TextButton(onClick={vm.acceptConfirmation()}){Text("CONFIRM")}},dismissButton={TextButton(onClick={vm.rejectConfirmation()}){Text("CANCEL")}}) }}
@Composable private fun Orb(state:RunState){val infinite=rememberInfiniteTransition(label="orb");val scale by infinite.animateFloat(1f,1.12f, infiniteRepeatable(tween(900),RepeatMode.Reverse),label="pulse");Box(Modifier.size(170.dp).scale(if(state==RunState.RUNNING||state==RunState.LISTENING)scale else 1f).background(Brush.radialGradient(listOf(Cyan.copy(.8f),Violet.copy(.35f),Color.Transparent)),CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Default.AutoAwesome,"JARVIS",tint=Color.White,modifier=Modifier.size(58.dp))}}
private fun MainActivity.startAccessibility(){startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}
