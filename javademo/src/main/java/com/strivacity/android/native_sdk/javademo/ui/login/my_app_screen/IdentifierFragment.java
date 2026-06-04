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
import com.strivacity.android.native_sdk.javademo.databinding.FragmentIdentifierBinding;
import com.strivacity.android.native_sdk.javademo.ui.login.LoginViewModel;
import com.strivacity.android.native_sdk.javademo.ui.login.ScreenFragment;
import com.strivacity.android.native_sdk.render.models.FormWidget;
import com.strivacity.android.native_sdk.render.models.PasskeyLoginWidget;
import com.strivacity.android.native_sdk.render.models.Screen;
import com.strivacity.android.native_sdk.render.models.SubmitWidget;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class IdentifierFragment extends ScreenFragment {

    private static final String FORM_ID = "identifier";

    private Screen screen;
    private FragmentIdentifierBinding binding;

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
        binding = FragmentIdentifierBinding.inflate(inflater, container, false);
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
                            binding.signUpButton.setEnabled(!isProcessing);
                            binding.identifierInput.setEnabled(!isProcessing);
                            binding.passkeyButton.setEnabled(!isProcessing);
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
                    final String identifier = String.valueOf(binding.identifierInput.getText());
                    viewModel.submit(
                            FORM_ID,
                            Map.of("identifier", identifier),
                            throwable -> {
                                Log.d("IdentifierFragment", "Submit completed", throwable);
                                if (throwable != null) viewModel.setSubmitError(throwable);
                            });
                });

        binding.signUpButton.setOnClickListener(
                v -> {
                    viewModel.submit(
                            "additionalActions/registration",
                            Map.of(),
                            throwable -> {
                                Log.d(
                                        "IdentifierFragment",
                                        "Sign up navigation completed",
                                        throwable);
                                if (throwable != null) viewModel.setSubmitError(throwable);
                            });
                });

        populateExternalLoginProviders(viewModel);
        populatePasskeyButton(viewModel);
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

        showOrDivider();
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
                                            "IdentifierFragment",
                                            "External login completed",
                                            throwable);
                                    if (throwable != null) viewModel.setSubmitError(throwable);
                                });
                    });
            binding.externalLoginContainer.addView(button);
        }
    }

    /**
     * Shows the passkey login button if a {@code passkey} form is present in the screen, using the
     * label from the {@link PasskeyLoginWidget}.
     */
    private void populatePasskeyButton(LoginViewModel viewModel) {
        Optional<FormWidget> passkeyForm =
                Optional.ofNullable(screen.getForms())
                        .flatMap(
                                forms ->
                                        forms.stream()
                                                .filter(f -> "passkey".equals(f.getId()))
                                                .findFirst());

        passkeyForm.ifPresent(
                form -> {
                    Optional<PasskeyLoginWidget> widget =
                            form.getWidgets().stream()
                                    .filter(w -> w instanceof PasskeyLoginWidget)
                                    .map(w -> (PasskeyLoginWidget) w)
                                    .findFirst();

                    widget.ifPresent(
                            w -> {
                                showOrDivider();
                                binding.passkeyButton.setText(w.getLabel());
                                binding.passkeyButton.setVisibility(View.VISIBLE);
                                binding.passkeyButton.setOnClickListener(
                                        v -> {
                                            viewModel.submit(
                                                    form.getId(),
                                                    Map.of(),
                                                    throwable -> {
                                                        Log.d(
                                                                "IdentifierFragment",
                                                                "Passkey login completed",
                                                                throwable);
                                                        if (throwable != null)
                                                            viewModel.setSubmitError(throwable);
                                                    });
                                        });
                            });
                });
    }

    private void showOrDivider() {
        if (binding != null) {
            binding.orDivider.setVisibility(View.VISIBLE);
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
                            if (binding != null) {
                                binding.identifierInputLayout.setError(
                                        messages.errorMessageForWidget(FORM_ID, "identifier"));
                            }
                        });
    }

    @Override
    public void refresh(Screen screen) {
        super.refresh(screen);
        setMessages();
    }
}
