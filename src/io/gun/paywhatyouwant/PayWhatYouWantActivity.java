package io.gun.paywhatyouwant;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

public class PayWhatYouWantActivity extends Activity {
	
	Button donateButton;
	PayWhatYouWant payWhatYouWant;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //No n00b bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
    }
    
    public void onStart(){
    	super.onStart();
        payWhatYouWant = new PayWhatYouWant(this, getApplicationContext());
    	payWhatYouWant.init();

    	donateButton = (Button)findViewById(R.id.donate);
    	donateButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
		    	payWhatYouWant.askForDonation(5, "Hey, buddy!", "Spare a dollar for a thirsty open source developer?");
				
			}});
    	
    	payWhatYouWant.getDonationAmount();
    	
    }
}