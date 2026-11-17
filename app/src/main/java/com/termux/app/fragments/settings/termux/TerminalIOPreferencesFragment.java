package com.termux.app.fragments.settings.termux;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.termux.R;
import com.termux.app.terminal.io.TermuxQuickCommands;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

@Keep
public class TerminalIOPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TerminalIOPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.termux_terminal_io_preferences, rootKey);

        EditTextPreference quickCommands = findPreference("terminal_quick_commands");
        if (quickCommands != null) {
            quickCommands.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                editText.setSingleLine(false);
                editText.setMinLines(4);
                editText.setSelection(editText.getText().length());
            });
            quickCommands.setSummaryProvider(preference -> getString(R.string.termux_quick_commands_summary,
                TextUtils.join("  ·  ", TermuxQuickCommands.parseCommands(((EditTextPreference) preference).getText()))));
        }
    }

}

class TerminalIOPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final TermuxAppSharedPreferences mPreferences;

    private static TerminalIOPreferencesDataStore mInstance;

    private TerminalIOPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = TermuxAppSharedPreferences.build(context, true);
    }

    public static synchronized TerminalIOPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TerminalIOPreferencesDataStore(context);
        }
        return mInstance;
    }



    @Override
    public void putString(String key, String value) {
        if (mPreferences == null) return;
        if (key == null) return;

        if ("terminal_quick_commands".equals(key)) {
            // Empty value restores the default list
            mPreferences.setTerminalQuickCommands(value == null ? "" : value.trim());
        }
    }

    @Override
    public String getString(String key, String defValue) {
        if (mPreferences == null) return defValue;
        if ("terminal_quick_commands".equals(key))
            return mPreferences.getTerminalQuickCommands();
        return defValue;
    }

    @Override
    public void putBoolean(String key, boolean value) {
        if (mPreferences == null) return;
        if (key == null) return;

        switch (key) {
            case "soft_keyboard_enabled":
                    mPreferences.setSoftKeyboardEnabled(value);
                break;
            case "soft_keyboard_enabled_only_if_no_hardware":
                mPreferences.setSoftKeyboardEnabledOnlyIfNoHardware(value);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (mPreferences == null) return false;

        switch (key) {
            case "soft_keyboard_enabled":
                return mPreferences.isSoftKeyboardEnabled();
            case "soft_keyboard_enabled_only_if_no_hardware":
                return mPreferences.isSoftKeyboardEnabledOnlyIfNoHardware();
            default:
                return false;
        }
    }

}
