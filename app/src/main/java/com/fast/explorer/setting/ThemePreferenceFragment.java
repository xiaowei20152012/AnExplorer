package com.fast.explorer.setting;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.fast.explorer.DocumentsApplication;
import com.fast.explorer.R;

import static com.fast.explorer.setting.SettingsActivity.KEY_ACCENT_COLOR;
import static com.fast.explorer.setting.SettingsActivity.KEY_PRIMARY_COLOR;
import static com.fast.explorer.setting.SettingsActivity.KEY_THEME_STYLE;

public class ThemePreferenceFragment extends PreferenceFragment
		implements OnPreferenceChangeListener, Preference.OnPreferenceClickListener{

	public ThemePreferenceFragment() {
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_theme);
		PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("ThemePreferenceScreen");
		
		Preference preferencePrimaryColor = findPreference(KEY_PRIMARY_COLOR);
		preferencePrimaryColor.setOnPreferenceChangeListener(this);
		preferencePrimaryColor.setOnPreferenceClickListener(this);

		findPreference(KEY_ACCENT_COLOR).setOnPreferenceClickListener(this);

		Preference preferenceThemeStyle = findPreference(KEY_THEME_STYLE);
		preferenceThemeStyle.setOnPreferenceChangeListener(this);
		preferenceThemeStyle.setOnPreferenceClickListener(this);
		if(DocumentsApplication.isTelevision()){
			preferenceScreen.removePreference(preferenceThemeStyle);
		}

	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		SettingsActivity.logSettingEvent(preference.getKey());
        ((SettingsActivity)getActivity()).changeActionBarColor(Integer.valueOf(newValue.toString()));
		getActivity().recreate();
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		SettingsActivity.logSettingEvent(preference.getKey());
		return false;
	}
}