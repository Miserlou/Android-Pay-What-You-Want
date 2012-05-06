package io.gun.paywhatyouwant;

import java.util.HashSet;
import java.util.Set;

import io.gun.paywhatyouwant.BillingService.RequestPurchase;
import io.gun.paywhatyouwant.BillingService.RestoreTransactions;
import io.gun.paywhatyouwant.Consts.PurchaseState;
import io.gun.paywhatyouwant.Consts.ResponseCode;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class PayWhatYouWant {
	
    /**
     * Each product in the catalog is either MANAGED or UNMANAGED.  MANAGED
     * means that the product can be purchased only once per user (such as a new
     * level in a game). The purchase is remembered by Android Market and
     * can be restored if this application is uninstalled and then
     * re-installed. UNMANAGED is used for products that can be used up and
     * purchased multiple times (such as poker chips). It is up to the
     * application to keep track of UNMANAGED products for the user.
     * 
     * For now we will assume that all donations are MANAGED.
     * 
     */
    private enum Managed { MANAGED, UNMANAGED }
	
	//Hold our things.
    private static class CatalogEntry {
        public String sku;
        public int nameId;
        public Managed managed;

        public CatalogEntry(String sku, int nameId, Managed managed) {
            this.sku = sku;
            this.nameId = nameId;
            this.managed = managed;
        }
    }
	
    /** An array of product list entries for the products that can be purchased. */
    private static final CatalogEntry[] CATALOG = new CatalogEntry[] {
        new CatalogEntry("1", R.string.one, Managed.MANAGED),
        new CatalogEntry("2", R.string.two, Managed.MANAGED),
        new CatalogEntry("3", R.string.three, Managed.MANAGED),
        new CatalogEntry("4", R.string.four, Managed.MANAGED),
        new CatalogEntry("5", R.string.five, Managed.MANAGED),
        new CatalogEntry("6", R.string.six, Managed.MANAGED),
        new CatalogEntry("7", R.string.seven, Managed.MANAGED),
        new CatalogEntry("8", R.string.eight, Managed.MANAGED),
        new CatalogEntry("9", R.string.nine, Managed.MANAGED),
        new CatalogEntry("10", R.string.ten, Managed.MANAGED), 
    };
	
	
	// Tastes like it smells.
	private class PayWhatYouWantPurchaseObserver extends PurchaseObserver {
	    public PayWhatYouWantPurchaseObserver(Handler handler) {
	        super(activity, handler);
	    }

	    @Override
	    public void onPurchaseStateChange(PurchaseState purchaseState, String itemId,
	            int quantity, long purchaseTime, String developerPayload) {

	        if (purchaseState == PurchaseState.PURCHASED) {
	            //XXX: Hooray!
	        }
	    }

	    @Override
	    public void onRequestPurchaseResponse(RequestPurchase request,
	            ResponseCode responseCode) {
	    	
	        if (responseCode == ResponseCode.RESULT_OK) {
	            // Sent okay
	        } else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
	            // Canceled
	        } else {
	            // Fucked
	        }
	    }

	    @Override
	    public void onRestoreTransactionsResponse(RestoreTransactions request,
	            ResponseCode responseCode) {
	        if (responseCode == ResponseCode.RESULT_OK) {

	            // Update the shared preferences so that we don't perform
	            // a RestoreTransactions again.
	            SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
	            SharedPreferences.Editor edit = prefs.edit();
	            edit.putBoolean("DB_INITIALIZED", true);
	            edit.commit();
	        } else {

	        }
	    }

		@Override
		public void onBillingSupported(boolean supported) {
			
		}
	}
	
	public Context context = null;
	public Activity activity = null;
	public String PUBLIC_KEY = null;
	public PayWhatYouWantPurchaseObserver payWhatYouWantPurchaseObserver;
	
	public Handler mHandler;
	public BillingService mBillingService;
	public PurchaseDatabase mPurchaseDatabase;
	
	public PayWhatYouWant(Activity a, Context c){
		context = c;
		activity = a;	
	}
	
	public void init(){
        mHandler = new Handler();
        payWhatYouWantPurchaseObserver = new PayWhatYouWantPurchaseObserver(mHandler);
        mBillingService = new BillingService();
        mBillingService.setContext(context);

        mPurchaseDatabase = new PurchaseDatabase(context);

        // Check if billing is supported.
        ResponseHandler.register(payWhatYouWantPurchaseObserver);
//        if (!mBillingService.checkBillingSupported()) {
//            showDialog(DIALOG_CANNOT_CONNECT_ID);
//        }
        
        ResponseHandler.register(payWhatYouWantPurchaseObserver);
        initializeOwnedItems();
	}
	
	public void finish(){
        ResponseHandler.unregister(payWhatYouWantPurchaseObserver);
        mPurchaseDatabase.close();
        mBillingService.unbind();
	}
	
	/**
     * If the database has not been initialized, we send a
     * RESTORE_TRANSACTIONS request to Android Market to get the list of purchased items
     * for this user. This happens if the application has just been installed
     * or the user wiped data. We do not want to do this on every startup, rather, we want to do
     * only when the database needs to be initialized.
     */
    private void restoreDatabase() {
        SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
        boolean initialized = prefs.getBoolean("DB_INITIALIZED", false);
        if (!initialized) {
            mBillingService.restoreTransactions();
        }
    }

    /**
     * Creates a background thread that reads the database and initializes the
     * set of owned items.
     */
    private void initializeOwnedItems() {
        new Thread(new Runnable() {
            public void run() {
                doInitializeOwnedItems();
            }
        }).start();
    }
    
    /**
     * Reads the set of purchased items from the database in a background thread
     * and then adds those items to the set of owned items in the main UI
     * thread.
     */
    private void doInitializeOwnedItems() {
        Cursor cursor = mPurchaseDatabase.queryAllPurchasedItems();
        if (cursor == null) {
            return;
        }

        final Set<String> ownedItems = new HashSet<String>();
        try {
            int productIdCol = cursor.getColumnIndexOrThrow(
                    PurchaseDatabase.PURCHASED_PRODUCT_ID_COL);
            while (cursor.moveToNext()) {
                String productId = cursor.getString(productIdCol);
                ownedItems.add(productId);
            }
        } finally {
            cursor.close();
        }

        // We will add the set of owned items in a new Runnable that runs on
        // the UI thread so that we don't need to synchronize access to
        // mOwnedItems.
        mHandler.post(new Runnable() {
            public void run() {
                //mOwnedItems.addAll(ownedItems);
            }
        });
    }
    
    public boolean askForDonation(int defaultValue, String title, String description){
    	LayoutInflater inflater = (LayoutInflater)
    		    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		    //final View npView = inflater.inflate(R.layout.number_picker_pref, null);
    			final View npView = inflater.inflate(R.layout.seeker, null);
    		    final NumberPicker np = (NumberPicker) npView.findViewById(R.id.pref_num_picker);
    		    final SeekBar seeker = (SeekBar) npView.findViewById(R.id.seekbar);
    		    final TextView donateAmount = (TextView) npView.findViewById(R.id.donateamount);
    		    
    		    seeker.setProgress(defaultValue);
    		    donateAmount.setText("$" + new Integer(defaultValue).toString());
    		    
    		    seeker.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						donateAmount.setText("$" + new Integer(progress).toString());
						
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						// Unused
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						// Unused
					}});
    		    
    		    new AlertDialog.Builder(activity)
    		        .setTitle(title)
    		        .setMessage(description)
    		        .setView(npView)
    		        .setPositiveButton("Donate!",
    		            new DialogInterface.OnClickListener() {
    		                public void onClick(DialogInterface dialog, int whichButton) {
    		                	
    		                	if(seeker.getProgress()<1){
    		                		return;
    		                	}
    		                	
    		                	mBillingService.requestPurchase(CATALOG[seeker.getProgress()-1].sku, null);
    		                }
    		            })
    		            
    		            .setNegativeButton("Cancel",
    		                new DialogInterface.OnClickListener() {
    		                    public void onClick(DialogInterface dialog, int whichButton) {
    		                    }
    		                })
    		            .create().show();

    	return true;
    }

	
	// API
	
	public void setActivity(Activity a){
		activity = a;
	}
	
	public void setContext(Context c){
		context = c;
	}
	
	public void setKey(String k){
		PUBLIC_KEY = k;
	}
	
	public double getDonationAmount(){
		return -1;
	}
	
	public void donate(double amount){
		
	}
	
	public boolean hasDonated(){
		return false;
	}

}
