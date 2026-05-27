package com.strivacity.android.native_sdk.javademo.ui.login.my_app_screen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.strivacity.android.native_sdk.javademo.databinding.FragmentPasskeyEnrollBinding;
import com.strivacity.android.native_sdk.javademo.ui.login.LoginViewModel;
import com.strivacity.android.native_sdk.javademo.ui.login.ScreenFragment;
import com.strivacity.android.native_sdk.render.models.Screen;
import com.strivacity.android.native_sdk.render.models.Widget;
import java.util.Map;
import java.util.Optional;

public class PasskeyEnrollFragment extends ScreenFragment {

    private static final String FORM_ID = "passkeyEnroll";
    private static final String FIELD_TARGET = "target";

    private Screen screen;
    private FragmentPasskeyEnrollBinding binding;

    @Override
    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPasskeyEnrollBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Pre-populate device name from the value provided by the backend
        String initialTarget =
                Optional.ofNullable(screen.getForms())
                        .flatMap(
                                forms ->
                                        forms.stream()
                                                .filter(f -> FORM_ID.equals(f.getId()))
                                                .findFirst())
                        .flatMap(
                                f ->
                                        f.getWidgets().stream()
                                                .filter(w -> FIELD_TARGET.equals(w.getId()))
                                                .findFirst())
                        .map(Widget::value)
                        .map(Object::toString)
                        .orElse("");
        binding.deviceNameInput.setText(initialTarget);

        LoginViewModel viewModel =
                new ViewModelProvider(requireParentFragment()).get(LoginViewModel.class);

        viewModel
                .getIsProcessing()
                .observe(
                        getViewLifecycleOwner(),
                        isProcessing -> {
                            binding.continueButton.setEnabled(!isProcessing);
                            binding.skipButton.setEnabled(!isProcessing);
                            binding.deviceNameInput.setEnabled(!isProcessing);
                        });

        binding.continueButton.setOnClickListener(
                v -> {
                    final String deviceName = String.valueOf(binding.deviceNameInput.getText());
                    viewModel.submit(
                            FORM_ID,
                            Map.of(FIELD_TARGET, deviceName),
                            throwable -> {
                                Log.d("PasskeyEnrollFragment", "Submit completed", throwable);
                                if (throwable != null) viewModel.setSubmitError(throwable);
                            });
                });

        binding.skipButton.setOnClickListener(
                v -> {
                    viewModel.submit(
                            "additionalActions/skip",
                            Map.of(),
                            throwable -> {
                                Log.d("PasskeyEnrollFragment", "Skip completed", throwable);
                                if (throwable != null) viewModel.setSubmitError(throwable);
                            });
                });

        setMessages();
    }

    /**
     * Sets or clears the error label on the device name input field.
     *
     * @param error the error message to display, or {@code null} to clear the error
     */
    public void setDeviceNameError(@Nullable String error) {
        if (binding != null) {
            binding.deviceNameLayout.setError(error);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setMessages() {
        Optional.ofNullable(screen.getMessages())
                .ifPresent(
                        messages ->
                                setDeviceNameError(
                                        messages.errorMessageForWidget(FORM_ID, FIELD_TARGET)));
    }

    @Override
    public void refresh(Screen screen) {
        super.refresh(screen);
        setMessages();
    }
}
