/*
 *  Copyright (C) 2017-2018 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
*/
package org.omnirom.omnigears.interfacesettings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import org.omnirom.omnilib.preference.SystemSettingSwitchPreference;
import org.omnirom.omnilib.preference.SeekBarPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StyleSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "StyleSettings";
    private static final String OMNI_SYSUI_ROUNDED_FWVALS = "sysui_rounded_fwvals";
    private static final String OMNI_SYSUI_ROUNDED_SIZE = "sysui_rounded_size";
    private static final String OMNI_SYSUI_ROUNDED_CONTENT_PADDING = "sysui_rounded_content_padding";
    private static final String CUSTOM_HEADER_BROWSE = "custom_header_browse";
    private static final String DAYLIGHT_HEADER_PACK = "daylight_header_pack";
    private static final String CUSTOM_HEADER_IMAGE_SHADOW = "status_bar_custom_header_shadow";
    private static final String CUSTOM_HEADER_PROVIDER = "custom_header_provider";
    private static final String STATUS_BAR_CUSTOM_HEADER = "status_bar_custom_header";
    private static final String FILE_HEADER_SELECT = "file_header_select";

    private static final int REQUEST_PICK_IMAGE = 0;

    private SystemSettingSwitchPreference mRoundedFwvals;
    private SeekBarPreference mCornerRadius;
    private SeekBarPreference mContentPadding;
    private Preference mHeaderBrowse;
    private ListPreference mDaylightHeaderPack;
    private SeekBarPreference mHeaderShadow;
    private ListPreference mHeaderProvider;
    private String mDaylightHeaderProvider;
    private SystemSettingSwitchPreference mHeaderEnabled;
    private Preference mFileHeader;
    private String mFileHeaderProvider;

    @Override
    public void onResume() {
        super.onResume();
        updateEnablement();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.OMNI_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.style_settings);

        Resources res = null;
        Context ctx = getContext();
        float density = Resources.getSystem().getDisplayMetrics().density;

        try {
            res = ctx.getPackageManager().getResourcesForApplication("com.android.systemui");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // Rounded Corner Radius
        mCornerRadius = (SeekBarPreference) findPreference(OMNI_SYSUI_ROUNDED_SIZE);
        mCornerRadius.setOnPreferenceChangeListener(this);
        int resourceIdRadius = res.getIdentifier("com.android.systemui:dimen/rounded_corner_radius", null, null);
        int cornerRadius = Settings.Secure.getInt(ctx.getContentResolver(),
                Settings.Secure.OMNI_SYSUI_ROUNDED_SIZE,
                (int) (res.getDimension(resourceIdRadius) / density));
        mCornerRadius.setValue(cornerRadius / 1);

        // Rounded Content Padding
        mContentPadding = (SeekBarPreference) findPreference(OMNI_SYSUI_ROUNDED_CONTENT_PADDING);
        mContentPadding.setOnPreferenceChangeListener(this);
        int resourceIdPadding = res.getIdentifier("com.android.systemui:dimen/rounded_corner_content_padding", null, null);
        int contentPadding = Settings.Secure.getInt(ctx.getContentResolver(),
                Settings.Secure.OMNI_SYSUI_ROUNDED_CONTENT_PADDING,
                (int) (res.getDimension(resourceIdPadding) / density));
        mContentPadding.setValue(contentPadding / 1);

        // Rounded use Framework Values
        mRoundedFwvals = (SystemSettingSwitchPreference) findPreference(OMNI_SYSUI_ROUNDED_FWVALS);
        mRoundedFwvals.setOnPreferenceChangeListener(this);


        mDaylightHeaderProvider = getResources().getString(R.string.daylight_header_provider);
        mFileHeaderProvider = getResources().getString(R.string.file_header_provider);
        mHeaderBrowse = findPreference(CUSTOM_HEADER_BROWSE);

        mHeaderEnabled = (SystemSettingSwitchPreference) findPreference(STATUS_BAR_CUSTOM_HEADER);
        mHeaderEnabled.setOnPreferenceChangeListener(this);

        mDaylightHeaderPack = (ListPreference) findPreference(DAYLIGHT_HEADER_PACK);

        List<String> entries = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        getAvailableHeaderPacks(entries, values);
        mDaylightHeaderPack.setEntries(entries.toArray(new String[entries.size()]));
        mDaylightHeaderPack.setEntryValues(values.toArray(new String[values.size()]));

        boolean headerEnabled = Settings.System.getInt(getContentResolver(),
                Settings.System.OMNI_STATUS_BAR_CUSTOM_HEADER, 0) != 0;
        updateHeaderProviderSummary(headerEnabled);
        mDaylightHeaderPack.setOnPreferenceChangeListener(this);

        mHeaderShadow = (SeekBarPreference) findPreference(CUSTOM_HEADER_IMAGE_SHADOW);
        final int headerShadow = Settings.System.getInt(getContentResolver(),
                Settings.System.OMNI_STATUS_BAR_CUSTOM_HEADER_SHADOW, 0);
        mHeaderShadow.setValue((int)(((double) headerShadow / 255) * 100));
        mHeaderShadow.setOnPreferenceChangeListener(this);

        mHeaderProvider = (ListPreference) findPreference(CUSTOM_HEADER_PROVIDER);
        mHeaderProvider.setOnPreferenceChangeListener(this);

        mFileHeader = findPreference(FILE_HEADER_SELECT);
    }

    private void restoreCorners() {
        Resources res = null;
        float density = Resources.getSystem().getDisplayMetrics().density;

        try {
            res = getContext().getPackageManager().getResourcesForApplication("com.android.systemui");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        int resourceIdRadius = res.getIdentifier("com.android.systemui:dimen/rounded_corner_radius", null, null);
        int resourceIdPadding = res.getIdentifier("com.android.systemui:dimen/rounded_corner_content_padding", null,
                null);
        mCornerRadius.setValue((int) (res.getDimension(resourceIdRadius) / density));
        mContentPadding.setValue((int) (res.getDimension(resourceIdPadding) / density));

    }

    private void updateHeaderProviderSummary(boolean headerEnabled) {
        mDaylightHeaderPack.setSummary(getResources().getString(R.string.header_provider_disabled));
        if (headerEnabled) {
            String settingHeaderPackage = Settings.System.getString(getContentResolver(),
                    Settings.System.OMNI_STATUS_BAR_DAYLIGHT_HEADER_PACK);
            if (settingHeaderPackage != null) {
                int valueIndex = mDaylightHeaderPack.findIndexOfValue(settingHeaderPackage);
                mDaylightHeaderPack.setValueIndex(valueIndex >= 0 ? valueIndex : 0);
                mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntry());
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mFileHeader) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
         if (preference == mCornerRadius) {
            Settings.Secure.putInt(getContext().getContentResolver(), Settings.Secure.OMNI_SYSUI_ROUNDED_SIZE,
                    ((int) newValue) * 1);
        } else if (preference == mContentPadding) {
            Settings.Secure.putInt(getContext().getContentResolver(), Settings.Secure.OMNI_SYSUI_ROUNDED_CONTENT_PADDING,
                    ((int) newValue) * 1);
        } else if (preference == mRoundedFwvals) {
            restoreCorners();
        } else if (preference == mDaylightHeaderPack) {
            String value = (String) newValue;
            Settings.System.putString(getContentResolver(),
                    Settings.System.OMNI_STATUS_BAR_DAYLIGHT_HEADER_PACK, value);
            int valueIndex = mDaylightHeaderPack.findIndexOfValue(value);
            mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntries()[valueIndex]);
        } else if (preference == mHeaderShadow) {
            Integer headerShadow = (Integer) newValue;
            int realHeaderValue = (int) (((double) headerShadow / 100) * 255);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.OMNI_STATUS_BAR_CUSTOM_HEADER_SHADOW, realHeaderValue);
        } else if (preference == mHeaderProvider) {
            String value = (String) newValue;
            Settings.System.putString(getContentResolver(),
                    Settings.System.OMNI_STATUS_BAR_CUSTOM_HEADER_PROVIDER, value);
            int valueIndex = mHeaderProvider.findIndexOfValue(value);
            mHeaderProvider.setSummary(mHeaderProvider.getEntries()[valueIndex]);
            updateEnablement();
        } else if (preference == mHeaderEnabled) {
            Boolean headerEnabled = (Boolean) newValue;
            updateHeaderProviderSummary(headerEnabled);
        }
        return true;
    }

    private boolean isBrowseHeaderAvailable() {
        PackageManager pm = getPackageManager();
        Intent browse = new Intent();
        browse.setClassName("org.omnirom.omnistyle", "org.omnirom.omnistyle.PickHeaderActivity");
        return pm.resolveActivity(browse, 0) != null;
    }

    private void getAvailableHeaderPacks(List<String> entries, List<String> values) {
        Map<String, String> headerMap = new HashMap<String, String>();
        Intent i = new Intent();
        PackageManager packageManager = getPackageManager();
        i.setAction("org.omnirom.DaylightHeaderPack");
        for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
            String packageName = r.activityInfo.packageName;
            String label = r.activityInfo.loadLabel(getPackageManager()).toString();
            if (label == null) {
                label = r.activityInfo.packageName;
            }
            headerMap.put(label, packageName);
        }
        i.setAction("org.omnirom.DaylightHeaderPack1");
        for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
            String packageName = r.activityInfo.packageName;
            String label = r.activityInfo.loadLabel(getPackageManager()).toString();
            if (r.activityInfo.name.endsWith(".theme")) {
                continue;
            }
            if (label == null) {
                label = packageName;
            }
            headerMap.put(label, packageName  + "/" + r.activityInfo.name);
        }
        List<String> labelList = new ArrayList<String>();
        labelList.addAll(headerMap.keySet());
        Collections.sort(labelList);
        for (String label : labelList) {
            entries.add(label);
            values.add(headerMap.get(label));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == REQUEST_PICK_IMAGE) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }
            final Uri imageUri = result.getData();
            Settings.System.putString(getContentResolver(), Settings.System.OMNI_STATUS_BAR_CUSTOM_HEADER_PROVIDER, "file");
            Settings.System.putString(getContentResolver(), Settings.System.OMNI_STATUS_BAR_FILE_HEADER_IMAGE, imageUri.toString());
        }
    }

    private void updateEnablement() {
        String providerName = Settings.System.getString(getContentResolver(),
                Settings.System.OMNI_STATUS_BAR_CUSTOM_HEADER_PROVIDER);
        if (providerName == null) {
            providerName = mDaylightHeaderProvider;
        }
        if (!providerName.equals(mDaylightHeaderProvider)) {
            providerName = mFileHeaderProvider;
        }
        int valueIndex = mHeaderProvider.findIndexOfValue(providerName);
        mHeaderProvider.setValueIndex(valueIndex >= 0 ? valueIndex : 0);
        mHeaderProvider.setSummary(mHeaderProvider.getEntry());
        mDaylightHeaderPack.setEnabled(providerName.equals(mDaylightHeaderProvider));
        mFileHeader.setEnabled(providerName.equals(mFileHeaderProvider));
        mHeaderBrowse.setEnabled(isBrowseHeaderAvailable() && providerName.equals(mFileHeaderProvider));
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.style_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    return result;
                }
            };

}
