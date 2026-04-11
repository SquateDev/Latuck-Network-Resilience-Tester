/*________________________________________________*//*


               Author: @Squatedev
               age: 19
               name: null


/*________________________________________________*/

// класс для работы с категорией ддос меню для ддос атаки создатель : SquateDev
// накидал комментариев так как кент попросил чтобы понять код держи друг мой будет как хотела буду стараться все комментировать каждый кусок чтобы ты понял брух мой

package dev.squatedev.latuckddos.ActivityDDos;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import dev.squatedev.latuckddos.FileManager.FileManager;
import dev.squatedev.latuckddos.ProxySend.ProxyClient;
import dev.squatedev.latuckddos.R;

public class DDosMenu extends AppCompatActivity {
    private FileManager fileManager;
    private TextView delayed_text;
    private EditText editText1;
    private EditText editText2;
    private Button button1;
    private Button button2;
    private Button button3;
    private SeekBar seek_delayed;
    private int delayed_send = 100;
    private boolean start_attack = false;
    private ProxyClient proxyClient;
    private String tag = "LatuckDDos";
    private Handler handler;
    private SharedPreferences sharedPreferences;
    private String get_selected_proxy = "n/a";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ddos);
        // поле ввода ип адресса или домена
        editText1 = findViewById(R.id.editTextIP);
        // поле ввода порт.
        editText2 = findViewById(R.id.editTextPort);
        // кнопка для старта атаки и также остановить атаку
        button1 = findViewById(R.id.buttonStart);
        // сам ищет порт открытый и делает нагрузку на него можно и на все там в коде помменяете
        button2 = findViewById(R.id.buttonPortAuto);
        // Proxy кнопка для обхода бана по одному ип адрессу
        button3 = findViewById(R.id.buttonProxy);
        // задержка между отправкой чем меньше тем быстрее и больше пакетов
        delayed_text = findViewById(R.id.msg_show);
        // слайдер для управления задержками
        seek_delayed = findViewById(R.id.msg_seekbar);
        // получаем один экземпляр класса для работы файла чтобы сохранять или получать прокси списки которые лежать в файлах можно кст чужие выгружать если чо там путь найдете загрузите чужой либо кнопку добавлю щас для export,load можно будет поделиться или загрузить чужой также по ссылке можно получать
        fileManager = FileManager.getInstance(this);
        //экземпляр класса прокси для отправки атаки и также масскировки ип
        proxyClient = ProxyClient.getInstance(this);
        // выставляем отсупы по размерам чтобы все красиво встало а не вылетело за рамки
        handler = new Handler(Looper.getMainLooper());
        // получаем выбранный файл прокси
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // сохраняю сразу имя выбранного файла с меню прокси
        get_selected_proxy = sharedPreferences.getString("name","n/a");
        // отсупы в статус бар и навигатор если он есть крч да да
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.linearLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // активируем все обработки по кнопкам, и слайдерам
        load_callback();
        tick();
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
            if(validate()){
                toast("Проверьте поле ввода IP и Port");
                return;
            }
            start_attack = !start_attack;
            if(start_attack){
                button1.setText("STOP");
            } else {
                button1.setText("START");
            }
        });
        // обработка на кнопку поиска порта крч штука такая если лень порты искать открытые он сам один найдет и ток по нему бить будет вот надеюсь понятно обяснил
        button2.setOnClickListener(view -> {

        });
        // обработка по прокси типо если кнопка выкл будет идти дос а не ддос дос когда с одного ип идет атака а ддос с разных для этого и есть прокси фукнция чтобы обойти бан и ваш ип не был забанен и могли нагружать сервера сайтов и так далее для чего юзать будете
        button3.setOnClickListener(v -> {

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

                }
                handler.postDelayed(this, delayed_send);
            }
        }, delayed_send);
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