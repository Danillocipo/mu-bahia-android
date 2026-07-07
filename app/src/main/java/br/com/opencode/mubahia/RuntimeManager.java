package br.com.opencode.mubahia;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RuntimeManager {
    private final Context context;

    public RuntimeManager(Context context) {
        this.context = context;
    }

    public File getGameDir() {
        return new File(Environment.getExternalStorageDirectory(), "MuBahia");
    }

    public File getApkFile() {
        return new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Winlator.apk");
    }

    public boolean isGameReady() {
        File dir = getGameDir();
        if (!dir.exists()) return false;
        File[] files = dir.listFiles();
        return files != null && files.length > 0;
    }

    public void downloadGameFiles(ProgressCallback cb) throws Exception {
        File destDir = getGameDir();
        if (isGameReady()) return;

        destDir.mkdirs();
        String gameUrl = context.getSharedPreferences("mubahia", Context.MODE_PRIVATE)
            .getString("game_url", "https://github.com/Danillocipo/mu-bahia-android/releases/download/v1.0-assets/mu-bahia-android.zip");
        File zipFile = new File(destDir.getParentFile(), "mubahia.zip");

        downloadFile(gameUrl, zipFile, cb);
        cb.onProgress(70, "Extraindo...");
        unzip(zipFile, destDir);
        zipFile.delete();
        flatten(destDir);
        cb.onProgress(100, "Download concluido!");
    }

    public boolean isWinlatorInstalled() {
        try {
            context.getPackageManager().getPackageInfo("com.winlator", 0);
            return true;
        } catch (Exception e) {
            try {
                context.getPackageManager().getPackageInfo("com.winlator.bionic", 0);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    public void downloadWinlator(ProgressCallback cb) throws Exception {
        File apkFile = getApkFile();
        if (apkFile.exists()) return;

        apkFile.getParentFile().mkdirs();
        String apkUrl = "https://github.com/brunodev85/winlator/releases/latest/download/Winlator_11.1.apk";
        downloadFile(apkUrl, apkFile, cb);
        cb.onProgress(100, "Winlator baixado!");
    }

    public void installWinlator() {
        File apkFile = getApkFile();
        if (!apkFile.exists()) return;

        Uri apkUri = FileProvider.getUriForFile(context,
            context.getPackageName() + ".fileprovider", apkFile);

        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(apkUri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return;
        }

        Intent fallback = new Intent(Intent.ACTION_VIEW);
        fallback.setDataAndType(apkUri, "application/vnd.android.package-archive");
        fallback.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (fallback.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(fallback);
        }
    }

    public void openWinlator() {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.winlator");
        if (intent == null) {
            intent = context.getPackageManager().getLaunchIntentForPackage("com.winlator.bionic");
        }
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
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
                    cb.onProgress(pct, "Baixando... " + pct + "%");
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

    private void flatten(File dir) {
        File[] children = dir.listFiles();
        if (children == null || children.length != 1 || !children[0].isDirectory()) return;
        File sub = children[0];
        File[] subFiles = sub.listFiles();
        if (subFiles != null) {
            for (File f : subFiles) {
                File dest = new File(dir, f.getName());
                f.renameTo(dest);
            }
        }
        sub.delete();
    }

    public void deleteGameFiles() {
        deleteDir(getGameDir());
    }

    private void deleteDir(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) {
            if (f.isDirectory()) deleteDir(f);
            f.delete();
        }
        dir.delete();
    }

    public interface ProgressCallback {
        void onProgress(int pct, String msg);
    }
}
