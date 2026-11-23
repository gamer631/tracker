package com.gamer631.crashtracker

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

class ScreenScanService:Service(){
 companion object{const val ACTION_MULTIPLIER="com.gamer631.crashtracker.MULTIPLIER";private const val CHANNEL="scan_channel";private const val NOTIFY=31}
 private var projection:MediaProjection?=null;private var reader:ImageReader?=null;private var display:android.hardware.display.VirtualDisplay?=null
 private val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
 private val busy=AtomicBoolean(false);private var lastValue=0.0;private var lastSent=0L
 private val handler=Handler(Looper.getMainLooper());private var scanThread:HandlerThread?=null
 override fun onBind(i:Intent?)=null
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
  createChannel();val notification=Notification.Builder(this,CHANNEL).setContentTitle("Crash Tracker screen scan").setContentText("OCR running · stop from tracker app").setSmallIcon(android.R.drawable.ic_menu_view).build()
  if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFY,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(NOTIFY,notification)
  val code=intent?.getIntExtra("resultCode",0)?:0;val data=intent?.getParcelableExtra<Intent>("data")
  if(projection==null&&data!=null){projection=(getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(code,data);startReader()}
  return START_NOT_STICKY
 }
 private fun createChannel(){if(Build.VERSION.SDK_INT>=26){val c=NotificationChannel(CHANNEL,"Screen OCR",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager::class.java).createNotificationChannel(c)}}
 private fun startReader(){
  val wm=getSystemService(WINDOW_SERVICE) as WindowManager;val metrics=DisplayMetrics();@Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(metrics)
  val w=metrics.widthPixels;val h=metrics.heightPixels;val density=metrics.densityDpi
  scanThread=HandlerThread("ocr-reader").apply{start()}
  reader=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2)
  reader?.setOnImageAvailableListener({ir->val image=ir.acquireLatestImage()?:return@setOnImageAvailableListener;process(image)},Handler(scanThread!!.looper))
  display=projection?.createVirtualDisplay("CrashTrackerCapture",w,h,density,0,reader?.surface,null,handler)
  projection?.registerCallback(object:MediaProjection.Callback(){override fun onStop(){stopSelf()}},handler)
 }
 private fun process(img:Image){
  if(!busy.compareAndSet(false,true)){img.close();return}
  try{
   val input=InputImage.fromMediaImage(img,0)
   recognizer.process(input).addOnSuccessListener{result->
    val regex=Regex("(?i)(\\d{1,5}(?:\\.\\d{1,3})?)\\s*[x×]")
    val v=regex.findAll(result.text).mapNotNull{it.groupValues[1].toDoubleOrNull()}.filter{it>=1.0&&it<=100000.0}.lastOrNull()
    if(v!=null){val now=System.currentTimeMillis();if(v!=lastValue||now-lastSent>5000){lastValue=v;lastSent=now;sendBroadcast(Intent(ACTION_MULTIPLIER).setPackage(packageName).putExtra("multiplier",v))}}
   }.addOnFailureListener{}.addOnCompleteListener{img.close();busy.set(false)}
  }catch(e:Exception){img.close();busy.set(false)}
 }
 override fun onDestroy(){display?.release();reader?.close();projection?.stop();scanThread?.quitSafely();recognizer.close();super.onDestroy()}
}