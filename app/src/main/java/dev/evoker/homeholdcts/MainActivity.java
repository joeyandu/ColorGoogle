// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse
// Modified for MindTrigger Assist on 2026-08-17.

package dev.evoker.homeholdcts;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.slider.Slider;

import java.util.List;
import java.util.ArrayList;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    static final String PREFS = "home_hold_cts";
    static final String PREF_ENABLED = "enabled";
    static final String PREF_TERMS_ACCEPTED = "terms_privacy_accepted_v5";
    static final String PREF_CTS_DELAY_MS = "cts_delay_ms";
    static final String PREF_VIBRATE_ON_CTS = "vibrate_on_cts";
    static final String PREF_SOUND_ON_ACTIVATION = "sound_on_activation";
    static final String PREF_POWER_GEMINI_EXPERIMENTAL =
            "power_gemini_experimental";
    static final String PREF_VOICE_WAKE_ASSISTANT_EXPERIMENTAL =
            "voice_wake_assistant_experimental";
    static final String PREF_SWAP_CTS_ASSISTANT_EXPERIMENTAL =
            "swap_cts_assistant_experimental";
    static final String PREF_BETA_SLOW_ANIMATIONS =
            "beta_slow_animations";
    static final String PREF_BETA_QUICK_SETTINGS_LOG_RECOVERY =
            "beta_quick_settings_log_recovery";

    static final boolean DEFAULT_VIBRATE_ON_CTS = true;
    static final boolean DEFAULT_SOUND_ON_ACTIVATION = true;
    static final boolean DEFAULT_POWER_GEMINI_EXPERIMENTAL = true;
    static final boolean DEFAULT_VOICE_WAKE_ASSISTANT_EXPERIMENTAL = false;
    static final boolean DEFAULT_SWAP_CTS_ASSISTANT_EXPERIMENTAL = false;
    static final boolean DEFAULT_BETA_SLOW_ANIMATIONS = false;
    static final boolean DEFAULT_BETA_QUICK_SETTINGS_LOG_RECOVERY = false;
    static final int DEFAULT_CTS_DELAY_MS = 0;
    static final int MAX_CTS_DELAY_MS = 1000;

    private static final int REQUEST_NOTIFICATIONS = 16103;
    private static final String PREF_NOTIFICATION_ASKED = "notification_permission_asked";
    private boolean runAfterNotificationRequest;
    private TextView notificationStatus;
    private MaterialButton notificationButton;

    private static final int REQUEST_PICK_CTS_SOUND = 16101;
    private static final int REQUEST_PICK_ASSISTANT_SOUND = 16102;

    private static final String PREF_GESTURE_CONFIRMED = "guide_gesture_confirmed";
    private static final String PREF_RECENTS_LOCKED = "guide_recents_locked";
    private static final String PREF_GOOGLE_AUTOSTART = "guide_google_autostart";
    private static final String PREF_GEMINI_AUTOSTART = "guide_gemini_autostart";

    // UI-only wizard state. Runtime/core V5 behavior remains untouched.
    private static final String PREF_SETUP_UI_PAGE = "stable_ui_setup_page";
    private static final String PREF_SETUP_UI_FINISHED = "stable_ui_setup_finished";
    private static final String STATE_SETUP_PAGE = "setup_page";
    private static final String EXTRA_RECREATE_SETUP_PAGE =
            "dev.evoker.homeholdcts.extra.RECREATE_SETUP_PAGE";

    private static final int SETUP_PAGE_PRIVILEGED = 0;
    private static final int SETUP_PAGE_GESTURE = 1;
    private static final int SETUP_PAGE_BACKGROUND = 2;
    private static final int SETUP_PAGE_GOOGLE = 3;
    private static final int SETUP_PAGE_RUN = 4;
    private static final int SETUP_PAGE_COUNT = 5;

    private static final int TAB_SETUP = 1;
    private static final int TAB_ADVANCED = 2;
    private static final int TAB_SUPPORT = 3;
    private static final int TAB_ABOUT = 4;
    private static final int TAB_BETA = 5;
    private static final String STATE_SELECTED_TAB = "selected_tab";
    private static final String EXTRA_RECREATE_TAB =
            "dev.evoker.homeholdcts.extra.RECREATE_TAB";

    // One visual grid for every tab.  These values deliberately live beside
    // the UI-only constants: changing them must never affect setup or watcher
    // behavior.
    private static final int UI_PAGE_SIDE_DP = 20;
    private static final int UI_PAGE_TOP_DP = 12;
    private static final int UI_PAGE_BOTTOM_DP = 32;
    private static final int UI_CARD_GAP_DP = 14;
    private static final int UI_CARD_RADIUS_DP = 20;
    private static final int UI_NESTED_CARD_RADIUS_DP = 16;

    private int currentTab = TAB_SETUP;
    private int currentSetupPage = SETUP_PAGE_PRIVILEGED;

    private final Handler ui = new Handler(Looper.getMainLooper());


private Messenger watcherMessenger;
private boolean watcherBound;
private boolean activityResumed;
private boolean activityHasWindowFocus;
private boolean pendingTopLogReconnect;
private boolean pendingTopLogReconnectForce;
private boolean topLogReconnectScheduled;
private boolean watcherUsesShellLogs;
private int watcherSessionState =
        WatcherIpc.STATE_STOPPED;

private final Messenger uiMessenger =
        new Messenger(
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        if (msg.what
                                == WatcherIpc.MSG_STATE_CHANGED) {
                            watcherSessionState = msg.arg1;
                            watcherUsesShellLogs = msg.getData().getBoolean(WatcherIpc.KEY_SHELL_LOGS);
                            refreshAll();
                            return;
                        }

                        super.handleMessage(msg);
                    }
                });

private final ServiceConnection watcherConnection =
        new ServiceConnection() {
            @Override
            public void onServiceConnected(
                    ComponentName name,
                    IBinder binder) {

                watcherMessenger =
                        new Messenger(binder);
                watcherBound = true;

                sendWatcherMessage(
                        WatcherIpc.MSG_REGISTER_CLIENT,
                        null,
                        true);

                syncWatcherPrefs();

                sendWatcherMessage(
                        WatcherIpc.MSG_REQUEST_STATE,
                        null,
                        false);


                if (activityResumed) {
                    armLogSessionReconnectWhenTop(false);
                }
            }

            @Override
            public void onServiceDisconnected(
                    ComponentName name) {

                watcherBound = false;
                watcherMessenger = null;
                watcherSessionState =
                        WatcherIpc.STATE_NEEDS_RECONNECT;
                refreshAll();
            }
        };

    private int primary;
    private int onPrimary;
    private int primaryContainer;
    private int onPrimaryContainer;
    private int surface;
    private int surfaceContainer;
    private int surfaceContainerHigh;
    private int onSurface;
    private int onSurfaceVariant;
    private int outline;
    private int success;
    private int warning;
    private int error;

    private FirstRunBootstrap bootstrap;
    // Shizuku may invoke its sticky binder callback synchronously. Do not let
    // a setup callback render into this activity until every view exists.
    private boolean uiReady;

    private FrameLayout contentHost;
    private BottomNavigationView bottomNav;
    private boolean setupWizardActive;

    private FrameLayout setupWizardHost;
    private final ArrayList<View> setupWizardPages = new ArrayList<>();
    private final ArrayList<View> setupProgressSegments = new ArrayList<>();
    private ScrollView setupScroll;
    private TextView setupWizardEyebrow;
    private TextView setupWizardTitle;
    private TextView setupWizardHint;
    private MaterialButton setupBackButton;
    private MaterialButton setupNextButton;
    private TextView privilegedActionFeedback;
    private TextView gestureActionFeedback;
    private String lastBootstrapUiState = "";
    private boolean buildingSetupWizard;

    private View setupPage;
    private View advancedPage;
    private View betaPage;
    private MaterialSwitch desktopSearchToggle;
    private TextView desktopSearchStatus;
    private MaterialSwitch wirelessToggle;
    private TextView wirelessStatus;
    private boolean wirelessRendering;
    private boolean desktopSearchRendering;
    private boolean desktopSearchBusy;
    private final java.util.concurrent.ExecutorService desktopSearchIo =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private final Runnable desktopSearchPoll = new Runnable() {
        @Override public void run() {
            refreshDesktopSearch(null);
            wirelessRequest("status", null);
            if (activityResumed) ui.postDelayed(this, 4000);
        }
    };
    private View supportPage;
    private View aboutPage;

    private TextView heroTitle;
    private TextView heroSubtitle;

    private TextView step1Status;
    private TextView step2Status;
    private TextView step3Status;
    private TextView criticalStatus;
    private TextView gestureStatus;

    private LinearLayout gestureSetupBlock;
    private LinearLayout gestureConfirmedBlock;
    private LinearLayout recentsSetupBlock;
    private LinearLayout recentsConfirmedBlock;
    private LinearLayout autoLaunchSetupBlock;
    private LinearLayout autoLaunchConfirmedBlock;

    private MaterialButton shizukuButton;
    private MaterialButton pcButton;
    private LinearLayout step1Methods;

    private ActionRow readLogsRow;
    private ActionRow logSessionRow;
    private ActionRow overlayRow;
    private ActionRow selfBatteryRow;
    private ActionRow googleBatteryRow;
    private ActionRow geminiBatteryRow;
    private ActionRow googleAssistantRow;

    private MaterialButton recentsOpenButton;
    private MaterialButton recentsConfirmButton;
    private MaterialButton autoLaunchOpenButton;
    private MaterialButton autoLaunchConfirmButton;
    private MaterialButton runButton;

    private TextView commandLogView;
    private TextView commandTimeView;

    private final Shizuku.OnBinderReceivedListener binderListener = () ->
            runOnUiThread(() -> {
                if (isUiReadyForUpdates() && bootstrap != null && termsAccepted()) {
                    bootstrap.beginIfNeeded();
                }
            });

    private final Shizuku.OnRequestPermissionResultListener permissionListener =
            (requestCode, grantResult) -> {
                if (bootstrap != null) {
                    bootstrap.onPermissionResult(requestCode, grantResult);
                    runOnUiThread(this::refreshAllIfReady);
                }
            };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrap(ThemeManager.wrap(newBase)));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        runAfterNotificationRequest = state != null && state.getBoolean("run_after_notification");

        int restoredTab = TAB_SETUP;
        Intent launchIntent = getIntent();
        boolean restoredFromRecreateMarker = launchIntent != null
                && launchIntent.hasExtra(EXTRA_RECREATE_TAB);
        if (restoredFromRecreateMarker) {
            restoredTab = launchIntent.getIntExtra(
                    EXTRA_RECREATE_TAB,
                    TAB_SETUP);
            // This marker is only for the current recreate() cycle.
            launchIntent.removeExtra(EXTRA_RECREATE_TAB);
        } else if (state != null) {
            restoredTab = state.getInt(STATE_SELECTED_TAB, TAB_SETUP);
        }
        if (isKnownTab(restoredTab)) {
            currentTab = restoredTab;
        }

        int restoredSetupPage = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(PREF_SETUP_UI_PAGE, SETUP_PAGE_PRIVILEGED);
        if (launchIntent != null
                && launchIntent.hasExtra(EXTRA_RECREATE_SETUP_PAGE)) {
            restoredSetupPage = launchIntent.getIntExtra(
                    EXTRA_RECREATE_SETUP_PAGE,
                    restoredSetupPage);
            launchIntent.removeExtra(EXTRA_RECREATE_SETUP_PAGE);
        } else if (state != null) {
            restoredSetupPage = state.getInt(
                    STATE_SETUP_PAGE,
                    restoredSetupPage);
        }
        currentSetupPage = Math.max(SETUP_PAGE_PRIVILEGED,
                Math.min(SETUP_PAGE_RUN, restoredSetupPage));

        // The onboarding wizard is a one-time surface. Once completed, the
        // Setup tab becomes the normal compact/scrollable Setup & Health page
        // and can never reopen the wizard automatically or from bottom nav.
        setupWizardActive = !getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_SETUP_UI_FINISHED, false);

        loadColors();
        configureWindow();

        bootstrap = new FirstRunBootstrap(this, new FirstRunBootstrap.Callback() {
            @Override
            public void onState(String state) {
                runOnUiThread(() -> {
                    if (!isUiReadyForUpdates()) return;
                    lastBootstrapUiState = state == null ? "" : state;
                    updatePrivilegedActionFeedback(lastBootstrapUiState);
                    if (commandLogView != null) {
                        commandLogView.setText(bootstrap.getLastCommandLog());
                    }
                    updateLastCommandTime();
                    refreshAll();
                });
            }

            @Override
            public void onLog(String log) {
                runOnUiThread(() -> {
                    if (!isUiReadyForUpdates()) return;
                    if (commandLogView != null) commandLogView.setText(log);
                    updateLastCommandTime();
                    refreshAll();
                });
            }
        });

        setContentView(buildRoot());
        showTab(currentTab);
        uiReady = true;

        // Register only after buildRoot(). This listener is sticky and can
        // fire immediately when Shizuku is already running.
        Shizuku.addBinderReceivedListenerSticky(binderListener);
        Shizuku.addRequestPermissionResultListener(permissionListener);
        refreshAll();

        if (termsAccepted()) {
            ui.postDelayed(bootstrap::beginIfNeeded, 350L);
        } else {
            ui.postDelayed(this::showFirstRunTerms, 250L);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_TAB, currentTab);
        outState.putInt(STATE_SETUP_PAGE, currentSetupPage);
        outState.putBoolean("run_after_notification", runAfterNotificationRequest);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode != REQUEST_PICK_CTS_SOUND
                && requestCode != REQUEST_PICK_ASSISTANT_SOUND)
                || resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {
            return;
        }

        final Uri uri = data.getData();
        final boolean assistant = requestCode == REQUEST_PICK_ASSISTANT_SOUND;

        try {
            final int takeFlags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (Throwable ignored) {
            // The selected clip is imported into app-private storage below,
            // so a persistable grant is only an optimization.
        }

        Toast.makeText(this, tr("Importing activation sound…"), Toast.LENGTH_SHORT).show();
        final Context appContext = getApplicationContext();
        new Thread(() -> {
            ActivationSoundPlayer.ImportResult result =
                    ActivationSoundPlayer.importCustomClip(appContext, uri, assistant);
            runOnUiThread(() -> {
                if (isFinishing() || (Build.VERSION.SDK_INT >= 17 && isDestroyed())) {
                    return;
                }
                Toast.makeText(
                        this,
                        tr(result.success
                                ? "Activation sound saved · max 3 seconds"
                                : "Could not import activation sound"),
                        result.success ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG)
                        .show();
                if (result.success) {
                    recreatePreservingTab();
                }
            });
        }, "MindTrigger-AudioImport").start();
    }

    @Override
    public void onBackPressed() {
        if (setupWizardActive && currentTab == TAB_SETUP) {
            if (currentSetupPage > SETUP_PAGE_PRIVILEGED) {
                setSetupWizardPage(currentSetupPage - 1, true);
            } else {
                super.onBackPressed();
            }
            return;
        }
        super.onBackPressed();
    }

@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);

    if (intent != null
            && intent.getBooleanExtra(
                    "homehold_auto_log_recovery",
                    false)) {

        armLogSessionReconnectWhenTop(true);
    }
}

@Override
protected void onStart() {
    super.onStart();

    if (termsAccepted()
            && getSharedPreferences(PREFS, MODE_PRIVATE)
            .getBoolean(PREF_ENABLED, false)) {
        bindWatcher();
    }
}

@Override
protected void onResume() {
    super.onResume();
    activityResumed = true;
    refreshAll();
    ui.removeCallbacks(desktopSearchPoll);
    ui.post(desktopSearchPoll);

    if (termsAccepted()
            && getSharedPreferences(PREFS, MODE_PRIVATE)
            .getBoolean(PREF_ENABLED, false)) {

        startWatcher();
        bindWatcher();

        armLogSessionReconnectWhenTop(false);
    }

    statusPollRemaining = 40;
    ui.removeCallbacks(pcDetectLoop);
    ui.post(pcDetectLoop);
}

@Override
protected void onPause() {
    activityResumed = false;
    ui.removeCallbacks(desktopSearchPoll);
    activityHasWindowFocus = false;
    ui.removeCallbacks(pcDetectLoop);
    super.onPause();
}

@Override
public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    activityHasWindowFocus = hasFocus;

    if (hasFocus && activityResumed) {
        maybeRunTopLogReconnect();
    }
}

@Override
protected void onStop() {
    unbindWatcher();
    super.onStop();
}

    @Override
    protected void onDestroy() {
        uiReady = false;
        desktopSearchIo.shutdownNow();
        Shizuku.removeBinderReceivedListener(binderListener);
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        super.onDestroy();
    }

    private int statusPollRemaining;

    private final Runnable pcDetectLoop = new Runnable() {
        @Override
        public void run() {
            refreshAll();

            int session =
                    watcherSessionState;

            boolean keepPolling =
                    !isReadLogsGranted()
                            || (isWatcherRunning()
                                && session != WatcherIpc.STATE_ACTIVE);

            if (watcherBound) {
                sendWatcherMessage(WatcherIpc.MSG_REQUEST_STATE, null, false);
            }

            if (statusPollRemaining-- > 0
                    && keepPolling) {
                ui.postDelayed(this, 500L);
            }
        }
    };

    private View buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(surface);

        contentHost = new FrameLayout(this);

        setupPage = setupWizardActive ? buildSetupPage() : buildMainSetupPage();
        advancedPage = buildAdvancedPage();
        betaPage = buildBetaPage();
        supportPage = buildSupportPage();
        aboutPage = buildAboutPage();

        contentHost.addView(setupPage, matchFrame());
        contentHost.addView(advancedPage, matchFrame());
        contentHost.addView(betaPage, matchFrame());
        contentHost.addView(supportPage, matchFrame());
        contentHost.addView(aboutPage, matchFrame());

        root.addView(contentHost, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f));

        bottomNav = new BottomNavigationView(this);
        bottomNav.setBackgroundColor(surfaceContainer);
        bottomNav.setItemIconTintList(makeNavColorStateList());
        bottomNav.setItemTextColor(makeNavColorStateList());
        bottomNav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        bottomNav.setItemActiveIndicatorEnabled(true);
        bottomNav.setItemActiveIndicatorColor(
                ColorStateList.valueOf(primaryContainer));
        bottomNav.setElevation(0f);
        bottomNav.setTranslationZ(0f);

        Menu menu = bottomNav.getMenu();
        menu.add(Menu.NONE, TAB_SETUP, Menu.NONE, tr("Setup"))
                .setIcon(R.drawable.ic_setup);
        menu.add(Menu.NONE, TAB_ADVANCED, Menu.NONE, tr("Advanced"))
                .setIcon(R.drawable.ic_advanced);
        menu.add(Menu.NONE, TAB_BETA, Menu.NONE, tr("Beta"))
                .setIcon(R.drawable.ic_extension);
        menu.add(Menu.NONE, TAB_SUPPORT, Menu.NONE, tr("Support me"))
                .setIcon(R.drawable.ic_support);
        menu.add(Menu.NONE, TAB_ABOUT, Menu.NONE, tr("About"))
                .setIcon(R.drawable.ic_about);

        bottomNav.setOnItemSelectedListener(item -> {
            // NavigationBarView invokes this listener before its internal selected
            // item state is guaranteed to be committed. Calling showTab() here
            // would programmatically call setSelectedItemId() again and recurse.
            showTabFromBottomNavigation(item.getItemId());
            return true;
        });
        bottomNav.setSelectedItemId(currentTab);

        root.addView(bottomNav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        return root;
    }

    private View buildSetupPage() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(surface);
        screen.setPadding(dp(22), dp(10), dp(22), 0);

        // Android/OOBE-style setup: no app bottom navigation and no Close button.
        setupWizardEyebrow = sectionEyebrow("SETUP · PAGE 1 OF 5");
        screen.addView(setupWizardEyebrow,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        setupWizardTitle = text("Shizuku setup", 25, onSurface, Typeface.BOLD);
        setupWizardTitle.setLineSpacing(0, 1.01f);
        screen.addView(setupWizardTitle, margins(0, 14, 0, 0));

        setupWizardHint = supporting(
                "Complete one stage at a time. ColorGoogle remembers this page when Android or ColorOS Settings recreates the app.");
        setupWizardHint.setTextSize(15);
        screen.addView(setupWizardHint, margins(0, 6, 0, 0));

        LinearLayout progress = new LinearLayout(this);
        progress.setOrientation(LinearLayout.HORIZONTAL);
        progress.setGravity(Gravity.CENTER_VERTICAL);
        setupProgressSegments.clear();
        for (int i = 0; i < SETUP_PAGE_COUNT; i++) {
            View segment = new View(this);
            setupProgressSegments.add(segment);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(4), 1f);
            if (i > 0) lp.leftMargin = dp(6);
            progress.addView(segment, lp);
        }
        screen.addView(progress, margins(0, 16, 0, 16));

        setupScroll = pageScroll();
        setupScroll.setFillViewport(true);
        setupScroll.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout scrollRoot = column();
        scrollRoot.setPadding(0, 0, 0, dp(14));
        setupWizardHost = new FrameLayout(this);
        setupWizardPages.clear();

        addSetupWizardPage(page -> addStep1(page));
        addSetupWizardPage(page -> addGestureRequirement(page));
        addSetupWizardPage(page -> addStep2(page));
        addSetupWizardPage(page -> addStep3(page));
        addSetupWizardPage(page -> {
            // StableUI R3: use the original V5 Run card and its untouched
            // click handler. The wizard footer never starts runtime code.
            addRunCard(page);
        });

        scrollRoot.addView(setupWizardHost,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        setupScroll.addView(scrollRoot,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        screen.addView(setupScroll,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f));

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dp(10), 0, dp(14));

        setupBackButton = textButton("Back");
        setupBackButton.setOnClickListener(v ->
                setSetupWizardPage(currentSetupPage - 1, true));
        footer.addView(setupBackButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        footer.addView(new View(this),
                new LinearLayout.LayoutParams(0, 1, 1f));

        setupNextButton = filledButton("Continue");
        setupNextButton.setMinWidth(dp(140));
        setupNextButton.setOnClickListener(v -> {
            if (currentSetupPage < SETUP_PAGE_RUN) {
                if (!isCurrentSetupPageComplete()) {
                    Toast.makeText(this,
                            tr("Complete this step before continuing."),
                            Toast.LENGTH_LONG).show();
                    refreshAll();
                    updateSetupWizardChrome();
                    return;
                }
                setSetupWizardPage(currentSetupPage + 1, true);
                return;
            }

            String missing = firstMissingRequiredStep();
            if (missing != null) {
                Toast.makeText(this, missing, Toast.LENGTH_LONG).show();
                refreshAll();
                updateSetupWizardChrome();
                return;
            }

            // StableUI R3: footer is navigation only. Runtime is started
            // exclusively by the original V5 Run button above. This prevents
            // wizard/UI lifecycle from entering the watcher start path.
            boolean watcherReady = isWatcherRunning()
                    && watcherSessionState == WatcherIpc.STATE_ACTIVE;
            if (!watcherReady) {
                Toast.makeText(
                        this,
                        tr("Run ColorGoogle above and approve Android's device-log access first."),
                        Toast.LENGTH_LONG).show();
                refreshAll();
                updateSetupWizardChrome();
                return;
            }

            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_SETUP_UI_FINISHED, true)
                    .putInt(PREF_SETUP_UI_PAGE, SETUP_PAGE_RUN)
                    .commit();

            // Rebuild only the Activity UI. Runtime is already ACTIVE and the
            // V5 watcher/service path is untouched. The rebuilt Setup tab is
            // the permanent compact scroll page with bottom navigation.
            setupWizardActive = false;
            currentTab = TAB_SETUP;
            recreatePreservingTab();
        });
        footer.addView(setupNextButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        screen.addView(footer,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        setSetupWizardPage(currentSetupPage, false);
        return screen;
    }

    /**
     * Permanent post-onboarding Setup tab. It deliberately reuses the same
     * action builders/verifiers as the one-time wizard so state and behavior
     * cannot drift between onboarding and the normal app UI.
     */
    private View buildMainSetupPage() {
        ScrollView scroll = pageScroll();
        LinearLayout root = pageRoot();

        addTopBar(root, "Setup", "Setup & health");

        TextView intro = supporting(
                "Manage setup and fix warnings here. The first-run guide will not return.");
        root.addView(intro, margins(0, 0, 0, 14));

        // Reuse the exact same sections/backends as onboarding. Because
        // buildingSetupWizard is false, each section renders in its normal
        // compact card form suitable for a scrollable app tab.
        addStep1(root);
        addGestureRequirement(root);
        addStep2(root);
        addStep3(root);
        addRunCard(root);
        root.addView(buildColorOsPermissionMonitoringWarning(),
                margins(0, 0, 0, UI_CARD_GAP_DP));

        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private interface SetupPageBuilder {
        void build(LinearLayout root);
    }

    private void addSetupWizardPage(SetupPageBuilder builder) {
        LinearLayout page = column();
        buildingSetupWizard = true;
        try {
            builder.build(page);
        } finally {
            buildingSetupWizard = false;
        }
        page.setVisibility(View.GONE);
        setupWizardPages.add(page);
        setupWizardHost.addView(page,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void setSetupWizardPage(int page, boolean scrollToTop) {
        int clamped = Math.max(SETUP_PAGE_PRIVILEGED,
                Math.min(SETUP_PAGE_RUN, page));
        currentSetupPage = clamped;

        // commit(), not apply(): ColorOS may kill this Activity immediately
        // after we launch a Settings screen.
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putInt(PREF_SETUP_UI_PAGE, currentSetupPage)
                .commit();

        updateSetupWizardChrome();
        if (scrollToTop && setupScroll != null) {
            setupScroll.post(() -> setupScroll.scrollTo(0, 0));
        }
    }

    private void updateSetupWizardChrome() {
        if (setupWizardPages.isEmpty()) return;

        for (int i = 0; i < setupWizardPages.size(); i++) {
            setupWizardPages.get(i).setVisibility(
                    i == currentSetupPage ? View.VISIBLE : View.GONE);
        }

        String[] titles = {
                "Shizuku setup",
                "ColorOS gesture",
                "Background access",
                "Google & Gemini",
                "Ready to run"
        };
        String[] hints = {
                "Run the privileged bootstrap first. Shizuku is only needed for setup, not normal runtime.",
                "Enable and verify the ColorOS long-press entry point used by ColorGoogle.",
                "Configure overlay and retention settings used by the stable V5 watcher.",
                "Verify Google and Gemini are actually unrestricted in Android battery settings.",
                "Review the final state, start ColorGoogle, then approve Android device-log access."
        };

        if (setupWizardEyebrow != null) {
            setupWizardEyebrow.setText(tr("SETUP · PAGE ")
                    + (currentSetupPage + 1)
                    + " / " + SETUP_PAGE_COUNT);
        }
        if (setupWizardTitle != null) {
            setupWizardTitle.setText(tr(titles[currentSetupPage]));
        }
        if (setupWizardHint != null) {
            setupWizardHint.setText(tr(hints[currentSetupPage]));
        }

        for (int i = 0; i < setupProgressSegments.size(); i++) {
            int color = i <= currentSetupPage ? primary : surfaceContainerHigh;
            setupProgressSegments.get(i).setBackground(roundRect(color, 6));
        }

        if (setupBackButton != null) {
            boolean canBack = currentSetupPage > SETUP_PAGE_PRIVILEGED;
            setupBackButton.setVisibility(canBack ? View.VISIBLE : View.INVISIBLE);
            setupBackButton.setEnabled(canBack);
        }

        if (setupNextButton != null) {
            boolean finalPage = currentSetupPage == SETUP_PAGE_RUN;
            boolean complete = isCurrentSetupPageComplete();
            boolean watcherReady = isWatcherRunning()
                    && watcherSessionState == WatcherIpc.STATE_ACTIVE;
            setupNextButton.setEnabled(finalPage ? watcherReady : complete);
            setupNextButton.setAlpha(setupNextButton.isEnabled() ? 1f : 0.45f);
            setupNextButton.setText(tr(finalPage ? "Finish" : "Continue"));
        }
    }

    private boolean isCurrentSetupPageComplete() {
        switch (currentSetupPage) {
            case SETUP_PAGE_PRIVILEGED:
                return isReadLogsGranted();
            case SETUP_PAGE_GESTURE:
                return pref(PREF_GESTURE_CONFIRMED);
            case SETUP_PAGE_BACKGROUND:
                return Settings.canDrawOverlays(this)
                        && pref(PREF_RECENTS_LOCKED);
            case SETUP_PAGE_GOOGLE:
                boolean googleInstalled = packageExists(SetupCommands.GOOGLE);
                boolean geminiInstalled = packageExists(SetupCommands.GEMINI);
                boolean assistantOk = !googleInstalled || isGoogleAssistantSelected();
                boolean googleBatteryOk = !googleInstalled
                        || isIgnoringBattery(SetupCommands.GOOGLE);
                boolean geminiBatteryOk = !geminiInstalled
                        || isIgnoringBattery(SetupCommands.GEMINI);
                boolean autoLaunchOk = (!googleInstalled && !geminiInstalled)
                        || (pref(PREF_GOOGLE_AUTOSTART)
                        && pref(PREF_GEMINI_AUTOSTART));
                return assistantOk
                        && googleBatteryOk
                        && geminiBatteryOk
                        && autoLaunchOk;
            case SETUP_PAGE_RUN:
            default:
                return firstMissingRequiredStep() == null;
        }
    }

    private void updatePrivilegedActionFeedback(String state) {
        if (privilegedActionFeedback == null || state == null) return;
        String message;
        int color = onSurfaceVariant;
        switch (state) {
            case "RUNNING_SETUP":
                message = "Running privileged setup…";
                color = primary;
                break;
            case "REQUESTING_SHIZUKU":
                message = "Waiting for Shizuku permission…";
                color = primary;
                break;
            case "READY":
                message = "✓ Privileged setup completed.";
                color = success;
                break;
            case "READY_WITH_WARNINGS":
                message = "⚠ Setup completed with warnings. Review the command log in Advanced.";
                color = warning;
                break;
            case "SHIZUKU_NOT_RUNNING":
                message = "Shizuku is not running. Start Shizuku, then try again.";
                color = error;
                break;
            case "SHIZUKU_TOO_OLD":
                message = "Shizuku is too old for this setup path.";
                color = error;
                break;
            case "SHIZUKU_DENIED":
                message = "Shizuku permission was denied.";
                color = error;
                break;
            case "READ_LOGS_NOT_GRANTED":
                message = "Setup ran, but READ_LOGS is still not granted.";
                color = error;
                break;
            case "BOOTSTRAP_ERROR":
            case "BOOTSTRAP_FAILED":
                message = "Privileged setup failed. Review the command log in Advanced.";
                color = error;
                break;
            default:
                message = state.isEmpty() ? "Waiting for setup." : state;
                break;
        }
        privilegedActionFeedback.setText(tr(message));
        privilegedActionFeedback.setTextColor(color);
        privilegedActionFeedback.setVisibility(View.VISIBLE);
        updateSetupWizardChrome();
    }

    private void setGestureActionFeedback(String message, boolean ok) {
        if (gestureActionFeedback == null) return;
        gestureActionFeedback.setText(tr(message));
        gestureActionFeedback.setTextColor(ok ? success : onSurfaceVariant);
        gestureActionFeedback.setVisibility(View.VISIBLE);
    }

    private void attachSetupSection(
            LinearLayout root,
            MaterialCardView shell,
            LinearLayout body,
            int bottomMarginDp) {
        if (buildingSetupWizard) {
            body.setPadding(0, 0, 0, 0);
            root.addView(body, margins(0, 0, 0, bottomMarginDp));
        } else {
            shell.addView(body);
            root.addView(shell, margins(0, 0, 0, bottomMarginDp));
        }
    }

    private View buildAdvancedPage() {
        ScrollView scroll = pageScroll();
        LinearLayout root = pageRoot();

        addTopBar(root, "Advanced", "Runtime & diagnostics");

        MaterialCardView runtime = sectionCard();
        LinearLayout runtimeBody = sectionBody();

        TextView title = text("Runtime", 20, onSurface, Typeface.BOLD);
        runtimeBody.addView(title);

        TextView desc = supporting(
                "Adjust activation delay, haptic feedback and the local success sound used by " +
                "Circle to Search and the Assistant session. Changes apply immediately.");
        runtimeBody.addView(desc, margins(0, 4, 0, 12));

        int current = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(PREF_CTS_DELAY_MS, DEFAULT_CTS_DELAY_MS);
        current = Math.max(0, Math.min(MAX_CTS_DELAY_MS, current));

        TextView delayTitle = miniTitle("Activation delay");
        runtimeBody.addView(delayTitle);

        TextView delayValue = supporting(current + " ms");
        runtimeBody.addView(delayValue, margins(0, 3, 0, 2));

        Slider slider = new Slider(this);
        slider.setValueFrom(0f);
        slider.setValueTo(MAX_CTS_DELAY_MS);
        slider.setStepSize(25f);
        slider.setValue(current);
        slider.addOnChangeListener((s, value, fromUser) -> {
            int v = Math.round(value);
            delayValue.setText(v + " ms");
            if (fromUser) {
                getSharedPreferences(PREFS, MODE_PRIVATE)
                        .edit().putInt(PREF_CTS_DELAY_MS, v).apply();
                syncWatcherPrefs();
            }
        });
        runtimeBody.addView(slider, margins(0, 2, 0, 8));

        MaterialSwitch vibration = new MaterialSwitch(this);
        vibration.setText(tr("Vibrate on activation"));
        vibration.setTextColor(onSurface);
        vibration.setChecked(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_VIBRATE_ON_CTS, DEFAULT_VIBRATE_ON_CTS));
        vibration.setOnCheckedChangeListener((button, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_VIBRATE_ON_CTS, checked)
                    .apply();
            syncWatcherPrefs();
        });
        runtimeBody.addView(vibration);

        MaterialSwitch activationSound = new MaterialSwitch(this);
        activationSound.setText(tr("Activation sound"));
        activationSound.setTextColor(onSurface);
        activationSound.setChecked(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_SOUND_ON_ACTIVATION, DEFAULT_SOUND_ON_ACTIVATION));
        activationSound.setOnCheckedChangeListener((button, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_SOUND_ON_ACTIVATION, checked)
                    .apply();
            syncWatcherPrefs();
        });
        runtimeBody.addView(activationSound, margins(0, 4, 0, 0));

        runtime.addView(runtimeBody);
        root.addView(runtime, margins(0, 0, 0, 14));

        MaterialCardView triggers = sectionCard();
        LinearLayout triggersBody = sectionBody();

        triggersBody.addView(text(
                "Trigger routing (experimental)",
                20,
                onSurface,
                Typeface.BOLD));

        TextView triggerDesc = supporting(
                "The isolated watcher uses one activation pipeline and routes by trigger: " +
                "Home/gesture → Circle to Search; Power → Assistant voice session. " +
                "Experimental voice wake and action swapping live in the Beta tab.");
        triggersBody.addView(triggerDesc, margins(0, 4, 0, 12));

        MaterialSwitch powerGemini = new MaterialSwitch(this);
        powerGemini.setText(tr("Power long press → Assistant voice session"));
        powerGemini.setTextColor(onSurface);
        powerGemini.setChecked(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(
                        PREF_POWER_GEMINI_EXPERIMENTAL,
                        DEFAULT_POWER_GEMINI_EXPERIMENTAL));
        powerGemini.setOnCheckedChangeListener((button, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_POWER_GEMINI_EXPERIMENTAL, checked)
                    .apply();
            syncWatcherPrefs();
        });
        triggersBody.addView(powerGemini);

        TextView recoveryBuiltIn = supporting(
                "Automatic recovery uses a transparent bridge Activity. If an active privileged logcat session is lost, the bridge briefly places the app UID in a foreground state so Android can present its own device-log access confirmation.");
        recoveryBuiltIn.setTextColor(success);
        triggersBody.addView(recoveryBuiltIn, margins(0, 10, 0, 10));

        TextView assistantTransportDesc = supporting(
                "Both stable routes use the same delay and haptic settings, then call the Android " +
                "VoiceInteractionManager binder interface. Power requests the active Assistant voice session; " +
                "Home/gesture sends the CTS-specific invocation bundle.");
        triggersBody.addView(assistantTransportDesc, margins(0, 6, 0, 10));

        TextView auraDesc = supporting(
                "Activation feedback uses bundled local PCM audio cues after a successful " +
                "request. No activation audio is downloaded or streamed.");
        triggersBody.addView(auraDesc, margins(0, 0, 0, 10));

        triggers.addView(triggersBody);
        root.addView(triggers, margins(0, 0, 0, 14));

        MaterialCardView shell = sectionCard();
        LinearLayout shellBody = sectionBody();

        shellBody.addView(text("Privileged setup", 20, onSurface, Typeface.BOLD));

        TextView shellDesc = supporting(
                "Re-run privileged setup to restore READ_LOGS and reapply the configured AppOps/background-execution settings.");
        shellBody.addView(shellDesc, margins(0, 4, 0, 12));

        MaterialButton rerun = filledTonalButton("Run again with Shizuku");
        rerun.setOnClickListener(v -> bootstrap.runNow());
        shellBody.addView(rerun);

        MaterialButton copyPc = outlinedButton("Copy PC one-shot");
        copyPc.setOnClickListener(v -> copy(
                "ColorGoogle one-shot",
                SetupCommands.pcOneShot(getPackageName())));
        shellBody.addView(copyPc, margins(0, 8, 0, 0));

        shellBody.addView(miniTitle("Last command execution"), margins(0, 16, 0, 4));

        commandTimeView = supporting(formatLastExecutionTime());
        commandTimeView.setTextColor(onSurface);
        shellBody.addView(commandTimeView, margins(0, 0, 0, 8));

        shellBody.addView(miniTitle("Last shell result"), margins(0, 4, 0, 4));

        commandLogView = text(
                bootstrap.getLastCommandLog(),
                11,
                onSurfaceVariant,
                Typeface.NORMAL);
        commandLogView.setTypeface(Typeface.MONOSPACE);
        commandLogView.setTextIsSelectable(true);
        commandLogView.setPadding(dp(14), dp(12), dp(14), dp(12));
        commandLogView.setBackground(roundRect(surfaceContainerHigh, 18));
        shellBody.addView(commandLogView);

        MaterialButton copyLog = outlinedButton("Copy result");
        copyLog.setOnClickListener(v -> copy(
                "ColorGoogle shell result",
                commandLogView.getText().toString()));
        shellBody.addView(copyLog, margins(0, 8, 0, 0));

        shell.addView(shellBody);
        root.addView(shell, margins(0, 0, 0, 14));

        MaterialCardView maintenance = sectionCard();
        LinearLayout maintenanceBody = sectionBody();

        maintenanceBody.addView(text("Maintenance", 20, onSurface, Typeface.BOLD));

        TextView mDesc = supporting(
                "Reset only the OEM settings confirmations stored by ColorGoogle.");
        maintenanceBody.addView(mDesc, margins(0, 4, 0, 12));

        MaterialButton reset = outlinedButton("Reset manual confirmations");
        reset.setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .remove(PREF_GESTURE_CONFIRMED)
                    .remove(PREF_RECENTS_LOCKED)
                    .remove(PREF_GOOGLE_AUTOSTART)
                    .remove(PREF_GEMINI_AUTOSTART)
                    .apply();
            refreshAll();
            showTab(TAB_SETUP);
        });
        maintenanceBody.addView(reset);

        maintenance.addView(maintenanceBody);
        root.addView(maintenance);

        scroll.addView(root);
        return scroll;
    }


    private View buildBetaPage() {
        ScrollView scroll = pageScroll();
        LinearLayout root = pageRoot();

        addTopBar(root, "Beta", "Experimental features");

        MaterialCardView intro = sectionCard();
        LinearLayout introBody = sectionBody();
        introBody.addView(text("V" + BuildConfig.VERSION_NAME, 20, onSurface, Typeface.BOLD));
        introBody.addView(supporting(
                "Try optional features without changing background protection."),
                margins(0, 4, 0, 0));
        intro.addView(introBody);
        root.addView(intro, margins(0, 0, 0, 14));

        root.addView(buildDesktopSearchCard(), margins(0, 0, 0, 14));
        root.addView(buildWirelessRecoveryCard(), margins(0, 0, 0, 14));

        MaterialCardView animation = sectionCard();
        LinearLayout animationBody = sectionBody();
        animationBody.addView(text("Slow-motion transitions", 20, onSurface, Typeface.BOLD));
        animationBody.addView(supporting(
                "Animate tab changes with fade, zoom and movement. This affects UI presentation only."),
                margins(0, 4, 0, 10));

        MaterialSwitch slowMotion = new MaterialSwitch(this);
        slowMotion.setText(tr("Enable slow-motion animation"));
        slowMotion.setTextColor(onSurface);
        slowMotion.setChecked(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_BETA_SLOW_ANIMATIONS, DEFAULT_BETA_SLOW_ANIMATIONS));
        slowMotion.setOnCheckedChangeListener((button, checked) ->
                getSharedPreferences(PREFS, MODE_PRIVATE)
                        .edit().putBoolean(PREF_BETA_SLOW_ANIMATIONS, checked).apply());
        animationBody.addView(slowMotion);
        animation.addView(animationBody);
        root.addView(animation, margins(0, 0, 0, 14));

        MaterialCardView voice = sectionCard();
        LinearLayout voiceBody = sectionBody();
        voiceBody.addView(text("Voice wake", 20, onSurface, Typeface.BOLD));
        voiceBody.addView(supporting(
                "Choose what happens after ColorOS detects voice wake."),
                margins(0, 4, 0, 10));

        MaterialSwitch voiceWake = new MaterialSwitch(this);
        voiceWake.setText(tr("Wakeup with voice"));
        voiceWake.setTextColor(onSurface);
        voiceWake.setChecked(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_VOICE_WAKE_ASSISTANT_EXPERIMENTAL,
                        DEFAULT_VOICE_WAKE_ASSISTANT_EXPERIMENTAL));
        voiceWake.setOnCheckedChangeListener((button, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit().putBoolean(PREF_VOICE_WAKE_ASSISTANT_EXPERIMENTAL, checked).apply();
            syncWatcherPrefs();
        });
        voiceBody.addView(voiceWake);
        voice.addView(voiceBody);
        root.addView(voice, margins(0, 0, 0, 14));

        MaterialCardView quickRecovery = sectionCard();
        LinearLayout quickRecoveryBody = sectionBody();
        quickRecoveryBody.addView(text("Quick Settings log recovery", 20, onSurface, Typeface.BOLD));
        quickRecoveryBody.addView(supporting(
                "Add a tile to check the log session. It does nothing when the session is healthy."),
                margins(0, 4, 0, 10));

        MaterialSwitch quickRecoveryToggle = new MaterialSwitch(this);
        quickRecoveryToggle.setText(tr("Show log recovery tile"));
        quickRecoveryToggle.setTextColor(onSurface);
        quickRecoveryToggle.setChecked(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_BETA_QUICK_SETTINGS_LOG_RECOVERY,
                        DEFAULT_BETA_QUICK_SETTINGS_LOG_RECOVERY));
        quickRecoveryToggle.setOnCheckedChangeListener((button, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_BETA_QUICK_SETTINGS_LOG_RECOVERY, checked)
                    .apply();
            setLogSessionRecoveryTileEnabled(checked);
        });
        quickRecoveryBody.addView(quickRecoveryToggle);
        quickRecoveryBody.addView(supporting(
                "Then add ColorGoogle from the phone's Quick Settings editor."),
                margins(0, 0, 0, 0));
        quickRecovery.addView(quickRecoveryBody);
        root.addView(quickRecovery, margins(0, 0, 0, 14));

        MaterialCardView sounds = sectionCard();
        LinearLayout soundsBody = sectionBody();
        soundsBody.addView(text("Custom activation sounds", 20, onSurface, Typeface.BOLD));
        soundsBody.addView(supporting(
                "Choose separate local audio for Circle to Search and Google Assistant. Playback is hard-limited to 3 seconds; compatible files are clipped during import."),
                margins(0, 4, 0, 12));
        soundsBody.addView(buildActivationSoundPicker(false));
        soundsBody.addView(buildActivationSoundPicker(true), margins(0, 10, 0, 0));
        sounds.addView(soundsBody);
        root.addView(sounds, margins(0, 0, 0, 14));

        MaterialCardView swap = sectionCard();
        LinearLayout swapBody = sectionBody();
        swapBody.addView(text("Swap CTS and Assistant", 20, onSurface, Typeface.BOLD));
        swapBody.addView(supporting(
                "Swap the Home/gesture and Power long-press actions: Home/gesture opens Assistant, while Power long press opens Circle to Search. Voice wake is not swapped."),
                margins(0, 4, 0, 10));

        MaterialSwitch swapActions = new MaterialSwitch(this);
        swapActions.setText(tr("Swap CTS ↔ Google Assistant"));
        swapActions.setTextColor(onSurface);
        swapActions.setChecked(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_SWAP_CTS_ASSISTANT_EXPERIMENTAL,
                        DEFAULT_SWAP_CTS_ASSISTANT_EXPERIMENTAL));
        swapActions.setOnCheckedChangeListener((button, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit().putBoolean(PREF_SWAP_CTS_ASSISTANT_EXPERIMENTAL, checked).apply();
            syncWatcherPrefs();
        });
        swapBody.addView(swapActions);
        swap.addView(swapBody);
        root.addView(swap);

        scroll.addView(root);
        return scroll;
    }

    private View buildDesktopSearchCard() {
        MaterialCardView card = sectionCard();
        LinearLayout body = sectionBody();
        body.addView(text("Desktop search", 20, onSurface, Typeface.BOLD));
        body.addView(supporting("Open Google Search from the launcher search button. Enabling closes old system-search pages; disabling restores the original route."),
                margins(0, 4, 0, 10));
        desktopSearchToggle = new MaterialSwitch(this);
        desktopSearchToggle.setText("Open Google from desktop search");
        desktopSearchToggle.setTextColor(onSurface);
        desktopSearchToggle.setChecked(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(DesktopSearchBridge.PREF, false));
        desktopSearchToggle.setOnCheckedChangeListener((button, checked) -> {
            if (!desktopSearchRendering) refreshDesktopSearch(checked);
        });
        body.addView(desktopSearchToggle);
        desktopSearchStatus = supporting("Checking the ADB agent…");
        body.addView(desktopSearchStatus, margins(0, 6, 0, 10));
        body.addView(supporting("The ADB agent also listens for wake triggers and can run after USB is disconnected. This switch controls desktop search only. Restart a stopped agent with ADB or enable wireless recovery below. Do not run another activity monitor at the same time."));
        MaterialButton command = compactButton("Copy ADB activation command");
        command.setOnClickListener(v -> {
            copy("ColorGoogle desktop search", DesktopSearchActivation.command());
            Toast.makeText(this, "Copied. Replace SERIAL with your device serial and run in a macOS/Linux terminal. Windows: use the documented script.", Toast.LENGTH_SHORT).show();
        });
        body.addView(command, margins(0, 10, 0, 0));
        card.addView(body);
        return card;
    }

    private View buildWirelessRecoveryCard() {
        MaterialCardView card = sectionCard();
        LinearLayout body = sectionBody();
        body.addView(text("Wireless ADB recovery", 20, onSurface, Typeface.BOLD));
        body.addView(supporting("After pairing, the phone can restart a stopped agent. Wi-Fi and wireless debugging must remain enabled. Recovery cannot work when wireless debugging is off or pairing is revoked."));
        wirelessToggle = new MaterialSwitch(this);
        wirelessToggle.setText("Recover automatically when the agent exits");
        wirelessToggle.setTextColor(onSurface);
        wirelessToggle.setOnCheckedChangeListener((button, enabled) -> {
            if (wirelessRendering) return;
            Bundle data = new Bundle(); data.putBoolean("enabled", enabled);
            wirelessRequest("enable", data);
        });
        body.addView(wirelessToggle);
        wirelessStatus = supporting("Reading recovery status…");
        body.addView(wirelessStatus, margins(0, 8, 0, 8));
        MaterialButton pair = compactButton("Start pairing");
        pair.setOnClickListener(v -> {
            if (!getSystemService(android.app.NotificationManager.class).areNotificationsEnabled()) {
                Toast.makeText(this, "Allow ColorGoogle notifications first so you can reply with the code while the system pairing window stays open.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName()));
                return;
            }
            wirelessRequest("prepare_pair", null);
            Toast.makeText(this, "Open Wireless debugging → Pair device with pairing code. Expand the ColorGoogle notification and reply with the 6-digit code.", Toast.LENGTH_LONG).show();
            openWirelessSettings();
        });
        body.addView(pair, margins(0, 6, 0, 0));
        MaterialButton settings = compactButton("Wireless debugging settings");
        settings.setOnClickListener(v -> openWirelessSettings());
        body.addView(settings, margins(0, 6, 0, 0));
        MaterialButton retry = compactButton("Retry recovery");
        retry.setOnClickListener(v -> wirelessRequest("retry", null));
        body.addView(retry, margins(0, 6, 0, 0));
        MaterialButton forget = compactButton("Delete local pairing key");
        forget.setOnClickListener(v -> new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Delete the ColorGoogle pairing key?")
                .setMessage("Recovery will be disabled. You can also remove the ColorGoogle entry in system wireless debugging settings. Pair again before using recovery.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> wirelessRequest("forget", null)).show());
        body.addView(forget, margins(0, 6, 0, 0));
        card.addView(body); return card;
    }

    private void openWirelessSettings() {
        Intent intent = new Intent("android.settings.WIRELESS_DEBUGGING_SETTINGS");
        if (intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
        else {
            Toast.makeText(this, "Open Wireless debugging in Developer options.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        }
    }

    private void wirelessRequest(String method, Bundle data) {
        if (wirelessStatus == null || isDestroyed()) return;
        desktopSearchIo.execute(() -> {
            Bundle result;
            try { result = getContentResolver().call(WirelessRecoveryBridge.URI, method, null, data); }
            catch (Exception e) {
                runOnUiThread(() -> { if (!isDestroyed()) wirelessStatus.setText("Recovery service unavailable: " + e.getClass().getSimpleName()); });
                return;
            }
            runOnUiThread(() -> {
                if (isDestroyed() || result == null) return;
                wirelessRendering = true;
                wirelessToggle.setChecked(result.getBoolean("enabled"));
                wirelessRendering = false;
                wirelessStatus.setText(result.getString("status", "") + "\n"
                        + (result.getBoolean("paired") ? "Paired locally" : "Not paired")
                        + " · Successful recoveries: " + result.getInt("recoveries"));
            });
        });
    }

    private void refreshDesktopSearch(Boolean requested) {
        if (desktopSearchToggle == null || desktopSearchBusy || isDestroyed()) return;
        desktopSearchBusy = true;
        desktopSearchToggle.setEnabled(false);
        desktopSearchIo.execute(() -> {
            boolean desired = getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getBoolean(DesktopSearchBridge.PREF, false);
            String message;
            IDesktopSearchAgent agent = DesktopSearchBridge.agent;
            try {
                if (agent == null || !agent.asBinder().pingBinder()) {
                    if (requested != null) desired = requested;
                    message = desired ? "Not active: agent disconnected. Check recovery below or activate with computer ADB." : "Disabled; ADB agent disconnected.";
                } else {
                    Bundle state = agent.getStatus();
                    if (state.getInt("version") != BuildConfig.VERSION_CODE) {
                        if (requested != null) desired = requested;
                        message = "Agent version is outdated. Run the ADB activation command again.";
                    } else {
                        if (requested != null) state = agent.setEnabled(requested);
                        desired = state.getBoolean("enabled");
                        String error = state.getString("error", "");
                        message = (desired ? "Enabled · Agent connected" : "Disabled · Original search route restored")
                                + "\nGoogle Search requests in this agent session: " + state.getInt("redirects")
                                + "\nWake logs: " + (state.getBoolean("wakeActive") ? "Connected" : "Disconnected")
                                + (state.getString("wakeError", "").isEmpty() ? "" : " · " + state.getString("wakeError"));
                        if (!error.isEmpty()) message += "\nError: " + error;
                    }
                }
            } catch (Exception e) {
                message = "Unable to verify agent status. Check again or activate with ADB: " + e.getClass().getSimpleName();
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean(DesktopSearchBridge.PREF, desired).apply();
            final boolean checked = desired;
            final String status = message;
            runOnUiThread(() -> {
                desktopSearchBusy = false;
                if (isDestroyed()) return;
                desktopSearchRendering = true;
                desktopSearchToggle.setChecked(checked);
                desktopSearchRendering = false;
                desktopSearchToggle.setEnabled(true);
                desktopSearchStatus.setText(status);
            });
        });
    }

    private View buildActivationSoundPicker(boolean assistant) {
        MaterialCardView shell = nestedCard();
        LinearLayout block = column();
        block.setPadding(dp(16), dp(16), dp(16), dp(16));

        String title = assistant ? "Google Assistant" : "Circle to Search";
        block.addView(text(title, 15, onSurface, Typeface.BOLD));

        String description = ActivationSoundPlayer.describeCustomClip(this, assistant);
        TextView status = supporting(description);
        block.addView(status, margins(0, 4, 0, 10));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        MaterialButton choose = compactButton("Choose sound");
        choose.setOnClickListener(v -> pickActivationSound(assistant));
        actions.addView(choose);

        if (ActivationSoundPlayer.hasCustomClip(this, assistant)) {
            MaterialButton reset = compactButton("Reset");
            reset.setOnClickListener(v -> {
                ActivationSoundPlayer.resetCustomClip(this, assistant);
                recreatePreservingTab();
            });
            actions.addView(reset, margins(8, 0, 0, 0));
        }

        block.addView(actions);
        shell.addView(block);
        return shell;
    }

    private void pickActivationSound(boolean assistant) {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("audio/*");
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        try {
            startActivityForResult(
                    picker,
                    assistant ? REQUEST_PICK_ASSISTANT_SOUND : REQUEST_PICK_CTS_SOUND);
        } catch (Throwable error) {
            Toast.makeText(this, tr("Audio picker unavailable"), Toast.LENGTH_SHORT).show();
        }
    }

private View buildSupportPage() {
    ScrollView scroll = pageScroll();
    LinearLayout root = pageRoot();

    addTopBar(root, "Support me", "Support development");

    root.addView(buildDonationCard(), margins(0, 0, 0, UI_CARD_GAP_DP));

    MaterialCardView hero = sectionCard();
    LinearLayout heroBody = sectionBody();

    heroBody.addView(text(
            "Support development",
            22,
            onSurface,
            Typeface.BOLD));

    TextView desc = supporting(
            "ColorGoogle is free and open source. The most useful support is testing, reproducible bug reports, translations, sharing the project, and code contributions.");
    heroBody.addView(desc, margins(0, 6, 0, 14));

    MaterialButton profile =
            filledTonalButton("Open project repository");
    profile.setOnClickListener(v -> {
        try {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/joeyandu/ColorGoogle")));
        } catch (Throwable ignored) {}
    });
    heroBody.addView(profile);

    MaterialButton share =
            outlinedButton("Share project");
    share.setOnClickListener(v -> shareProject());
    heroBody.addView(share, margins(0, 8, 0, 0));

    MaterialButton copyHandle =
            textButton("Copy upstream developer handle");
    copyHandle.setOnClickListener(v -> {
        copy("Developer", "@EvokerUniverse");
        Toast.makeText(
                this,
                "@EvokerUniverse",
                Toast.LENGTH_SHORT).show();
    });
    heroBody.addView(copyHandle, margins(0, 6, 0, 0));

    hero.addView(heroBody);
    root.addView(hero, margins(0, 0, 0, 14));

    MaterialCardView contribution = sectionCard();
    LinearLayout contributionBody = sectionBody();
    contributionBody.addView(text(
            "Ways to help",
            20,
            onSurface,
            Typeface.BOLD));

    contributionBody.addView(
            supporting(
                    "• Test ColorOS updates and attach reproducible logs.\n" +
                    "• Improve translations and accessibility wording.\n" +
                    "• Review GPL / third-party notices before redistribution.\n" +
                    "• Share fixes without removing upstream attribution."),
            margins(0, 8, 0, 0));

    contribution.addView(contributionBody);
    root.addView(contribution, margins(0, 0, 0, 14));

    scroll.addView(root);
    return scroll;
}

private MaterialCardView buildDonationCard() {
    MaterialCardView card = sectionCard();
    card.setStrokeColor(Color.argb(
            ThemeManager.isDark(this) ? 150 : 120,
            Color.red(primary),
            Color.green(primary),
            Color.blue(primary)));

    LinearLayout body = sectionBody();
    body.addView(text("Support on Ko-fi", 22, onSurface, Typeface.BOLD));
    body.addView(supporting(
            "This link supports EvokerUniverse, the upstream MindTrigger Assist author. ColorGoogle is maintained separately by joeyandu."),
            margins(0, 6, 0, 12));

    MaterialButton open = filledButton("Open Ko-fi · evokeruniverse");
    open.setOnClickListener(v -> {
        try {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://ko-fi.com/evokeruniverse")));
        } catch (Throwable e) {
            Toast.makeText(this, tr("Unable to open Ko-fi."), Toast.LENGTH_LONG).show();
        }
    });
    body.addView(open);

    TextView url = supporting("ko-fi.com/evokeruniverse");
    url.setTextColor(primary);
    body.addView(url, margins(0, 8, 0, 0));

    card.addView(body);
    return card;
}

private View buildAboutPage() {
    ScrollView scroll = pageScroll();
    LinearLayout root = pageRoot();

    addTopBar(root, "About", "Project information");

    MaterialCardView overview = elevatedCard(primaryContainer, 28, 0f);
    LinearLayout body = column();
    body.setPadding(dp(22), dp(22), dp(22), dp(22));

    body.addView(text("ColorGoogle", 24, onPrimaryContainer, Typeface.BOLD));
    body.addView(supporting("By joeyandu, based on MindTrigger Assist v16.2.0 by EvokerUniverse. Changes include direct power wake, notifications, desktop search, ADB log monitoring and optional local wireless recovery."));

    TextView version = text(
            tr("Version") + " " + BuildConfig.VERSION_NAME,
            13,
            onPrimaryContainer,
            Typeface.BOLD);
    version.setPadding(0, dp(4), 0, dp(12));
    body.addView(version);

    TextView desc = text(
            "Open-source ColorOS compatibility utility that maps the configured Home/gesture " +
            "long press to Circle to Search on supported ColorOS CN builds.",
            14,
            onPrimaryContainer,
            Typeface.NORMAL);
    desc.setLineSpacing(0, 1.10f);
    body.addView(desc);

    overview.addView(body);
    root.addView(overview, margins(0, 0, 0, 14));

    MaterialCardView language = sectionCard();
    LinearLayout languageBody = sectionBody();

    languageBody.addView(text("Language", 20, onSurface, Typeface.BOLD));

    TextView languageDesc = supporting(
            "Select the app interface language. Only fully maintained release languages are listed.");
    languageBody.addView(languageDesc, margins(0, 6, 0, 10));

    MaterialButton languageButton = filledTonalButton(
            LanguageManager.displayName(this, LanguageManager.get(this)));
    languageButton.setOnClickListener(v -> showLanguagePicker());
    languageBody.addView(languageButton);

    TextView translationNote = supporting(
            "Translations are maintained for the supported release languages. Technical identifiers and Android API names remain untranslated where appropriate.");
    translationNote.setTextColor(warning);
    languageBody.addView(translationNote, margins(0, 10, 0, 0));

    language.addView(languageBody);
    root.addView(language, margins(0, 0, 0, 14));

    MaterialCardView appearance = sectionCard();
    LinearLayout appearanceBody = sectionBody();
    appearanceBody.addView(text("Appearance", 20, onSurface, Typeface.BOLD));

    TextView appearanceDesc = supporting(
            "Choose the Material 3 accent and display mode. The current tab is preserved when the Activity recreates.");
    appearanceBody.addView(appearanceDesc, margins(0, 6, 0, 12));

    appearanceBody.addView(miniTitle("Theme color"));
    addChromeLikeColorPicker(appearanceBody);
    appearanceBody.addView(miniTitle("Display mode"), margins(0, 16, 0, 0));


    LinearLayout modeRow1 = weightedButtonRow();
    modeRow1.addView(themeModeButton("White", ThemeManager.MODE_LIGHT), weightedButtonParams(0));
    modeRow1.addView(themeModeButton("System", ThemeManager.MODE_SYSTEM), weightedButtonParams(8));
    appearanceBody.addView(modeRow1, margins(0, 8, 0, 0));

    LinearLayout modeRow2 = weightedButtonRow();
    modeRow2.addView(themeModeButton("Dark", ThemeManager.MODE_DARK), weightedButtonParams(0));
    modeRow2.addView(themeModeButton("Night", ThemeManager.MODE_NIGHT), weightedButtonParams(8));
    appearanceBody.addView(modeRow2, margins(0, 8, 0, 8));

    TextView modeDesc = supporting(
            "White is the default. System follows the device. Dark uses Material dark surfaces; Night uses deeper near-black surfaces.");
    appearanceBody.addView(modeDesc);

    appearance.addView(appearanceBody);
    root.addView(appearance, margins(0, 0, 0, 14));

    MaterialCardView materialNotice = sectionCard();
    LinearLayout materialBody = sectionBody();

    materialBody.addView(text(
            "Material Design attribution",
            20,
            onSurface,
            Typeface.BOLD));

    materialBody.addView(
            supporting(
                    "Google Material Components and Material Icons are used under Apache License 2.0. Their license is bundled with the source and viewable in the app."),
            margins(0, 6, 0, 0));

    materialNotice.addView(materialBody);
    root.addView(materialNotice, margins(0, 0, 0, 14));

    MaterialCardView ossLicenses = sectionCard();
    LinearLayout ossBody = sectionBody();
    ossBody.addView(text("Open-source licenses", 20, onSurface, Typeface.BOLD));
    ossBody.addView(
            supporting(
                    "View the project license and bundled notices for MiCTS, Material Components, Material Icons, Shizuku API, and AndroidHiddenApiBypass."),
            margins(0, 6, 0, 10));
    MaterialButton licensesButton = filledTonalButton("View licenses");
    licensesButton.setOnClickListener(v -> showOpenSourceLicenses());
    ossBody.addView(licensesButton);
    ossLicenses.addView(ossBody);
    root.addView(ossLicenses, margins(0, 0, 0, 14));

    MaterialCardView termsCard = sectionCard();
    LinearLayout termsCardBody = sectionBody();
    termsCardBody.addView(text("Terms & Privacy", 20, onSurface, Typeface.BOLD));
    TextView termsDesc = supporting(
            "Review local log handling, privileged setup changes, Google-side data processing, redistribution notice, and warranty disclaimer.");
    termsCardBody.addView(termsDesc, margins(0, 6, 0, 10));
    MaterialButton termsButton = filledTonalButton("Review Terms & Privacy");
    termsButton.setOnClickListener(v -> showTermsDialog(false));
    termsCardBody.addView(termsButton);
    termsCard.addView(termsCardBody);
    root.addView(termsCard, margins(0, 0, 0, 14));

    MaterialCardView credits = sectionCard();
    LinearLayout creditsBody = sectionBody();

    creditsBody.addView(text("Credits", 20, onSurface, Typeface.BOLD));

    creditsBody.addView(infoPair(
            "Developer",
            "@EvokerUniverse"
    ), margins(0, 10, 0, 0));

    creditsBody.addView(infoPair(
            "Maintainer",
            "@EvokerUniverse"
    ), margins(0, 8, 0, 0));

    creditsBody.addView(infoPair(
            "AI coding assistance",
            "Chat GPT"
    ), margins(0, 8, 0, 0));

    creditsBody.addView(infoPair(
            "Package",
            BuildConfig.APPLICATION_ID
    ), margins(0, 8, 0, 0));

    credits.addView(creditsBody);
    root.addView(credits, margins(0, 0, 0, 14));

    MaterialCardView license = sectionCard();
    LinearLayout licenseBody = sectionBody();

    licenseBody.addView(text("License", 20, onSurface, Typeface.BOLD));

    TextView licenseName = text(
            "GNU General Public License v3.0",
            15,
            onSurface,
            Typeface.BOLD);
    licenseBody.addView(licenseName, margins(0, 10, 0, 3));

    TextView licenseDesc = supporting(
            "ColorGoogle is free and open-source software distributed under GPL-3.0-only. " +
            "Distribution of modified or unmodified copies must comply with the GPL, including " +
            "applicable notice and corresponding-source obligations.");
    licenseBody.addView(licenseDesc);

    TextView associationNotice = supporting(
            "Content, branding, commentary, screenshots, links, or other material added by a redistributor is solely that redistributor's responsibility and does not represent the original MindTrigger Assist author. The original author assumes no responsibility for such added material, including controversial or unrelated content. This statement is informational only and does not limit rights granted by GPL-3.0-only.");
    associationNotice.setTextColor(warning);
    licenseBody.addView(associationNotice, margins(0, 10, 0, 0));

    TextView copyright = supporting(
            "MindTrigger Assist modifications © 2026 EvokerUniverse");
    copyright.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    licenseBody.addView(copyright, margins(0, 10, 0, 0));

    TextView licenseFile = supporting(
            "The complete GPL-3.0 license text is bundled with the source and is also viewable in the app.");
    licenseBody.addView(licenseFile, margins(0, 8, 0, 0));

    license.addView(licenseBody);
    root.addView(license, margins(0, 0, 0, 14));

    MaterialCardView upstream = sectionCard();
    LinearLayout upstreamBody = sectionBody();

    upstreamBody.addView(text("Upstream", 20, onSurface, Typeface.BOLD));

    upstreamBody.addView(infoPair(
            "CTS source",
            "MiCTS"
    ), margins(0, 10, 0, 0));

    upstreamBody.addView(infoPair(
            "Upstream",
            "parallelcc"
    ), margins(0, 8, 0, 0));

    upstreamBody.addView(infoPair(
            "Upstream license",
            "GPL-3.0"
    ), margins(0, 8, 0, 0));

    TextView upstreamNote = supporting(
            "The Circle to Search invocation path was implemented with reference to MiCTS. " +
            "MindTrigger Assist adds its own ColorOS trigger detection, setup, process lifecycle, " +
            "recovery flow and user interface.");
    upstreamBody.addView(upstreamNote, margins(0, 10, 0, 0));

    upstream.addView(upstreamBody);
    root.addView(upstream, margins(0, 0, 0, 14));

    MaterialCardView architecture = sectionCard();
    LinearLayout archBody = sectionBody();

    archBody.addView(text("Architecture", 20, onSurface, Typeface.BOLD));

    TextView flow = supporting(
            "Home/gesture long press → ColorOS SpeechAssist start failure → :watcher classifier → Circle to Search");
    flow.setTypeface(Typeface.MONOSPACE);
    archBody.addView(flow, margins(0, 8, 0, 10));

    archBody.addView(supporting(
            "Shizuku or a one-time PC shell is required only for privileged setup. " +
            "Normal CTS activation runs independently afterward."));

    architecture.addView(archBody);
    root.addView(architecture, margins(0, 0, 0, 14));

    MaterialCardView notes = sectionCard();
    LinearLayout notesBody = sectionBody();

    notesBody.addView(text("Platform notes", 20, onSurface, Typeface.BOLD));

    TextView note = supporting(
            "Recent-task lock and Auto launch are OEM-managed ColorOS settings. " +
            "Their state is not exposed reliably to third-party applications and therefore " +
            "requires explicit user confirmation during setup.");
    notesBody.addView(note, margins(0, 8, 0, 0));

    notes.addView(notesBody);
    root.addView(notes);

    scroll.addView(root);
    return scroll;
}

private void showLanguagePicker() {
    String[] labels = LanguageManager.displayNames(this);
    int current = LanguageManager.indexOfCurrent(this);

    ScrollView scroll = new ScrollView(this);

    RadioGroup group = new RadioGroup(this);
    group.setOrientation(RadioGroup.VERTICAL);
    group.setPadding(dp(8), dp(4), dp(8), dp(8));

    TextView section = text(
            "Release languages",
            13,
            onSurfaceVariant,
            Typeface.BOLD);
    section.setPadding(dp(12), dp(6), dp(12), dp(6));
    group.addView(section);

    for (int i = 0; i < labels.length; i++) {
        final int index = i;

        MaterialRadioButton radio =
                new MaterialRadioButton(this);

        radio.setId(View.generateViewId());
        radio.setText(labels[i]);
        radio.setTextColor(onSurface);
        radio.setTextSize(16);
        radio.setMinHeight(dp(56));
        radio.setPadding(dp(8), 0, dp(8), 0);
        radio.setChecked(i == current);
        radio.setButtonTintList(makeRadioColorStateList());

        radio.setOnClickListener(v -> {
            LanguageManager.set(
                    this,
                    LanguageManager.codeAt(index));
            recreatePreservingTab();
        });

        group.addView(
                radio,
                new RadioGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    TextView note = supporting(
            "Only fully maintained release languages are listed.");
    note.setTextColor(warning);
    note.setPadding(dp(12), dp(10), dp(12), dp(4));
    group.addView(note);

    scroll.addView(group);

    showAppDialog(new MaterialAlertDialogBuilder(this)
            .setTitle(tr("Select language"))
            .setView(scroll)
            .setNegativeButton(tr("Cancel"), null));
}


private void showOpenSourceLicenses() {
    final String[] labels = {
            tr("ColorGoogle — GPL-3.0-only"),
            tr("MiCTS — GPL-3.0"),
            tr("Material Components / Material Icons — Apache-2.0"),
            tr("Shizuku API — MIT"),
            tr("AndroidHiddenApiBypass — Apache-2.0"),
            tr("Audio asset provenance")
    };

    final String[] titles = {
            "ColorGoogle — GPL-3.0-only",
            "MiCTS — GPL-3.0",
            "Apache License 2.0",
            "Shizuku API — MIT",
            "AndroidHiddenApiBypass — Apache-2.0",
            "Audio asset provenance"
    };
    final int[] notices = {
            R.raw.license_gpl_3_0,
            R.raw.notice_micts,
            R.raw.license_apache_2_0,
            R.raw.license_shizuku_api_mit,
            R.raw.notice_hidden_api_bypass,
            R.raw.notice_audio_provenance
    };

    LinearLayout list = column();
    list.setPadding(dp(8), 0, dp(8), 0);
    for (int i = 0; i < labels.length; i++) {
        final int index = i;
        TextView row = text(labels[i], 16, onSurface, Typeface.NORMAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinHeight(dp(56));
        row.setPadding(dp(12), dp(6), dp(12), dp(6));
        row.setBackground(roundRect(surfaceContainerHigh, 14));
        row.setClickable(true);
        row.setOnClickListener(v -> showBundledNotice(titles[index], notices[index]));
        list.addView(row, margins(0, i == 0 ? 0 : 8, 0, 0));
    }

    ScrollView scroll = new ScrollView(this);
    scroll.addView(list);
    showAppDialog(new MaterialAlertDialogBuilder(this)
            .setTitle(tr("Open-source licenses"))
            .setView(scroll)
            .setPositiveButton(tr("Close"), null));
}

private void showBundledNotice(String title, int rawResId) {
    TextView content = text(readRawText(rawResId), 13, onSurface, Typeface.NORMAL);
    content.setTypeface(Typeface.MONOSPACE);
    content.setTextIsSelectable(true);
    content.setPadding(dp(18), dp(10), dp(18), dp(18));

    ScrollView scroll = new ScrollView(this);
    scroll.addView(content);

    showAppDialog(new MaterialAlertDialogBuilder(this)
            .setTitle(tr(title))
            .setView(scroll)
            .setPositiveButton(tr("Close"), null));
}

private String readRawText(int rawResId) {
    StringBuilder out = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(getResources().openRawResource(rawResId)))) {
        String line;
        while ((line = reader.readLine()) != null) {
            if (out.length() > 0) out.append('\n');
            out.append(line);
        }
    } catch (Throwable e) {
        return tr("Unable to read bundled notice.");
    }
    return out.toString();
}

private View infoPair(String label, String value) {
    MaterialCardView shell = nestedCard();

    LinearLayout row = column();
    row.setPadding(dp(16), dp(13), dp(16), dp(13));

    TextView left = text(
            label,
            13,
            onSurfaceVariant,
            Typeface.NORMAL);

    TextView right = text(
            value,
            15,
            onSurface,
            Typeface.BOLD);
    right.setPadding(0, dp(4), 0, 0);

    row.addView(left);
    row.addView(right);

    shell.addView(row);
    return shell;
}


private boolean termsAccepted() {
    return getSharedPreferences(PREFS, MODE_PRIVATE)
            .getBoolean(PREF_TERMS_ACCEPTED, false);
}

private String termsBody() {
    return tr("ColorGoogle itself does not collect, store, sell, or upload personal data, analytics, telemetry, screenshots, queries, or system logs to a developer-operated server.")
            + "\n\n"
            + tr("The app reads selected local Android/ColorOS log events only to detect the configured long-press trigger. Those log events are processed locally by the watcher.")
            + "\n\n"
            + tr("READ_LOGS is a package-level permission retained across reboot, while privileged logcat access is session-scoped. When a session must be recreated, Android may require its own device-log access confirmation. ColorGoogle does not bypass that system confirmation.")
            + "\n\n"
            + tr("When Circle to Search or an Assistant session is invoked, Google software and services may process screen context, account information, queries, or other data under Google's own terms and privacy policies. That processing is outside ColorGoogle.")
            + "\n\n"
            + tr("Shizuku setup changes local device state. On its first setup path, ColorGoogle can grant READ_LOGS, adjust background policy, and run pm uninstall --user 0 for com.heytap.speechassist and com.coloros.colordirectservice.")
            + "\n\n"
            + tr("The watcher runs in the private :watcher process. Display over other apps is used by the non-interactive overlay and the transparent Log Session Bridge. If an active privileged logcat session is lost, Android may present its own device-log access confirmation; ColorGoogle does not bypass it.")
            + "\n\n"
            + tr("ColorGoogle is an unofficial compatibility utility. ColorOS or Google updates can change these behaviors. Privileged setup changes local device state; review the listed commands before applying them.")
            + "\n\n"
            + tr("Redistribution must comply with GPL-3.0-only and all applicable copyright, notice, and corresponding-source requirements.")
            + "\n\n"
            + tr("Content, branding, commentary, screenshots, links, or other material added by a redistributor is solely that redistributor's responsibility and does not represent the original MindTrigger Assist author. The original author assumes no responsibility for such added material, including controversial or unrelated content. This statement is informational only and does not limit rights granted by GPL-3.0-only.")
            + "\n\n"
            + tr("By continuing, you agree to these terms and acknowledge that Google-side data processing is governed by Google, not ColorGoogle.");
}

private void showFirstRunTerms() {
    showTermsDialog(true);
}

private void showTermsDialog(boolean firstRun) {
    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
            .setTitle(tr("Terms & Privacy"))
            .setMessage(termsBody())
            .setCancelable(!firstRun);

    if (firstRun) {
        builder.setNegativeButton(tr("Exit"), (dialog, which) -> {
            dialog.dismiss();
            try {
                finishAndRemoveTask();
            } catch (Throwable ignored) {
                finish();
            }
        });
        builder.setPositiveButton(tr("Agree & continue"), (dialog, which) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_TERMS_ACCEPTED, true)
                    .commit();
            dialog.dismiss();
            refreshAll();
            if (bootstrap != null) bootstrap.beginIfNeeded();
        });
    } else {
        builder.setPositiveButton(tr("Close"), null);
    }

    showAppDialog(builder);
}



private void addGestureRequirement(LinearLayout root) {
    MaterialCardView shell = priorityCard();
    LinearLayout body = sectionBody();

    LinearLayout header = column();

    header.addView(text(
            "ColorOS gesture entry point",
            22,
            onSurface,
            Typeface.BOLD));

    TextView sub = supporting(
            "Required · Home/gesture activation depends on this ColorOS setting.");
    sub.setTextColor(warning);
    header.addView(sub, margins(0, 4, 0, 8));

    LinearLayout badgeRow = new LinearLayout(this);
    badgeRow.setOrientation(LinearLayout.HORIZONTAL);
    badgeRow.addView(
            pill(
                    "REQUIRED",
                    primaryContainer,
                    onPrimaryContainer));
    header.addView(badgeRow);

    body.addView(header);

    gestureStatus = statusText();
    body.addView(
            gestureStatus,
            margins(0, 12, 0, 8));

    gestureSetupBlock = column();

    TextView required = supporting(
            "Enable “Touch and hold gesture guide bar to wake Breeno”. ColorOS blocks ColorGoogle from opening the real System navigation page directly, so this step must be completed manually.");
    required.setTextColor(onSurface);
    gestureSetupBlock.addView(
            required,
            margins(0, 2, 0, 10));

    gestureActionFeedback = statusText();
    gestureActionFeedback.setVisibility(View.GONE);
    gestureSetupBlock.addView(gestureActionFeedback, margins(0, 0, 0, 8));

    addGestureWalkthrough(gestureSetupBlock);

    MaterialButton showRecovery = outlinedButton("Can't find the gesture switch?");
    gestureSetupBlock.addView(showRecovery, margins(0, 12, 0, 0));

    LinearLayout gestureRecovery = column();
    gestureRecovery.setVisibility(View.GONE);

    TextView removal = supporting(
            "Shizuku setup removed SpeechAssist. Restore it briefly, enable the switch, then remove it again.");
    removal.setTextColor(warning);
    gestureRecovery.addView(
            removal,
            margins(0, 12, 0, 8));

    gestureRecovery.addView(
            miniTitle("Recovery · Restore SpeechAssist"),
            margins(0, 6, 0, 4));

    TextView restoreCommand =
            supporting(
                    SetupCommands.speechAssistRestoreCommands());
    restoreCommand.setTypeface(Typeface.MONOSPACE);
    restoreCommand.setTextColor(onSurface);
    gestureRecovery.addView(restoreCommand);

    MaterialButton copyRestore =
            outlinedButton("Copy ADB restore commands");
    copyRestore.setOnClickListener(v -> {
        copy(
                "Restore SpeechAssist",
                SetupCommands.speechAssistRestoreCommands());
        Toast.makeText(
                this,
                tr("Restore commands copied."),
                Toast.LENGTH_SHORT).show();
        setGestureActionFeedback(
                "ADB restore commands copied. Run them on the connected computer, then return here.",
                true);
    });
    gestureRecovery.addView(
            copyRestore,
            margins(0, 8, 0, 8));

    TextView middle = supporting(
            "Then follow the three screenshots again and enable the highlighted ColorOS switch.");
    middle.setTextColor(onSurface);
    gestureRecovery.addView(
            middle,
            margins(0, 4, 0, 8));

    gestureRecovery.addView(
            miniTitle("Recovery · Remove SpeechAssist again"),
            margins(0, 4, 0, 4));

    TextView removeCommand =
            supporting(
                    SetupCommands.speechAssistRemoveCommand());
    removeCommand.setTypeface(Typeface.MONOSPACE);
    removeCommand.setTextColor(onSurface);
    gestureRecovery.addView(removeCommand);

    MaterialButton copyRemove =
            outlinedButton("Copy ADB uninstall command");
    copyRemove.setOnClickListener(v -> {
        copy(
                "Uninstall SpeechAssist",
                SetupCommands.speechAssistRemoveCommand());
        Toast.makeText(
                this,
                tr("Uninstall command copied."),
                Toast.LENGTH_SHORT).show();
        setGestureActionFeedback(
                "ADB uninstall command copied. Run it on the connected computer, then return here.",
                true);
    });
    gestureRecovery.addView(
            copyRemove,
            margins(0, 8, 0, 0));

    showRecovery.setOnClickListener(v -> {
        boolean show = gestureRecovery.getVisibility() != View.VISIBLE;
        gestureRecovery.setVisibility(show ? View.VISIBLE : View.GONE);
        showRecovery.setText(tr(show
                ? "Hide recovery steps"
                : "Can't find the gesture switch?"));
    });
    gestureSetupBlock.addView(gestureRecovery);

    TextView alreadyEnabled = supporting(
            "If a slight zoom animation already appears when you long-press the gesture guide bar, the entry point is probably active. You can skip recovery and confirm this step.");
    alreadyEnabled.setTextColor(success);
    gestureSetupBlock.addView(
            alreadyEnabled,
            margins(0, 14, 0, 0));

    MaterialButton confirmGesture =
            warningConfirmButton(
                    "Confirm gesture is enabled");

    confirmGesture.setOnClickListener(v ->
            confirmManualStep(
                    "Confirm ColorOS gesture",
                    "Only confirm after the highlighted ColorOS switch is enabled. A slight zoom animation when long-pressing the gesture guide bar is a useful sign that the entry point is active. ColorGoogle cannot reliably query this OEM setting.",
                    () -> getSharedPreferences(
                            PREFS,
                            MODE_PRIVATE)
                            .edit()
                            .putBoolean(
                                    PREF_GESTURE_CONFIRMED,
                                    true)
                            .apply()));

    gestureSetupBlock.addView(
            confirmGesture,
            margins(0, 12, 0, 0));

    body.addView(gestureSetupBlock);

    gestureConfirmedBlock =
            confirmedManualRow(
                    "ColorOS gesture confirmed",
                    "The setup guide is collapsed because you already confirmed this step.",
                    "Review guide",
                    v -> showGestureGuideDialog());

    body.addView(
            gestureConfirmedBlock,
            margins(0, 10, 0, 0));

    attachSetupSection(root, shell, body, 16);
}

private void addGestureWalkthrough(LinearLayout parent) {
    MaterialCardView walkthrough = nestedCard();
    LinearLayout content = column();
    content.setPadding(
            dp(14),
            dp(14),
            dp(14),
            dp(14));

    TextView title = text(
            "Manual ColorOS path",
            16,
            onSurface,
            Typeface.BOLD);
    content.addView(title);

    TextView desc = supporting(
            "Use these screenshots as the source of truth for this ColorOS build.");
    content.addView(
            desc,
            margins(0, 3, 0, 8));

    addGestureGuideStep(
            content,
            "1",
            "Open Home screen settings",
            R.drawable.guide_gesture_step1_home_screen);

    addGestureGuideStep(
            content,
            "2",
            "Open System navigation",
            R.drawable.guide_gesture_step2_system_navigation);

    addGestureGuideStep(
            content,
            "3",
            "Enable “Touch and hold gesture guide bar to wake Breeno”",
            R.drawable.guide_gesture_step3_breeno_hold);

    walkthrough.addView(content);
    parent.addView(
            walkthrough,
            margins(0, 4, 0, 0));
}

private void addGestureGuideStep(
        LinearLayout parent,
        String number,
        String caption,
        int drawableId) {

    LinearLayout header = new LinearLayout(this);
    header.setOrientation(LinearLayout.HORIZONTAL);
    header.setGravity(Gravity.CENTER_VERTICAL);

    header.addView(
            pill(
                    number,
                    primaryContainer,
                    onPrimaryContainer));

    TextView label = text(
            caption,
            14,
            onSurface,
            Typeface.BOLD);
    label.setPadding(
            dp(10),
            0,
            0,
            0);

    header.addView(
            label,
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f));

    parent.addView(
            header,
            margins(0, 10, 0, 6));

    MaterialCardView frame = elevatedCard(
            surfaceContainerHigh,
            UI_NESTED_CARD_RADIUS_DP,
            0f);
    frame.setStrokeColor(
            Color.argb(
                    100,
                    Color.red(outline),
                    Color.green(outline),
                    Color.blue(outline)));
    frame.setStrokeWidth(dp(1));
    frame.setContentPadding(
            dp(4),
            dp(4),
            dp(4),
            dp(4));

    ImageView image = new ImageView(this);
    image.setImageResource(drawableId);
    image.setAdjustViewBounds(true);
    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
    image.setContentDescription(tr(caption));

    frame.addView(image);
    parent.addView(frame);
}

private void showGestureGuideDialog() {
    ScrollView scroll = new ScrollView(this);
    LinearLayout content = column();
    content.setPadding(
            dp(4),
            dp(2),
            dp(4),
            dp(10));

    addGestureWalkthrough(content);
    scroll.addView(content);

    showAppDialog(new MaterialAlertDialogBuilder(this)
            .setTitle(tr("ColorOS gesture guide"))
            .setView(scroll)
            .setPositiveButton(tr("Close"), null));
}

/**
 * ColorOS can apply the device night overlay to a Material dialog even when
 * this Activity is explicitly using the app's light palette.  Use the live
 * palette after the window is attached so dialog chrome and custom content
 * always agree on a foreground/background pair.
 */
private void showAppDialog(MaterialAlertDialogBuilder builder) {
    androidx.appcompat.app.AlertDialog dialog = builder.create();
    dialog.setOnShowListener(ignored -> {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(roundRect(surfaceContainer, 28));
        }

        TextView title = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (title != null) title.setTextColor(onSurface);

        TextView message = dialog.findViewById(android.R.id.message);
        if (message != null) message.setTextColor(onSurfaceVariant);

        android.widget.Button positive = dialog.getButton(
                android.content.DialogInterface.BUTTON_POSITIVE);
        if (positive != null) positive.setTextColor(primary);

        android.widget.Button negative = dialog.getButton(
                android.content.DialogInterface.BUTTON_NEGATIVE);
        if (negative != null) negative.setTextColor(primary);
    });
    dialog.show();
}


private void confirmShizukuSetup() {
    try {
        if (!Shizuku.pingBinder()) {
            updatePrivilegedActionFeedback("SHIZUKU_NOT_RUNNING");
            Toast.makeText(this, tr("Shizuku is not running."), Toast.LENGTH_LONG).show();
            return;
        }
    } catch (Throwable t) {
        updatePrivilegedActionFeedback("SHIZUKU_NOT_RUNNING");
        Toast.makeText(this, tr("Shizuku is not available."), Toast.LENGTH_LONG).show();
        return;
    }

    showAppDialog(new MaterialAlertDialogBuilder(this)
            .setTitle(tr("Confirm Shizuku setup"))
            .setMessage(tr("Shizuku first-run will grant privileged setup access and remove com.heytap.speechassist plus com.coloros.colordirectservice from user 0 using pm uninstall --user 0. The system-partition APKs are not erased. Continue only if you understand these changes."))
            .setNegativeButton(tr("Cancel"), null)
            .setPositiveButton(tr("Continue"), (dialog, which) -> { dialog.dismiss(); if (bootstrap != null) bootstrap.runNow(); }));
}

private void confirmManualStep(String title, String warningText, Runnable onConfirmed) {
    showAppDialog(new MaterialAlertDialogBuilder(this)
            .setTitle(tr(title))
            .setMessage(tr(warningText))
            .setNegativeButton(tr("Cancel"), null)
            .setPositiveButton(tr("I verified this setting"), (dialog, which) -> { dialog.dismiss(); onConfirmed.run(); refreshAll(); }));
}

private LinearLayout confirmedManualRow(
        String title,
        String description,
        String action,
        View.OnClickListener listener) {

    LinearLayout row = column();
    row.setPadding(dp(16), dp(16), dp(16), dp(16));
    row.setBackground(roundRect(
            surfaceContainerHigh,
            UI_NESTED_CARD_RADIUS_DP));

    row.addView(text(
            "✓ " + tr(title),
            15,
            success,
            Typeface.BOLD));

    TextView desc = supporting(description);
    row.addView(desc, margins(0, 4, 0, 8));

    MaterialButton redo = compactButton(action);
    redo.setOnClickListener(listener);

    LinearLayout actionRow = new LinearLayout(this);
    actionRow.setOrientation(LinearLayout.HORIZONTAL);
    actionRow.setGravity(Gravity.END);
    actionRow.addView(redo);

    row.addView(actionRow);

    return row;
}

private MaterialButton warningConfirmButton(String label) {
    MaterialButton b = filledTonalButton(label);
    int warningContainer = Color.argb(
            ThemeManager.isDark(this) ? 42 : 24,
            Color.red(warning),
            Color.green(warning),
            Color.blue(warning));
    b.setBackgroundTintList(flatTint(warningContainer));
    b.setTextColor(warning);
    b.setStrokeColor(ColorStateList.valueOf(warning));
    b.setStrokeWidth(dp(1));
    b.setElevation(0f);
    return b;
}

private void applyPressDepth(View view, float restZDp, float pressedZDp) {
    view.setTranslationZ(0f);
    view.setElevation(0f);
}

private MaterialCardView priorityCard() {
    MaterialCardView card = elevatedCard(
            surfaceContainer,
            UI_CARD_RADIUS_DP,
            0f);
    card.setStrokeColor(Color.argb(
            120,
            Color.red(primary),
            Color.green(primary),
            Color.blue(primary)));
    card.setStrokeWidth(dp(1));
    return card;
}

private LinearLayout weightedButtonRow() {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    return row;
}

private LinearLayout.LayoutParams weightedButtonParams(int leftMarginDp) {
    LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f);
    lp.setMargins(dp(leftMarginDp), 0, 0, 0);
    return lp;
}

private MaterialButton themeColorButton(String label, String colorCode) {
    MaterialButton b = compactButton(label);
    int swatch = ThemeManager.previewPrimary(this, colorCode);
    int onSwatch = ThemeManager.previewOnPrimary(this, colorCode);
    boolean selected = colorCode.equals(ThemeManager.getColor(this));

    b.setBackgroundTintList(ColorStateList.valueOf(swatch));
    b.setTextColor(onSwatch);
    b.setStrokeWidth(selected ? dp(2) : 0);
    b.setStrokeColor(ColorStateList.valueOf(
            selected ? onSwatch : Color.TRANSPARENT));
    b.setElevation(0f);

    b.setOnClickListener(v -> {
        ThemeManager.setColor(this, colorCode);
        recreatePreservingTab();
    });
    return b;
}

private MaterialButton themeModeButton(String label, String mode) {
    boolean selected = mode.equals(ThemeManager.getMode(this));
    MaterialButton b = compactButton(label);
    b.setMinHeight(dp(50));
    b.setBackgroundTintList(ColorStateList.valueOf(
            selected ? primary : surface));
    b.setTextColor(selected ? onPrimary : onSurface);
    b.setStrokeColor(ColorStateList.valueOf(
            selected ? primary : outline));
    b.setStrokeWidth(dp(1));
    b.setRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));
    b.setElevation(0f);
    b.setOnClickListener(v -> {
        ThemeManager.setMode(this, mode);
        recreatePreservingTab();
    });
    return b;
}

    private void addTopBar(LinearLayout root, String titleText, String subtitleText) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(0, dp(12), 0, dp(16));

        LinearLayout titles = column();
        TextView pageTitle = text(titleText, 27, onSurface, Typeface.BOLD);
        pageTitle.setLineSpacing(0, 1.02f);
        titles.addView(pageTitle);

        TextView subtitle = text(
                subtitleText,
                14,
                primary,
                Typeface.BOLD);
        subtitle.setPadding(0, dp(3), 0, 0);
        titles.addView(subtitle);

        bar.addView(titles, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if ("ColorGoogle".equals(titleText)) {
            bar.addView(pill("ColorGoogle", primaryContainer, onPrimaryContainer));
        }

        root.addView(bar, matchWrap());
    }

    private void addHero(LinearLayout root) {
        MaterialCardView hero = elevatedCard(surfaceContainerHigh, 24, 0f);
        hero.setStrokeColor(
                Color.argb(
                        145,
                        Color.red(primary),
                        Color.green(primary),
                        Color.blue(primary)));
        hero.setStrokeWidth(dp(1));

        LinearLayout body = column();
        body.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView eyebrow = text(
                "CIRCLE TO SEARCH",
                11,
                primary,
                Typeface.BOLD);
        eyebrow.setLetterSpacing(0.14f);
        body.addView(eyebrow);

        heroTitle = text(
                "Checking setup…",
                27,
                onSurface,
                Typeface.BOLD);
        heroTitle.setPadding(0, dp(9), 0, dp(5));
        body.addView(heroTitle);

        heroSubtitle = text(
                "Complete the required ColorOS and Google settings before starting the watcher.",
                14,
                onSurfaceVariant,
                Typeface.NORMAL);
        heroSubtitle.setLineSpacing(0, 1.10f);
        body.addView(heroSubtitle);

        TextView rebootLogHint = text(
                tr("After reboot, if Android does not show the device-log access confirmation, open ColorGoogle once."),
                12,
                onSurfaceVariant,
                Typeface.NORMAL);
        rebootLogHint.setLineSpacing(0, 1.08f);
        rebootLogHint.setAlpha(0.86f);
        body.addView(rebootLogHint, margins(0, 12, 0, 0));

        hero.addView(body);
        root.addView(hero, margins(0, 0, 0, UI_CARD_GAP_DP));
    }



private void addCriticalRequirements(LinearLayout root) {
    MaterialCardView card =
            elevatedCard(
                    surfaceContainer,
                    UI_CARD_RADIUS_DP,
                    0f);

    card.setStrokeWidth(dp(1));
    card.setStrokeColor(
            Color.argb(
                    130,
                    Color.red(primary),
                    Color.green(primary),
                    Color.blue(primary)));

    LinearLayout body = column();
    body.setPadding(
            dp(20),
            dp(18),
            dp(20),
            dp(18));

    LinearLayout header = new LinearLayout(this);
    header.setOrientation(LinearLayout.HORIZONTAL);
    header.setGravity(Gravity.CENTER_VERTICAL);

    LinearLayout copy = column();
    copy.addView(text(
            "Required before Run",
            20,
            onSurface,
            Typeface.BOLD));

    TextView desc = supporting(
            "Run stays disabled until every required item below is ready.");
    copy.addView(
            desc,
            margins(0, 3, 0, 0));

    header.addView(
            copy,
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f));

    header.addView(
            pill(
                    "REQUIRED",
                    primaryContainer,
                    onPrimaryContainer));

    body.addView(header);

    criticalStatus = text(
            "Checking…",
            14,
            onSurface,
            Typeface.BOLD);
    criticalStatus.setLineSpacing(
            dp(4),
            1.05f);

    body.addView(
            criticalStatus,
            margins(0, 14, 0, 0));

    card.addView(body);
    root.addView(
            card,
            margins(0, 0, 0, UI_CARD_GAP_DP));
}


    private void addStep1(LinearLayout root) {
        MaterialCardView shell = sectionCard();
        LinearLayout body = sectionBody();

        addSectionHeader(
                body,
                "1",
                "Privileged setup",
                "Grant READ_LOGS once. Shizuku can do this without root.");

        step1Status = statusText();
        body.addView(step1Status, margins(0, 12, 0, 6));

        readLogsRow = actionRow(
                "READ_LOGS permission",
                "Granted once and kept after restart.",
                "Check",
                v -> refreshAll());
        body.addView(readLogsRow.root, margins(0, 6, 0, 0));

        logSessionRow = actionRow(
                "Device log access session",
                "A stopped agent needs reactivation. Optional wireless recovery is in Beta. After force-stop, reopen the app first.",
                "Reconnect",
                v -> armLogSessionReconnectWhenTop(true));
        body.addView(logSessionRow.root, margins(0, 8, 0, 0));

        step1Methods = new LinearLayout(this);
        step1Methods.setOrientation(LinearLayout.HORIZONTAL);
        step1Methods.setGravity(Gravity.CENTER_VERTICAL);

        shizukuButton = filledTonalButton("Run setup with Shizuku");
        shizukuButton.setOnClickListener(v -> confirmShizukuSetup());

        pcButton = outlinedButton("Copy ADB one-shot");
        pcButton.setOnClickListener(v -> {
            String cmd = SetupCommands.pcOneShot(getPackageName());
            copy("ColorGoogle one-shot", cmd);
            Toast.makeText(this,
                    tr("ADB command copied. Run it once from a computer, then return to ColorGoogle."),
                    Toast.LENGTH_LONG).show();
            if (privilegedActionFeedback != null) {
                privilegedActionFeedback.setText(tr(
                        "ADB one-shot copied. Waiting for you to run it on the connected computer."));
                privilegedActionFeedback.setTextColor(primary);
                privilegedActionFeedback.setVisibility(View.VISIBLE);
            }
        });

        step1Methods.addView(shizukuButton, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams pcLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pcLp.setMargins(dp(12), 0, 0, 0);
        step1Methods.addView(pcButton, pcLp);

        body.addView(step1Methods, margins(0, 10, 0, 0));

        privilegedActionFeedback = statusText();
        privilegedActionFeedback.setVisibility(View.GONE);
        body.addView(privilegedActionFeedback, margins(0, 10, 0, 0));
        if (!lastBootstrapUiState.isEmpty()) {
            updatePrivilegedActionFeedback(lastBootstrapUiState);
        }

        attachSetupSection(root, shell, body, 14);
    }


private void addStep2(LinearLayout root) {
    MaterialCardView shell = sectionCard();
    LinearLayout body = sectionBody();
    addSectionHeader(body, "2", "Background reliability", "Keep ColorGoogle running when ColorOS clears apps.");
    step2Status = statusText(); body.addView(step2Status, margins(0, 12, 0, 6));

    overlayRow = actionRow("Display over other apps", "Required for the transparent Log Session Bridge and the non-interactive watcher overlay.", "Open settings", v -> SettingsNavigator.overlay(this));
    body.addView(overlayRow.root, margins(0, 6, 0, 0));
    selfBatteryRow = actionRow("Battery optimization", "Recommended so ColorOS is less likely to restrict the isolated watcher in background.", "Open settings", v -> SettingsNavigator.requestBatteryExemption(this, getPackageName()));
    body.addView(selfBatteryRow.root, margins(0, 8, 0, 0));

    addGuide(body, "Background activity guide", R.drawable.guide_self_background);

    recentsSetupBlock = column();
    recentsSetupBlock.addView(miniTitle("Lock in Recent tasks"), margins(2, 18, 0, 4));
    recentsSetupBlock.addView(supporting("Required. Lock ColorGoogle in ColorOS Recent Tasks Manager so Clear All does not stop the watcher."));
    recentsOpenButton = filledTonalButton("Open Recent Tasks Manager");
    recentsOpenButton.setOnClickListener(v -> SettingsNavigator.homeScreenSettings(this));
    recentsSetupBlock.addView(recentsOpenButton, margins(0, 10, 0, 0));
    addGuide(recentsSetupBlock, "Recent-task lock guide", R.drawable.guide_recents_lock);
    recentsConfirmButton = warningConfirmButton("Confirm ColorGoogle is locked");
    recentsConfirmButton.setOnClickListener(v -> confirmManualStep(
            "Confirm Recent-task lock",
            "Only confirm after you have actually locked ColorGoogle in ColorOS Recent Tasks Manager. ColorGoogle cannot query this OEM state. A false confirmation can make Clear All stop the watcher.",
            () -> getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(PREF_RECENTS_LOCKED, true).apply()));
    recentsSetupBlock.addView(recentsConfirmButton, margins(0, 8, 0, 0));
    body.addView(recentsSetupBlock);

    recentsConfirmedBlock = confirmedManualRow("Recent-task lock confirmed", "The detailed guide is hidden because you already confirmed this step.", "Open again", v -> SettingsNavigator.homeScreenSettings(this));
    body.addView(recentsConfirmedBlock, margins(0, 14, 0, 0));
    attachSetupSection(root, shell, body, 14);
}

private MaterialCardView buildColorOsPermissionMonitoringWarning() {
    MaterialCardView card = elevatedCard(
            Color.argb(30, Color.red(error), Color.green(error), Color.blue(error)),
            UI_CARD_RADIUS_DP,
            0f);
    card.setStrokeColor(error);
    card.setStrokeWidth(dp(1));

    LinearLayout body = column();
    body.setPadding(dp(18), dp(18), dp(18), dp(18));

    body.addView(text(
            "ColorOS 16.0.7 · Disable permission monitoring",
            17,
            error,
            Typeface.BOLD));
    body.addView(supporting(
            "For ColorOS 16.0.7, temporarily switch the system language to English. In Developer options, turn on Disable system optimization."),
            margins(0, 6, 0, 0));
    body.addView(supporting(
            "This is your choice. If you do not agree, ColorGoogle may have background or privileged-setup problems."),
            margins(0, 8, 0, 0));
    body.addView(supporting(
            "If you have already completed these steps, you can ignore this reminder."),
            margins(0, 8, 0, 0));

    MaterialButton developerOptions = outlinedButton("Open Developer options");
    developerOptions.setStrokeColor(ColorStateList.valueOf(error));
    developerOptions.setTextColor(error);
    developerOptions.setOnClickListener(v -> SettingsNavigator.developerOptions(this));
    body.addView(developerOptions, margins(0, 12, 0, 0));

    card.addView(body);
    return card;
}


private void addStep3(LinearLayout root) {
    MaterialCardView shell = sectionCard(); LinearLayout body = sectionBody();
    addSectionHeader(body, "3", "Google integration", "Keep the Google app and Gemini available when ColorOS reclaims background processes.");
    step3Status = statusText(); body.addView(step3Status, margins(0, 12, 0, 6));

    googleAssistantRow = actionRow(
            "Default assistant · Google",
            "Open Android's digital assistant settings to review or change the active assistant.",
            "Assistant Settings",
            v -> SettingsNavigator.assistantSettings(this));
    body.addView(googleAssistantRow.root, margins(0, 6, 0, 0));

    googleBatteryRow = actionRow("Google · Battery unrestricted", "Required for reliable Circle to Search activation.", "Fix", v -> SettingsNavigator.requestBatteryExemption(this, SetupCommands.GOOGLE));
    body.addView(googleBatteryRow.root, margins(0, 6, 0, 0));
    geminiBatteryRow = actionRow("Gemini · Battery unrestricted", "Required when Gemini is installed.", "Fix", v -> SettingsNavigator.requestBatteryExemption(this, SetupCommands.GEMINI));
    body.addView(geminiBatteryRow.root, margins(0, 8, 0, 0));
    addGuide(body, "Battery settings guide", R.drawable.guide_google_background, R.drawable.guide_gemini_background);

    autoLaunchSetupBlock = column();
    autoLaunchSetupBlock.addView(miniTitle("Allow automatic launch"), margins(2, 18, 0, 4));
    autoLaunchSetupBlock.addView(supporting("Allow Google and Gemini to launch automatically in ColorOS. This OEM state cannot be queried reliably, so confirmation remains manual."));
    autoLaunchOpenButton = filledTonalButton("Open Settings"); autoLaunchOpenButton.setOnClickListener(v -> SettingsNavigator.autoLaunch(this));
    autoLaunchSetupBlock.addView(autoLaunchOpenButton, margins(0, 10, 0, 0));
    addGuide(autoLaunchSetupBlock, "Auto launch guide", R.drawable.guide_auto_launch);
    autoLaunchConfirmButton = warningConfirmButton("Confirm Auto launch is enabled");
    autoLaunchConfirmButton.setOnClickListener(v -> confirmManualStep(
            "Confirm Auto launch",
            "Only confirm after Auto launch is enabled for the installed Google/Gemini packages. ColorGoogle cannot query this ColorOS OEM state. A false confirmation can make CTS or Gemini fail after the apps are killed.",
            () -> getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(PREF_GOOGLE_AUTOSTART, true).putBoolean(PREF_GEMINI_AUTOSTART, true).apply()));
    autoLaunchSetupBlock.addView(autoLaunchConfirmButton, margins(0, 8, 0, 0)); body.addView(autoLaunchSetupBlock);

    autoLaunchConfirmedBlock = confirmedManualRow("Auto launch confirmed", "The detailed guide is hidden because you already confirmed this step.", "Open again", v -> SettingsNavigator.autoLaunch(this));
    body.addView(autoLaunchConfirmedBlock, margins(0, 14, 0, 0));
    attachSetupSection(root, shell, body, 14);
}

    private void addRunCard(LinearLayout root) {
        MaterialCardView shell = elevatedCard(
                surfaceContainerHigh,
                UI_CARD_RADIUS_DP,
                0f);
        shell.setStrokeColor(subtleOutline());
        shell.setStrokeWidth(dp(1));
        LinearLayout body = column();
        body.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout copy = column();
        copy.addView(text("Watcher", 20, onSurface, Typeface.BOLD));

        TextView desc = supporting(
                "Starts the foreground-service watcher after all required checks pass.");
        desc.setPadding(0, dp(3), 0, 0);
        copy.addView(desc);

        header.addView(copy, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        header.addView(pill("FGS", primaryContainer, onPrimaryContainer));
        body.addView(header);

        notificationStatus = supporting("");
        body.addView(notificationStatus, margins(0, 14, 0, 0));
        notificationButton = outlinedButton("Allow notifications");
        notificationButton.setOnClickListener(v -> requestNotificationAccess());
        body.addView(notificationButton, margins(0, 8, 0, 0));

        runButton = filledButton("Run ColorGoogle");
        runButton.setOnClickListener(v -> {
            if (isWatcherRunning()
                    && watcherSessionState
                    != WatcherIpc.STATE_ACTIVE) {
                armLogSessionReconnectWhenTop(true);
            } else {
                runWatcherWithGate();
            }
        });
        body.addView(runButton, margins(0, 14, 0, 0));

        attachSetupSection(root, shell, body, 18);
    }

    private void runWatcherWithGate() {
        String missing = firstMissingRequiredStep();
        if (missing != null) {
            Toast.makeText(this, missing, Toast.LENGTH_LONG).show();
            refreshAll();
            return;
        }

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                && !pref(PREF_NOTIFICATION_ASKED)) {
            runAfterNotificationRequest = true;
            requestNotificationAccess();
            return;
        }

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putBoolean(PREF_ENABLED, true).commit();

        startWatcher();
        bindWatcher();
        syncWatcherPrefs();
        refreshAll();

        Toast.makeText(
                this,
                tr("Approve Android's device-log access confirmation to start the privileged logcat reader."),
                Toast.LENGTH_LONG).show();

        statusPollRemaining = 120;

        armLogSessionReconnectWhenTop(true);
    }

    private void requestNotificationAccess() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                && !pref(PREF_NOTIFICATION_ASKED)) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean(PREF_NOTIFICATION_ASKED, true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
        } else {
            startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATIONS) return;
        boolean resumeRun = runAfterNotificationRequest;
        runAfterNotificationRequest = false;
        refreshAll();
        // Notification visibility and log-session health are separate states.
        // A denied/dismissed notification request does not forbid a foreground service.
        if (resumeRun) runWatcherWithGate();
        else if (pref(PREF_ENABLED)) startWatcher();
    }

    private void refreshNotificationStatus() {
        if (notificationStatus == null || notificationButton == null) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel channel = manager.getNotificationChannel(
                WatcherNotificationHelper.CHANNEL);
        boolean allowed = manager.areNotificationsEnabled()
                && (channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE);
        notificationStatus.setText(allowed
                ? "Notifications allowed · watcher status is shown separately."
                : "Notifications restricted · this does not mean the watcher has stopped.");
        notificationButton.setText(allowed || pref(PREF_NOTIFICATION_ASKED)
                ? "Notification settings" : "Allow notifications");
    }

    private String firstMissingRequiredStep() {
        if (!termsAccepted())
            return tr("Accept Terms & Privacy before running.");

        if (!isReadLogsGranted())
            return tr("Step 1: READ_LOGS has not been granted.");

        if (!pref(PREF_GESTURE_CONFIRMED))
            return tr("Required ColorOS gesture has not been confirmed.");

        if (!pref(PREF_RECENTS_LOCKED))
            return tr("Step 2: Recent-task lock has not been confirmed.");

        if (Build.VERSION.SDK_INT >= 23
                && !Settings.canDrawOverlays(this))
            return tr("Step 2: Display over other apps is required for automatic Log Session Bridge recovery.");

        boolean googleInstalled = packageExists(SetupCommands.GOOGLE);
        boolean geminiInstalled = packageExists(SetupCommands.GEMINI);

        if (googleInstalled && !isGoogleAssistantSelected())
            return tr("Step 3: Google is not selected as the default assistant.");

        if (googleInstalled && !isIgnoringBattery(SetupCommands.GOOGLE))
            return tr("Step 3: Google is still battery-optimized.");

        if (geminiInstalled && !isIgnoringBattery(SetupCommands.GEMINI))
            return tr("Step 3: Gemini is still battery-optimized.");

        boolean autoLaunchConfirmed =
                pref(PREF_GOOGLE_AUTOSTART) && pref(PREF_GEMINI_AUTOSTART);

        if ((googleInstalled || geminiInstalled) && !autoLaunchConfirmed)
            return tr("Step 3: Auto launch has not been confirmed.");

        return null;
    }

    private boolean isUiReadyForUpdates() {
        return uiReady
                && !isFinishing()
                && (Build.VERSION.SDK_INT < 17 || !isDestroyed());
    }

    private void refreshAllIfReady() {
        if (isUiReadyForUpdates()) refreshAll();
    }

    private void refreshAll() {
        if (!isUiReadyForUpdates()) return;
        refreshNotificationStatus();
        updateLastCommandTime();
        boolean readLogs = isReadLogsGranted();
        boolean overlay = Settings.canDrawOverlays(this);
        boolean selfBattery = isIgnoringBattery(getPackageName());
        boolean gesture = pref(PREF_GESTURE_CONFIRMED);
        boolean recents = pref(PREF_RECENTS_LOCKED);

        boolean googleInstalled = packageExists(SetupCommands.GOOGLE);
        boolean geminiInstalled = packageExists(SetupCommands.GEMINI);

        boolean googleAssistant =
                !googleInstalled || isGoogleAssistantSelected();

        boolean googleBattery =
                !googleInstalled || isIgnoringBattery(SetupCommands.GOOGLE);

        boolean geminiBattery =
                !geminiInstalled || isIgnoringBattery(SetupCommands.GEMINI);

        boolean autoLaunch =
                (!googleInstalled && !geminiInstalled)
                        || (pref(PREF_GOOGLE_AUTOSTART)
                        && pref(PREF_GEMINI_AUTOSTART));

        boolean step1Ready = readLogs;
        boolean step2Ready = recents && overlay;
        boolean step3Ready =
                googleAssistant
                        && googleBattery
                        && geminiBattery
                        && autoLaunch;
        boolean ready = step1Ready && gesture && step2Ready && step3Ready;
        boolean running = isWatcherRunning();

        int logSession =
                watcherSessionState;

        boolean logSessionActive =
                running
                        && logSession
                        == WatcherIpc.STATE_ACTIVE;

        boolean logSessionConnecting =
                running
                        && logSession
                        == WatcherIpc.STATE_CONNECTING;

        if (criticalStatus != null) {
            criticalStatus.setText(
                    checklistLine(readLogs, "READ_LOGS package permission granted · live session checked separately") + "\n" +
                    checklistLine(logSessionActive, "Device-log access session active") + "\n" +
                    checklistLine(gesture, "ColorOS long-press gesture confirmed") + "\n" +
                    checklistLine(recents, "Recent-task lock confirmed") + "\n" +
                    checklistLine(overlay, "Display over other apps enabled") + "\n" +
                    checklistLine(googleAssistant, "Google default assistant configured") + "\n" +
                    checklistLine(googleBattery && geminiBattery && autoLaunch, "Google services background setup complete"));
        }
        if (gestureStatus != null) {
            gestureStatus.setText(gesture ? "✓ " + tr("ColorOS gesture confirmed") : "⚠ " + tr("ColorOS gesture confirmation required"));
            gestureStatus.setTextColor(gesture ? success : warning);
        }
        if (gestureSetupBlock != null) gestureSetupBlock.setVisibility(gesture ? View.GONE : View.VISIBLE);
        if (gestureConfirmedBlock != null) gestureConfirmedBlock.setVisibility(gesture ? View.VISIBLE : View.GONE);
        if (recentsSetupBlock != null) recentsSetupBlock.setVisibility(recents ? View.GONE : View.VISIBLE);
        if (recentsConfirmedBlock != null) recentsConfirmedBlock.setVisibility(recents ? View.VISIBLE : View.GONE);
        if (autoLaunchSetupBlock != null) autoLaunchSetupBlock.setVisibility(autoLaunch ? View.GONE : View.VISIBLE);
        if (autoLaunchConfirmedBlock != null) autoLaunchConfirmedBlock.setVisibility(autoLaunch ? View.VISIBLE : View.GONE);

        if (heroTitle != null) {
            heroTitle.setText(tr(
                    running && logSessionActive
                            ? "Watcher active"
                            : running
                            ? (watcherUsesShellLogs ? "ADB log agent not ready" : "Android log-access confirmation required")
                            : ready
                            ? "Ready"
                            : "Setup required"));
        }

        if (heroSubtitle != null) {
            heroSubtitle.setText(tr(
                    running && logSessionActive
                            ? "Long-press the configured Home/gesture entry point to invoke Circle to Search."
                            : running && logSessionConnecting
                            ? (watcherUsesShellLogs ? "Verifying system-log access through the ADB agent." : "Waiting for Android's device-log access confirmation.")
                            : running
                            ? (watcherUsesShellLogs ? "Check wireless recovery in Beta, or copy the activation command and run it on your computer." : "Open ColorGoogle in the foreground to restore device log access.")
                            : ready
                            ? "All required setup is complete. Run will request a privileged logcat access session."
                            : "Complete the remaining required items below."));
        }

        if (watcherUsesShellLogs) {
            setStepStatus(step1Status, running && logSessionActive,
                    !running ? "ADB log listener not started"
                            : logSessionActive ? "ADB log agent connected"
                            : logSessionConnecting ? "Verifying the ADB log agent"
                            : "ADB log agent disconnected. Check recovery in Beta or reactivate.");
        } else if (!readLogs) {
            setStepStatus(
                    step1Status,
                    false,
                    "Privileged setup required");
        } else if (!running) {
            setStepStatus(
                    step1Status,
                    true,
                    "READ_LOGS retained · logcat access starts on Run");
        } else if (logSessionActive) {
            setStepStatus(
                    step1Status,
                    true,
                    "READ_LOGS retained · privileged logcat access active");
        } else if (logSessionConnecting) {
            setStepStatus(
                    step1Status,
                    false,
                    "READ_LOGS retained · waiting for Android log-access confirmation");
        } else {
            setStepStatus(
                    step1Status,
                    false,
                    "READ_LOGS retained · logcat access must reconnect");
        }

        setActionState(
                readLogsRow,
                readLogs,
                readLogs
                        ? "READ_LOGS package permission granted"
                        : "READ_LOGS not granted");

        updateLogSessionRow(
                readLogs,
                running,
                logSession);

        if (step1Methods != null) {
            step1Methods.setVisibility(step1Ready ? View.GONE : View.VISIBLE);
        }
        shizukuButton.setEnabled(!step1Ready);
        pcButton.setEnabled(!step1Ready);
        shizukuButton.setText("Run setup with Shizuku");
        pcButton.setText("Copy ADB one-shot");

        setStepStatus(
                step2Status,
                step2Ready,
                !recents
                        ? "Recent-task lock confirmation required"
                        : !overlay
                        ? "Display over other apps is required for the automatic Log Session Bridge"
                        : "Watcher retention and automatic recovery prerequisites complete");

        setActionState(
                overlayRow,
                overlay,
                overlay
                        ? "Overlay enabled · automatic Log Session Bridge available"
                        : "Required · enable the automatic Log Session Bridge");

        setActionState(
                selfBatteryRow,
                selfBattery,
                selfBattery ? "Background activity allowed"
                        : "Recommended · optional");

        if (googleAssistantRow != null) {
            if (!googleInstalled) {
                googleAssistantRow.status.setText(tr("Google is not installed"));
                googleAssistantRow.status.setTextColor(onSurfaceVariant);
            } else {
                googleAssistantRow.status.setText(tr(
                        googleAssistant
                                ? "Google is selected as the default assistant"
                                : "Google is not the default assistant"));
                googleAssistantRow.status.setTextColor(
                        googleAssistant ? success : onSurfaceVariant);
            }

            // Always expose the Android settings surface. The privileged secure
            // settings commands remain an internal bootstrap detail.
            googleAssistantRow.button.setVisibility(View.VISIBLE);
            googleAssistantRow.button.setEnabled(true);
            googleAssistantRow.button.setText(tr("Assistant Settings"));
        }

        setPackageBatteryState(
                googleBatteryRow,
                googleInstalled,
                googleBattery,
                "Google");

        setPackageBatteryState(
                geminiBatteryRow,
                geminiInstalled,
                geminiBattery,
                "Gemini");


        setStepStatus(
                step3Status,
                step3Ready,
                !googleAssistant ? "Google default assistant required"
                        : !googleBattery ? "Google battery setting required"
                        : !geminiBattery ? "Gemini battery setting required"
                        : !autoLaunch ? "Auto launch confirmation required"
                        : "Google services configuration complete");

        if (running) {
            if (logSessionActive) {
                runButton.setEnabled(false);
                runButton.setText(tr("Running"));
            } else if (logSessionConnecting) {
                runButton.setEnabled(false);
                runButton.setText(tr("Waiting for Android log access"));
            } else {
                runButton.setEnabled(ready && readLogs);
                runButton.setText(tr("Reconnect device-log access"));
            }
        } else {
            runButton.setEnabled(ready);
            runButton.setText(tr(
                    ready
                            ? "Run ColorGoogle"
                            : "Complete setup first"));
        }

        updateSetupWizardChrome();
    }


    private boolean isGoogleAssistantSelected() {
        try {
            String assistant =
                    Settings.Secure.getString(
                            getContentResolver(),
                            "assistant");

            String voice =
                    Settings.Secure.getString(
                            getContentResolver(),
                            "voice_interaction_service");

            String expected =
                    SetupCommands.GOOGLE_ASSISTANT_SERVICE;

            return expected.equals(assistant)
                    && expected.equals(voice);

        } catch (Throwable ignored) {
            return false;
        }
    }

    private String formatLastExecutionTime() {
        if (bootstrap == null) {
            return tr("Never");
        }

        long when = bootstrap.getLastExecutionTimeMs();
        if (when <= 0L) {
            return tr("Never");
        }

        try {
            Locale locale = Locale.forLanguageTag(LanguageManager.get(this));
            DateFormat exact = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.MEDIUM,
                    locale);

            CharSequence relative = android.text.format.DateUtils.getRelativeTimeSpanString(
                    when,
                    System.currentTimeMillis(),
                    android.text.format.DateUtils.MINUTE_IN_MILLIS,
                    android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE);

            return exact.format(new Date(when)) + "  ·  " + relative;
        } catch (Throwable ignored) {
            return Long.toString(when);
        }
    }

    private void updateLastCommandTime() {
        if (commandTimeView != null) {
            commandTimeView.setText(
                    formatLastExecutionTime());
        }
    }

    private void shareProject() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(
                Intent.EXTRA_SUBJECT,
                "ColorGoogle");
        send.putExtra(
                Intent.EXTRA_TEXT,
                "ColorGoogle — open-source Gemini and Circle to Search shortcuts for ColorOS. Based on MindTrigger Assist by EvokerUniverse. https://github.com/joeyandu/ColorGoogle");

        try {
            startActivity(
                    Intent.createChooser(
                            send,
                            tr("Share project")));
        } catch (Throwable ignored) {}
    }

    private ColorStateList makeRadioColorStateList() {
        return new ColorStateList(
                new int[][] {
                        new int[] { android.R.attr.state_checked },
                        new int[] {}
                },
                new int[] {
                        primary,
                        onSurfaceVariant
                });
    }

    private void addChromeLikeColorPicker(
            LinearLayout parent) {

        String[] colors = {
                ThemeManager.COLOR_BLUE,
                ThemeManager.COLOR_TEAL,
                ThemeManager.COLOR_GREEN,
                ThemeManager.COLOR_SAGE,
                ThemeManager.COLOR_YELLOW,
                ThemeManager.COLOR_ORANGE,
                ThemeManager.COLOR_PINK,
                ThemeManager.COLOR_LAVENDER,
                ThemeManager.COLOR_CORAL
        };

        int columns = 4;
        for (int rowIndex = 0; rowIndex * columns < colors.length; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.START);

            for (int column = 0; column < columns; column++) {
                int index = rowIndex * columns + column;
                if (index >= colors.length) {
                    break;
                }

                View swatch = chromeColorSwatch(colors[index]);

                LinearLayout.LayoutParams lp =
                        new LinearLayout.LayoutParams(
                                dp(60),
                                dp(60));
                lp.setMargins(column == 0 ? 0 : dp(12), rowIndex == 0 ? dp(8) : dp(12), 0, 0);
                row.addView(swatch, lp);
            }

            parent.addView(row);
        }
    }

    private View chromeColorSwatch(
            String colorCode) {

        boolean selected =
                colorCode.equals(
                        ThemeManager.getColor(this));

        MaterialCardView outer =
                new MaterialCardView(this);

        outer.setRadius(dp(30));
        outer.setCardElevation(0f);
        outer.setUseCompatPadding(false);
        outer.setPreventCornerOverlap(true);
        outer.setCardBackgroundColor(surface);
        outer.setRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));
        outer.setStrokeWidth(selected ? dp(3) : dp(1));
        outer.setStrokeColor(selected ? primary : outline);

        FrameLayout frame = new FrameLayout(this);

        MaterialCardView inner = new MaterialCardView(this);
        inner.setRadius(dp(24));
        inner.setCardElevation(0f);
        inner.setCardBackgroundColor(ThemeManager.previewPrimary(this, colorCode));
        inner.setStrokeWidth(0);

        View lower = new View(this);
        lower.setBackground(roundRect(ThemeManager.previewContainer(this, colorCode), 24));
        FrameLayout.LayoutParams lowerLp = new FrameLayout.LayoutParams(dp(48), dp(24));
        lowerLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

        frame.addView(inner, new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER));
        frame.addView(lower, lowerLp);

        if (selected) {
            TextView check = text("✓", 16, onPrimary, Typeface.BOLD);
            check.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams checkLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            frame.addView(check, checkLp);
        }

        outer.addView(frame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        outer.setContentDescription(colorCode);
        outer.setOnClickListener(v -> {
            ThemeManager.setColor(this, colorCode);
            recreatePreservingTab();
        });

        return outer;
    }

    private void recreatePreservingTab() {
        Intent intent = getIntent();
        if (intent != null) {
            intent.putExtra(EXTRA_RECREATE_TAB, currentTab);
            intent.putExtra(EXTRA_RECREATE_SETUP_PAGE, currentSetupPage);
        }
        recreate();
    }

    /**
     * Programmatic tab navigation. This may synchronize the BottomNavigationView.
     * User-originated BottomNavigationView callbacks MUST use
     * showTabFromBottomNavigation() so they never call setSelectedItemId() again.
     */
    private void showTab(int tab) {
        showTabInternal(tab, true);
    }

    /** Render a tab selected by the user in BottomNavigationView without re-entry. */
    private void showTabFromBottomNavigation(int tab) {
        showTabInternal(tab, false);
    }

    private void showTabInternal(int tab, boolean syncBottomNavigation) {
        if (!isKnownTab(tab)) {
            tab = TAB_SETUP;
        }

        int previousTab = currentTab;
        currentTab = tab;

        if (setupPage == null) return;

        View incoming = pageForTab(tab);

        setupPage.animate().cancel();
        advancedPage.animate().cancel();
        betaPage.animate().cancel();
        supportPage.animate().cancel();
        aboutPage.animate().cancel();

        setupPage.setVisibility(tab == TAB_SETUP ? View.VISIBLE : View.GONE);
        advancedPage.setVisibility(tab == TAB_ADVANCED ? View.VISIBLE : View.GONE);
        betaPage.setVisibility(tab == TAB_BETA ? View.VISIBLE : View.GONE);
        supportPage.setVisibility(tab == TAB_SUPPORT ? View.VISIBLE : View.GONE);
        aboutPage.setVisibility(tab == TAB_ABOUT ? View.VISIBLE : View.GONE);

        boolean slowMotion = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_BETA_SLOW_ANIMATIONS,
                        DEFAULT_BETA_SLOW_ANIMATIONS);

        if (previousTab != tab
                && !setupWizardActive
                && incoming != null) {
            incoming.setAlpha(0f);
            incoming.setScaleX(0.985f);
            incoming.setScaleY(0.985f);
            incoming.setTranslationY(dp(10));
            incoming.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setInterpolator(new android.view.animation.PathInterpolator(
                            0.2f, 0f, 0f, 1f))
                    .setDuration(slowMotion ? 440L : 240L)
                    .start();
        } else if (incoming != null) {
            incoming.setAlpha(1f);
            incoming.setScaleX(1f);
            incoming.setScaleY(1f);
            incoming.setTranslationY(0f);
        }

        // Hide app navigation only while the one-time onboarding wizard is
        // active. After onboarding, Setup is a normal scrollable bottom-nav tab.
        if (bottomNav != null) {
            boolean hideForWizard = setupWizardActive && tab == TAB_SETUP;
            bottomNav.setVisibility(hideForWizard ? View.GONE : View.VISIBLE);
            if (syncBottomNavigation
                    && !hideForWizard
                    && bottomNav.getSelectedItemId() != tab) {
                bottomNav.setSelectedItemId(tab);
            }
        }

        if (tab == TAB_SETUP && setupWizardActive) {
            updateSetupWizardChrome();
            if (setupScroll != null) {
                setupScroll.post(() -> setupScroll.scrollTo(0, 0));
            }
        }
    }

    private boolean isKnownTab(int tab) {
        return tab == TAB_SETUP
                || tab == TAB_ADVANCED
                || tab == TAB_BETA
                || tab == TAB_SUPPORT
                || tab == TAB_ABOUT;
    }

    private View pageForTab(int tab) {
        if (tab == TAB_SETUP) return setupPage;
        if (tab == TAB_ADVANCED) return advancedPage;
        if (tab == TAB_BETA) return betaPage;
        if (tab == TAB_SUPPORT) return supportPage;
        if (tab == TAB_ABOUT) return aboutPage;
        return null;
    }

    private String checklistLine(boolean done, String label) {
        return (done ? "✓ " : "⚠ ") + tr(label);
    }

    private void setActionState(
            ActionRow row,
            boolean done,
            String state) {

        if (row == null) return;

        row.status.setText(tr(state));
        row.status.setTextColor(done ? success : onSurfaceVariant);

        // Completed items should read as status, not as a wall of disabled buttons.
        row.button.setVisibility(done ? View.GONE : View.VISIBLE);
        row.button.setEnabled(!done);
    }

    private void setOptionalActionState(
            ActionRow row,
            boolean enabled,
            String state) {

        if (row == null) return;

        row.status.setText(tr(state));
        row.status.setTextColor(enabled ? success : onSurfaceVariant);

        // Optional diagnostics remain actionable when disabled.
        row.button.setVisibility(enabled ? View.GONE : View.VISIBLE);
        row.button.setEnabled(!enabled);
        if (!enabled) row.button.setText(tr("Enable"));
    }

    private void setPackageBatteryState(
            ActionRow row,
            boolean installed,
            boolean done,
            String name) {

        if (row == null) return;

        if (!installed) {
            row.status.setText(name + tr(" is not installed"));
            row.status.setTextColor(onSurfaceVariant);
            row.button.setVisibility(View.VISIBLE);
            row.button.setEnabled(false);
            row.button.setText(tr("Unavailable"));
            return;
        }

        setActionState(
                row,
                done,
                done ? name + tr(" is unrestricted")
                        : name + tr(" is still battery-optimized"));
    }

    private void setManualState(
            MaterialButton open,
            MaterialButton confirm,
            boolean done,
            String doneText,
            String openText) {

        if (open == null || confirm == null) return;

        open.setEnabled(!done);
        confirm.setEnabled(!done);

        open.setVisibility(done ? View.GONE : View.VISIBLE);
        confirm.setVisibility(done ? View.GONE : View.VISIBLE);

        if (!done) {
            open.setText(tr(openText));
        }
    }

    private void setStepStatus(
            TextView view,
            boolean ok,
            String message) {

        if (view == null) return;
        view.setText((ok ? "✓ " : "• ") + tr(message));
        view.setTextColor(ok ? success : warning);
    }

    private ActionRow actionRow(
            String title,
            String description,
            String action,
            View.OnClickListener listener) {

        MaterialCardView shell = nestedCard();

        LinearLayout row = column();
        row.setPadding(dp(16), dp(16), dp(16), dp(16));

        row.addView(text(
                title,
                15,
                onSurface,
                Typeface.BOLD));

        TextView desc = supporting(description);
        row.addView(desc, margins(0, 3, 0, 0));

        TextView status = text(
                "Checking…",
                12,
                onSurfaceVariant,
                Typeface.BOLD);
        row.addView(status, margins(0, 6, 0, 10));

        MaterialButton button = compactButton(action);
        button.setOnClickListener(listener);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        actions.addView(button);

        row.addView(actions);

        shell.addView(row);
        return new ActionRow(shell, status, button);
    }


    private void addSectionHeader(
            LinearLayout parent,
            String number,
            String title,
            String description) {

        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);

        TextView numberText = text(
                number,
                14,
                onPrimaryContainer,
                Typeface.BOLD);
        numberText.setGravity(Gravity.CENTER);
        numberText.setMinWidth(dp(36));
        numberText.setMinHeight(dp(36));
        numberText.setPadding(0, 0, 0, 0);
        numberText.setBackground(
                roundRect(primaryContainer, 18));

        LinearLayout.LayoutParams numberLp =
                new LinearLayout.LayoutParams(
                        dp(36),
                        dp(36));
        numberLp.setMargins(0, 0, dp(12), 0);
        line.addView(numberText, numberLp);

        LinearLayout copy = column();

        TextView titleView = text(
                title,
                20,
                onSurface,
                Typeface.BOLD);
        titleView.setIncludeFontPadding(false);
        titleView.setMaxLines(3);
        copy.addView(titleView);

        TextView d = supporting(description);
        d.setPadding(0, dp(4), 0, 0);
        copy.addView(d);

        line.addView(
                copy,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f));

        parent.addView(line);
    }


    private void addGuide(
            LinearLayout parent,
            String buttonText,
            int... drawableIds) {

        MaterialButton toggle = textButton(buttonText);
        toggle.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

        LinearLayout images = column();
        images.setVisibility(View.GONE);

        toggle.setOnClickListener(v -> {
            boolean show = images.getVisibility() != View.VISIBLE;
            images.setVisibility(show ? View.VISIBLE : View.GONE);
            toggle.setText(show ? tr("Hide guide") : tr(buttonText));
        });

        parent.addView(toggle, margins(0, 6, 0, 0));

        for (int id : drawableIds) {
            MaterialCardView frame = nestedCard();
            frame.setContentPadding(dp(6), dp(6), dp(6), dp(6));

            ImageView image = new ImageView(this);
            image.setImageResource(id);
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setContentDescription(tr("ColorOS setup guide"));

            frame.addView(image);
            images.addView(frame, margins(0, 6, 0, 0));
        }

        parent.addView(images);
    }

    private ScrollView pageScroll() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        // A single surface behind every independent tab keeps the content
        // edge visually stable while cards scroll underneath it.
        scroll.setBackgroundColor(surface);
        scroll.setClipToPadding(false);
        return scroll;
    }

    private LinearLayout pageRoot() {
        LinearLayout root = column();
        root.setPadding(
                dp(UI_PAGE_SIDE_DP),
                dp(UI_PAGE_TOP_DP),
                dp(UI_PAGE_SIDE_DP),
                dp(UI_PAGE_BOTTOM_DP));
        return root;
    }

    private FrameLayout.LayoutParams matchFrame() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private MaterialCardView sectionCard() {
        MaterialCardView card = elevatedCard(
                surfaceContainer,
                UI_CARD_RADIUS_DP,
                0f);
        card.setStrokeColor(subtleOutline());
        card.setStrokeWidth(dp(1));
        return card;
    }

    private MaterialCardView nestedCard() {
        MaterialCardView card = elevatedCard(
                surfaceContainerHigh,
                UI_NESTED_CARD_RADIUS_DP,
                0f);
        card.setStrokeColor(subtleOutline());
        card.setStrokeWidth(dp(1));
        return card;
    }

    private MaterialCardView elevatedCard(
            int color,
            float radiusDp,
            float elevationDp) {

        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(color);
        card.setRadius(dp(radiusDp));
        card.setCardElevation(dp(Math.min(1f, Math.max(0f, elevationDp))));
        card.setUseCompatPadding(false);
        card.setPreventCornerOverlap(true);
        card.setStateListAnimator(null);
        card.setElevation(0f);
        card.setTranslationZ(0f);
        return card;
    }

    private LinearLayout sectionBody() {
        LinearLayout body = column();
        body.setPadding(dp(20), dp(18), dp(20), dp(18));
        return body;
    }

    private TextView sectionEyebrow(String value) {
        TextView t = text(value, 11, onSurfaceVariant, Typeface.BOLD);
        t.setLetterSpacing(0.12f);
        return t;
    }

    private TextView statusText() {
        return text("Checking…", 14, warning, Typeface.BOLD);
    }

    private TextView miniTitle(String value) {
        return text(value, 14, onSurface, Typeface.BOLD);
    }

    private TextView supporting(String value) {
        TextView v = text(value, 15, onSurfaceVariant, Typeface.NORMAL);
        v.setLineSpacing(dp(1), 1.12f);
        return v;
    }

    private MaterialButton filledButton(String label) {
        MaterialButton b = baseButton(label);
        b.setBackgroundTintList(flatTint(primary));
        b.setTextColor(onPrimary);
        b.setElevation(0f);
        return b;
    }

    private MaterialButton filledTonalButton(String label) {
        MaterialButton b = baseButton(label);
        b.setBackgroundTintList(flatTint(primaryContainer));
        b.setTextColor(onPrimaryContainer);
        b.setElevation(0f);
        return b;
    }

    private MaterialButton outlinedButton(String label) {
        MaterialButton b = baseButton(label);
        b.setBackgroundTintList(flatTint(Color.TRANSPARENT));
        b.setTextColor(primary);
        b.setStrokeColor(ColorStateList.valueOf(outline));
        b.setStrokeWidth(dp(1));
        return b;
    }

    private MaterialButton textButton(String label) {
        MaterialButton b = baseButton(label);
        b.setBackgroundTintList(flatTint(Color.TRANSPARENT));
        b.setTextColor(primary);
        b.setInsetTop(0);
        b.setInsetBottom(0);
        return b;
    }

    private MaterialButton compactButton(String label) {
        MaterialButton b = new MaterialButton(this);
        b.setAllCaps(false);
        b.setText(tr(label));
        b.setTextSize(13);
        b.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        b.setMinHeight(dp(44));
        b.setMinWidth(dp(72));
        b.setCornerRadius(dp(20));
        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setBackgroundTintList(flatTint(primaryContainer));
        b.setTextColor(onPrimaryContainer);
        b.setRippleColor(primaryStateLayer());
        b.setStateListAnimator(null);
        b.setElevation(0f);
        applyPressDepth(b, 0f, 0f);
        return b;
    }

    private MaterialButton baseButton(String label) {
        MaterialButton b = new MaterialButton(this);
        b.setAllCaps(false);
        b.setText(tr(label));
        b.setTextSize(15);
        b.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        b.setMinHeight(dp(48));
        b.setCornerRadius(dp(24));
        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setRippleColor(primaryStateLayer());
        b.setStateListAnimator(null);
        b.setElevation(0f);
        applyPressDepth(b, 0f, 0f);
        return b;
    }

    private int subtleOutline() {
        return Color.argb(
                ThemeManager.isDark(this) ? 54 : 76,
                Color.red(outline),
                Color.green(outline),
                Color.blue(outline));
    }

    private ColorStateList flatTint(int color) {
        return new ColorStateList(
                new int[][] {
                        new int[] { android.R.attr.state_pressed },
                        new int[] { android.R.attr.state_focused },
                        new int[] { android.R.attr.state_hovered },
                        new int[] {}
                },
                new int[] { color, color, color, color });
    }

    private ColorStateList primaryStateLayer() {
        int alpha = ThemeManager.isDark(this) ? 52 : 34;
        int state = Color.argb(
                alpha,
                Color.red(primary),
                Color.green(primary),
                Color.blue(primary));
        return ColorStateList.valueOf(state);
    }

    private TextView pill(String value, int bg, int fg) {
        TextView t = text(value, 11, fg, Typeface.BOLD);
        t.setPadding(dp(12), dp(7), dp(12), dp(7));
        t.setBackground(roundRect(bg, 14));
        return t;
    }

    private TextView text(
            String value,
            float sp,
            int color,
            int style) {

        TextView t = new TextView(this);
        t.setText(tr(value));
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", style));
        if (Build.VERSION.SDK_INT >= 23) {
            t.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);
            t.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
        }
        return t;
    }

    private android.graphics.drawable.GradientDrawable roundRect(
            int color,
            float radiusDp) {

        android.graphics.drawable.GradientDrawable d =
                new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams margins(
            int left,
            int top,
            int right,
            int bottom) {

        LinearLayout.LayoutParams p = matchWrap();
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return p;
    }

    private int dp(float value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density);
    }

    private String tr(String source) {
        return UiText.tr(this, source);
    }

    private void copy(String label, String value) {
        ClipboardManager cm =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, value));
    }



private void armLogSessionReconnectWhenTop(boolean force) {
    pendingTopLogReconnect = true;
    pendingTopLogReconnectForce |= force;

    if (activityResumed && activityHasWindowFocus) {
        maybeRunTopLogReconnect();
    }
}

private void maybeRunTopLogReconnect() {
    if (!pendingTopLogReconnect
            || topLogReconnectScheduled
            || !activityResumed
            || !activityHasWindowFocus) {
        return;
    }

    topLogReconnectScheduled = true;

    // LogcatManagerService only displays its confirmation when the requesting
    // UID is PROCESS_STATE_TOP. Window focus is a stronger signal than
    // onResume() on OEM builds, and the short delay lets ActivityManager commit
    // the TOP state before :watcher opens a new logd reader.
    ui.postDelayed(
            () -> {
                topLogReconnectScheduled = false;

                if (!pendingTopLogReconnect
                        || !activityResumed
                        || !activityHasWindowFocus) {
                    return;
                }

                final boolean force = pendingTopLogReconnectForce;
                pendingTopLogReconnect = false;
                pendingTopLogReconnectForce = false;
                requestLogSessionReconnect(force);
            },
            360L);
}

private void requestLogSessionReconnect(
        boolean force) {

    if (!isReadLogsGranted()) {
        Toast.makeText(
                this,
                tr("READ_LOGS is not granted. Complete privileged setup first."),
                Toast.LENGTH_LONG).show();
        return;
    }

    boolean enabled =
            getSharedPreferences(
                    PREFS,
                    MODE_PRIVATE)
                    .getBoolean(
                            PREF_ENABLED,
                            false);

    if (!enabled) {
        if (force) {
            Toast.makeText(
                    this,
                    tr("Start the watcher first. The privileged logcat session is created when the watcher starts."),
                    Toast.LENGTH_LONG).show();
        }
        return;
    }

    if (!force
            && (watcherSessionState
                    == WatcherIpc.STATE_ACTIVE
                    || watcherSessionState
                    == WatcherIpc.STATE_CONNECTING)) {
        return;
    }

    startWatcher();
    bindWatcher();
    syncWatcherPrefs();

    Bundle reconnectData = new Bundle();
    reconnectData.putBoolean(WatcherIpc.KEY_FORCE_RECONNECT, force);
    boolean sent =
            sendWatcherMessage(
                    WatcherIpc.MSG_RECONNECT_LOGCAT,
                    reconnectData,
                    false);

    if (!sent) {
        Intent reconnect =
                new Intent(
                        this,
                        HomeHoldService.class);

        reconnect.setAction(
                HomeHoldService.ACTION_RECONNECT_LOGCAT);
        reconnect.putExtra(WatcherIpc.KEY_FORCE_RECONNECT, force);

        try {
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(reconnect);
            } else {
                startService(reconnect);
            }
        } catch (Throwable e) {
            Toast.makeText(
                    this,
                    tr("Unable to request a new device-log access session."),
                    Toast.LENGTH_LONG).show();
            return;
        }
    }

    statusPollRemaining = 40;
    ui.removeCallbacks(pcDetectLoop);
    ui.post(pcDetectLoop);

    if (force) {
        Toast.makeText(
                this,
                watcherUsesShellLogs ? "Reconnecting to the ADB agent. If it has exited, check recovery in Beta or reactivate."
                        : tr("Android may require device-log access confirmation before a new privileged logcat session can start."),
                Toast.LENGTH_LONG).show();
    }
}

private void updateLogSessionRow(
        boolean readLogs,
        boolean running,
        int state) {

    if (logSessionRow == null) {
        return;
    }

    if (!readLogs) {
        logSessionRow.status.setText(
                tr("READ_LOGS permission is required first"));
        logSessionRow.status.setTextColor(warning);
        logSessionRow.button.setVisibility(View.GONE);
        return;
    }

    if (!running) {
        logSessionRow.status.setText(
                tr("Not started · Run will request privileged logcat access"));
        logSessionRow.status.setTextColor(onSurfaceVariant);
        logSessionRow.button.setVisibility(View.GONE);
        return;
    }

    if (watcherUsesShellLogs) {
        boolean active = state == WatcherIpc.STATE_ACTIVE;
        boolean connecting = state == WatcherIpc.STATE_CONNECTING;
        logSessionRow.status.setText(active ? "ADB log agent connected · System logs verified"
                : connecting ? "Connecting to the ADB log agent; waiting for system-log verification"
                : "ADB log agent disconnected. Check recovery in Beta or reactivate.");
        logSessionRow.status.setTextColor(active ? success : warning);
        logSessionRow.button.setVisibility(View.VISIBLE);
        logSessionRow.button.setEnabled(!connecting);
        logSessionRow.button.setText("Reconnect agent");
        return;
    }

    if (state
            == WatcherIpc.STATE_ACTIVE) {
        logSessionRow.status.setText(
                tr("System log access verified"));
        logSessionRow.status.setTextColor(success);
        logSessionRow.button.setVisibility(View.VISIBLE);
        logSessionRow.button.setEnabled(true);
        logSessionRow.button.setText(tr("Reconnect"));
        return;
    }

    logSessionRow.button.setVisibility(View.VISIBLE);

    if (state
            == WatcherIpc.STATE_CONNECTING) {
        logSessionRow.status.setText(
                tr("Waiting for Android log-access confirmation dialog"));
        logSessionRow.status.setTextColor(warning);
        logSessionRow.button.setEnabled(false);
        logSessionRow.button.setText(tr("Waiting"));
        return;
    }

    if (state
            == WatcherIpc.STATE_NO_PERMISSION) {
        logSessionRow.status.setText(
                tr("READ_LOGS permission unavailable"));
        logSessionRow.status.setTextColor(error);
        logSessionRow.button.setEnabled(false);
        logSessionRow.button.setText(tr("Unavailable"));
        return;
    }

    logSessionRow.status.setText(
            tr("Foreground ready · requesting a new logcat access session"));
    logSessionRow.status.setTextColor(warning);
    logSessionRow.button.setEnabled(true);
    logSessionRow.button.setText(tr("Reconnect"));
}


private void bindWatcher() {
    if (watcherBound) {
        return;
    }

    boolean enabled =
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getBoolean(PREF_ENABLED, false);

    if (!enabled) {
        return;
    }

    try {
        Intent i =
                new Intent(
                        this,
                        HomeHoldService.class);

        bindService(
                i,
                watcherConnection,
                Context.BIND_AUTO_CREATE);
    } catch (Throwable ignored) {
    }
}

private void unbindWatcher() {
    if (!watcherBound) {
        return;
    }

    sendWatcherMessage(
            WatcherIpc.MSG_UNREGISTER_CLIENT,
            null,
            true);

    try {
        unbindService(watcherConnection);
    } catch (Throwable ignored) {
    }

    watcherBound = false;
    watcherMessenger = null;
}

private boolean sendWatcherMessage(
        int what,
        Bundle data,
        boolean includeReplyTo) {

    Messenger target =
            watcherMessenger;

    if (!watcherBound || target == null) {
        return false;
    }

    Message msg =
            Message.obtain(
                    null,
                    what);

    if (data != null) {
        msg.setData(data);
    }

    if (includeReplyTo) {
        msg.replyTo = uiMessenger;
    }

    try {
        target.send(msg);
        return true;

    } catch (RemoteException e) {
        watcherBound = false;
        watcherMessenger = null;
        return false;
    }
}

private void syncWatcherPrefs() {
    if (!watcherBound) {
        return;
    }

    android.content.SharedPreferences prefs =
            getSharedPreferences(
                    PREFS,
                    MODE_PRIVATE);

    Bundle data =
            new Bundle();

    data.putInt(
            WatcherIpc.KEY_DELAY_MS,
            prefs.getInt(
                    PREF_CTS_DELAY_MS,
                    DEFAULT_CTS_DELAY_MS));

    data.putBoolean(
            WatcherIpc.KEY_VIBRATE,
            prefs.getBoolean(
                    PREF_VIBRATE_ON_CTS,
                    DEFAULT_VIBRATE_ON_CTS));

    data.putBoolean(
            WatcherIpc.KEY_SOUND,
            prefs.getBoolean(
                    PREF_SOUND_ON_ACTIVATION,
                    DEFAULT_SOUND_ON_ACTIVATION));

    data.putBoolean(
            WatcherIpc.KEY_POWER_GEMINI,
            prefs.getBoolean(
                    PREF_POWER_GEMINI_EXPERIMENTAL,
                    DEFAULT_POWER_GEMINI_EXPERIMENTAL));

    data.putBoolean(
            WatcherIpc.KEY_VOICE_WAKE_ASSISTANT,
            prefs.getBoolean(
                    PREF_VOICE_WAKE_ASSISTANT_EXPERIMENTAL,
                    DEFAULT_VOICE_WAKE_ASSISTANT_EXPERIMENTAL));

    data.putBoolean(
            WatcherIpc.KEY_SWAP_CTS_ASSISTANT,
            prefs.getBoolean(
                    PREF_SWAP_CTS_ASSISTANT_EXPERIMENTAL,
                    DEFAULT_SWAP_CTS_ASSISTANT_EXPERIMENTAL));

    sendWatcherMessage(
            WatcherIpc.MSG_SYNC_PREFS,
            data,
            false);
}

    private void setLogSessionRecoveryTileEnabled(boolean enabled) {
        try {
            ComponentName component = new ComponentName(
                    this,
                    LogSessionRecoveryTileService.class);
            getPackageManager().setComponentEnabledSetting(
                    component,
                    enabled
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        } catch (Throwable error) {
            Toast.makeText(
                    this,
                    tr("Unable to update the Quick Settings tile."),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void startWatcher() {
        Intent i = new Intent(this, HomeHoldService.class)
                .setAction(HomeHoldService.ACTION_REFRESH_FGS);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
    }

    private boolean isWatcherRunning() {
        if (watcherBound) {
            return true;
        }

        ActivityManager am =
                (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am == null) return false;

        try {
            List<ActivityManager.RunningServiceInfo> list =
                    am.getRunningServices(Integer.MAX_VALUE);

            for (ActivityManager.RunningServiceInfo s : list) {
                ComponentName c = s.service;

                if (c != null
                        && getPackageName().equals(c.getPackageName())
                        && HomeHoldService.class.getName()
                        .equals(c.getClassName())) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    private boolean isReadLogsGranted() {
        return checkSelfPermission("android.permission.READ_LOGS")
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isIgnoringBattery(String pkg) {
        try {
            PowerManager pm =
                    (PowerManager) getSystemService(POWER_SERVICE);
            return pm != null
                    && pm.isIgnoringBatteryOptimizations(pkg);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean packageExists(String pkg) {
        try {
            getPackageManager().getApplicationInfo(pkg, 0);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean pref(String key) {
        return getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(key, false);
    }

    private ColorStateList makeNavColorStateList() {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] {}
        };

        int[] colors = new int[] {
                primary,
                onSurfaceVariant
        };

        return new ColorStateList(states, colors);
    }

    private void loadColors() {
        ThemeManager.Palette p = ThemeManager.palette(this);
        primary = p.primary;
        onPrimary = p.onPrimary;
        primaryContainer = p.primaryContainer;
        onPrimaryContainer = p.onPrimaryContainer;
        surface = p.surface;
        surfaceContainer = p.surfaceContainer;
        surfaceContainerHigh = p.surfaceContainerHigh;
        onSurface = p.onSurface;
        onSurfaceVariant = p.onSurfaceVariant;
        outline = p.outline;
        success = p.success;
        warning = p.warning;
        error = p.error;
    }

    private int color(int resId) {
        return ContextCompat.getColor(this, resId);
    }

    private void configureWindow() {
        Window w = getWindow();
        w.setStatusBarColor(surface);
        w.setNavigationBarColor(surface);

        View decor = w.getDecorView();
        int flags = decor.getSystemUiVisibility();

        if (!ThemeManager.isDark(this)) {
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            if (Build.VERSION.SDK_INT >= 26) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        } else {
            if (Build.VERSION.SDK_INT >= 23) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            if (Build.VERSION.SDK_INT >= 26) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }

        decor.setSystemUiVisibility(flags);
    }

    private static final class ActionRow {
        final MaterialCardView root;
        final TextView status;
        final MaterialButton button;

        ActionRow(
                MaterialCardView root,
                TextView status,
                MaterialButton button) {

            this.root = root;
            this.status = status;
            this.button = button;
        }
    }
}
