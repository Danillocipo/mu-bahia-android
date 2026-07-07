package br.com.opencode.mubahia;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class GameLauncher {
    private static final String TAG = "GameLauncher";

    private final Context context;
    private final RuntimeManager runtime;
    private Process gameProcess;

    public GameLauncher(Context context, RuntimeManager runtime) {
        this.context = context;
        this.runtime = runtime;
    }

    public void launch(File executable, String box64Preset, String resolution, boolean windowMode) {
        if (gameProcess != null && gameProcess.isAlive()) {
            Log.w(TAG, "Jogo ja esta rodando");
            return;
        }

        try {
            File runtimeDir = new File(context.getFilesDir(), "runtime");
            String winePath = new File(runtimeDir, "wine/bin/wine").getAbsolutePath();
            File wineBin = new File(winePath);
            if (!wineBin.exists()) {
                winePath = new File(runtimeDir, "wine/wine").getAbsolutePath();
            }

            String box64Path = new File(runtimeDir, "box64/box64").getAbsolutePath();
            String prefixPath = new File(context.getFilesDir(), "wineprefix").getAbsolutePath();
            String exePath = executable.getAbsolutePath();
            String gameDirPath = executable.getParentFile().getAbsolutePath();

            ProcessBuilder pb = new ProcessBuilder(
                box64Path,
                winePath,
                exePath
            );

            if (windowMode) {
                pb.command().add("-w");
            }

            if (resolution != null && resolution.contains("x")) {
                String[] parts = resolution.split("x");
                pb.command().add("--resolution");
                pb.command().add(parts[0]);
                pb.command().add(parts[1]);
            }

            pb.directory(new File(gameDirPath));

            // Wine environment
            pb.environment().put("WINEPREFIX", prefixPath);
            pb.environment().put("WINEARCH", "win32");
            pb.environment().put("DISPLAY", ":0");
            pb.environment().put("BOX64_LOG", "0");
            pb.environment().put("SDL_AUDIODRIVER", "opensl");
            pb.environment().put("PULSE_SERVER", "tcp:127.0.0.1:4713");
            pb.environment().put("WINEDLLOVERRIDES", "d3d8,d3d9,d3d10,d3d11,dxgi=n,b");

            switch (box64Preset) {
                case "Performance":
                    pb.environment().put("BOX64_DYNAREC_FASTROUND", "1");
                    break;
                case "Intermediate":
                    pb.environment().put("BOX64_DYNAREC_BIGBLOCK", "1");
                    pb.environment().put("BOX64_DYNAREC_FASTROUND", "1");
                    break;
            }

            pb.redirectErrorStream(true);
            gameProcess = pb.start();

            // Log output
            new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(gameProcess.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        Log.d(TAG, "[Box64/Wine] " + line);
                    }
                } catch (IOException ignored) {}
                Log.i(TAG, "Processo encerrado");
            }).start();

            Log.i(TAG, "Jogo iniciado: " + exePath);

        } catch (IOException e) {
            Log.e(TAG, "Falha ao lancar jogo", e);
        }
    }

    public boolean isRunning() {
        return gameProcess != null && gameProcess.isAlive();
    }

    public void kill() {
        if (gameProcess != null && gameProcess.isAlive()) {
            gameProcess.destroyForcibly();
            Log.i(TAG, "Processo encerrado forcadamente");
        }
    }
}
