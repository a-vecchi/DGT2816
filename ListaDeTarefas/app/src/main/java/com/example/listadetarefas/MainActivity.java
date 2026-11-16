package com.example.listadetarefas;

import android.app.AlertDialog;
import android.media.AudioDeviceInfo;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AudioController audioController;
    private TextToSpeech textToSpeech;
    private Button addTaskButton;
    private ListView taskListView;

    private ArrayList<String> listaTarefas;
    private ArrayAdapter<String> adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        audioController = new AudioController(this);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("pt", "BR"));
            }
        });

        addTaskButton = findViewById(R.id.addTaskButton);
        taskListView = findViewById(R.id.taskListView);

        listaTarefas = new ArrayList<>();
        listaTarefas.add("Testar áudio no relógio");

        adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaTarefas);
        taskListView.setAdapter(adaptador);

        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.playSoundEffect(SoundEffectConstants.CLICK);
                mostrarDialogoNovaTarefa();
            }
        });

        taskListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String tarefaConcluida = listaTarefas.remove(position);
                adaptador.notifyDataSetChanged();

                tocarNotificacaoNativa("Tarefa concluída: " + tarefaConcluida);
                Toast.makeText(MainActivity.this, "Concluída: " + tarefaConcluida, Toast.LENGTH_SHORT).show();
            }
        });

        audioController.monitorarDispositivosAudio(new AudioController.AudioConnectionListener() {
            @Override
            public void onBluetoothConnected() {
                Toast.makeText(MainActivity.this, "Fone Bluetooth Conectado!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBluetoothDisconnected() {
                Toast.makeText(MainActivity.this, "Fone Bluetooth Desconectado!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoNovaTarefa() {
        EditText inputTarefa = new EditText(this);
        inputTarefa.setHint("Nome da tarefa...");

        new AlertDialog.Builder(this)
                .setTitle("Nova Tarefa")
                .setView(inputTarefa)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String texto = inputTarefa.getText().toString().trim();
                    if (!texto.isEmpty()) {
                        listaTarefas.add(texto);
                        adaptador.notifyDataSetChanged();
                        tocarNotificacaoNativa("Nova tarefa adicionada: " + texto);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void tocarNotificacaoNativa(String mensagemVoz) {
        boolean temSaida = audioController.audioOutputAvailable(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) ||
                audioController.audioOutputAvailable(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);

        if (temSaida) {
            try {
                Uri somNotificacao = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), somNotificacao);
                r.play();

                if (textToSpeech != null && mensagemVoz != null) {
                    textToSpeech.speak(mensagemVoz, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Nenhuma saída de áudio encontrada. Abrindo Bluetooth...", Toast.LENGTH_SHORT).show();
            audioController.abrirConfiguracoesBluetooth();
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}