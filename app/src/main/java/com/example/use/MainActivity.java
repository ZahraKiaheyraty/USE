package com.example.use;

import androidx.appcompat.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    static String MQTTHOST = "tcp://45.149.79.49:1883";
    static String USERNAME = "1wNMcgkZcB4PFZIu3CVt";
    static String PASSWORD = "admin@hamta";
    MqttAndroidClient client;
    TextView subText;
    MqttConnectOptions options;
    TextView tempTextView;
    TextView temperature;
    TextView Speed;
    TextView wholeView;
    IntentFilter intentfilter;
    Random Number;
    Switch startAllServices;
    Handler updateHandler;
    String WindDm;
    String WindSm;
    String wholeUptime;
    float batteryTemp;
    int Rnumber;
    String SDirection[] = {"North West"};
    String topicStr = "v1/devices/me/telemetry";
    boolean isActiveService = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), MQTTHOST, clientId);
        options = new MqttConnectOptions();
        options.setUserName(USERNAME);
        options.setPassword(PASSWORD.toCharArray());

        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(getBaseContext(), "Connect", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(getBaseContext(), "dis Connect", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {}

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                subText.setText(new String(message.getPayload()));
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });

        TextView textViewWindDirection = (TextView) findViewById(R.id.text_wind_dir);
        TextView textViewTemp = (TextView) findViewById(R.id.text_temperature);
        TextView textViewWind = (TextView) findViewById(R.id.text_wind_spe);
        startAllServices = (Switch) findViewById(R.id.simpleSwitch);
        tempTextView = (TextView) findViewById(R.id.text_cpu_temperature);
        temperature = (TextView) findViewById(R.id.text_temperature);
        Speed = (TextView) findViewById(R.id.text_wind_spe);

        intentfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        MainActivity.this.registerReceiver(broadcastreceiver, intentfilter);
        wholeView = findViewById(R.id.text_up_time);
        updateHandler = new Handler();

        Thread trd = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(2000);  //1000ms = 1 sec
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Number = new Random();
                                Rnumber = ThreadLocalRandom.current().nextInt(12, 14);
                                textViewTemp.setText(String.valueOf(Rnumber + " " + (char) 0x00B0 + "C"));

                                double WindS = ThreadLocalRandom.current().nextDouble(2.7, 2.75);
                                textViewWind.setText(String.valueOf(WindS));
                                textViewWind.setText("WindSpeed:"+" "+new DecimalFormat("##.##").format(WindS)+" "+" m/s");
                                WindSm = new DecimalFormat("##.##").format(WindS);

                                Random RDirection = new Random();
                                int index = RDirection.nextInt(SDirection.length - 0) + 0;
                                textViewWindDirection.setText("Wind direction:"+" "+ SDirection[index]);
                                WindDm = SDirection[index];

                                if (isActiveService) {
                                    updateUptimes();
                                    updateServices();
                                }
                            }
                        });

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        trd.start();
        startAllServices.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isActiveService = b;
            }
        });
    }

    private BroadcastReceiver broadcastreceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            batteryTemp = (float) (intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10;
            tempTextView.setText(batteryTemp + " " + (char) 0x00B0 + "C");
        }
    };

    private void updateUptimes() {
        long uptimeMillis = SystemClock.elapsedRealtime();
        wholeUptime = String.format(Locale.getDefault(),
                "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(uptimeMillis),
                TimeUnit.MILLISECONDS.toMinutes(uptimeMillis)
                        - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS
                        .toHours(uptimeMillis)),
                TimeUnit.MILLISECONDS.toSeconds(uptimeMillis)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
                        .toMinutes(uptimeMillis)));
        wholeView.setText(wholeUptime);
    }

    public void updateServices() {

        String topic = topicStr;
        String message = "{\"Temp\":\"" + Rnumber + " ºc" + "\"}";
        try {
            client.publish(topic, message.getBytes(), 1, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        String topic2 = topicStr;
        String message2 = "{\"Up time\":\"" + wholeUptime + "\"}";
        try {
            client.publish(topic2, message2.getBytes(), 1, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        String topic3 = topicStr;
        String message3 = "{\"CPU Temp\":\"" + batteryTemp + " ºc" + "\"}";
        try {
            client.publish(topic3, message3.getBytes(), 1, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        String topic4 = topicStr;
        String message4 = "{\"Wind Direction\":\"" + WindDm + "\"}";
        try {
            client.publish(topic4, message4.getBytes(), 1, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        String topic5 = topicStr;
        String message5 = "{\"Wind Speed\":\"" + WindSm + " m/s" + "\"}";
        try {
            client.publish(topic5, message5.getBytes(), 1, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
