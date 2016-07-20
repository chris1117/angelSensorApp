package edu.umich.engin.dpm.angel_grabber.stream;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import edu.umich.engin.dpm.angel_grabber.HomeActivity;
import edu.umich.engin.dpm.angel_grabber.R;

/**
 * Created by User on 6/30/2016.
 */
public class AgeActivity extends HomeActivity {

    NumberPicker np;
    TextView tv1, tv2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        np = (NumberPicker) findViewById(R.id.numberPicker1);
        tv1 = (TextView) findViewById(R.id.textView2);
        tv2 = (TextView) findViewById(R.id.textView3);

        np.setMinValue(18);
        np.setMaxValue(50);
        np.setWrapSelectorWheel(false);

        np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

                String Old = "Old Value : ";
                String New = "New Value : ";

                tv1.setText(Old.concat(String.valueOf(oldVal)));
                tv2.setText(New.concat(String.valueOf(newVal)));

                //Intent intent = new Intent(np.getContext(), MainActivity.class);
                //intent.putExtra("number_Selector", np.getValue());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Context context = getApplicationContext();
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){

            case R.id.new_age:

                // int oldVal = 0, newVal = 0;
                np.setWrapSelectorWheel(false);

                String Old = "Old Value : ";
                String New = "New Value : ";

                //    tv1.setText(Old.concat(String.valueOf(oldVal)));
                //   tv2.setText(New.concat(String.valueOf(newVal)));


                Toast toast1 = Toast.makeText(context, "Please Select New Age",
                        Toast.LENGTH_LONG);
                toast1.show();
                break;

            case R.id.save_age:

                int num = np.getValue();

                SharedPreferences saveAge = getSharedPreferences("AgeSaved", MODE_PRIVATE);
                SharedPreferences.Editor editor = saveAge.edit();
                editor.putInt("CurrentAge", num);
                editor.apply();

                Toast toast2 = Toast.makeText(context, "The Age Saved is: " + num,
                        Toast.LENGTH_LONG);
                toast2.show();

                break;

            case R.id.load_age:

                int age_num = np.getValue();
                np.setValue(age_num);

                SharedPreferences loadAge = getSharedPreferences("AgeSaved", MODE_PRIVATE);
                age_num = loadAge.getInt("CurrentAge", 5);

                String OldL = "Old Value : ";
                String NewL = "New Value : ";

                tv1.setText(OldL.concat(String.valueOf(age_num)));
                tv2.setText(NewL.concat(String.valueOf(age_num)));


                Toast toast3 = Toast.makeText(context, "Age Loaded Succesfully",
                        Toast.LENGTH_LONG);
                toast3.show();

                break;
        }

        if (id == R.id.menu_main) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
