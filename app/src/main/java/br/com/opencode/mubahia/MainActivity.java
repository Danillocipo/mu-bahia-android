package br.com.opencode.mubahia;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView statusText, percentText, infoText, doneInfoText;
    private CardView progressCard, setupCard, doneCard;
    private Button downloadBtn, installWinlatorBtn, openWinlatorBtn, deleteBtn;

    private RuntimeManager runtimeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        runtimeManager = new RuntimeManager(this);

        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        percentText = findViewById(R.id.percentText);
        infoText = findViewById(R.id.infoText);
        progressCard = findViewById(R.id.progressCard);
        setupCard = findViewById(R.id.setupCard);
        doneCard = findViewById(R.id.doneCard);

        doneInfoText = findViewById(R.id.doneInfoText);
        downloadBtn = findViewById(R.id.downloadBtn);
        installWinlatorBtn = findViewById(R.id.installWinlatorBtn);
        openWinlatorBtn = findViewById(R.id.openWinlatorBtn);
        deleteBtn = findViewById(R.id.deleteBtn);

        downloadBtn.setOnClickListener(v -> downloadGame());
        installWinlatorBtn.setOnClickListener(v -> downloadAndInstallWinlator());
        openWinlatorBtn.setOnClickListener(v -> runtimeManager.openWinlator());
        deleteBtn.setOnClickListener(v -> confirmDelete());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshState();
    }

    private void refreshState() {
        boolean gameReady = runtimeManager.isGameReady();
        boolean winlatorInstalled = runtimeManager.isWinlatorInstalled();

        if (gameReady && winlatorInstalled) {
            setupCard.setVisibility(View.GONE);
            doneCard.setVisibility(View.VISIBLE);
            doneInfoText.setText("Tudo pronto!\nWinlator instalado e jogos em /storage/emulated/0/MuBahia/");
            return;
        }

        setupCard.setVisibility(View.VISIBLE);
        doneCard.setVisibility(View.GONE);

        if (!gameReady) {
            downloadBtn.setVisibility(View.VISIBLE);
            installWinlatorBtn.setVisibility(View.GONE);
            infoText.setText("Passo 1: Baixar arquivos do Mu Bahia");
        } else if (!winlatorInstalled) {
            downloadBtn.setVisibility(View.GONE);
            installWinlatorBtn.setVisibility(View.VISIBLE);
            infoText.setText("Passo 2: Instalar Winlator");
        }
    }

    private void downloadGame() {
        progressCard.setVisibility(View.VISIBLE);
        downloadBtn.setEnabled(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                runtimeManager.downloadGameFiles((pct, msg) -> runOnUiThread(() -> {
                    progressBar.setProgress(pct);
                    statusText.setText(msg);
                    percentText.setText(pct + "%");
                }));

                runOnUiThread(() -> {
                    progressCard.setVisibility(View.GONE);
                    downloadBtn.setVisibility(View.GONE);
                    infoText.setText("Jogo baixado! Agora instale o Winlator.");
                    installWinlatorBtn.setVisibility(View.VISIBLE);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressCard.setVisibility(View.GONE);
                    downloadBtn.setEnabled(true);
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Erro")
                        .setMessage("Falha ao baixar: " + e.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
                });
            }
        });
    }

    private void downloadAndInstallWinlator() {
        progressCard.setVisibility(View.VISIBLE);
        installWinlatorBtn.setEnabled(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                runtimeManager.downloadWinlator((pct, msg) -> runOnUiThread(() -> {
                    progressBar.setProgress(pct);
                    statusText.setText(msg);
                    percentText.setText(pct + "%");
                }));

                runOnUiThread(() -> {
                    progressCard.setVisibility(View.GONE);
                    try {
                        runtimeManager.installWinlator();
                    } catch (Exception e2) {
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Erro")
                            .setMessage("Nao foi possivel instalar: " + e2.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressCard.setVisibility(View.GONE);
                    installWinlatorBtn.setEnabled(true);
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Erro")
                        .setMessage("Falha ao baixar Winlator: " + e.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
                });
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
            .setTitle("Apagar arquivos")
            .setMessage("Isso vai apagar os arquivos do jogo em /storage/emulated/0/MuBahia/")
            .setPositiveButton("SIM", (d, w) -> {
                runtimeManager.deleteGameFiles();
                refreshState();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }
}
