/*
 * Copyright 2014 Yaroslav Mytkalyk aka Doctoror
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.doctoror.surprise;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ViewAnimator;

public final class SurpriseActivity extends Activity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    private static final int ANIMATOR_CHILD_CONTENT = 0;
    private static final int ANIMATOR_CHILD_PROGRESS = 1;

    private static final String TAG_DIALOG = ".tags.DIALOG";

    private PackageManager mPackageManager;
    private ComponentName mReceiverComponent;

    private ViewAnimator mAnimatorSurprise;
    private ViewAnimator mAnimatorUpgrade;

    private ServiceReceiver mServiceReceiver = new ServiceReceiver();
    private LocalBroadcastManager mLocalBroadcastManager;

    private boolean mFragmentTransactionsAllowed;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentTransactionsAllowed = true;
        setContentView(R.layout.activity_surprise);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mLocalBroadcastManager.registerReceiver(mServiceReceiver, mServiceReceiver.mIntentFilter);

        mAnimatorSurprise = (ViewAnimator) findViewById(R.id.animator_surprise);
        mAnimatorUpgrade = (ViewAnimator) findViewById(R.id.animator_upgrade);

        mPackageManager = getPackageManager();
        mReceiverComponent = new ComponentName(this, SurpriseReceiver.class);
        final CompoundButton surprise = (CompoundButton) findViewById(R.id.btn_surprise);
        surprise.setChecked(mPackageManager.getComponentEnabledSetting(mReceiverComponent)
                == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        surprise.setOnCheckedChangeListener(this);

        findViewById(R.id.btn_surprise_to_su).setOnClickListener(this);
        findViewById(R.id.info_surprise).setOnClickListener(this);
        findViewById(R.id.info_upgrade).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mFragmentTransactionsAllowed = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mServiceReceiver);
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        mFragmentTransactionsAllowed = false;
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        mPackageManager.setComponentEnabledSetting(mReceiverComponent, isChecked ?
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);

        if (isChecked) {
            mAnimatorSurprise.setDisplayedChild(ANIMATOR_CHILD_PROGRESS);
            SurpriseService.executeSurprise(SurpriseActivity.this, true);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_surprise_to_su:
                mAnimatorUpgrade.setDisplayedChild(ANIMATOR_CHILD_PROGRESS);
                SurpriseService.upgradeSurprise(SurpriseActivity.this);
                break;

            case R.id.info_surprise:
                if (mFragmentTransactionsAllowed) {
                    InfoDialogFragment.show(this, getText(R.string.Surprise),
                            getText(R.string.Info_surprise));
                }
                break;

            case R.id.info_upgrade:
                if (mFragmentTransactionsAllowed) {
                    InfoDialogFragment.show(this, getText(R.string.su_surprise),
                            getText(R.string.Info_upgrade));
                }
                break;

            default:
                break;
        }
    }

    private final class ServiceReceiver extends BroadcastReceiver {

        final IntentFilter mIntentFilter = new IntentFilter();

        ServiceReceiver() {
            mIntentFilter.addAction(SurpriseService.ACTION_SURPRISE_FINISHED);
            mIntentFilter.addAction(SurpriseService.ACTION_UPGRADE_FINISHED);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case SurpriseService.ACTION_SURPRISE_FINISHED:
                    if (mAnimatorSurprise != null) {
                        mAnimatorSurprise.setDisplayedChild(ANIMATOR_CHILD_CONTENT);
                    }
                    break;

                case SurpriseService.ACTION_UPGRADE_FINISHED:
                    if (mAnimatorUpgrade != null) {
                        mAnimatorUpgrade.setDisplayedChild(ANIMATOR_CHILD_CONTENT);
                    }
                    break;
            }
        }
    }

    public static final class InfoDialogFragment extends DialogFragment {

        private static final String EXTRA_TITLE = ".extras.TITLE";
        private static final String EXTRA_MESSAGE = ".extras.MESSAGE";

        static void show(@NonNull final Activity context,
                         @NonNull final CharSequence title,
                         @NonNull final CharSequence message) {
            final Bundle args = new Bundle();
            args.putCharSequence(EXTRA_TITLE, title);
            args.putCharSequence(EXTRA_MESSAGE, message);

            final InfoDialogFragment f = (InfoDialogFragment) Fragment.instantiate(context,
                    InfoDialogFragment.class.getName(), args);
            f.show(context.getFragmentManager(), TAG_DIALOG);
        }

        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final Bundle args = getArguments();
            return new AlertDialog.Builder(getActivity()).setTitle(args.getCharSequence(EXTRA_TITLE))
                    .setMessage(args.getCharSequence(EXTRA_MESSAGE))
                    .setNeutralButton(android.R.string.ok, null).create();
        }
    }
}
