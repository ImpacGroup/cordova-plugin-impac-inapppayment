package de.impacgroup.swissrxlogin;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class IMPSwissRxActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("activity_impswiss_rx", "layout", getPackageName()));
    }
}
