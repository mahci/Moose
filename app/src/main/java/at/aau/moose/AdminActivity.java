package at.aau.moose;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class AdminActivity extends PreferenceActivity {

    // Interaction with the DevicePolicyManager
    DevicePolicyManager mDPM;
    ComponentName mDeviceAdminSample;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Prepare to work with the DPM
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
//        mDeviceAdminSample = new ComponentName(this, DeviceAdminSampleReceiver.class);
    }

    public static class AdminSampleFragment
            extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener,
            Preference.OnPreferenceClickListener {

        // Useful instance variables
        protected AdminActivity mActivity;
        protected DevicePolicyManager mDPM;
        protected ComponentName mDeviceAdminSample;
        protected boolean mAdminActive;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            // Retrieve the useful instance variables
            mActivity = (AdminActivity) getActivity();
            mDPM = mActivity.mDPM;
            mDeviceAdminSample = mActivity.mDeviceAdminSample;
//            mAdminActive = mActivity.isActiveAdmin();
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            return false;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            return false;
        }
    }

}
