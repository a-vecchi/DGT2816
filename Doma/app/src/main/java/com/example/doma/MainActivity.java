package com.example.doma;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_CODE_VOICE = 100;

    private AudioHelper audioHelper;
    private TextToSpeech tts;
    private TextView statusText;
    private boolean isTtsReady = false;

    private final AudioDeviceCallback audioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            super.onAudioDevicesAdded(addedDevices);
            if (audioHelper.isBluetoothHeadsetConnected()) {
                atualizarStatus("Fone Bluetooth conectado. Áudio pronto.");
                falarTexto("Fone de ouvido Bluetooth conectado.");
            }
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            super.onAudioDevicesRemoved(removedDevices);
            if (!audioHelper.isBluetoothHeadsetConnected()) {
                atualizarStatus("Fone Bluetooth desconectado.");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.tv_status);
        Button btnAlerta = findViewById(R.id.btn_alerta);
        Button btnBluetooth = findViewById(R.id.btn_bluetooth);
        Button btnVoz = findViewById(R.id.btn_voz);

        audioHelper = new AudioHelper(this);
        tts = new TextToSpeech(this, this);

        verificarSaidaInicial();

        btnAlerta.setOnClickListener(v -> dispararAlertaAcessibilidade(
                "Alerta de segurança Doma: Reunião de equipe no setor B em 5 minutos."
        ));

        btnBluetooth.setOnClickListener(v -> abrirConfiguracoesBluetooth());

        btnVoz.setOnClickListener(v -> iniciarReconhecimentoVoz());
    }

    private void iniciarReconhecimentoVoz() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale seu comando para a Doma...");

        try {
            startActivityForResult(intent, REQUEST_CODE_VOICE);
        } catch (ActivityNotFoundException e) {
            atualizarStatus("Reconhecimento de voz indisponível neste dispositivo.");
            falarTexto("Serviço de voz não encontrado no relógio.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_VOICE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> resultados = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);

            if (resultados != null && !resultados.isEmpty()) {
                String comandoFalado = resultados.get(0).toLowerCase(Locale.ROOT);
                processarComandoDeVoz(comandoFalado);
            }
        }
    }

    private void processarComandoDeVoz(String comando) {
        atualizarStatus("Você disse: \"" + comando + "\"");

        if (comando.contains("ajuda") || comando.contains("emergência") || comando.contains("socorro")) {
            falarTexto("Pedido de assistência enviado à equipe de apoio Doma. Aguarde no seu posto.");
        } else if (comando.contains("alerta") || comando.contains("notificação") || comando.contains("mensagem")) {
            falarTexto("Você tem um alerta crítico: Treinamento de evacuação hoje às 15 horas.");
        } else if (comando.contains("confirmar") || comando.contains("ok") || comando.contains("recebido")) {
            falarTexto("Confirmação registrada com sucesso no sistema interno.");
        } else {
            falarTexto("Comando recebido: " + comando + ". Diga ajuda, alerta ou confirmar.");
        }
    }

    private void verificarSaidaInicial() {
        if (audioHelper.isBluetoothHeadsetConnected()) {
            atualizarStatus("Saída ativa: Fone Bluetooth");
        } else if (audioHelper.isSpeakerAvailable()) {
            atualizarStatus("Saída ativa: Alto-falante do relógio");
        } else {
            atualizarStatus("Nenhuma saída de áudio detectada.");
            abrirConfiguracoesBluetooth();
        }
    }

    private void dispararAlertaAcessibilidade(String mensagem) {
        if (audioHelper.isBluetoothHeadsetConnected() || audioHelper.isSpeakerAvailable()) {
            falarTexto(mensagem);
        } else {
            atualizarStatus("Conecte um fone para ouvir o alerta.");
            abrirConfiguracoesBluetooth();
        }
    }

    private void abrirConfiguracoesBluetooth() {
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("EXTRA_CONNECTION_ONLY", true);
        intent.putExtra("EXTRA_CLOSE_ON_CONNECT", true);
        intent.putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 1);
        startActivity(intent);
    }

    private void falarTexto(String texto) {
        if (isTtsReady && tts != null) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "DOMA_MSG_ID");
        }
    }

    private void atualizarStatus(String texto) {
        runOnUiThread(() -> statusText.setText(texto));
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("pt", "BR"));
            isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        audioHelper.registerAudioCallback(audioDeviceCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        audioHelper.unregisterAudioCallback(audioDeviceCallback);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}