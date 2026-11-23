package com.gamer631.crashtracker

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
 private val prefs by lazy { getSharedPreferences("round_history", MODE_PRIVATE) }
 private val rounds= mutableListOf<Double>()
 private lateinit var status: TextView
 private lateinit var detected: TextView
 private lateinit var history: TextView
 private lateinit var input: EditText
 private var latest: Double?=null
 private val captureRequest=700
 private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){val v=i?.getDoubleExtra("multiplier",0.0)?:0.0;if(v>0){latest=v;detected.text="Detected: %.2fx".format(v);status.text="Screen scan active · review detected value before saving"}} 
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);load(); buildUI(); refresh()
 }
 private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
 private fun tv(s:String,size:Float,color:Int=Color.WHITE)=TextView(this).apply{text=s;textSize=size;setTextColor(color);setPadding(0,dp(7),0,dp(7))}
 private fun buildUI(){
  window.statusBarColor=Color.rgb(10,14,25);window.navigationBarColor=Color.rgb(10,14,25)
  val root=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(14),dp(18),dp(12));setBackgroundColor(Color.rgb(10,14,25))}
  val scroll=ScrollView(this);val content=LinearLayout(this).apply{orientation=1}
  content.addView(tv("CRASH TRACKER",24f,Color.rgb(94,220,255)));content.addView(tv("SCREEN OCR · ROUND HISTORY · ANALYTICS",12f,Color.LTGRAY))
  status=tv("Screen tracking stopped",14f,Color.rgb(160,210,170));content.addView(status)
  detected=tv("Detected: —",23f,Color.rgb(100,220,255));content.addView(detected)
  content.addView(Button(this).apply{text="START SCREEN TRACKING";setOnClickListener{startCapture()}})
  content.addView(Button(this).apply{text="STOP SCREEN TRACKING";setOnClickListener{stopCapture()}})
  content.addView(Button(this).apply{text="SAVE DETECTED MULTIPLIER";setOnClickListener{latest?.let{addRound(it)}?:Toast.makeText(this@MainActivity,"No multiplier detected yet",Toast.LENGTH_SHORT).show()}})
  input=EditText(this).apply{hint="Manual multiplier (e.g. 2.45x)";inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;setSingleLine()}
  content.addView(input);content.addView(Button(this).apply{text="ADD MANUAL ROUND";setOnClickListener{input.text.toString().replace("x","",true).toDoubleOrNull()?.let{addRound(it)}?:Toast.makeText(this@MainActivity,"Enter a valid number",Toast.LENGTH_SHORT).show()}})
  content.addView(tv("STATISTICS",18f,Color.rgb(100,220,255)));history=tv("",15f);content.addView(history)
  content.addView(Button(this).apply{text="UNDO LAST ROUND";setOnClickListener{if(rounds.isNotEmpty()){rounds.removeAt(rounds.lastIndex);save();refresh()}}})
  content.addView(Button(this).apply{text="CLEAR HISTORY";setOnClickListener{AlertDialog.Builder(this@MainActivity).setTitle("Clear saved rounds?").setNegativeButton("Cancel",null).setPositiveButton("Clear"){_,_->rounds.clear();save();refresh()}.show()}})
  content.addView(tv("Screen OCR can misread values. Confirm each multiplier before saving. Statistics describe past observations and cannot predict the next crash.",12f,Color.LTGRAY))
  scroll.addView(content);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
 }
 private fun startCapture(){val m=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager;startActivityForResult(m.createScreenCaptureIntent(),captureRequest)}
 @Deprecated("Activity result legacy API")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==captureRequest&&resultCode==RESULT_OK&&data!=null){val svc=Intent(this,ScreenScanService::class.java).putExtra("resultCode",resultCode).putExtra("data",data);startForegroundService(svc);status.text="Starting OCR…";registerReceiver(receiver,IntentFilter(ScreenScanService.ACTION_MULTIPLIER),RECEIVER_NOT_EXPORTED)}}
 private fun stopCapture(){stopService(Intent(this,ScreenScanService::class.java));try{unregisterReceiver(receiver)}catch(_:Exception){};status.text="Screen tracking stopped"}
 private fun addRound(v:Double){if(v<=0||v.isNaN()){Toast.makeText(this,"Invalid multiplier",Toast.LENGTH_SHORT).show();return};rounds.add(v);save();input.text.clear();refresh();Toast.makeText(this,"Saved %.2fx".format(v),Toast.LENGTH_SHORT).show()}
 private fun refresh(){val n=rounds.size;val sorted=rounds.sorted();val avg=if(n>0)rounds.average() else 0.0;val med=if(n==0)0.0 else if(n%2==1)sorted[n/2] else (sorted[n/2-1]+sorted[n/2])/2;val below=rounds.count{it<2};val high=rounds.count{it>=10};history.text="Rounds recorded: $n\nAverage: %.2fx  ·  Median: %.2fx\nBelow 2x: %d (%.1f%%)\n10x+: %d (%.1f%%)\n\nRECENT ROUNDS\n".format(avg,med,below,if(n>0)100.0*below/n else 0.0,high,if(n>0)100.0*high/n else 0.0)+rounds.takeLast(40).reversed().mapIndexed{idx,v->"%02d  %.2fx".format(n-idx,v)}.joinToString("\n")}
 private fun save(){prefs.edit().putString("data",JSONArray(rounds).toString()).apply()}
 private fun load(){try{val a=JSONArray(prefs.getString("data","[]"));for(i in 0 until a.length()){val v=a.optDouble(i);if(v>0)rounds.add(v)}}catch(_:Exception){}}
}
