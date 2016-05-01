package com.viking.logking;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences mPrefs;

    private BroadcastReceiver mRootFailReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            processRootFail();
        }
    };

    private void processRootFail() {

        int failCount = mPrefs.getInt(getString(R.string.pref_root_fail_count), 0);
        if (failCount == 0) {
            // show dialog first time
            AlertDialog dlg = new AlertDialog.Builder(this)
                    .setTitle(R.string.no_root)
                    .setMessage(R.string.no_root_dialog)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // ok, do nothing
                        }
                    })
                    .setNeutralButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                        }
                    })
                    .create();
            dlg.show();
            mPrefs.edit().putInt(getString(R.string.pref_root_fail_count), failCount+1).apply();
        } else if (failCount <= 3) {
            // show toast 3 more times
            Toast.makeText(this, R.string.toast_no_root, Toast.LENGTH_LONG).show();
            mPrefs.edit().putInt(getString(R.string.pref_root_fail_count), failCount+1).apply();
        }

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        SwitchCompat switchCompat = (SwitchCompat) menu.findItem(R.id.myswitch).getActionView().findViewById(R.id.switchAB);
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent(MainActivity.this, LogService.class);
                if (isChecked) {
                    if (!LogService.isRunning()) {
                        startService(intent);
                    }
                } else {
                    stopService(intent);
                }
            }
        });
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(LogService.ACTION_ROOT_FAILED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mRootFailReceiver, f);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRootFailReceiver);
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_headers);

            setupOpenSourceInfoPreference(getActivity(), findPreference(getString(R.string.pref_info_open_source)));
            setupVersionPref(getActivity(), findPreference(getString(R.string.pref_version)));
        }
    }

    private static void setupOpenSourceInfoPreference(final Activity activity, Preference preference) {
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentManager fm = activity.getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                Fragment prev = fm.findFragmentByTag("dialog");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);
                new OpenSourceLicensesDialog().show(ft, "dialog");
                return true;
            }
        });
    }

    private static void setupVersionPref(final Activity activity, Preference preference) {
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentManager fm = activity.getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                Fragment prev = fm.findFragmentByTag("dialog");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);
                new AboutDialog().show(ft, "dialog");
                return true;
            }
        });
    }

    public static class OpenSourceLicensesDialog extends DialogFragment {

        public OpenSourceLicensesDialog() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            WebView webView = new WebView(getActivity());
            webView.loadUrl("file:///android_asset/licenses.html");

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.open_source_licences)
                    .setView(webView)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
        }
    }

    public static class AboutDialog extends DialogFragment {

        public AboutDialog() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View content = inflater.inflate(R.layout.dialog_about, null, false);
            TextView version = (TextView) content.findViewById(R.id.version);

            try {
                String name = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
                version.setText(getString(R.string.version) + " " + name);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.app_name)
                    .setView(content)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
        }
    }
}
