package edu.umich.engin.dpm.angel_grabber.stream;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import edu.umich.engin.dpm.angel_grabber.R;

/**
 * Created by User on 7/15/2016.
 */
public class NotificationActivity extends Activity {

    TextView tv1;
    int max_hr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.notification_main);

        Bundle extras = getIntent().getExtras();
        max_hr = extras.getInt("%HRR");

        tv1 = (TextView) findViewById(R.id.textView2);

        tv1.setText(max_hr);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finish();
    }
}
