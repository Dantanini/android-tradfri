package com.uyas.speaker.tradfri.sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.uyas.speaker.tradfri.Device;
import com.uyas.speaker.tradfri.R;
import com.uyas.speaker.tradfri.Tradfri;

import java.util.List;

public class TestActivity extends AppCompatActivity {
    private final static String TAG = "TestActivity";

    private Button add;
    private TextView stt;
    private List<Device> mDevices;

    private Tradfri mTradfri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        add = findViewById(R.id.add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SetupGateway(TestActivity.this, mTradfri);
            }
        });

        ListView devices = findViewById(R.id.devices);
        devices.setAdapter(mAdapter);
        devices.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                new SetupDevice(TestActivity.this, mDevices.get(i));
                return true;
            }
        });

        stt = findViewById(R.id.stt);

        findViewById(R.id.ptt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                voiceControl();
            }
        });

        mTradfri = new Tradfri(getApplicationContext(), new Tradfri.Listener() {
            @Override
            public void onRefresh() {
                add.setVisibility(mTradfri.getUnboundGateways().size() > 0 ? View.VISIBLE : View.GONE);
                mDevices = mTradfri.getDevices();
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private void voiceControl(){
        if (ContextCompat.checkSelfPermission(TestActivity.this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            speechInput();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(TestActivity.this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(TestActivity.this, "Grant RECORD_AUDIO in settings.", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(TestActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        }
    }

    private void speechInput(){
        SpeechRecognizer sr = SpeechRecognizer.createSpeechRecognizer(TestActivity.this);
        sr.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                stt.setText("...");
            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {
                stt.setText("(error)");
            }

            @Override
            public void onResults(Bundle bundle) {
                String text = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
                stt.setText(text);
                handleSpeechText(text);
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        sr.startListening(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    speechInput();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mTradfri.discovery();
    }

    enum Action {
        TURN_ON,
        TURN_OFF,
        TOGGLE,
    };

    private void handleSpeechText(String text){
        String tokenOn[] = new String[]{"打開", "開"};
        for(String tk : tokenOn){
            if(text.startsWith(tk)){
                performControl(Action.TURN_ON, text.substring(tk.length()));
                return;
            }
        }
        for(String tk : tokenOn){
            if(text.endsWith(tk)){
                performControl(Action.TURN_ON, text.substring(0, text.length()-tk.length()));
                return;
            }
        }

        String tokenOff[] = new String[]{"關掉", "關閉", "關"};
        for(String tk : tokenOff){
            if(text.startsWith(tk)){
                performControl(Action.TURN_OFF, text.substring(tk.length()));
                return;
            }
        }
        for(String tk : tokenOff){
            if(text.endsWith(tk)){
                performControl(Action.TURN_OFF, text.substring(0, text.length()-tk.length()));
                return;
            }
        }
    }

    private void performControl(Action action, String obj){
        if("全部".equals(obj)){
            obj = null;
        }
        if(obj == null){
            switch (action) {
                case TURN_ON:
                    for(Device d : mTradfri.getDevices()){
                        d.turnOn();
                    }
                    break;
                case TURN_OFF:
                    for(Device d : mTradfri.getDevices()){
                        d.turnOff();
                    }
                    break;
            }

        }
        Device d = mTradfri.getDeviceByName(obj);
        if(d != null){
            switch (action) {
                case TURN_ON:
                    d.turnOn();
                    break;
                case TURN_OFF:
                    d.turnOff();
                    break;
            }
        }
    }

    BaseAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            if(mDevices == null){
                return 0;
            }
            return mDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        class ViewHolder {
            TextView path;
            Button toggle;
            SeekBar brightness;
            SeekBar spectrum;
            SeekBar hue;
            SeekBar saturation;
            View control_brightness;
            View control_spectrum;
            View control_hue;
            View control_saturation;
            Device device;

            ViewHolder(View view){
                path = view.findViewById(R.id.path);
                toggle = view.findViewById(R.id.toggle);
                toggle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        device.toggle();
                    }
                });

                control_brightness = view.findViewById(R.id.control_brightness);
                control_spectrum = view.findViewById(R.id.control_spectrum);
                control_hue = view.findViewById(R.id.control_hue);
                control_saturation = view.findViewById(R.id.control_saturation);

                brightness = view.findViewById(R.id.brightness);
                brightness.setMin(Device.MIN_BRIGHTNESS);
                brightness.setMax(Device.MAX_BRIGHTNESS);
                brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        device.setBrightness(seekBar.getProgress());
                    }
                });

                spectrum = view.findViewById(R.id.spectrum);
                spectrum.setMin(Device.MIN_SPECTRUM);
                spectrum.setMax(Device.MAX_SPECTRUM);
                spectrum.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        device.setSpectrum(seekBar.getProgress());
                    }
                });

                hue = view.findViewById(R.id.hue);
                hue.setMin(Device.MIN_HUE);
                hue.setMax(Device.MAX_HUE);
                hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        device.setHue(seekBar.getProgress());
                    }
                });

                saturation = view.findViewById(R.id.saturation);
                saturation.setMin(Device.MIN_SATURATION);
                saturation.setMax(Device.MAX_SATURATION);
                saturation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        device.setSaturation(seekBar.getProgress());
                    }
                });
            }

            void bind(Device d){
                device = d;
                path.setText(d.getName());
                if(!device.isReady()){
                    toggle.setVisibility(View.GONE);
                    control_brightness.setVisibility(View.GONE);
                    control_spectrum.setVisibility(View.GONE);
                    control_hue.setVisibility(View.GONE);
                    control_saturation.setVisibility(View.GONE);
                    return;
                }
                if(d.supportLightControl()){
                    toggle.setText(d.getState() ? "Off" : "On");
                }
                toggle.setVisibility(d.supportLightControl() ? View.VISIBLE : View.GONE);
                control_brightness.setVisibility(d.supportLightControl() ? View.VISIBLE : View.GONE);
                brightness.setProgress(device.getBrightness());
                control_spectrum.setVisibility(d.supportSpectrum() ? View.VISIBLE : View.GONE);
                spectrum.setProgress(device.getSpectrum());
                control_hue.setVisibility(d.supportColor() ? View.VISIBLE : View.GONE);
                hue.setProgress(device.getHue());
                control_saturation.setVisibility(d.supportColor() ? View.VISIBLE : View.GONE);
                saturation.setProgress(device.getSaturation());
            }
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder vh;
            if(view == null){
                LayoutInflater layoutInflater = LayoutInflater.from(TestActivity.this);
                view = layoutInflater.inflate(R.layout.item_device, viewGroup, false);
                vh = new ViewHolder(view);
                view.setTag(vh);
            } else {
                vh = (ViewHolder) view.getTag();
            }
            Device device = (Device) getItem(i);
            vh.bind(device);
            return view;
        }
    };
}
