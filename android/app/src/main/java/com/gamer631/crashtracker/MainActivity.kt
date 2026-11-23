package com.gamer631.crashtracker

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import android.content.Context
import org.json.JSONArray

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("round_history", Context.MODE_PRIVATE) }
    private val rounds = mutableListOf<Double>()
    private lateinit var list: LinearLayout
    private lateinit var stats: TextView
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 16, 30)
        window.navigationBarColor = Color.rgb(12, 16, 30)
        window.decorView.setSystemUiVisibility(0)
        load()
        buildUi()
        refresh()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun text(s: String, size: Float, color: Int = Color.WHITE): TextView =
        TextView(this).apply { text=s; textSize=size; setTextColor(color); setPadding(dp(2),dp(5),dp(2),dp(5)) }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(12),dp(16),dp(12))
        setBackgroundColor(Color.rgb(25,32,51))
    }
    private fun buildUi() {
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(12,16,30)); setPadding(dp(16),dp(12),dp(16),dp(10)) }
        val scroll=ScrollView(this)
        val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        body.addView(text("CRASH TRACKER",22f,Color.rgb(100,220,255)))
        body.addView(text("Round history & statistics",14f,Color.LTGRAY))
        body.addView(card().apply {
            addView(text("TRACK A ROUND",16f,Color.rgb(100,220,255)))
            input=EditText(this@MainActivity).apply { hint="Multiplier (e.g. 2.45)"; setSingleLine(); textSize=18f; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setPadding(dp(12),dp(12),dp(12),dp(12)); setBackgroundColor(Color.rgb(40,48,68)); inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }
            addView(input)
            addView(Button(this@MainActivity).apply { text="ADD ROUND"; setOnClickListener { addRound() } })
            addView(Button(this@MainActivity).apply { text="UNDO LAST ROUND"; setOnClickListener { if(rounds.isNotEmpty()){rounds.removeAt(rounds.lastIndex);save();refresh()} } })
            addView(Button(this@MainActivity).apply { text="CLEAR HISTORY"; setOnClickListener { AlertDialog.Builder(this@MainActivity).setTitle("Clear history?").setMessage("This permanently removes saved rounds from this phone.").setNegativeButton("Cancel",null).setPositiveButton("Clear"){_,_->rounds.clear();save();refresh()}.show() } })
        })
        stats=text("",14f)
        body.addView(card().apply { addView(text("OVERVIEW",16f,Color.rgb(100,220,255))); addView(stats) })
        body.addView(text("RECENT MULTIPLIERS",16f,Color.rgb(100,220,255)))
        list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        body.addView(list)
        body.addView(text("Statistics describe recorded rounds only. They do not predict the next crash result or provide guaranteed betting signals.",12f,Color.LTGRAY))
        scroll.addView(body); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f)); setContentView(root)
    }
    private fun addRound() {
        val v=input.text.toString().toDoubleOrNull()
        if(v==null || v<=0){Toast.makeText(this,"Enter a positive multiplier",Toast.LENGTH_SHORT).show();return}
        rounds.add(v);save();input.text.clear();refresh()
    }
    private fun refresh() {
        val n=rounds.size
        val avg=if(n>0) rounds.average() else 0.0
        val sorted=rounds.sorted()
        val median=if(n==0) 0.0 else if(n%2==1) sorted[n/2] else (sorted[n/2-1]+sorted[n/2])/2
        val low=rounds.count{it<2}; val high=rounds.count{it>=10}
        var lowStreak=0; for(v in rounds.reversed()){if(v<2)lowStreak++ else break}
        var highStreak=0; for(v in rounds.reversed()){if(v>=10)highStreak++ else break}
        stats.text="Rounds: $n\nAverage: %.2fx   Median: %.2fx\nBelow 2x: %d (%.1f%%)\n10x+: %d (%.1f%%)\nCurrent low streak: %d   High streak: %d".format(avg,median,low,if(n>0)100.0*low/n else 0.0,high,if(n>0)100.0*high/n else 0.0,lowStreak,highStreak)
        list.removeAllViews()
        rounds.takeLast(60).reversed().forEachIndexed { idx,v ->
            val label=text("%02d   %.2fx".format(rounds.size-idx,v),16f,when{v<2->Color.rgb(255,130,140);v>=10->Color.rgb(120,255,180);else->Color.WHITE})
            label.setPadding(dp(8),dp(8),dp(8),dp(8))
            list.addView(label)
        }
    }
    private fun save(){prefs.edit().putString("data",JSONArray(rounds).toString()).apply()}
    private fun load(){try{val arr=JSONArray(prefs.getString("data","[]"));for(i in 0 until arr.length()){val v=arr.optDouble(i);if(v>0)rounds.add(v)}}catch(_:Exception){}}
}
