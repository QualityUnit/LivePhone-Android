package com.qualityunit.android.liveagentphone.ui.auth.login;

import android.content.Context;
import android.graphics.Rect;
import androidx.appcompat.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;

import com.qualityunit.android.liveagentphone.App;

/**
 * Created by rasto on 7.3.18.
 */

public class UrlEditText extends AppCompatEditText implements TextWatcher {

    private static String PREFIX = "https://";

    public UrlEditText(Context context) {
        super(context);
        addTextChangedListener(this);
    }

    public UrlEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        addTextChangedListener(this);
    }

    public UrlEditText(Context context, AttributeSet attrs,
                       int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        addTextChangedListener(this);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        if (App.ALLOW_HTTP) {
            return;
        }
        if (getText().length() >= PREFIX.length() && selStart < PREFIX.length()) {
            setSelection(PREFIX.length(), selEnd);
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (App.ALLOW_HTTP) {
            return;
        }
        if (focused && TextUtils.isEmpty(getText())) {
            setText(PREFIX);
        }
        if (!focused && PREFIX.equals(getText().toString())) {
            setText("");
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (App.ALLOW_HTTP) {
            return;
        }
        if (isFocused()) {
            String text = getText().toString();
            if (text.startsWith(PREFIX)) {
                return;
            }
            if (text.length() >= PREFIX.length() - 1) {
                String newText = PREFIX + text.substring(PREFIX.length() - 1);
                setText(newText);
                post(new Runnable() {
                    @Override
                    public void run() {
                        UrlEditText.this.setSelection(PREFIX.length());
                    }
                });

            } else {
                setText(PREFIX);
            }
        }
    }
}
