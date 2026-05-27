package com.strivacity.android.native_sdk.javademo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import com.strivacity.android.native_sdk.javademo.ui.AppViewModel;
import com.strivacity.android.native_sdk.javademo.ui.home.HomeFragment;

public class JavaDemoActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Pre-create AppViewModel so all Fragments can retrieve it from Activity scope
        // without needing a direct reference to NativeSDKJava.
        JavaDemoApplication app = (JavaDemoApplication) getApplication();
        new ViewModelProvider(this, new AppViewModel.AppViewModelFactory(app.getNativeSDK()))
                .get(AppViewModel.class);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // Initialize session in the background.
        // This is a non-blocking call and will update the current logged-in state.
        app.getNativeSDK().initializeSession();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        JavaDemoApplication app = (JavaDemoApplication) getApplication();
        if (app.getNativeSDK().isRedirectExpected()) {
            Uri data = intent.getData();
            // Pass null when the user backs out of the hosted flow to signal cancellation.
            app.getNativeSDK().continueFlow(data != null ? data.toString() : null);
        }
    }
}
