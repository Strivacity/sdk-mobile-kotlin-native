package com.strivacity.android.native_sdk.javademo.ui.login;

import androidx.fragment.app.Fragment;
import com.strivacity.android.native_sdk.render.models.Screen;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

public abstract class ScreenFragment extends Fragment {
    protected abstract void setScreen(Screen screen);

    @MustBeInvokedByOverriders
    public void refresh(Screen screen) {
        setScreen(screen);
    }
}
