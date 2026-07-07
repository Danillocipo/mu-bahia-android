package br.com.opencode.mubahia;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        setContentView(R.layout.activity_splash);

        TextView title = findViewById(R.id.splashTitle);
        TextView subtitle = findViewById(R.id.splashSubtitle);
        View progress = findViewById(R.id.splashProgress);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(title, "alpha", 0f, 1f).setDuration(800),
            ObjectAnimator.ofFloat(title, "translationY", 60f, 0f).setDuration(800),
            ObjectAnimator.ofFloat(subtitle, "alpha", 0f, 1f).setDuration(800),
            ObjectAnimator.ofFloat(subtitle, "translationY", 40f, 0f).setDuration(800)
        );
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator a) {}
            @Override public void onAnimationCancel(Animator a) {}
            @Override public void onAnimationRepeat(Animator a) {}
            @Override
            public void onAnimationEnd(Animator a) {
                progress.setVisibility(View.VISIBLE);
                ObjectAnimator.ofFloat(progress, "alpha", 0f, 1f).setDuration(300).start();
                new Handler().postDelayed(() -> proceedAfterSplash(), 400);
            }
        });
        set.start();
    }

    private void proceedAfterSplash() {
        if (!hasStoragePermission()) {
            requestStoragePermission();
            return;
        }
        navigateToMain();
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 100);
        } else {
            requestPermissions(
                new String[]{
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                }, 100);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (hasStoragePermission()) {
                navigateToMain();
            } else {
                findViewById(R.id.splashProgress).setVisibility(View.GONE);
                TextView subtitle = findViewById(R.id.splashSubtitle);
                subtitle.setText("Permissao necessaria para acessar os arquivos do jogo");
                subtitle.setTextColor(0xFFE94560);
                subtitle.setOnClickListener(v -> requestStoragePermission());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 100) {
            boolean granted = true;
            for (int r : results) {
                if (r != PackageManager.PERMISSION_GRANTED) { granted = false; break; }
            }
            if (granted) {
                navigateToMain();
            } else {
                findViewById(R.id.splashProgress).setVisibility(View.GONE);
                TextView subtitle = findViewById(R.id.splashSubtitle);
                subtitle.setText("Permissao necessaria - toque para tentar novamente");
                subtitle.setTextColor(0xFFE94560);
                subtitle.setOnClickListener(v -> requestStoragePermission());
            }
        }
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
