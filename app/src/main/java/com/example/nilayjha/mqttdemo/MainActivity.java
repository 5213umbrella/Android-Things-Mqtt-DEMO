package com.example.nilayjha.mqttdemo;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;

public class MainActivity extends Activity implements MqttCallback {

    // led pin
    private final String PIN_LED = "BCM5";
    // Tag for logger
    private final String TAG = "MqttDemo";
    // Paho Mqtt constants    
    // The topic to which one publish message 
    //or subscribe to
    private final String TOPIC = "testauber";
    // MQTT broker URL
    private final String BROKER_URL = "tcp://iot.eclipse.org:1883";
    // MQTT client ID (can be any valid string)
    private final String CLIENT_ID = "aubergineTestMqttAT";
    // Quality of Service 
    private final int QOS = 2;
    // to access any device connected to GPIO on rpi3
    private Gpio mLedGpio;
    Switch led_switch;
    MqttClient client=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // initialize the peripheral manager
        PeripheralManagerService service = new PeripheralManagerService();

        // register the GPIO pin for the output
        try {
            // Create GPIO connection for LED
            // listen to GPIO on pin BCM5
            mLedGpio = service.openGpio(PIN_LED);

            // Configure as an output
            // set the voltage to initially low
            // initially 0v
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e){
            Log.e(TAG, "Error on PeripheralIO API", e);
        }

        // connect to broker and subscribe to the topic
        try {
            subscribeToTopic();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        
        //click listner to for Switch on UI
        led_switch = (Switch) findViewById(R.id.ledswitch);
        led_switch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        setLedState(!isChecked);
                        sendMessage(isChecked?"OFF":"ON");
                    }
                });
    }

    //method to publish message
    private void sendMessage(String message){
        try{
            // get the client
            client = getClient();
            // if the client is not connected
            // to broker then connect it
	    // with valid connection options
            if(!client.isConnected()){
                client.connect(getConnectops());
            }
            //Initialize MqttMessage object
            MqttMessage mqttMessage = new MqttMessage();
            // set the message to be sent
            mqttMessage.setPayload(message.getBytes());
	    // set the MQTT quality of service
            mqttMessage.setQos(QOS);
	    // publish message to the topic
            client.publish(TOPIC,mqttMessage);
            // disconnect the client 
            client.disconnect();
        }
        catch (MqttPersistenceException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // initiate client
    // return existing object if already initialized
    // else return a new object
    private MqttClient getClient() throws MqttException {
        if (client!=null){
            return client;
        }
        return (new MqttClient(
                BROKER_URL,
                CLIENT_ID,
                new MemoryPersistence()));
    }

    // set connection options
    private MqttConnectOptions getConnectops(){
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(false);
        return connOpts;
    }

    // subscribe to TOPIC
    private void subscribeToTopic() throws MqttException {
        // make connection to MQTT BROKER_URL
            client = getClient();
            client.setCallback(this);
            client.connect(getConnectops());
            client.subscribe(TOPIC);
    }
    //method to change the state of led i.e. on/off
    private void setLedState(boolean isOn) {
        try {
            mLedGpio.setValue(isOn);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                led_switch.setChecked(isOn);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error on setLedState", e);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(client!=null){
            try {
                // disconnect the client
                client.disconnect();
                // close client
                client.close();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        //close the GPIO connection
        if (mLedGpio != null) {
            try {
                mLedGpio.close();
            } catch (IOException e) {
            } finally{
                mLedGpio = null;
            }
        }
    }

    // in the case of connection lost
    // reconnect and subscribe to the topic
    @Override
    public void connectionLost(Throwable throwable) {
        Log.d(TAG, "connectionLost....reconnecting");
        try {
            subscribeToTopic();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // to perform action based on the received MQTT message
    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        String payload = new String(mqttMessage.getPayload());
        Log.d(TAG, payload);
        switch (payload) {
            case "ON":
                Log.d(TAG, "LED ON");
                setLedState(false);
                break;
            case "OFF":
                Log.d(TAG, "LED OFF");
                setLedState(true);
                break;
            default:
                Log.d(TAG, "Message not supported!");
                break;
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Log.d(TAG, "deliveryComplete....");
    }
}
