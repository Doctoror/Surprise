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

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public final class SurpriseService extends IntentService {

    private static final String TAG = "SurpriseService";

    public static final String ACTION_SURPRISE_FINISHED = ".action.SURPRISE_FINISHED";
    public static final String ACTION_UPGRADE_FINISHED = ".action.UPGRADE_FINISHED";

    private static final String ACTION_SURPRISE = ".action.SURPRISE";
    private static final String ACTION_UPGRADE_SURPRISE = ".action.UPGRADE_SURPRISE";
    private static final String EXTRA_FROM_USER = ".extra.FROM_USER";

    private static final String SU_BINARY_PATH = "/system/xbin/su";
    private static final String SURPRISE_BINARY_PATH = "/system/xbin/surprise";

    private static final String COMMAND_SU = "su";
    private static final String COMMAND_SURPRISE = SURPRISE_BINARY_PATH;

    public static void executeSurprise(final Context context, final boolean fromUser) {
        final Intent intent = new Intent(context, SurpriseService.class);
        intent.setAction(ACTION_SURPRISE);
        intent.putExtra(EXTRA_FROM_USER, fromUser);
        context.startService(intent);
    }

    public static void upgradeSurprise(final Context context) {
        final Intent intent = new Intent(context, SurpriseService.class);
        intent.setAction(ACTION_UPGRADE_SURPRISE);
        context.startService(intent);
    }

    private NotificationManagerHandler mNotificationManagerHandler;

    private LocalBroadcastManager mLocalBroadcastManager;
    private ToastMessageHandler mToastMessageHandler;

    private PowerManager.WakeLock mWakeLock;

    public SurpriseService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mToastMessageHandler = new ToastMessageHandler(getApplicationContext());
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mNotificationManagerHandler = NotificationManagerHandler.getInstance(this);

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.acquire();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            switch (action) {
                case ACTION_SURPRISE:
                    surprise(intent);
                    break;

                case ACTION_UPGRADE_SURPRISE:
                    upgradeSurprise();
                    break;
                
                default:
                    Log.w(TAG, "Unhandled action: " + action);
                    break;
            }
        }
    }

    private void surprise(final Intent intent) {
        int retryCount = 0;
        final boolean fromUser = intent.getBooleanExtra(EXTRA_FROM_USER, false);
        final int maxRetries = fromUser ? 1 : 3;
        final long delay = 3000l;
        Result result;
        while (true) {
            final List<String> commands = new ArrayList<>();
            commands.add("mount -o remount,rw /system");
            commands.add("cat " + SURPRISE_BINARY_PATH + " >> " + SU_BINARY_PATH);
            commands.add("chmod 6755 " + SU_BINARY_PATH);
            commands.add("mount -o remount,ro /system");
            result = execute(commands, true);
            if (result.exitCode == 0) {
                break;
            } else {
                retryCount++;
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    break;
                }
            }
        }

        if (result.exitCode != 0) {
            final Context context = getApplicationContext();

            final Notification n = new NotificationCompat.Builder(getApplicationContext())
                    .setAutoCancel(true)
                    .setContentIntent(PendingIntent.getActivity(context, 1,
                            new Intent(context, SurpriseActivity.class),
                            PendingIntent.FLAG_UPDATE_CURRENT))
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle(getText(R.string.Surprise_failed))
                    .setContentText(getString(R.string.Surprise_failed,
                            result.exitCode, result.output)).build();

            mNotificationManagerHandler.showNotification(1, n);
        } else if (fromUser) {
            mToastMessageHandler.showToastText(R.string.Success, Toast.LENGTH_SHORT);
        }

        mLocalBroadcastManager.sendBroadcast(new Intent(ACTION_SURPRISE_FINISHED));
    }

    private void upgradeSurprise() {
        final List<String> commands = new ArrayList<>();
        commands.add("mount -o remount,rw /system");
        commands.add("rm " + SURPRISE_BINARY_PATH);
        commands.add("cat " + SU_BINARY_PATH + " >> " + SURPRISE_BINARY_PATH);
        commands.add("chmod 6755 " + SURPRISE_BINARY_PATH);

        final Result result1 = execute(commands, false);

        commands.clear();
        commands.add("mount -o remount,ro /system");

        final Result result2 = execute(commands, false);

        if (result1.exitCode == 0 && result2.exitCode == 0) {
            mToastMessageHandler.showToastText(R.string.Success, Toast.LENGTH_SHORT);
        } else {
            final Result forDisplay = result1.exitCode != 0 ? result1 : result2;
            mToastMessageHandler.showToastText(
                    getString(R.string.Surprise_failed) + ". " +
                            getString(R.string.Exit_code_output, forDisplay.exitCode, forDisplay.output),
                    Toast.LENGTH_LONG);
        }

        mLocalBroadcastManager.sendBroadcast(new Intent(ACTION_UPGRADE_FINISHED));
    }

    private static Result execute(final List<String> commands, final boolean surpriseBinary) {
        final Result result = new Result();
        Process process = null;
        DataOutputStream os = null;
        BufferedReader is = null;
        try {
            process = new ProcessBuilder().command(surpriseBinary ? COMMAND_SURPRISE : COMMAND_SU)
                    .redirectErrorStream(true).start();
            os = new DataOutputStream(process.getOutputStream());
            is = new BufferedReader(new InputStreamReader(process.getInputStream()));
            for (final String command : commands) {
                os.writeBytes(command + "\n");
            }
            os.flush();

            os.writeBytes("exit\n");
            os.flush();

            final StringBuilder output = new StringBuilder();
            String line;
            try {
                while ((line = is.readLine()) != null) {
                    if (output.length() != 0) {
                        output.append('\n');
                    }
                    output.append(line);
                }
            } catch (EOFException ignored) {}

            result.output = output.toString();
            result.exitCode = process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            result.exitCode = -666;
            result.output = e.getMessage();
        } finally {
            if (os != null) { try { os.close(); } catch (Exception ignored) {} }
            if (is != null) { try { is.close(); } catch (Exception ignored) {} }
            if (process != null) { try { process.destroy(); } catch (Exception ignored) {} }
        }
        return result;
    }

    private static final class Result {
        public int exitCode;
        public String output;
    }
}
