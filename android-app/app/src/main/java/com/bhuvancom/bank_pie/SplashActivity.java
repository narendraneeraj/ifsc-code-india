package com.bhuvancom.bank_pie;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.widget.ImageView;
import android.graphics.Color;
public final class SplashActivity extends Activity {
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable launch=()->{if(!isFinishing()){startActivity(new Intent(this,MainActivity.class));finish();}};
 @Override public void onCreate(Bundle state){super.onCreate(state);ImageView image=new ImageView(this);image.setImageResource(com.bhuvancom.bank_pie.R.drawable.splash);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setBackgroundColor(Color.rgb(235,245,255));image.setContentDescription("Bank Pie — IFSC and MICR Code");setContentView(image);handler.postDelayed(launch,450);}
 @Override protected void onDestroy(){handler.removeCallbacks(launch);super.onDestroy();}
}
