package com.termux.app.terminal.io;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.termux.app.TermuxActivity;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.extrakeys.ExtraKeyButton;
import com.termux.shared.termux.extrakeys.ExtraKeysConstants;
import com.termux.shared.termux.extrakeys.ExtraKeysInfo;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants.TERMUX_APP;
import com.termux.terminal.TerminalSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * The "quick commands" page of the terminal toolbar (swipe the extra keys row left).
 *
 * Each button types a command into the current session and presses Enter. The list comes from
 * the {@link TERMUX_APP#KEY_TERMINAL_QUICK_COMMANDS} preference (one command per line) and can be
 * edited in Settings -> Terminal I/O.
 */
public class TermuxQuickCommands implements ExtraKeysView.IExtraKeysView {

    /** Delay before sending Enter, so that TUI apps like Claude Code see the command text and the
     * Enter key as two separate inputs instead of a single pasted chunk. */
    private static final int ENTER_DELAY_MS = 60;

    private static final String LOG_TAG = "TermuxQuickCommands";

    private final TermuxActivity mActivity;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public TermuxQuickCommands(@NonNull TermuxActivity activity) {
        mActivity = activity;
    }

    /** Parse the raw preference text into a list of commands, skipping blank lines. */
    @NonNull
    public static List<String> parseCommands(@Nullable String text) {
        List<String> commands = new ArrayList<>();
        if (text == null) return commands;
        for (String line : text.split("\n")) {
            String command = line.trim();
            if (!command.isEmpty()) commands.add(command);
        }
        return commands;
    }

    /**
     * Build the buttons matrix, spreading the commands over {@code rows} rows so that the page has
     * the same height as the extra keys page.
     */
    @Nullable
    public ExtraKeysInfo buildExtraKeysInfo(int rows) {
        List<String> commands = parseCommands(mActivity.getPreferences().getTerminalQuickCommands());
        if (commands.isEmpty())
            commands = parseCommands(TERMUX_APP.DEFAULT_VALUE_TERMINAL_QUICK_COMMANDS);

        rows = Math.max(1, Math.min(rows, commands.size()));
        int columns = (int) Math.ceil(commands.size() / (double) rows);

        try {
            JSONArray matrix = new JSONArray();
            for (int row = 0; row < rows; row++) {
                JSONArray line = new JSONArray();
                for (int col = 0; col < columns; col++) {
                    int index = row * columns + col;
                    if (index >= commands.size()) break;
                    JSONObject button = new JSONObject();
                    button.put(ExtraKeyButton.KEY_KEY_NAME, commands.get(index));
                    button.put(ExtraKeyButton.KEY_DISPLAY_NAME, commands.get(index));
                    line.put(button);
                }
                if (line.length() > 0) matrix.put(line);
            }
            // No display map and no aliases: commands are shown and sent exactly as written
            return new ExtraKeysInfo(matrix.toString(), new ExtraKeysConstants.ExtraKeyDisplayMap(),
                new ExtraKeysConstants.ExtraKeyDisplayMap());
        } catch (JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to build quick commands", e);
            return null;
        }
    }

    /** Set up a freshly inflated {@link ExtraKeysView} as the quick commands page. */
    public void setupView(@NonNull ExtraKeysView view, int rows) {
        // Commands are plain buttons: no sticky CTRL/ALT and no key repeat on long press
        view.setSpecialButtons(new HashMap<>());
        view.setRepetitiveKeys(Collections.emptyList());
        view.setButtonTextAllCaps(false);
        view.setExtraKeysViewClient(this);
        view.reload(buildExtraKeysInfo(rows), mActivity.getTerminalToolbarDefaultHeight());
    }

    @Override
    public void onExtraKeyButtonClick(View view, ExtraKeyButton buttonInfo, MaterialButton button) {
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null || !session.isRunning()) return;

        session.write(buttonInfo.getKey());
        mHandler.postDelayed(() -> {
            if (session.isRunning()) session.write("\r");
        }, ENTER_DELAY_MS);
    }

    @Override
    public boolean performExtraKeyButtonHapticFeedback(View view, ExtraKeyButton buttonInfo, MaterialButton button) {
        return false;
    }

}
