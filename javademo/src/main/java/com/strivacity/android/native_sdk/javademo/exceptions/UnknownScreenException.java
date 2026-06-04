package com.strivacity.android.native_sdk.javademo.exceptions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.strivacity.android.native_sdk.render.models.Screen;

public class UnknownScreenException extends RuntimeException {

    private final Screen screen;

    public UnknownScreenException(Screen screen) {
        super("Unknown screen type: \"" + screen.getScreen() + "\"");
        this.screen = screen;
    }

    @Nullable
    public String getFallbackUrl() {
        return screen.getHostedUrl();
    }

    @NonNull
    @Override
    public String toString() {
        return "UnknownScreenException{screen=" + screen + "}";
    }
}
