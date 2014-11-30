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

import java.lang.ref.WeakReference;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * Shows {@link Toast} messages avoiding overflow
 */
public final class ToastMessageManager {

    private static ToastMessageManager sInstance;

    @SuppressWarnings("null")
    @NonNull
    public static synchronized ToastMessageManager getInstance() {
        if (sInstance == null) {
            sInstance = new ToastMessageManager();
        }
        return sInstance;
    }

    private WeakReference<Toast> mToastReference;
    private CharSequence mPreviousMessage;

    private ToastMessageManager() {
    }

    @SuppressWarnings("null")
    public void showTextMessage(@NonNull final Context context,
                                final int message,
                                final int length) {
        showTextMessage(context, context.getText(message), length);
    }

    public void showTextMessage(@NonNull final Context context,
                                @NonNull final CharSequence message,
                                final int length) {
        Toast toast;
        if (mToastReference != null) {
            toast = mToastReference.get();
        } else {
            toast = null;
        }
        if (toast == null || !TextUtils.equals(message, mPreviousMessage)) {
            toast = Toast.makeText(context, message, length);
            mToastReference = new WeakReference<>(toast);
            mPreviousMessage = message;
        }
        if (toast.getView().getWindowToken() == null) {
            toast.show();
        }
    }
}