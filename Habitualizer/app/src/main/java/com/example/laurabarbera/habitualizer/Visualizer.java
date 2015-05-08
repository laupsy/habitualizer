package com.example.laurabarbera.habitualizer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import static android.util.FloatMath.sqrt;


public class Visualizer extends ActionBarActivity implements SensorEventListener {

    private float acceleration, acceleration_cur, acceleration_prev;
    private SensorManager sensorManager;
    private Sensor sensor;
    private float steps = 0;
    private ArrayList<String> questions;

    boolean doMotion;

    private Database db;
    private Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualizer);
        c = this;
        init();

        db = new Database(this);

        startService(new Intent(c, BgSensor.class));

        // MOTION

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        acceleration = 0.00f;
        acceleration_cur = SensorManager.GRAVITY_EARTH;
        acceleration_prev = SensorManager.GRAVITY_EARTH;

        // Check if motion is enabled
        // Check if location is enabled
        // Check question level
        int questionLevel = db.getQuestionSetting();
        int notifDelay = 10800000;
        // seconds per week * 1000
        if ( questionLevel == 0 ) notifDelay = 604800000;
        // seconds per day * 1000
        else if ( questionLevel == 1) notifDelay = 86400000;
        // seconds per 3 hours * 1000
        else notifDelay = 10800000;

        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask notifyInterval = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @SuppressWarnings("unchecked")
                    public void run() {
                        try {
                            askQuestion();
                        } catch(Exception e){}
                    }
                });
            }
        };
        timer.schedule(notifyInterval, 0, notifDelay);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable(){
                    public void run() {

                    }
                });
            }
        };
        Thread t = new Thread(r);
        t.start();
        GetQuestions getQuestions = new GetQuestions();
        getQuestions.execute();
    }
    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
    }
    @Override
    public void onPause() {
        super.onPause();
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
    }
    @Override
    public void onSensorChanged(SensorEvent e) {
        db = new Database(c);
        float THRESHOLD = 1;
        if ( e.sensor.getType() == Sensor.TYPE_ACCELEROMETER ) {
            float x = e.values.clone()[0];
            float y = e.values.clone()[1];
            float z = e.values.clone()[2];
            acceleration_prev = acceleration_cur;
            acceleration_cur = sqrt(x * x + y * y + z * z);
            float change = acceleration_cur - acceleration_prev;
            acceleration += change;
            if ( acceleration > THRESHOLD ) {
                db.updateMotion();
                TextView a = (TextView) findViewById(R.id.motion_daily);
                TextView b = (TextView) findViewById(R.id.motion_hour0);
                TextView c = (TextView) findViewById(R.id.motion_hour3);
                TextView d = (TextView) findViewById(R.id.motion_hour6);
                TextView e2 = (TextView) findViewById(R.id.motion_hour9);
                TextView f = (TextView) findViewById(R.id.motion_hour12);
                TextView g = (TextView) findViewById(R.id.motion_hour15);
                TextView h = (TextView) findViewById(R.id.motion_hour18);
                TextView i = (TextView) findViewById(R.id.motion_hour21);
                //db.getMotion();
                float[] steps = db.getMotion();
                b.setText(steps[0] + " steps from midnight to 2am");
                c.setText(steps[1] + " steps from 2am to 5am");
                d.setText(steps[2] + " steps from 5am to 8am");
                e2.setText(steps[3] + " steps from 8am to 11am");
                f.setText(steps[4] + " steps from 11am to 2pm");
                g.setText(steps[5] + " steps from 2pm to 5pm");
                h.setText(steps[6] + " steps from 5pm to 8pm");
                i.setText(steps[7] + " steps from 8pm to 11pm");
                a.setText(steps[8] + " steps total");
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    public void init() {
        final Handler handler = new Handler();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable(){
                    public void run() {
                        ActionBar a = getSupportActionBar();
                        setTitle(R.string.getstarted);
                        a.setElevation(0);
                        a.setDisplayHomeAsUpEnabled(false);
                        a.setDisplayShowHomeEnabled(false);
                        a.setDisplayShowCustomEnabled(true);
                        a.setDisplayShowTitleEnabled(false);
                        getSupportActionBar().setCustomView(R.layout.actionbar_layout);
                        ImageView v = (ImageView) findViewById(R.id.visualizer);
                        v.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                Intent visualizer = new Intent(Visualizer.this, Visualizer.class);
                                Visualizer.this.startActivity(visualizer);
                                Visualizer.this.finish();
                            }
                        });
                        ImageView icon = (ImageView) findViewById(R.id.home_icon);
                        icon.setBackgroundResource(R.drawable.icon_back);
                        icon.setOnClickListener(new View.OnClickListener(){
                            public void onClick(View v) {
                                Intent back = new Intent(Visualizer.this, Dashboard.class);
                                Visualizer.this.startActivity(back);
                                Visualizer.this.finish();
                            }
                        });
                    }
                });
            }
        };
        Thread t = new Thread(r);
        t.start();
    }
    public void askQuestion() {

        /* COPY/PASTED THIS FROM
            http://developer.android.com/training/notify-user/build-notification.html */

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");

        Intent resultIntent = new Intent(this, AskQuestion.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);
        int mNotificationId = 001;
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

        /* COPY/PASTE OVER */
    }

    private class GetQuestions extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            Database db = new Database(c);
            questions = db.getQuestions();
            return true;
        }

        protected void onPostExecute(Boolean result) {
            Database db = new Database(c);
            int num, yes, no;
            String question;
            LinearLayout qm = (LinearLayout) findViewById(R.id.visualizer_view);
            for ( int i = 0; i < questions.size(); i++ ) {

                num = Integer.parseInt(questions.get(i).split(">>")[0]);
                yes = db.getAnswerYes(num);
                no = db.getAnswerNo(num);
                question = questions.get(i).split(">>")[1];

                /* Got how to add views programmatically from here:
                    http://stackoverflow.com/questions/2395769/how-to-programmatically-add-views-to-views
                 */
                TextView t = new TextView(c);
                View d = new View(c);
                d.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        2
                ));
                d.setBackgroundColor(getResources().getColor(R.color.gray_light));
                t.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

                t.setText("(" + (i+1) + ") " + question + " | Yes: " + yes + " | No: " + no);
                t.setPadding(
                        Math.round(getResources().getDimension(R.dimen.activity_horizontal_margin)),
                        Math.round(getResources().getDimension(R.dimen.header_margin)),
                        Math.round(getResources().getDimension(R.dimen.activity_horizontal_margin)),
                        Math.round(getResources().getDimension(R.dimen.header_margin))
                );
                t.setTextSize(16);
                t.setTextColor(getResources().getColor(R.color.label_text));
                qm.addView(t);
                qm.addView(d);
            }
        }
    }
}
