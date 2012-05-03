package io.gun.paywhatyouwant;

import android.app.Activity;
import android.os.Bundle;

public class PayWhatYouWantActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        
    }
    
    public void onStart(){
    	super.onStart();
    	PayWhatYouWant payWhatYouWant = new PayWhatYouWant(this, getApplicationContext());
    	payWhatYouWant.init();
    	payWhatYouWant.askForDonation(1, "Hey, buddy!", "Got a dollar?");
    }
}