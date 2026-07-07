package br.com.opencode.mubahia;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RuntimeManager {
    private static final String TAG = "RuntimeManager";
    private static final String WINE_VERSION = "wine-9.0";

    private final Context context;
    private final File runtimeDir;
    private final File wineDir;
    private final File box64Dir;
    private final File prefixDir;

    public RuntimeManager(Context context) {
        this.context = context;
        this.runtimeDir = new File(context.getFilesDir(), "runtime");
        this.wineDir = new File(runtimeDir, "wine");
        this.box64Dir = new File(runtimeDir, "box64");
        this.prefixDir = new File(context.getFilesDir(), "wineprefix");
    }

    public boolean isRuntimeReady() {
        File wineBin = new File(wineDir, "bin/wine");
        File box64Bin = new File(box64Dir, "box64");
        File gameDir = new File(context.getFilesDir(), "mubahia");
        boolean gameReady = gameDir.exists() && gameDir.listFiles() != null && gameDir.listFiles().length > 0;
        return (wineBin.exists() || new File(wineDir, "wine").exists())
            && (box64Bin.exists())
            && prefixDir.exists()
            && gameReady;
    }

    public void downloadRuntime(ProgressCallback cb) throws Exception {
        if (isRuntimeReady()) return;

        runtimeDir.mkdirs();

        // Download Wine ARM64 build from GitHub release
        String wineUrl = "https://github.com/Kron4ek/Wine-Builds/releases/download/11.0/wine-11.0-staging-tkg-amd64-wow64.tar.xz";
        File wineArchive = new File(runtimeDir, "wine.tar.xz");

        if (!wineArchive.exists()) {
            downloadFile(wineUrl, wineArchive, cb);
        }

        // Extract
        cb.onProgress(50, "Extraindo Wine...");
        extractTarXz(wineArchive, wineDir);
        wineArchive.delete();
        flatten(wineDir);

        // Make binaries executable
        setExecutable(wineDir);

        cb.onProgress(100, "");
    }

    public void downloadBox64(ProgressCallback cb) throws Exception {
        File box64Bin = new File(box64Dir, "box64");
        if (box64Bin.exists()) return;

        box64Dir.mkdirs();

        String box64Url = "https://github.com/ptitSeb/box64/releases/latest/download/box64-android";
        downloadFile(box64Url, box64Bin, cb);
        box64Bin.setExecutable(true, false);

        cb.onProgress(100, "");
    }

    public void downloadGameFiles(File destDir, ProgressCallback cb) throws Exception {
        if (destDir.exists() && destDir.listFiles() != null && destDir.listFiles().length > 0) return;

        destDir.mkdirs();
        String gameUrl = context.getSharedPreferences("mubahia", Context.MODE_PRIVATE)
            .getString("game_url", "https://github.com/Danillocipo/mu-bahia-android/releases/download/v1.0-assets/mu-bahia-android.zip");
        File zipFile = new File(destDir.getParentFile(), "mubahia.zip");

        downloadFile(gameUrl, zipFile, cb);
        cb.onProgress(70, "Extraindo arquivos do jogo...");
        unzip(zipFile, destDir);
        zipFile.delete();
        flatten(destDir);
        cb.onProgress(100, "");
    }

    public void setupWinePrefix(File gameDir) throws IOException {
        if (prefixDir.exists()) return;

        prefixDir.mkdirs();
        new File(prefixDir, "drive_c").mkdirs();
        new File(prefixDir, "drive_c/Program Files").mkdirs();
        new File(prefixDir, "drive_c/Windows").mkdirs();
        new File(prefixDir, "drive_c/Windows/System32").mkdirs();
        new File(prefixDir, "drive_c/Windows/SysWOW64").mkdirs();

        // Link game to drive_c
        File gameLink = new File(prefixDir, "drive_c/MuBahia");
        copyDirectory(gameDir, gameLink);

        // Create registry
        writeRegistry();
        writeLaunchScript(gameDir);
    }

    private void writeRegistry() throws IOException {
        String prefixPath = prefixDir.getAbsolutePath().replace("\\", "/");

        String userReg = "WINE REGISTRY Version 2\n" +
            "\n" +
            "[Software\\\\Wine\\\\Direct3D]\n" +
            "\"DirectDrawRenderer\"=\"opengl\"\n" +
            "\"OffscreenRenderingMode\"=\"fbo\"\n" +
            "\"AlwaysOffscreen\"=\"1\"\n" +
            "\"MultiSampling\"=\"0\"\n" +
            "\"StrictDrawOrdering\"=\"0\"\n" +
            "\"UseGLSL\"=\"enabled\"\n" +
            "\n" +
            "[Software\\\\Wine\\\\X11 Driver]\n" +
            "\"Desktop\"=\"800x600\"\n" +
            "\"Managed\"=\"Y\"\n" +
            "\n" +
            "[Software\\\\Wine\\\\DirectInput]\n" +
            "\"MouseWarpOverride\"=\"enable\"\n";

        writeFile(new File(prefixDir, "user.reg"), userReg);

        String sysReg = "WINE REGISTRY Version 2\n" +
            "\n" +
            "[Software\\\\Microsoft\\\\Windows\\\\CurrentVersion]\n" +
            "\"ProductName\"=\"Microsoft Windows 7\"\n" +
            "\"CSDVersion\"=\"\"\n" +
            "\"CurrentVersion\"=\"6.1\"\n";

        writeFile(new File(prefixDir, "system.reg"), sysReg);
    }

    private void writeLaunchScript(File gameDir) throws IOException {
        String winePath = getWineBin().getAbsolutePath().replace("\\", "/");
        String box64Path = getBox64Bin().getAbsolutePath().replace("\\", "/");
        String prefixPath = prefixDir.getAbsolutePath().replace("\\", "/");
        String gamePath = gameDir.getAbsolutePath().replace("\\", "/");

        String exeName = new File(gameDir, "Mu Bahia.exe").exists() ? "Mu Bahia.exe" : "Main.exe";

        String script = "#!/system/bin/sh\n" +
            "export WINEPREFIX=\"" + prefixPath + "\"\n" +
            "export WINEARCH=\"win32\"\n" +
            "export BOX64_LOG=0\n" +
            "export DISPLAY=:0\n" +
            "export SDL_AUDIODRIVER=opensl\n" +
            "cd \"" + gamePath + "\"\n" +
            "exec \"" + box64Path + "\" \"" + winePath + "\" \"" + exeName + "\" \"$@\"\n";

        File scriptFile = new File(runtimeDir, "launch.sh");
        writeFile(scriptFile, script);
        scriptFile.setExecutable(true, false);
    }

    public void launchGame(File executable, String box64Preset, String resolution, boolean windowMode) {
        try {
            String winePath = getWineBin().getAbsolutePath().replace("\\", "/");
            String box64Path = getBox64Bin().getAbsolutePath().replace("\\", "/");
            String prefixPath = prefixDir.getAbsolutePath().replace("\\", "/");
            String exePath = executable.getAbsolutePath().replace("\\", "/");
            String gameDir = executable.getParentFile().getAbsolutePath().replace("\\", "/");

            ProcessBuilder pb = new ProcessBuilder(
                box64Path, winePath, exePath
            );

            if (windowMode) {
                pb.command().add("-w");
            }

            pb.directory(new File(gameDir));
            pb.environment().put("WINEPREFIX", prefixPath);
            pb.environment().put("WINEARCH", "win32");
            pb.environment().put("DISPLAY", ":0");
            pb.environment().put("BOX64_LOG", "0");
            pb.environment().put("SDL_AUDIODRIVER", "opensl");

            // Box64 preset
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
            Process process = pb.start();

            Log.i(TAG, "Jogo iniciado");
        } catch (IOException e) {
            Log.e(TAG, "Falha ao iniciar", e);
        }
    }

    private File getWineBin() {
        File f = new File(wineDir, "bin/wine");
        if (!f.exists()) f = new File(wineDir, "wine");
        return f;
    }

    private File getBox64Bin() {
        return new File(box64Dir, "box64");
    }

    public void reset() {
        deleteDir(runtimeDir);
        deleteDir(prefixDir);
    }

    private void downloadFile(String urlStr, File output, ProgressCallback cb) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.connect();

        int totalSize = conn.getContentLength();
        int downloaded = 0;
        int lastPct = 0;

        try (InputStream in = conn.getInputStream();
             OutputStream out = new FileOutputStream(output)) {
            byte[] buf = new byte[32768];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
                downloaded += len;
                int pct = totalSize > 0 ? (int)((long)downloaded * 100 / totalSize) : 0;
                if (pct != lastPct) {
                    lastPct = pct;
                    String name = output.getName();
                    cb.onProgress(pct, "Baixando " + name + "... " + pct + "%");
                }
            }
        }
        conn.disconnect();
    }

    private void unzip(File zipFile, File dest) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new java.io.FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(dest, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int len;
                        while ((len = zis.read(buf)) != -1) fos.write(buf, 0, len);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void extractTarXz(File archive, File dest) throws Exception {
        dest.mkdirs();
        try (InputStream fis = new FileInputStream(archive);
             InputStream bis = new BufferedInputStream(fis);
             XZCompressorInputStream xzIn = new XZCompressorInputStream(bis);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(xzIn)) {
            TarArchiveEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = tarIn.getNextTarEntry()) != null) {
                File outFile = new File(dest, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int len;
                        while ((len = tarIn.read(buf)) != -1) fos.write(buf, 0, len);
                    }
                }
            }
        }
    }

    private void setExecutable(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) setExecutable(f);
            else if (!f.getName().endsWith(".so") && !f.getName().endsWith(".dll")) {
                f.setExecutable(true, false);
            }
        }
    }

    private void copyDirectory(File src, File dst) throws IOException {
        if (!dst.exists() && !dst.mkdirs()) return;
        File[] files = src.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) copyDirectory(f, new File(dst, f.getName()));
            else copyFile(f, new File(dst, f.getName()));
        }
    }

    private void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new java.io.FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
        }
    }

    private void writeFile(File f, String content) throws IOException {
        try (OutputStream os = new FileOutputStream(f)) {
            os.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void flatten(File dir) {
        File[] children = dir.listFiles();
        if (children == null || children.length != 1 || !children[0].isDirectory()) return;
        File sub = children[0];
        File[] subFiles = sub.listFiles();
        if (subFiles != null) {
            for (File f : subFiles) {
                File dest = new File(dir, f.getName());
                if (!f.renameTo(dest)) {
                    try {
                        if (f.isDirectory()) {
                            copyDirectory(f, dest);
                            deleteDir(f);
                        } else {
                            copyFile(f, dest);
                            f.delete();
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        sub.delete();
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

    public interface ProgressCallback {
        void onProgress(int pct, String msg);
    }
}
