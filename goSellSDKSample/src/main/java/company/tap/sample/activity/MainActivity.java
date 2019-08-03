package company.tap.sample.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.widget.Toast;

import company.tap.gosellapi.GoSellSDK;
import company.tap.gosellapi.internal.api.callbacks.GoSellError;
import company.tap.gosellapi.internal.api.models.Authorize;
import company.tap.gosellapi.internal.api.models.Charge;
import company.tap.gosellapi.internal.api.models.PhoneNumber;
import company.tap.gosellapi.internal.api.models.SaveCard;
import company.tap.gosellapi.internal.api.models.SavedCard;
import company.tap.gosellapi.internal.api.models.Token;
import company.tap.gosellapi.open.buttons.PayButtonView;
import company.tap.gosellapi.open.controllers.SDKSession;
import company.tap.gosellapi.open.controllers.ThemeObject;
import company.tap.gosellapi.open.delegate.SessionDelegate;
import company.tap.gosellapi.open.enums.AppearanceMode;
import company.tap.gosellapi.open.enums.TransactionMode;
import company.tap.gosellapi.open.models.CardsList;
import company.tap.gosellapi.open.models.Customer;
import company.tap.gosellapi.open.models.Receipt;
import company.tap.gosellapi.open.models.TapCurrency;
import company.tap.sample.R;
import company.tap.sample.managers.SettingsManager;
import company.tap.sample.viewmodels.CustomerViewModel;


public class MainActivity extends AppCompatActivity implements SessionDelegate {
    private final int SDK_REQUEST_CODE = 1001;
    private SDKSession sdkSession;
    private PayButtonView payButtonView;
    private SettingsManager settingsManager;
    private ProgressDialog progress;




    private static RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private static RecyclerView recyclerView;
    private static ArrayList<SavedCard> data;
    static View.OnClickListener myOnClickListener;
    private static ArrayList<Integer> removedItems;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settingsManager = SettingsManager.getInstance();
        settingsManager.setPref(this);

        // start tap goSellSDK
        startSDK();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(settingsManager==null)
        {
            settingsManager = SettingsManager.getInstance();
            settingsManager.setPref(this);
        }
    }

    /**
     * Integrating SDK.
     */
    private void startSDK(){
        /**
         * Required step.
         * Configure SDK with your Secret API key and App Bundle name registered with tap company.
         */
        configureApp();

        /**
         * Optional step
         * Here you can configure your app theme (Look and Feel).
         */
        configureSDKThemeObject();

        /**
         * Required step.
         * Configure SDK Session with all required data.
         */
        configureSDKSession();

        /**
         * Required step.
         * Choose between different SDK modes
         */
        configureSDKMode();

        /**
         * If you included Tap Pay Button then configure it first, if not then ignore this step.
         */
        initPayButton();

    }

    /**
     * Required step.
     * Configure SDK with your Secret API key and App Bundle name registered with tap company.
     */
    private void configureApp(){
        GoSellSDK.init(this, "sk_test_kovrMB0mupFJXfNZWx6Etg5y","company.tap.goSellSDKExample");  // to be replaced by merchant
    }
    /**
     * Configure SDK Theme
     */
    private void configureSDKThemeObject() {

        ThemeObject.getInstance()
        .setAppearanceMode(AppearanceMode.WINDOWED_MODE)

        .setHeaderFont(Typeface.createFromAsset(getAssets(),"fonts/roboto_light.ttf"))
        .setHeaderTextColor(getResources().getColor(R.color.black1))
        .setHeaderTextSize(17)
        .setHeaderBackgroundColor(getResources().getColor(R.color.french_gray_new))


        .setCardInputFont(Typeface.createFromAsset(getAssets(),"fonts/roboto_light.ttf"))
        .setCardInputTextColor(getResources().getColor(R.color.black))
        .setCardInputInvalidTextColor(getResources().getColor(R.color.red))
        .setCardInputPlaceholderTextColor(getResources().getColor(R.color.gray))


        .setSaveCardSwitchOffThumbTint(getResources().getColor(R.color.french_gray_new))
        .setSaveCardSwitchOnThumbTint(getResources().getColor(R.color.vibrant_green))
        .setSaveCardSwitchOffTrackTint(getResources().getColor(R.color.french_gray))
        .setSaveCardSwitchOnTrackTint(getResources().getColor(R.color.vibrant_green_pressed))

        .setScanIconDrawable(getResources().getDrawable(R.drawable.btn_card_scanner_normal))

        .setPayButtonResourceId(R.drawable.btn_pay_selector)  //btn_pay_merchant_selector
        .setPayButtonFont(Typeface.createFromAsset(getAssets(),"fonts/roboto_light.ttf"))

        .setPayButtonDisabledTitleColor(getResources().getColor(R.color.white))
        .setPayButtonEnabledTitleColor(getResources().getColor(R.color.white))
        .setPayButtonTextSize(14)
        .setPayButtonLoaderVisible(true)
        .setPayButtonSecurityIconVisible(true)
        ;

    }


    /**
     * Configure SDK Session
     */
    private void configureSDKSession() {

        // Instantiate SDK Session
        if(sdkSession==null) sdkSession = new SDKSession();   //** Required **

        // pass your activity as a session delegate to listen to SDK internal payment process follow
        sdkSession.addSessionDelegate(this);    //** Required **

        // initiate PaymentDataSource
        sdkSession.instantiatePaymentDataSource();    //** Required **

        // set transaction currency associated to your account
        sdkSession.setTransactionCurrency(new TapCurrency("KWD"));    //** Required **

        // Using static CustomerBuilder method available inside TAP Customer Class you can populate TAP Customer object and pass it to SDK
        sdkSession.setCustomer(getCustomer());    //** Required **

        // Set Total Amount. The Total amount will be recalculated according to provided Taxes and Shipping
        sdkSession.setAmount(new BigDecimal(40));  //** Required **

        // Set Payment Items array list
        sdkSession.setPaymentItems(new ArrayList<>());// ** Optional ** you can pass empty array list

        // Set Taxes array list
        sdkSession.setTaxes(new ArrayList<>());// ** Optional ** you can pass empty array list

        // Set Shipping array list
        sdkSession.setShipping(new ArrayList<>());// ** Optional ** you can pass empty array list

        // Post URL
        sdkSession.setPostURL(""); // ** Optional **

        // Payment Description
        sdkSession.setPaymentDescription(""); //** Optional **

        // Payment Extra Info
        sdkSession.setPaymentMetadata(new HashMap<>());// ** Optional ** you can pass empty array hash map

        // Payment Reference
        sdkSession.setPaymentReference(null); // ** Optional ** you can pass null

        // Payment Statement Descriptor
        sdkSession.setPaymentStatementDescriptor(""); // ** Optional **

        // Enable or Disable Saving Card
        sdkSession.isUserAllowedToSaveCard(true); //  ** Required ** you can pass boolean

        // Enable or Disable 3DSecure
        sdkSession.isRequires3DSecure(true);

        //Set Receipt Settings [SMS - Email ]
        sdkSession.setReceiptSettings(new Receipt(false,false)); // ** Optional ** you can pass Receipt object or null

        // Set Authorize Action
        sdkSession.setAuthorizeAction(null); // ** Optional ** you can pass AuthorizeAction object or null

        sdkSession.setDestination(null); // ** Optional ** you can pass Destinations object or null

        sdkSession.setMerchantID(null); // ** Optional ** you can pass merchant id or null

    }


    /**
     * Configure SDK Theme
     */
    private void configureSDKMode(){

        /**
         * You have to choose only one Mode of the following modes:
         * Note:-
         *      - In case of using PayButton, then don't call sdkSession.start(this); because the SDK will start when user clicks the tap pay button.
         */
        //////////////////////////////////////////////////////    SDK with UI //////////////////////
        /**
         * 1- Start using  SDK features through SDK main activity (With Tap CARD FORM)
         */
        startSDKWithUI();

        //////////////////////////////////////////////////////    SDK Tokenization without UI //////////////////////
        /**
         * 2- Start using  SDK to tokenize your card without using SDK main activity (Without Tap CARD FORM)
         * After the SDK finishes card tokenization, it will notify this activity with tokenization result in either
         * cardTokenizedSuccessfully(@NonNull String token) or sdkError(@Nullable GoSellError goSellError)
         */
//          startSDKTokenizationWithoutUI();

        //////////////////////////////////////////////////////    SDK Saving card without UI //////////////////////
        /**
         *  3- Start using  SDK to save your card without using SDK main activity ((Without Tap CARD FORM))
         *  After the SDK finishes card tokenization, it will notify this activity with save card result in either
         *  cardSaved(@NonNull Charge charge) or sdkError(@Nullable GoSellError goSellError)
         *
         */
//         startSDKSavingCardWithoutUI();
    }


    /**
     * Start using  SDK features through SDK main activity
     */
    private void startSDKWithUI(){
        if(sdkSession!=null){
           TransactionMode trx_mode =(settingsManager!=null)? settingsManager.getTransactionsMode("key_sdk_transaction_mode"): TransactionMode.PURCHASE;
            // set transaction mode [TransactionMode.PURCHASE - TransactionMode.AUTHORIZE_CAPTURE - TransactionMode.SAVE_CARD - TransactionMode.TOKENIZE_CARD ]
            sdkSession.setTransactionMode(trx_mode);    //** Required **
            // if you are not using tap button then start SDK using the following call
            //sdkSession.start(this);
        }
    }


    /**
     * Start using  SDK to tokenize your card without using SDK main activity
     */

    private void startSDKTokenizationWithoutUI(){
        if(sdkSession!=null){
            // set transaction mode [ TransactionMode.TOKENIZE_CARD_NO_UI ]
            sdkSession.setTransactionMode(TransactionMode.TOKENIZE_CARD_NO_UI);    //** Required **
            // pass card info to SDK
            sdkSession.setCardInfo("5123450000000008","05","21","100","Haitham Elsheshtawy",null); //** Required **
            // if you are not using tap button then start SDK using the following call
           // sdkSession.start(this);
        }
    }


    /**
     * Start using  SDK to save your card without using SDK main activity
     */
    private void startSDKSavingCardWithoutUI(){
        if(sdkSession!=null){
            // set transaction mode [ TransactionMode.SAVE_CARD_NO_UI ]
            sdkSession.setTransactionMode(TransactionMode.SAVE_CARD_NO_UI);    //** Required **
            // pass card info to SDK
            sdkSession.setCardInfo("5123450000000008","05","21","100","Haitham Elsheshtawy",null); //** Required **
            // if you are not using tap button then start SDK using the following call
//            sdkSession.start(this);
        }
    }


    /**
     * Include pay button in merchant page
     */
    private void initPayButton() {

        payButtonView = findViewById(R.id.payButtonId);

        payButtonView.setupFontTypeFace(ThemeObject.getInstance().getPayButtonFont());

        payButtonView.setupTextColor(ThemeObject.getInstance().getPayButtonEnabledTitleColor(),
                ThemeObject.getInstance().getPayButtonDisabledTitleColor());
//
        payButtonView.getPayButton().setTextSize(ThemeObject.getInstance().getPayButtonTextSize());
//
        payButtonView.getSecurityIconView().setVisibility(ThemeObject.getInstance().isPayButtSecurityIconVisible()?View.VISIBLE:View.INVISIBLE);

        payButtonView.setBackgroundSelector(ThemeObject.getInstance().getPayButtonResourceId());

        if(sdkSession!=null){
            TransactionMode trx_mode = sdkSession.getTransactionMode();
            if(trx_mode!=null){

                if (TransactionMode.SAVE_CARD == trx_mode  || TransactionMode.SAVE_CARD_NO_UI ==trx_mode) {
                    payButtonView.getPayButton().setText(getString(company.tap.gosellapi.R.string.save_card));
                }
                else if(TransactionMode.TOKENIZE_CARD == trx_mode || TransactionMode.TOKENIZE_CARD_NO_UI == trx_mode){
                    payButtonView.getPayButton().setText(getString(company.tap.gosellapi.R.string.tokenize));
                }
                else {
                    payButtonView.getPayButton().setText(getString(company.tap.gosellapi.R.string.pay));
                }
            }else{
                configureSDKMode();
            }
            sdkSession.setButtonView(payButtonView, this, SDK_REQUEST_CODE);
        }


    }


    //    //////////////////////////////////////////////////////  List Saved Cards  ////////////////////////

    /**
     * retrieve list of saved cards from the backend.
     */
    private void listSavedCards(){
        if(sdkSession!=null)
            sdkSession.listAllCards("cus_s4H13120191115x0R12606480",this);
    }

//    //////////////////////////////////////////////////////  Overridden section : Session Delegation ////////////////////////

    @Override
    public void paymentSucceed(@NonNull Charge charge) {

        System.out.println("Payment Succeeded : "+ charge.getStatus());
        System.out.println("Payment Succeeded : "+ charge.getDescription());
        System.out.println("Payment Succeeded : "+ charge.getResponse().getMessage());
        saveCustomerRefInSession(charge);
        configureSDKSession();
        showDialog(charge.getId(),charge.getResponse().getMessage(),company.tap.gosellapi.R.drawable.ic_checkmark_normal);
    }

    @Override
    public void paymentFailed(@Nullable Charge charge) {
        System.out.println("Payment Failed : "+ charge.getStatus());
        System.out.println("Payment Failed : "+ charge.getDescription());
        System.out.println("Payment Failed : "+ charge.getResponse().getMessage());


        showDialog(charge.getId(),charge.getResponse().getMessage(),company.tap.gosellapi.R.drawable.icon_failed);
    }

    @Override
    public void authorizationSucceed(@NonNull Authorize authorize) {
        System.out.println("Authorize Succeeded : "+ authorize.getStatus());
        System.out.println("Authorize Succeeded : "+ authorize.getResponse().getMessage());
        saveCustomerRefInSession(authorize);
        configureSDKSession();
        showDialog(authorize.getId(),authorize.getResponse().getMessage(),company.tap.gosellapi.R.drawable.ic_checkmark_normal);
    }

    @Override
    public void authorizationFailed(Authorize authorize) {
        System.out.println("Authorize Failed : "+ authorize.getStatus());
        System.out.println("Authorize Failed : "+ authorize.getDescription());
        System.out.println("Authorize Failed : "+ authorize.getResponse().getMessage());
        showDialog(authorize.getId(),authorize.getResponse().getMessage(),company.tap.gosellapi.R.drawable.icon_failed);
    }


    @Override
    public void cardSaved(@NonNull Charge charge) {
        // Cast charge object to SaveCard first to get all the Card info.
        if(charge instanceof SaveCard){
            System.out.println("Card Saved Succeeded : first six digits : "+ ((SaveCard)charge).getCard().getFirstSix() + "  last four :"+ ((SaveCard)charge).getCard().getLast4());
        }
        System.out.println("Card Saved Succeeded : "+ charge.getStatus());
        System.out.println("Card Saved Succeeded : "+ charge.getDescription());
        System.out.println("Card Saved Succeeded : "+ charge.getResponse(). getMessage());
        saveCustomerRefInSession(charge);
        showDialog(charge.getId(),charge.getStatus().toString(),company.tap.gosellapi.R.drawable.ic_checkmark_normal);
    }

    @Override
    public void cardSavingFailed(@NonNull Charge charge) {
        System.out.println("Card Saved Failed : "+ charge.getStatus());
        System.out.println("Card Saved Failed : "+ charge.getDescription());
        System.out.println("Card Saved Failed : "+ charge.getResponse().getMessage());
        showDialog(charge.getId(),charge.getStatus().toString(),company.tap.gosellapi.R.drawable.icon_failed);
    }

    @Override
    public void cardTokenizedSuccessfully(@NonNull Token token) {
        System.out.println("Card Tokenized Succeeded : ");
        System.out.println("Token card : "+token.getCard().getFirstSix() + " **** "+ token.getCard().getLastFour() );
        System.out.println("Token card : "+token.getCard().getFingerprint() +  " **** "+ token.getCard().getFunding() );
        System.out.println("Token card : "+token.getCard().getId() +" ****** "+ token.getCard().getName());
        System.out.println("Token card : "+token.getCard().getAddress() +" ****** "+ token.getCard().getObject());
        System.out.println("Token card : "+token.getCard().getExpirationMonth() +" ****** "+ token.getCard().getExpirationYear());

        showDialog(token. getId(),"Token",company.tap.gosellapi.R.drawable.ic_checkmark_normal);
    }

    @Override
    public void savedCardsList(@NonNull CardsList cardsList) {
        System.out.println(" Card List Response Code : "+cardsList.getResponseCode());
        System.out.println(" Card List Top 10 : "+cardsList.getCards().size());
        System.out.println(" Card List Has More : "+cardsList.isHas_more());

        showSavedCardsDialog(cardsList);
    }


    @Override
    public void sdkError(@Nullable GoSellError goSellError) {
        if(progress!=null)
            progress.dismiss();
        if(goSellError!=null) {
            System.out.println("SDK Process Error : " + goSellError.getErrorBody());
            System.out.println("SDK Process Error : " + goSellError.getErrorMessage());
            System.out.println("SDK Process Error : " + goSellError.getErrorCode());
            showDialog(goSellError.getErrorCode() + "", goSellError.getErrorMessage(), company.tap.gosellapi.R.drawable.icon_failed);
        }
    }


    @Override
    public void sessionIsStarting() {
        System.out.println(" Session Is Starting.....");
    }

    @Override
    public void sessionHasStarted() {
        System.out.println(" Session Has Started .......");
    }


    @Override
    public void sessionCancelled() {
        Log.d("MainActivity","Session Cancelled.........");
    }

    @Override
    public void sessionFailedToStart() {
        Log.d("MainActivity","Session Failed to start.........");
    }


    @Override
    public void invalidCardDetails() {
        System.out.println(" Card details are invalid....");
    }

    @Override
    public void backendUnknownError(String message) {
        System.out.println("Backend Un-Known error.... : "+ message);
    }

    @Override
    public void invalidTransactionMode() {
        System.out.println(" invalidTransactionMode  ......");
    }

    @Override
    public void invalidCustomerID() {
        if(progress!=null)
            progress.dismiss();
        System.out.println("Invalid Customer ID .......");

    }


/////////////////////////////////////////////////////////  needed only for demo ////////////////////


    public void showSavedCardsDialog(CardsList cardsList){
        if(progress!=null)
            progress.dismiss();

        if(cardsList!=null && cardsList.getCards()!=null && cardsList.getCards().size()==0 ) {
            Toast.makeText(this,"There is no card saved for this customer",Toast.LENGTH_LONG).show();
            return;
        }

        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
//        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        data = new ArrayList<SavedCard>();

        removedItems = new ArrayList<Integer>();

        adapter = new CustomAdapter(cardsList.getCards());
        recyclerView.setAdapter(adapter);


    }

    public void openSettings(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private Customer getCustomer() { // test customer id cus_Kh1b4220191939i1KP2506448

        Customer customer = (settingsManager!=null)?settingsManager.getCustomer():null;

        PhoneNumber   phoneNumber = customer!=null ? customer.getPhone(): new PhoneNumber("965","65562630");

        return new Customer.CustomerBuilder("cus_s4H13120191115x0R12606480").email("abc@abc.com").firstName("firstname")
                .lastName("lastname").metadata("").phone(new PhoneNumber(phoneNumber.getCountryCode(),phoneNumber.getNumber()))
                .middleName("middlename").build();
    }

    private void showDialog(String chargeID, String msg,int icon)
    {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        PopupWindow popupWindow;
        try {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (inflater != null) {

                View layout = inflater.inflate(company.tap.gosellapi.R.layout.charge_status_layout, findViewById(
                        company.tap.gosellapi.R.id.popup_element));

                popupWindow = new PopupWindow(layout, width, 250, true);

                ImageView status_icon = layout.findViewById(company.tap.gosellapi.R.id.status_icon);
                TextView statusText = layout.findViewById(company.tap.gosellapi.R.id.status_text);
                TextView chargeText = layout.findViewById(company.tap.gosellapi.R.id.charge_id_txt);
                status_icon.setImageResource(icon);
//                status_icon.setVisibility(View.INVISIBLE);
                chargeText.setText(chargeID);
                statusText.setText((msg!=null&& msg.length()>30)?msg.substring(0,29):msg);



                popupWindow.showAtLocation(layout, Gravity.TOP, 0, 50);
                popupWindow.getContentView().startAnimation(AnimationUtils.loadAnimation(this, R.anim.popup_show));

                setupTimer(popupWindow);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void setupTimer(PopupWindow popupWindow) {
        // Hide after some seconds
        final Handler handler = new Handler();
        final Runnable runnable = () -> {
            if (popupWindow.isShowing()) {
                popupWindow.dismiss();
            }
        };

        popupWindow.setOnDismissListener(() -> handler.removeCallbacks(runnable));

        handler.postDelayed(runnable, 4000);
    }

    private void saveCustomerRefInSession(Charge charge) {
        SharedPreferences preferences =  PreferenceManager.getDefaultSharedPreferences(this);

        Gson gson = new Gson();

        String response = preferences.getString("customer" , "");


        ArrayList<CustomerViewModel> customersList = gson.fromJson(response,
                new TypeToken<List<CustomerViewModel>>(){}.getType());

        if(customersList!=null) {
            customersList.clear();
            customersList.add(new CustomerViewModel(
                    charge.getCustomer().getIdentifier(),
                    charge.getCustomer().getFirstName(),
                    charge.getCustomer().getMiddleName(),
                    charge.getCustomer().getLastName(),
                    charge.getCustomer().getEmail(),
                    charge.getCustomer().getPhone().getCountryCode(),
                    charge.getCustomer().getPhone().getNumber()));

            String data = gson.toJson(customersList);

            writeCustomersToPreferences(data, preferences);
        }
    }


    private void writeCustomersToPreferences(String data, SharedPreferences preferences){
        SharedPreferences.Editor editor =  preferences.edit();
        editor.putString("customer",data);
        editor.commit();
    }


    public void getCustomerSavedCardsList(View view) {
        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.show();
        listSavedCards();
    }


    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.MyViewHolder> {

        private ArrayList<SavedCard> dataSet;

        public  class MyViewHolder extends RecyclerView.ViewHolder {

            TextView textViewName;
            TextView textViewVersion;
            TextView textViewexp;
            ImageView imageViewIcon;

            public MyViewHolder(View itemView) {
                super(itemView);
                this.textViewName = (TextView) itemView.findViewById(R.id.textViewName);
                this.textViewVersion = (TextView) itemView.findViewById(R.id.textViewVersion);
                this.textViewexp = (TextView) itemView.findViewById(R.id.textViewexp);
                this.imageViewIcon = (ImageView) itemView.findViewById(R.id.imageView);
            }
        }

        public CustomAdapter(ArrayList<SavedCard> data) {
            this.dataSet = data;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent,
                                               int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cards_layout, parent, false);

            // view.setOnClickListener(MainActivity.myOnClickListener);

            MyViewHolder myViewHolder = new MyViewHolder(view);
            return myViewHolder;
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, final int listPosition) {

            TextView textViewName = holder.textViewName;
            TextView textViewVersion = holder.textViewVersion;
            TextView textViewexp = holder.textViewexp;
            ImageView imageView = holder.imageViewIcon;

            textViewName.setText(dataSet.get(listPosition).getFirstSix() + " ***** " + dataSet.get(listPosition).getLastFour());
            textViewVersion.setText((dataSet.get(listPosition)).getExp_month() + " / "+ (dataSet.get(listPosition)).getExp_year());
            imageView.setImageResource(R.drawable.cards1);
        }

        @Override
        public int getItemCount() {
            return dataSet.size();
        }
    }

}