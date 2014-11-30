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
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.widget.Toast;

/**
 * {@link Handler} for showing {@link Toast} text messages
 */
public final class ToastMessageHandler extends Handler {

    private static final int MESSAGE_SHOW_TOAST = 1;

    private final WeakReference<Context> mContextReference;
    private final ToastMessageManager mToastMessageManager = ToastMessageManager.getInstance();

    public ToastMessageHandler(@NonNull final Context context) {
        super(context.getMainLooper());
        mContextReference = new WeakReference<>(context);
    }

    @SuppressWarnings("null")
    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {
            case MESSAGE_SHOW_TOAST:
                final Context context = mContextReference.get();
                if (context == null) {
                    return;
                }

                final CharSequence message;
                if (msg.arg1 != -1) {
                    message = context.getText(msg.arg1);
                } else {
                    message = (CharSequence) msg.obj;
                    if (message == null) {
                        throw new IllegalArgumentException("No text res id nor text object is passed");
                    }
                }

                mToastMessageManager.showTextMessage(context, message, msg.arg2);
                break;

            default:
                super.handleMessage(msg);
                break;
        }
    }

    public void showToastText(@NonNull final CharSequence text, final int duration) {
        removeMessages(MESSAGE_SHOW_TOAST);
        sendMessage(obtainMessage(MESSAGE_SHOW_TOAST, -1, duration, text));
    }

    public void showToastText(final int text, final int duration) {
        removeMessages(MESSAGE_SHOW_TOAST);
        sendMessage(obtainMessage(MESSAGE_SHOW_TOAST, text, duration, null));
    }
}