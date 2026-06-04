package com.strivacity.android.native_sdk.javademo.ui.login.my_app_screen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.strivacity.android.native_sdk.javademo.databinding.FragmentRegistrationBinding;
import com.strivacity.android.native_sdk.javademo.ui.login.LoginViewModel;
import com.strivacity.android.native_sdk.javademo.ui.login.ScreenFragment;
import com.strivacity.android.native_sdk.render.models.FormWidget;
import com.strivacity.android.native_sdk.render.models.InputWidget;
import com.strivacity.android.native_sdk.render.models.Screen;
import com.strivacity.android.native_sdk.render.models.SubmitWidget;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RegistrationFragment extends ScreenFragment {

    private static final String FORM_ID = "registration";

    private Screen screen;
    private FragmentRegistrationBinding binding;

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
        binding = FragmentRegistrationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Pre-populate email if the backend provides an initial value
        String initialEmail =
                Optional.ofNullable(screen.getForms())
                        .flatMap(
                                forms ->
                                        forms.stream()
                                                .filter(f -> FORM_ID.equals(f.getId()))
                                                .findFirst())
                        .flatMap(
                                f ->
                                        f.getWidgets().stream()
                                                .filter(w -> "email".equals(w.getId()))
                                                .findFirst())
                        .filter(w -> w instanceof InputWidget)
                        .map(w -> ((InputWidget) w).getValue())
                        .orElse("");
        binding.emailInput.setText(initialEmail);

        LoginViewModel viewModel =
                new ViewModelProvider(requireParentFragment()).get(LoginViewModel.class);

        viewModel
                .getIsProcessing()
                .observe(
                        getViewLifecycleOwner(),
                        isProcessing -> {
                            binding.continueButton.setEnabled(!isProcessing);
                            binding.backToLoginButton.setEnabled(!isProcessing);
                            binding.emailInput.setEnabled(!isProcessing);
                            binding.passwordInput.setEnabled(!isProcessing);
                            binding.passwordConfirmationInput.setEnabled(!isProcessing);
                            binding.keepMeLoggedInCheckbox.setEnabled(!isProcessing);
                            for (int i = 0;
                                    i < binding.externalLoginContainer.getChildCount();
                                    i++) {
                                binding.externalLoginContainer
                                        .getChildAt(i)
                                        .setEnabled(!isProcessing);
                            }
                        });

        binding.continueButton.setOnClickListener(
                v -> {
                    final String email = String.valueOf(binding.emailInput.getText());
                    final String password = String.valueOf(binding.passwordInput.getText());
                    final String passwordConfirmation =
                            String.valueOf(binding.passwordConfirmationInput.getText());
                    final boolean keepMeLoggedIn = binding.keepMeLoggedInCheckbox.isChecked();
                    viewModel.submit(
                            FORM_ID,
                            Map.of(
                                    "email", email,
                                    "password", password,
                                    "passwordConfirmation", passwordConfirmation,
                                    "keepMeLoggedIn", keepMeLoggedIn),
                            throwable -> {
                                Log.d("RegistrationFragment", "Submit completed", throwable);
                                if (throwable != null) viewModel.setSubmitError(throwable);
                            });
                });

        binding.backToLoginButton.setOnClickListener(
                v -> {
                    viewModel.submit(
                            "reset",
                            Map.of(),
                            throwable -> {
                                Log.d("RegistrationFragment", "Back to login completed", throwable);
                                if (throwable != null) viewModel.setSubmitError(throwable);
                            });
                });

        populateExternalLoginProviders(viewModel);
        setMessages();
    }

    /**
     * Dynamically adds a button for each external login provider form found in the screen. External
     * provider form IDs follow the pattern {@code externalLoginProvider*}.
     */
    private void populateExternalLoginProviders(LoginViewModel viewModel) {
        List<FormWidget> forms = screen.getForms();
        if (forms == null) return;

        List<FormWidget> externalProviders =
                forms.stream()
                        .filter(f -> f.getId().startsWith("externalLoginProvider"))
                        .collect(Collectors.toList());

        if (externalProviders.isEmpty()) return;

        binding.externalLoginContainer.setVisibility(View.VISIBLE);
        binding.externalLoginContainer.removeAllViews();

        for (FormWidget provider : externalProviders) {
            String label =
                    provider.getWidgets().stream()
                            .filter(w -> w instanceof SubmitWidget)
                            .map(w -> ((SubmitWidget) w).getLabel())
                            .findFirst()
                            .orElse(provider.getId());

            Button button = new Button(requireContext());
            button.setText(label);
            button.setOnClickListener(
                    v -> {
                        viewModel.submit(
                                provider.getId(),
                                Map.of(),
                                throwable -> {
                                    Log.d(
                                            "RegistrationFragment",
                                            "External login completed",
                                            throwable);
                                    if (throwable != null) viewModel.setSubmitError(throwable);
                                });
                    });
            binding.externalLoginContainer.addView(button);
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
                        messages -> {
                            binding.emailLayout.setError(
                                    messages.errorMessageForWidget(FORM_ID, "email"));
                            binding.passwordLayout.setError(
                                    messages.errorMessageForWidget(FORM_ID, "password"));
                            binding.passwordConfirmationLayout.setError(
                                    messages.errorMessageForWidget(
                                            FORM_ID, "passwordConfirmation"));
                        });
    }

    @Override
    public void refresh(Screen screen) {
        super.refresh(screen);
        setMessages();
    }
}
