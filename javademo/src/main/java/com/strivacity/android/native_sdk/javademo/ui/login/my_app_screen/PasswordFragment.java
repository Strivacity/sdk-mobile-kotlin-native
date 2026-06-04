package com.strivacity.android.native_sdk.javademo.ui.login.my_app_screen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.strivacity.android.native_sdk.javademo.databinding.FragmentPasswordBinding;
import com.strivacity.android.native_sdk.javademo.ui.login.LoginViewModel;
import com.strivacity.android.native_sdk.javademo.ui.login.ScreenFragment;
import com.strivacity.android.native_sdk.render.models.Screen;
import java.util.Map;
import java.util.Optional;

public class PasswordFragment extends ScreenFragment {

    private static final String FORM_ID = "password";
    private FragmentPasswordBinding binding;
    private Screen screen;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LoginViewModel viewModel =
                new ViewModelProvider(requireParentFragment()).get(LoginViewModel.class);
        viewModel
                .getIsProcessing()
                .observe(
                        getViewLifecycleOwner(),
                        isProcessing -> {
                            binding.continueButton.setEnabled(!isProcessing);
                            binding.passwordInput.setEnabled(!isProcessing);
                        });
        binding.continueButton.setOnClickListener(
                v -> {
                    final String password = String.valueOf(binding.passwordInput.getText());
                    viewModel.submit(
                            "password",
                            Map.of("password", password),
                            throwable -> {
                                Log.d("PasswordFragment", "Submit completed", throwable);
                                if (throwable != null) viewModel.setSubmitError(throwable);
                            });
                });

        setMessages();
    }

    /**
     * Sets or clears the error label on the password input field.
     *
     * @param error the error message to display, or {@code null} to clear the error
     */
    public void setPasswordError(@Nullable String error) {
        if (binding != null) {
            binding.passwordLayout.setError(error);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void refresh(Screen screen) {
        super.refresh(screen);
        setMessages();
    }

    @Override
    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    private void setMessages() {
        Optional.ofNullable(screen.getMessages())
                .ifPresent(
                        messages -> {
                            setPasswordError(messages.errorMessageForWidget(FORM_ID, "password"));
                        });
    }
}
