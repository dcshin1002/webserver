package com.lge.pickitup;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Checkable;

public class CheckableLinearLayout extends LinearLayout implements Checkable {
    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setChecked(boolean checked) {
        CheckBox cb = findViewById(R.id.listItemCheck);
        if (cb.isChecked() != checked) {
            cb.setChecked(checked);
        }
    }

    @Override
    public boolean isChecked() {
        CheckBox cb = findViewById(R.id.listItemCheck);
        return cb.isChecked() ;
    }

    @Override
    public void toggle() {
        CheckBox cb = findViewById(R.id.listItemCheck) ;
        setChecked(cb.isChecked() ? false : true) ;
    }
}
