package br.com.opencode.mubahia;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView statusText, percentText;
    private CardView progressCard;
    private Button launchBtn, settingsBtn;
    private View contentRoot;

    private RuntimeManager runtimeManager;
    private GameLauncher gameLauncher;
    private SharedPreferences prefs;

    private boolean isReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("mubahia", MODE_PRIVATE);
        runtimeManager = new RuntimeManager(this);
        gameLauncher = new GameLauncher(this, runtimeManager);

        contentRoot = findViewById(R.id.contentRoot);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        percentText = findViewById(R.id.percentText);
        progressCard = findViewById(R.id.progressCard);
        launchBtn = findViewById(R.id.launchBtn);
        settingsBtn = findViewById(R.id.settingsBtn);

        launchBtn.setOnClickListener(v -> launchGame());
        settingsBtn.setOnClickListener(v -> showSettings());

        animateEntry();
        checkSetup();
    }

    private void animateEntry() {
        contentRoot.setAlpha(0f);
        contentRoot.setTranslationY(40f);
        contentRoot.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start();
    }

    private void checkSetup() {
        if (runtimeManager.isRuntimeReady() && getGameDir().exists()) {
            onSetupComplete();
            return;
        }

        progressCard.setVisibility(View.VISIBLE);
        launchBtn.setEnabled(false);
        settingsBtn.setEnabled(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File gameDir = getGameDir();

                // Step 1: Download game files
                updateStatus("Baixando Mu Bahia...", 5);
                runtimeManager.downloadGameFiles(gameDir, new RuntimeManager.ProgressCallback() {
                    @Override public void onProgress(int pct, String msg) {
                        int totalPct = (int)(pct * 0.25);
                        runOnUiThread(() -> {
                            progressBar.setProgress(totalPct);
                            statusText.setText(msg);
                            percentText.setText(totalPct + "%");
                        });
                    }
                });

                // Step 2: Download Wine runtime
                updateStatus("Baixando Wine runtime...", 25);
                runtimeManager.downloadRuntime(new RuntimeManager.ProgressCallback() {
                    @Override public void onProgress(int pct, String msg) {
                        runOnUiThread(() -> {
                            progressBar.setProgress(30 + (int)(pct * 0.4));
                            statusText.setText(msg);
                            percentText.setText((30 + (int)(pct * 0.4)) + "%");
                        });
                    }
                });

                // Step 3: Download Box64
                updateStatus("Baixando Box64...", 70);
                runtimeManager.downloadBox64(new RuntimeManager.ProgressCallback() {
                    @Override public void onProgress(int pct, String msg) {
                        int totalPct = 70 + (int)(pct * 0.15);
                        runOnUiThread(() -> {
                            progressBar.setProgress(totalPct);
                            statusText.setText(msg);
                            percentText.setText(totalPct + "%");
                        });
                    }
                });

                // Step 4: Setup Wine prefix
                updateStatus("Configurando Wine...", 85);
                runtimeManager.setupWinePrefix(gameDir);

                // Step 5: Verify
                updateStatus("Verificando...", 95);
                Thread.sleep(300);

                if (!runtimeManager.isRuntimeReady()) {
                    throw new Exception("Runtime nao configurado corretamente");
                }

                updateStatus("Pronto!", 100);
                Thread.sleep(500);
                runOnUiThread(this::onSetupComplete);

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("Erro: " + e.getMessage());
                    percentText.setText("Falha");
                    findViewById(R.id.retryBtn).setVisibility(View.VISIBLE);
                    findViewById(R.id.retryBtn).setOnClickListener(v -> {
                        findViewById(R.id.retryBtn).setVisibility(View.GONE);
                        progressBar.setVisibility(View.VISIBLE);
                        checkSetup();
                    });
                });
            }
        });
    }

    private void updateStatus(String msg, int pct) {
        runOnUiThread(() -> {
            statusText.setText(msg);
            percentText.setText(pct + "%");
            progressBar.setProgress(pct);
        });
    }

    private void onSetupComplete() {
        isReady = true;
        progressCard.animate().alpha(0f).setDuration(300).withEndAction(() -> {
            progressCard.setVisibility(View.GONE);
            launchBtn.setEnabled(true);
            settingsBtn.setEnabled(true);
            launchBtn.animate().alpha(1f).setDuration(300).start();
            settingsBtn.animate().alpha(1f).setDuration(300).start();
        }).start();
    }

    private void launchGame() {
        if (!isReady) return;

        File gameDir = getGameDir();
        String exeName = new File(gameDir, "Mu Bahia.exe").exists() ? "Mu Bahia.exe" : "Main.exe";
        File executable = new File(gameDir, exeName);

        if (!executable.exists()) {
            new AlertDialog.Builder(this)
                .setTitle("Erro")
                .setMessage("Executavel nao encontrado em:\n" + executable.getAbsolutePath())
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        String boxPreset = prefs.getString("box64_preset", "Compatibility");
        String resolution = prefs.getString("resolution", "800x600");
        boolean windowMode = prefs.getBoolean("window_mode", true);

        gameLauncher.launch(executable, boxPreset, resolution, windowMode);
    }

    private void showSettings() {
        String[] items = {
            "Box64 Preset: " + prefs.getString("box64_preset", "Compatibility"),
            "Resolucao: " + prefs.getString("resolution", "800x600"),
            "Modo janela: " + (prefs.getBoolean("window_mode", true) ? "SIM" : "NAO"),
            "Redefinir tudo",
            "Sobre"
        };
        new AlertDialog.Builder(this)
            .setTitle("Configuracoes")
            .setItems(items, (d, which) -> {
                switch (which) {
                    case 0 -> selectBox64Preset();
                    case 1 -> selectResolution();
                    case 2 -> toggleWindowMode();
                    case 3 -> resetEverything();
                    case 4 -> showAbout();
                }
            })
            .show();
    }

    private void selectBox64Preset() {
        String current = prefs.getString("box64_preset", "Compatibility");
        int idx = current.equals("Performance") ? 1 : current.equals("Intermediate") ? 2 : 0;
        new AlertDialog.Builder(this)
            .setTitle("Box64 Preset")
            .setSingleChoiceItems(new String[]{"Compatibility", "Performance", "Intermediate"}, idx, (d, w) -> {
                String[] vals = {"Compatibility", "Performance", "Intermediate"};
                prefs.edit().putString("box64_preset", vals[w]).apply();
                d.dismiss();
            })
            .show();
    }

    private void selectResolution() {
        String current = prefs.getString("resolution", "800x600");
        String[] resolutions = {"640x480", "800x600", "1024x768", "1280x720", "1920x1080"};
        int idx = java.util.Arrays.asList(resolutions).indexOf(current);
        if (idx < 0) idx = 1;
        new AlertDialog.Builder(this)
            .setTitle("Resolucao")
            .setSingleChoiceItems(resolutions, idx, (d, w) -> {
                prefs.edit().putString("resolution", resolutions[w]).apply();
                d.dismiss();
            })
            .show();
    }

    private void toggleWindowMode() {
        boolean current = prefs.getBoolean("window_mode", true);
        prefs.edit().putBoolean("window_mode", !current).apply();
    }

    private void resetEverything() {
        new AlertDialog.Builder(this)
            .setTitle("Redefinir")
            .setMessage("Isso vai apagar todos os arquivos e baixar novamente. Continuar?")
            .setPositiveButton("SIM", (d, w) -> {
                runtimeManager.reset();
                deleteDir(getGameDir());
                isReady = false;
                launchBtn.setEnabled(false);
                settingsBtn.setEnabled(false);
                progressBar.setProgress(0);
                progressCard.setVisibility(View.VISIBLE);
                progressCard.setAlpha(1f);
                checkSetup();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
            .setTitle("Mu Bahia Android")
            .setMessage("Versao: 2.0\n\n" +
                       "Baseado em Winlator (Wine + Box86/Box64)\n" +
                       "Mu Bahia - Servidor Private de MuOnline\n\n" +
                       "Criado por OpenCode")
            .setPositiveButton("OK", null)
            .show();
    }

    private File getGameDir() {
        return new File(getFilesDir(), "mubahia");
    }

    private void deleteDir(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) {
            if (f.isDirectory()) deleteDir(f);
            else f.delete();
        }
        dir.delete();
    }
}
