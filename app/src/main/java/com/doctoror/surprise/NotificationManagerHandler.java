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

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;


public final class NotificationManagerHandler {

    private static NotificationManagerHandler sInstance;

    public static synchronized NotificationManagerHandler getInstance(@NonNull final Context context) {
        if (sInstance == null) {
            sInstance = new NotificationManagerHandler(context.getApplicationContext());
        }
        return sInstance;
    }

    @NonNull
    private final NotificationManager mNotificationManager;

    @NonNull
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private NotificationManagerHandler(@NonNull final Context context) {
        mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void showNotification(final int id, @NonNull final Notification notification) {
        mHandler.post(new ShowNotificationRunnable(mNotificationManager, id, notification));
    }

    private final class ShowNotificationRunnable implements Runnable {

        @NonNull
        private final NotificationManager mNotificationManager;

        @NonNull
        private final Notification mNotification;

        private final int mId;

        ShowNotificationRunnable(@NonNull final NotificationManager notificationManager,
                                 final int id,
                                 @NonNull final Notification notification) {
            mNotificationManager = notificationManager;
            mId = id;
            mNotification = notification;
        }

        @Override
        public void run() {
            mNotificationManager.notify(mId, mNotification);
        }
    }
}
