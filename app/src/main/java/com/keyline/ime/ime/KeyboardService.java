/*
 * Copyright 2026 deepcodecreate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.keyline.ime.ime;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Toast;

import com.keyline.ime.R;
import com.keyline.ime.clipboard.ClipboardPanel;
import com.keyline.ime.clipboard.ClipboardRepository;
import com.keyline.ime.emoji.EmojiPanel;
import com.keyline.ime.layout.LayoutModel;
import com.keyline.ime.layout.LayoutRepository;
import com.keyline.ime.security.CredentialRecord;
import com.keyline.ime.security.CredentialVault;
import com.keyline.ime.settings.Prefs;
import com.keyline.ime.settings.PrefKeys;
import com.keyline.ime.settings.CredentialVaultActivity;
import com.keyline.ime.settings.SettingsActivity;
import com.keyline.ime.settings.ThemeSettingsActivity;
import com.keyline.ime.suggest.SuggestionEngine;
import com.keyline.ime.theme.BackgroundImageManager;
import com.keyline.ime.theme.KeyboardTheme;
import com.keyline.ime.theme.ThemeManager;
import com.keyline.ime.util.Bg;
import com.keyline.ime.util.Haptics;
import com.keyline.ime.util.PersianLocale;
import com.keyline.ime.view.ImeRoot;
import com.keyline.ime.view.CredentialSuggestionBar;
import com.keyline.ime.view.KeyboardView;
import com.keyline.ime.view.MenuPanel;
import com.keyline.ime.view.TextEditPanel;
import com.keyline.ime.view.SuggestionBar;
import com.keyline.ime.view.ToolbarView;
import com.keyline.ime.view.TopBarFade;
import com.keyline.ime.voice.PermissionActivity;
import com.keyline.ime.voice.VoiceController;

import java.util.List;

public class KeyboardService extends android.inputmethodservice.InputMethodService
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        InputController.Host,
        KeyboardView.Listener,
        ToolbarView.Listener,
        SuggestionBar.Listener,
        MenuPanel.Listener,
        TextEditPanel.Listener,
        ImeRoot.Callbacks,
        CredentialSuggestionBar.Listener,
        EmojiPanel.Listener,
        ClipboardPanel.Listener,
        com.keyline.ime.translate.TranslatePanel.Listener,
        VoiceController.Host {

    private static final int MODE_KEYS = 0;
    private static final int MODE_EMOJI = 1;
    private static final int MODE_CLIP = 2;
    private static final int MODE_TRANSLATE = 3;
    private static final int MODE_MENU = 4;
    private static final int MODE_EDIT = 5;

    private ImeRoot root;
    private ToolbarView toolbar;
    private SuggestionBar suggestions;
    private CredentialSuggestionBar credentialSuggestions;
    private android.widget.TextView banner;
    private KeyboardView keyboard;
    private EmojiPanel emojiPanel;
    private ClipboardPanel clipPanel;
    private com.keyline.ime.translate.TranslatePanel translatePanel;
    private MenuPanel menuPanel;
    private TextEditPanel editPanel;
    private InputController input;
    private VoiceController voice;
    private ClipboardManager clipboard;
    private KeyboardTheme theme;
    private int mode = MODE_KEYS;
    private LayoutModel lang;
    private LayoutModel padNumber;
    private LayoutModel padPhone;
    private LayoutModel padDate;
    private boolean destroyed;
    private EditorInfo currentEditorInfo;
    private long credentialRequestId;

    private final ClipboardManager.OnPrimaryClipChangedListener clipListener = this::onClipChanged;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(PersianLocale.wrapKeepingSystemDirection(newBase));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        input = new InputController(this);
        input.bindContext(this);
        voice = new VoiceController(this, this);
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.addPrimaryClipChangedListener(clipListener);
        Prefs.get(this).listen(this);
        SuggestionEngine.get().warm(this, Prefs.get(this).activeLang());
    }

    @Override
    public View onCreateInputView() {
        root = (ImeRoot) getLayoutInflater().inflate(R.layout.ime_root, null);
        root.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        toolbar = root.findViewById(R.id.toolbar);
        suggestions = root.findViewById(R.id.suggestions);
        credentialSuggestions = root.findViewById(R.id.credential_suggestions);
        banner = root.findViewById(R.id.banner);
        keyboard = root.findViewById(R.id.keyboard);
        emojiPanel = root.findViewById(R.id.emoji);
        clipPanel = root.findViewById(R.id.clipboard);
        toolbar.setListener(this);
        suggestions.setListener(this);
        credentialSuggestions.setListener(this);
        keyboard.setListener(this);
        emojiPanel.setListener(this);
        clipPanel.setListener(this);
        translatePanel = root.findViewById(R.id.translate);
        translatePanel.setListener(this);
        menuPanel = root.findViewById(R.id.menu_panel);
        menuPanel.setListener(this);
        editPanel = root.findViewById(R.id.text_edit);
        editPanel.setListener(this);
        root.setCallbacks(this);
        loadFunctional();
        applyAll();
        showMode(MODE_KEYS);
        return root;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        applyNavBar();
        registerBackCallback();
        hideHandler.removeCallbacks(hideRetry);
        hideHandler.removeCallbacks(hideForce);
        if (input == null) return;
        currentEditorInfo = info;
        input.start(info);
        if (keyboard != null) choosePageFor(info);
        applyEnterIcon();
        syncShift();
        reloadCredentialSuggestions(info);
        refreshSuggestVisibility();
        showMode(MODE_KEYS);
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        credentialRequestId++;
        if (input != null) input.finishComposingNow();
        currentEditorInfo = null;
        if (credentialSuggestions != null) credentialSuggestions.clear();
        if (keyboard != null) keyboard.dismissPopups();
        if (voice != null && voice.isListening()) voice.stop();
        super.onFinishInputView(finishingInput);
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        if (input != null) {
            input.onSelectionUpdate(newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        }
        if (input != null && input.isEmail() && currentEditorInfo != null && mode == MODE_KEYS) {
            reloadCredentialSuggestions(currentEditorInfo);
        }
        if (mode == MODE_TRANSLATE && translatePanel != null && newSelEnd > newSelStart) {
            String sel = selectedText();
            if (sel != null) translatePanel.show(sel);
        }
    }

    private final android.os.Handler hideHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable hideRetry = () -> {
        if (!destroyed && isInputViewShown()) requestHideSelf(0);
    };
    private final Runnable hideForce = () -> {
        if (!destroyed && isInputViewShown()) {
            try {
                hideWindow();
            } catch (RuntimeException ignored) {
            }
        }
    };

    private static final boolean DEBUG_TOASTS = false;

    private void dbg(String msg) {
        android.util.Log.d("KeylineIME", msg);
        if (!DEBUG_TOASTS) return;
        try {
            android.widget.Toast.makeText(getApplicationContext(), "KL: " + msg, android.widget.Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    private void hideKeyboard() {
        dbg("hideKeyboard shown=" + isInputViewShown() + " req=" + isShowInputRequested());
        if (keyboard != null) keyboard.dismissPopups();
        hideHandler.removeCallbacks(hideRetry);
        hideHandler.removeCallbacks(hideForce);
        requestHideSelf(0);
        hideHandler.postDelayed(hideRetry, 120);
        hideHandler.postDelayed(hideForce, 380);
    }

    private final Runnable hideIfOrphaned = () -> {
        if (destroyed) return;
        if (isInputViewShown() && (getCurrentInputConnection() == null
                || getCurrentInputEditorInfo() == null)) {
            dbg("no editor left, hiding");
            hideKeyboard();
        }
    };

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        hideHandler.removeCallbacks(hideIfOrphaned);
        super.onStartInput(attribute, restarting);
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        hideHandler.removeCallbacks(hideIfOrphaned);
        hideHandler.postDelayed(hideIfOrphaned, 350);
    }

    private boolean keyboardOnScreen() {
        return isInputViewShown() || isShowInputRequested();
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK) dbg("onKeyDown BACK");
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK && keyboardOnScreen()) {
            if (event != null && event.getRepeatCount() == 0) hideKeyboard();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK && keyboardOnScreen()) return true;
        return super.onKeyUp(keyCode, event);
    }

    private Object backCallback;

    private void registerBackCallback() {
        if (android.os.Build.VERSION.SDK_INT < 33 || backCallback != null) return;
        android.view.Window w = getWindow() == null ? null : getWindow().getWindow();
        if (w == null) return;
        android.window.OnBackInvokedDispatcher d;
        try {
            d = w.getOnBackInvokedDispatcher();
        } catch (Throwable t) {
            return;
        }
        android.window.OnBackInvokedCallback cb;
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            cb = new android.window.OnBackAnimationCallback() {
                @Override
                public void onBackInvoked() {
                    hideKeyboard();
                }
            };
        } else {
            cb = this::hideKeyboard;
        }
        int[] priorities = {android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT};
        for (int pr : priorities) {
            try {
                d.registerOnBackInvokedCallback(pr, cb);
                backCallback = cb;
                android.util.Log.d("KeylineIME", "back callback registered prio=" + pr);
                return;
            } catch (Throwable t) {
                dbg("back callback refused prio=" + pr + " " + t.getClass().getSimpleName());
            }
        }
    }

    private void unregisterBackCallback() {
        if (backCallback == null) return;
        try {
            android.view.Window w = getWindow() == null ? null : getWindow().getWindow();
            if (w != null && android.os.Build.VERSION.SDK_INT >= 33) {
                w.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
                        (android.window.OnBackInvokedCallback) backCallback);
            }
        } catch (Throwable ignored) {
        }
        backCallback = null;
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        hideHandler.removeCallbacks(hideRetry);
        hideHandler.removeCallbacks(hideForce);
        hideHandler.removeCallbacks(hideIfOrphaned);
        unregisterBackCallback();
        if (keyboard != null) keyboard.dismissPopups();
        if (mode != MODE_KEYS) showMode(MODE_KEYS);
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (root != null) root.requestLayout();
        applyAll();
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        if (subtype == null || subtype.getExtraValue() == null) return;
        for (String part : subtype.getExtraValue().split(",")) {
            if (part.startsWith("layout=")) {
                String id = part.substring("layout=".length());
                if (lang != null && id.equals(lang.id)) return;
                switchLanguage(id);
            }
        }
    }

    @Override
    public void onDestroy() {
        destroyed = true;
        hideHandler.removeCallbacksAndMessages(null);
        credentialRequestId++;
        Prefs.get(this).unlisten(this);
        if (clipboard != null) clipboard.removePrimaryClipChangedListener(clipListener);
        if (voice != null) voice.destroy();
        if (keyboard != null) keyboard.dismissPopups();
        if (input != null) input.shutdown();
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Bg.main(() -> {
            try {
                if (PrefKeys.ONE_HAND_WIDTH.equals(key) || PrefKeys.FLOAT_SCALE.equals(key)) {
                    if (root != null) root.requestLayout();
                    return;
                }
                if (PrefKeys.ONE_HAND.equals(key) || PrefKeys.ONE_HAND_RIGHT.equals(key)) {

                    applyLayoutMode();
                    return;
                }
                applyAll();
                if (PrefKeys.EMAIL_SUGGESTIONS.equals(key)
                        || PrefKeys.LOGIN_SUGGESTIONS.equals(key)
                        || PrefKeys.INCOGNITO.equals(key)) {
                    reloadCredentialSuggestions(currentEditorInfo);
                }
            } catch (Throwable t) {
                android.util.Log.e("Keyline", "applying settings failed", t);
            }
        });
    }

    private void choosePageFor(EditorInfo info) {
        int type = info == null ? 0 : info.inputType;
        int cls = type & EditorInfo.TYPE_MASK_CLASS;
        LayoutModel pad = null;
        boolean signed = false;
        boolean decimal = false;
        if (cls == EditorInfo.TYPE_CLASS_NUMBER) {
            pad = padNumber;
            signed = (type & EditorInfo.TYPE_NUMBER_FLAG_SIGNED) != 0;
            decimal = (type & EditorInfo.TYPE_NUMBER_FLAG_DECIMAL) != 0;
        } else if (cls == EditorInfo.TYPE_CLASS_PHONE) {
            pad = padPhone;
        } else if (cls == EditorInfo.TYPE_CLASS_DATETIME) {
            pad = padDate;
        }
        if (pad != null) {
            keyboard.setPad(pad, signed, decimal);
            keyboard.setPage(KeyboardView.PAGE_PAD);
        } else if (cls == EditorInfo.TYPE_CLASS_NUMBER || cls == EditorInfo.TYPE_CLASS_PHONE) {
            keyboard.setPage(KeyboardView.PAGE_NUM);
        } else {
            keyboard.setPage(KeyboardView.PAGE_LETTERS);
        }
    }

    private void loadFunctional() {
        padNumber = LayoutRepository.functional(this, "pad_number");
        padPhone = LayoutRepository.functional(this, "pad_phone");
        padDate = LayoutRepository.functional(this, "pad_datetime");
        LayoutModel numbers = LayoutRepository.functional(this, "numbers");
        LayoutModel sym = LayoutRepository.functional(this, "symbols");
        LayoutModel sym2 = LayoutRepository.functional(this, "symbols2");
        lang = LayoutRepository.load(this, Prefs.get(this).activeLang());
        if (lang == null) lang = LayoutRepository.load(this, "en");
        if (keyboard != null) keyboard.setLayouts(lang, numbers, sym, sym2);
        if (lang != null) SuggestionEngine.get().warm(this, lang.id);
        java.util.Locale uiLocale = lang == null ? java.util.Locale.US : lang.javaLocale();
        if (clipPanel != null) clipPanel.setLanguage(uiLocale);
        if (emojiPanel != null) emojiPanel.setLanguage(uiLocale);
        if (menuPanel != null) menuPanel.setLanguage(uiLocale);
        if (editPanel != null) editPanel.setLanguage(uiLocale);
    }

    private com.keyline.ime.theme.KeyboardTheme uiTheme() {
        return Prefs.get(this).hasBg() ? theme.overImage() : theme;
    }

    private void applyAll() {
        if (keyboard == null || destroyed) return;
        theme = ThemeManager.resolve(this);
        Prefs p = Prefs.get(this);
        com.keyline.ime.theme.KeyboardTheme ui = p.hasBg() ? theme.overImage() : theme;
        keyboard.setTheme(theme);
        keyboard.setKeyAlpha(p.keyAlpha());
        keyboard.setHideKeyBackground(p.hideKeyBg());
        keyboard.setGlassTint(ui.toolbarIconColor());
        keyboard.setFont(com.keyline.ime.theme.FontManager.typeface(this, p.font()));
        keyboard.setFontScale(p.fontSize() / 100f);
                keyboard.setPreviewEnabled(p.preview());
        keyboard.setLongPressEnabled(p.longPress());
        suggestions.apply(ui);
        applyToolbar(voice != null && voice.isListening());
        if (credentialSuggestions != null) credentialSuggestions.apply(ui);
        emojiPanel.apply(ui);
        clipPanel.apply(ui);
        translatePanel.apply(ui);
        menuPanel.apply(ui);
        editPanel.apply(ui);
        root.setIconColor(ui.toolbarIconColor());
        applyLayoutMode();
        banner.setBackgroundColor(ui.toolbar);
        banner.setTextColor(theme.text);
        root.setBackgroundColor(theme.background);
        root.setGlassDark(theme.dark);
        applyNavBar();
        if (root != null) root.requestLayout();
        loadBackground();
        refreshSuggestVisibility();
        syncShift();
    }

    private void applyToolbar(boolean listening) {
        if (theme == null) return;
        if (toolbar != null) toolbar.apply(uiTheme(), mode == MODE_CLIP, mode == MODE_EMOJI, false, listening);
        if (suggestions != null) suggestions.setMicActive(listening);
    }

    private void applyLayoutMode() {
        if (root == null) return;
        Prefs p = Prefs.get(this);
        boolean floatingOn = p.floating();
        boolean oneHandOn = !floatingOn && p.oneHand();
        root.setFloating(floatingOn, p.floatX(), p.floatY());
        root.setOneHand(oneHandOn
                ? (p.oneHandRight() ? ImeRoot.ONE_HAND_KEYS_RIGHT : ImeRoot.ONE_HAND_KEYS_LEFT)
                : ImeRoot.ONE_HAND_OFF);
        if (menuPanel != null) menuPanel.setStates(oneHandOn, floatingOn);
    }

    @Override
    public void onComputeInsets(Insets outInsets) {
        super.onComputeInsets(outInsets);
        if (root == null || !root.isFloating()) return;
        android.graphics.Rect rect = new android.graphics.Rect();
        if (!root.floatingTouchRect(rect)) return;
        android.view.Window w = getWindow() == null ? null : getWindow().getWindow();
        if (w == null) return;
        int h = w.getDecorView().getHeight();

        outInsets.contentTopInsets = h;
        outInsets.visibleTopInsets = h;
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION;
        outInsets.touchableRegion.set(rect);
    }

    private void applyNavBar() {
        if (theme == null || getWindow() == null) return;
        try {
            android.view.Window w = getWindow().getWindow();
            if (w == null) return;
            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setNavigationBarColor(theme.background | 0xFF000000);
            if (android.os.Build.VERSION.SDK_INT >= 28) w.setNavigationBarDividerColor(android.graphics.Color.TRANSPARENT);
            androidx.core.view.WindowInsetsControllerCompat c =
                    androidx.core.view.WindowCompat.getInsetsController(w, w.getDecorView());
            if (c != null) c.setAppearanceLightNavigationBars(!theme.dark);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onWindowShown() {
        super.onWindowShown();
        applyNavBar();
        registerBackCallback();
    }

    private long bgSig = Long.MIN_VALUE;

    private long currentBgSig() {
        if (!Prefs.get(this).hasBg()) return 0L;
        java.io.File f = BackgroundImageManager.display(this);
        return f.exists() ? (f.lastModified() * 31L + f.length() + 1L) : 0L;
    }

    private void loadBackground() {
        if (root != null) root.setDim(Prefs.get(this).bgDim());
        final long sig = currentBgSig();
        if (sig == bgSig) return;
        bgSig = sig;
        Bg.io(() -> {
            android.graphics.Bitmap bmp = null;
            try {
                bmp = BackgroundImageManager.loadDisplay(this);
            } catch (Throwable ignored) {
                bgSig = Long.MIN_VALUE;
            }
            final android.graphics.Bitmap result = bmp;
            Bg.main(() -> {
                if (root != null && !destroyed) root.setBackgroundImage(result, Prefs.get(this).bgDim());
            });
        });
    }

    private void applyEnterIcon() {
        if (keyboard == null || input == null) return;
        int icon = R.drawable.ic_enter;
        if (!input.noEnterAction()) {
            switch (input.enterAction()) {
                case EditorInfo.IME_ACTION_SEARCH: icon = R.drawable.ic_search; break;
                case EditorInfo.IME_ACTION_GO: icon = R.drawable.ic_go; break;
                case EditorInfo.IME_ACTION_SEND: icon = R.drawable.ic_send; break;
                case EditorInfo.IME_ACTION_NEXT: icon = R.drawable.ic_next; break;
                case EditorInfo.IME_ACTION_DONE: icon = R.drawable.ic_done; break;
                default: break;
            }
        }
        keyboard.setEnterIcon(icon);
    }

    private void syncShift() {
        if (keyboard != null && input != null) keyboard.setShift(input.shift(), input.autoShift());
    }

    private void refreshSuggestVisibility() {
        if (suggestions == null || input == null) return;
        boolean padOpen = mode == MODE_KEYS && keyboard != null && keyboard.page() == KeyboardView.PAGE_PAD;
        boolean keyboardEligible = mode == MODE_KEYS && !padOpen;
        boolean showCredentials = keyboardEligible && credentialSuggestions != null
                && credentialSuggestions.hasEntries();

        boolean showSuggestions = keyboardEligible && !showCredentials && suggestions.hasWords();
        boolean showToolbar = !padOpen && !showSuggestions && !showCredentials;
        if (toolbar != null) toolbar.setKeysMode(mode == MODE_KEYS);
        TopBarFade.set(suggestions, showSuggestions);
        if (credentialSuggestions != null) TopBarFade.set(credentialSuggestions, showCredentials);
        if (toolbar != null) TopBarFade.set(toolbar, showToolbar);
    }

    private String emailTyped() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return "";
        try {
            CharSequence before = ic.getTextBeforeCursor(256, 0);
            return before == null ? "" : before.toString();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private void reloadCredentialSuggestions(EditorInfo info) {
        long request = ++credentialRequestId;
        if (credentialSuggestions == null || input == null) return;
        credentialSuggestions.clear();
        Prefs prefs = Prefs.get(this);
        boolean isPassword = input.isPassword();
        boolean isEmail = input.isEmail();
        boolean emailMode = isEmail && prefs.emailSuggestions();
        boolean enabled = !prefs.incognito()
                && (isPassword ? prefs.loginSuggestions()
                : emailMode || prefs.loginSuggestions());
        String packageName = info == null || info.packageName == null ? "" : info.packageName;
        if (!enabled || (!emailMode && packageName.isEmpty())) {
            credentialSuggestions.clear();
            refreshSuggestVisibility();
            return;
        }
        final String typed = emailMode ? emailTyped() : "";
        Bg.io(() -> {
            List<CredentialRecord> entries;
            try {
                entries = CredentialVault.suggestions(getApplicationContext(), packageName,
                        emailMode, isPassword, typed);
            } catch (Exception ignored) {
                entries = java.util.Collections.emptyList();
            }
            List<CredentialRecord> result = entries;
            Bg.main(() -> {
                if (destroyed || request != credentialRequestId || credentialSuggestions == null) return;
                credentialSuggestions.setEntries(result, isPassword);
                refreshSuggestVisibility();
            });
        });
    }

    private void showMode(int next) {
        int previous = mode;
        mode = next;
        if (keyboard == null) return;
        if (previous != next) animateIn(next);
        keyboard.setVisibility(next == MODE_KEYS ? View.VISIBLE : View.GONE);
        emojiPanel.setVisibility(next == MODE_EMOJI ? View.VISIBLE : View.GONE);
        clipPanel.setVisibility(next == MODE_CLIP ? View.VISIBLE : View.GONE);
        translatePanel.setVisibility(next == MODE_TRANSLATE ? View.VISIBLE : View.GONE);
        menuPanel.setVisibility(next == MODE_MENU ? View.VISIBLE : View.GONE);
        editPanel.setVisibility(next == MODE_EDIT ? View.VISIBLE : View.GONE);
        if (previous == MODE_EDIT || next == MODE_EDIT) editPanel.reset();
        if (next == MODE_EMOJI) emojiPanel.reload();
        if (next == MODE_CLIP) clipPanel.load();
        banner.setVisibility(View.GONE);
        applyToolbar(voice != null && voice.isListening());
        refreshSuggestVisibility();
    }

    private void animateIn(int next) {
        View v = next == MODE_EMOJI ? emojiPanel : next == MODE_CLIP ? clipPanel
                : next == MODE_TRANSLATE ? translatePanel : next == MODE_MENU ? menuPanel
                : next == MODE_EDIT ? editPanel : keyboard;
        if (v == null) return;
        float d = getResources().getDisplayMetrics().density;
        v.animate().cancel();
        v.setAlpha(0f);
        v.setTranslationY(16f * d);
        v.animate().alpha(1f).translationY(0f).setDuration(180)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
    }

    private void switchLanguage(String id) {
        LayoutModel m = LayoutRepository.load(this, id);
        if (m == null) return;
        Prefs.get(this).activeLang(id);
        lang = m;
        loadFunctional();
        if (keyboard != null) keyboard.setPage(KeyboardView.PAGE_LETTERS);
        SuggestionEngine.get().warm(this, id);
        syncShift();
        syncSystemSubtype(id);
    }

    @SuppressWarnings("deprecation")
    private void syncSystemSubtype(String id) {
        try {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            android.os.IBinder token = getWindow() == null || getWindow().getWindow() == null
                    ? null : getWindow().getWindow().getAttributes().token;
            if (imm == null || token == null) return;
            for (android.view.inputmethod.InputMethodInfo info : imm.getEnabledInputMethodList()) {
                if (!info.getPackageName().equals(getPackageName())) continue;
                for (android.view.inputmethod.InputMethodSubtype st
                        : imm.getEnabledInputMethodSubtypeList(info, true)) {
                    String extra = st.getExtraValue();
                    if (extra != null && extra.contains("layout=" + id)) {
                        imm.setInputMethodAndSubtype(token, info.getId(), st);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void showKeyboardPicker() {
        try {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        } catch (Throwable ignored) {
        }
    }

    private void cycleLanguage() {
        List<String> enabled = LayoutRepository.enabled(this);
        if (enabled.isEmpty()) return;
        String cur = Prefs.get(this).activeLang();
        int i = enabled.indexOf(cur);
        String next = enabled.get((i + 1) % enabled.size());
        switchLanguage(next);
    }

    @Override public InputConnection connection() { return getCurrentInputConnection(); }
    @Override public boolean canLearn() {
        return input != null && !input.isPassword() && Prefs.get(this).suggestions() && !Prefs.get(this).incognito();
    }
    @Override public boolean suggestionsOn() { return Prefs.get(this).suggestions(); }
    @Override public boolean autoCapOn() { return Prefs.get(this).autoCap(); }
    @Override public boolean doubleSpacePeriod() { return Prefs.get(this).doubleSpacePeriod(); }
    @Override public boolean spellCheckOn() { return Prefs.get(this).suggestions() && Prefs.get(this).spellCheck(); }
    @Override public boolean autoCorrectOn() { return Prefs.get(this).autoCorrect(); }
    @Override public String languageId() { return lang == null ? "en" : lang.id; }
    @Override public java.util.Locale locale() { return lang == null ? java.util.Locale.US : lang.javaLocale(); }

    @Override public void onSuggestions(List<String> words) {
        if (suggestions != null) {
            suggestions.setWords(words);
            refreshSuggestVisibility();
        }
    }

    @Override public void onShiftChanged() { syncShift(); }

    @Override
    public void onText(String text) {
        if (input != null) input.onChar(text);
    }

    @Override
    public void onAction(String action) {
        if ("#feedback".equals(action)) {
            buzz();
            click();
            return;
        }
        if ("#switch_keyboard".equals(action)) {
            buzz();
            showKeyboardPicker();
            return;
        }
        if (input == null) return;
        switch (action) {
            case "shift": input.toggleShift(); break;
            case "enter":
                input.onEnter();
                break;
            case "space":
                input.onSpace();
                break;
            case "lang": cycleLanguage(); break;
            case "emoji":
                showMode(mode == MODE_EMOJI ? MODE_KEYS : MODE_EMOJI);
                break;
            default: break;
        }
    }

    @Override
    public void onBackspace(boolean wholeWord) {
        if (input != null) input.onBackspace(wholeWord);
    }

    @Override public void onCursorMove(int delta) { if (input != null) input.moveCursor(delta); }

    @Override public void onPick(String word) { if (input != null) input.pickSuggestion(word); }

    @Override public void onClipboard() { showMode(mode == MODE_CLIP ? MODE_KEYS : MODE_CLIP); }

    @Override public void onEmoji() { showMode(mode == MODE_EMOJI ? MODE_KEYS : MODE_EMOJI); }

    @Override
    public void onMic() {
        if (voice.isListening()) {
            voice.stop();
            return;
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            PermissionActivity.callback = granted -> Bg.main(() -> {
                if (granted) voice.start(voiceTag());
                else toast(getString(R.string.voice_no_permission));
            });
            Intent i = new Intent(this, PermissionActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return;
        }
        voice.start(voiceTag());
    }

    private String voiceTag() {
        String over = Prefs.get(this).voiceLang();
        if (over != null && !over.isEmpty()) return over;
        return lang == null ? "en-US" : lang.locale;
    }

    @Override
    public void onSettings() {
        Intent i = new Intent(this, SettingsActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    public void onMenu() { showMode(MODE_MENU); }

    @Override
    public void onCollapse() { showMode(MODE_KEYS); }

    @Override
    public void onTheme() {
        Intent i = new Intent(this, ThemeSettingsActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    public void onTextEdit() { showMode(MODE_EDIT); }

    @Override
    public void onOneHand() {
        Prefs p = Prefs.get(this);
        boolean on = !(p.oneHand() && !p.floating());
        if (on) p.floating(false);
        p.oneHand(on);
        showMode(MODE_KEYS);
    }

    @Override
    public void onFloating() {
        Prefs p = Prefs.get(this);
        boolean on = !p.floating();
        if (on) p.oneHand(false);
        p.floating(on);
        showMode(MODE_KEYS);
    }

    @Override
    public void onNextLanguage() {
        cycleLanguage();
        showMode(MODE_KEYS);
    }

    @Override
    public void onEditPrepare() {
        if (input != null) input.finishComposingNow();
        buzz();
    }

    @Override public void onFloatMoved(float fx, float fy) { Prefs.get(this).floatPos(fx, fy); }

    @Override public void onDock() { Prefs.get(this).floating(false); }

    @Override public void onOneHandSwap() {
        Prefs p = Prefs.get(this);
        p.oneHandRight(!p.oneHandRight());
    }

    @Override public void onOneHandFull() { Prefs.get(this).oneHand(false); }

    @Override
    public void onTranslate() {
        if (mode == MODE_TRANSLATE) { showMode(MODE_KEYS); return; }
        if (input != null && input.isPassword()) {
            toast(getString(R.string.translate_password_blocked));
            return;
        }
        String text = selectedText();
        if (text == null || text.isEmpty()) {
            try {
                InputConnection ic = getCurrentInputConnection();
                CharSequence all = ic == null ? null : CursorHelper.selectedOrAll(ic);
                text = all == null ? "" : all.toString();
            } catch (RuntimeException ignored) {
                text = "";
            }
        }
        showMode(MODE_TRANSLATE);
        translatePanel.show(text);
    }

    private String selectedText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return null;
        try {
            CharSequence sel = ic.getSelectedText(0);
            return sel == null || sel.length() == 0 ? null : sel.toString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public void onReplace(String translated) {
        if (input != null && translated != null) input.commitRaw(translated);
        showMode(MODE_KEYS);
    }

    @Override
    public void onCopy(String translated) {
        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null && translated != null) {
            cm.setPrimaryClip(android.content.ClipData.newPlainText("keyline", translated));
        }
    }

    @Override
    public void onCredentials() {
        Intent intent = new Intent(this, CredentialVaultActivity.class);
        if (currentEditorInfo != null && currentEditorInfo.packageName != null) {
            intent.putExtra(CredentialVaultActivity.EXTRA_CURRENT_PACKAGE,
                    currentEditorInfo.packageName);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onFillCredential(CredentialRecord record, boolean passwordField) {
        if (input == null || record == null || input.isPassword() != passwordField) return;
        EditorInfo active = input.info();
        if (!input.isEmail() && (active == null || active.packageName == null
                || !active.packageName.equals(record.appPackage))) return;
        if (passwordField && (record.password == null || record.password.isEmpty())) return;
        String value = passwordField ? record.password : record.username;
        if (value == null || value.isEmpty()) return;
        if (!passwordField && input.isEmail()) {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
                String typed = emailTyped();
                if (!typed.isEmpty() && value.toLowerCase(java.util.Locale.ROOT)
                        .contains(typed.trim().toLowerCase(java.util.Locale.ROOT))) {
                    ic.deleteSurroundingText(typed.length(), 0);
                }
            }
        }
        input.commitRaw(value);
        if (credentialSuggestions != null) credentialSuggestions.clear();
        refreshSuggestVisibility();
    }

    @Override
    public void onUndo() {
        android.view.inputmethod.InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.finishComposingText();
        long now = android.os.SystemClock.uptimeMillis();
        ic.sendKeyEvent(new android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_DOWN,
                android.view.KeyEvent.KEYCODE_Z, 0, android.view.KeyEvent.META_CTRL_ON));
        ic.sendKeyEvent(new android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_UP,
                android.view.KeyEvent.KEYCODE_Z, 0, android.view.KeyEvent.META_CTRL_ON));
    }

    @Override public void onEmoji(String emoji) {
        if (input != null) input.commitRaw(emoji);
    }

    @Override public void onLetters() { showMode(MODE_KEYS); }

    @Override public void onPaste(String text) {
        if (input != null && text != null) input.commitRaw(text);
        showMode(MODE_KEYS);
    }

    @Override public void onClose() { showMode(MODE_KEYS); }

    @Override public void onChanged() { }

    @Override
    public void onBackspace() {
        if (input != null) input.onBackspace(false);
    }

    @Override public void onPartial(String text) {
        if (banner == null) return;
        banner.setVisibility(View.VISIBLE);
        banner.setText(getString(R.string.voice_listening) + " " + text);
    }

    @Override public void onFinal(String text) {
        if (banner != null) banner.setVisibility(View.GONE);
        if (input != null && text != null && !text.isEmpty()) {
            String out = text.endsWith(" ") ? text : text + " ";
            input.commitRaw(out);
        }
        applyToolbar(false);
    }

    @Override public void onError(String message) {
        if (banner != null) banner.setVisibility(View.GONE);
        toast(message);
        applyToolbar(false);
    }

    @Override public void onListening(boolean listening) {
        if (banner == null) return;
        if (listening) {
            banner.setVisibility(View.VISIBLE);
            banner.setText(R.string.voice_listening);
        }
        applyToolbar(listening);
    }

    private void onClipChanged() {
        if (clipboard == null || !clipboard.hasPrimaryClip() || clipboard.getPrimaryClip() == null) return;
        android.content.ClipData data = clipboard.getPrimaryClip();
        if (data.getItemCount() == 0) return;
        if (Prefs.get(this).clipIgnoreSensitive()) {
            if (input != null && input.isPassword()) return;
            android.content.ClipDescription desc = data.getDescription();
            if (desc != null && desc.getExtras() != null
                    && desc.getExtras().getBoolean("android.content.extra.IS_SENSITIVE", false)) return;
        }
        CharSequence text = data.getItemAt(0).coerceToText(this);
        if (text == null) return;
        String value = text.toString();
        Bg.io(() -> {
            ClipboardRepository.remember(this, value);
            Bg.main(() -> { if (clipPanel != null && clipPanel.getVisibility() == View.VISIBLE) clipPanel.load(); });
        });
    }

    private void buzz() {
        Prefs p = Prefs.get(this);
        if (!p.vibrate()) return;
        Haptics.tap(this, keyboard != null ? keyboard : root, p.vibrateStrength());
    }

    private void click() {
        if (!Prefs.get(this).sound()) return;
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am != null) am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.7f);
    }

    private void toast(String msg) {
        if (msg == null) return;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
