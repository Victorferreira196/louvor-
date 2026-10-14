package org.schabi.newpipe.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.BaseFragment;
import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.util.NavigationHelper;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Tela inicial simples focada em louvor, pensada para pessoas idosas:
 * letras grandes, botões altos e busca por voz em destaque. Toda busca é
 * feita no YouTube e recebe o termo "louvor" para manter os resultados em
 * música gospel.
 */
public class LouvorFragment extends BaseFragment {

    private static final String FILTRO = "louvor";
    private static final Locale PT_BR = new Locale("pt", "BR");

    private EditText campoBusca;

    private final ActivityResultLauncher<Intent> vozLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() != Activity.RESULT_OK
                        || resultado.getData() == null) {
                    return;
                }
                final ArrayList<String> falas = resultado.getData()
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (falas != null && !falas.isEmpty()) {
                    campoBusca.setText(falas.get(0));
                    buscar(falas.get(0));
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_louvor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View rootView,
                              @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);

        campoBusca = rootView.findViewById(R.id.louvor_busca);
        campoBusca.setOnEditorActionListener((v, actionId, event) -> {
            final boolean enter = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_SEARCH || enter) {
                buscar(campoBusca.getText().toString());
                return true;
            }
            return false;
        });
        rootView.findViewById(R.id.louvor_botao_buscar)
                .setOnClickListener(v -> buscar(campoBusca.getText().toString()));
        rootView.findViewById(R.id.louvor_botao_voz).setOnClickListener(v -> ouvir());

        final LinearLayout atalhos = rootView.findViewById(R.id.louvor_atalhos);
        for (final String atalho : getResources().getStringArray(R.array.louvor_atalhos)) {
            atalhos.addView(criarBotaoAtalho(atalho));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitle(getString(R.string.louvor_titulo));
    }

    /** Botão largo e alto, fácil de acertar com o dedo. */
    private TextView criarBotaoAtalho(@NonNull final String texto) {
        final Context ctx = requireContext();
        final TextView botao = new TextView(ctx);
        botao.setText(texto);
        botao.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        botao.setGravity(Gravity.CENTER_VERTICAL);
        botao.setMinHeight(dp(68));
        botao.setPadding(dp(20), dp(12), dp(20), dp(12));
        botao.setBackgroundResource(R.drawable.louvor_atalho_fundo);
        botao.setClickable(true);
        botao.setFocusable(true);
        botao.setOnClickListener(v -> buscar(texto));

        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        botao.setLayoutParams(lp);
        return botao;
    }

    private void ouvir() {
        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.louvor_voz_pergunta))
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try {
            vozLauncher.launch(intent);
        } catch (final ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.louvor_voz_indisponivel,
                    Toast.LENGTH_LONG).show();
            campoBusca.requestFocus();
        }
    }

    private void buscar(@Nullable final String texto) {
        if (texto == null || TextUtils.isEmpty(texto.trim())) {
            campoBusca.requestFocus();
            return;
        }
        esconderTeclado();
        NavigationHelper.openSearchFragment(getFM(),
                ServiceList.YouTube.getServiceId(), comFiltro(texto.trim()));
    }

    /** Acrescenta "louvor" quando a busca ainda não indica música gospel. */
    static String comFiltro(@NonNull final String texto) {
        final String minusculo = texto.toLowerCase(PT_BR);
        if (minusculo.contains(FILTRO) || minusculo.contains("gospel")
                || minusculo.contains("hino") || minusculo.contains("adoração")) {
            return texto;
        }
        return texto + " " + FILTRO;
    }

    private int dp(final int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    private void esconderTeclado() {
        final InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(campoBusca.getWindowToken(), 0);
        }
        campoBusca.clearFocus();
    }
}
