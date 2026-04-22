package dev.squatedev.latuckddos.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.squate.interfaces.AttackCallback;

import dev.squatedev.latuckddos.ClientTester.ClientTesterAttack;
import dev.squatedev.latuckddos.R;

public class ActivityTEST extends AppCompatActivity {
    private TextView delayed_text;
    private EditText editText1;
    private EditText editText2;
    private Button button1;
    private SeekBar seek_delayed;
    private TextView textView1;
    private TextView textViewPackets;
    private TextView textViewConnections;
    private int delayed_send = 100;
    private boolean start_attack = false;
    private ClientTesterAttack clientTesterAttack;
    private Handler handler;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test);

        editText1 = findViewById(R.id.editTextIP);
        editText2 = findViewById(R.id.editTextPort);
        button1 = findViewById(R.id.buttonStart);
        delayed_text = findViewById(R.id.msg_show);
        seek_delayed = findViewById(R.id.msg_seekbar);
        textView1 = findViewById(R.id.statusText);
        textViewPackets = findViewById(R.id.textViewPackets);
        textViewConnections = findViewById(R.id.textViewConnections);

        clientTesterAttack = ClientTesterAttack.getInstance(this);
        handler = new Handler(Looper.getMainLooper());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.linearLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        load_callback();
    }

    private boolean validate(){
        String ip = editText1.getText().toString().trim();
        String port = editText2.getText().toString().trim();
        return ip.isEmpty() || port.isEmpty();
    }

    private void load_callback(){
        button1.setOnClickListener(v -> {
            if(validate()){
                toast("Проверьте поле ввода IP и Port");
                return;
            }

            if(!start_attack){
                String ip = editText1.getText().toString().trim();
                String port = editText2.getText().toString().trim();

                clientTesterAttack.setCallback(new AttackCallback() {
                    @Override
                    public void onStatusUpdate(String status) {
                        runOnUiThread(() -> textView1.setText(status));
                    }

                    @Override
                    public void onStatsUpdate(int packets, int connections) {
                        runOnUiThread(() -> {
                            textViewPackets.setText("Packets: " + packets);
                            textViewConnections.setText("Connections: " + connections);
                        });
                    }
                });

                clientTesterAttack.startAttack(ip, port);
                start_attack = true;
                button1.setText("STOP");
                toast("Атака запущена");
            } else {
                clientTesterAttack.stopAttack();
                start_attack = false;
                button1.setText("START");
                textView1.setText("STATUS: READY");
                textViewPackets.setText("Packets: 0");
                textViewConnections.setText("Connections: 0");
                toast("Атака остановлена");
            }
        });

        seek_delayed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private final int[] SNAP_VALUES = {1, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
            private boolean isUserInteracting = false;

            private int snapToValue(int progress) {
                int closest = SNAP_VALUES[0];
                int minDiff = Math.abs(progress - closest);
                for (int value : SNAP_VALUES) {
                    int diff = Math.abs(progress - value);
                    if (diff < minDiff) {
                        minDiff = diff;
                        closest = value;
                    }
                }
                if (minDiff <= 30) {
                    return closest;
                }
                return progress;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                delayed_text.setText(String.format("Delay: %dms", i));
                if (b && isUserInteracting) {
                    int snapped = snapToValue(i);
                    if (snapped != i) {
                        seekBar.setProgress(snapped);
                        delayed_send = snapped;
                        delayed_text.setText(String.format("Delay: %dms", snapped));
                        clientTesterAttack.setDelay(snapped);
                        return;
                    }
                }
                delayed_send = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserInteracting = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserInteracting = false;
                int finalProgress = seekBar.getProgress();
                int snapped = snapToValue(finalProgress);
                if (snapped != finalProgress) {
                    seekBar.setProgress(snapped);
                    delayed_send = snapped;
                    delayed_text.setText(String.format("Delay: %dms", snapped));
                    clientTesterAttack.setDelay(snapped);
                }
            }
        });
    }

    private void toast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(clientTesterAttack != null && clientTesterAttack.isRunning()){
            clientTesterAttack.stopAttack();
        }
        if(handler != null){
            handler.removeCallbacksAndMessages(null);
        }
    }
}