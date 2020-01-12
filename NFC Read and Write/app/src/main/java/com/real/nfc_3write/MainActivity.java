package com.real.nfc_3write;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    NfcAdapter nfcAdapter;
    ToggleButton tglReadWrite;
    EditText txtTagContent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcAdapter=NfcAdapter.getDefaultAdapter(this);
        tglReadWrite =(ToggleButton)findViewById(R.id.tglReadWrite);
        txtTagContent =(EditText) findViewById(R.id.et);

    }

    @Override
    protected void onResume() {
        super.onResume();
        enableForegroundDispatchSystem();

    }

    @Override
    protected void onPause() {
        super.onPause();
        disableForegroundDispatchSystem();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent.hasExtra(NfcAdapter.EXTRA_TAG)){

            Toast.makeText(getApplicationContext(),"NFC-Intent",Toast.LENGTH_SHORT).show();

            if(tglReadWrite.isChecked()){

                Parcelable[] parcelables =intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                if(parcelables != null && parcelables.length>0){
                    readTextFromMessage((NdefMessage)parcelables[0]);

                }else{

                    Toast.makeText(getApplicationContext(),"NO NDEF Message Found",Toast.LENGTH_SHORT).show();

                }


            }else{
                Tag tag =intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                NdefMessage ndefMessage=createNdefMessage(txtTagContent.getText()+"");
                writeNdefMessage(tag,ndefMessage);
            }
        }
    }

    private void readTextFromMessage(NdefMessage ndefMessage){
        NdefRecord[] ndefRecords =ndefMessage.getRecords();

        if(ndefRecords!= null && ndefRecords.length>0){
            NdefRecord ndefRecord =ndefRecords[0];
            String tagContent=getTextFromNdefRecord(ndefRecord);
            txtTagContent.setText(tagContent);


        }else{
            Toast.makeText(getApplicationContext(),"NO  NDEF Records Found",Toast.LENGTH_SHORT).show();

        }



    }

    private void enableForegroundDispatchSystem(){

        Intent intent =new Intent(this,MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent =PendingIntent.getActivity(this,0,intent,0);
        IntentFilter[] intentFilters=new IntentFilter[]{};
        nfcAdapter.enableForegroundDispatch(this,pendingIntent,intentFilters,null);

    }


    private void disableForegroundDispatchSystem(){

        nfcAdapter.disableForegroundDispatch(this);

    }

    private void formatTag(Tag tag, NdefMessage ndefMessage){

        try{
            NdefFormatable ndefFormatable=NdefFormatable.get(tag);
            if(ndefFormatable==null){

                Toast.makeText(getApplicationContext(),"Tag is not ndef Formatable",Toast.LENGTH_SHORT).show();
            }
            ndefFormatable.connect();
            ndefFormatable.format(ndefMessage);
            ndefFormatable.close();

        }catch (Exception e){

            Log.e("formatTag",e.getMessage());
        }
    }

    private void writeNdefMessage(Tag tag, NdefMessage ndefMessage){

        try{
            if(tag==null){
                Toast.makeText(getApplicationContext(),"Tag object cannot be null",Toast.LENGTH_SHORT).show();
                return;
            }
            Ndef ndef =Ndef.get(tag); // used to exchange the data
            if(ndef==null){

                //format the tag with ndef format and writes the message
                formatTag(tag,ndefMessage);
            }else{

                ndef.connect();
                if(!ndef.isWritable()){ //checking for writing
                    Toast.makeText(getApplicationContext(),"Tag is not right able",Toast.LENGTH_SHORT).show();
                    ndef.close();
                    return;

                }
                ndef.writeNdefMessage(ndefMessage);
                ndef.close();
                Toast.makeText(getApplicationContext(),"Tag written",Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){

            Log.e("writeNdefMessage",e.getMessage());
        }

    }

    private NdefRecord createTextRecord(String content){

        try{
            byte[] language;
            language= Locale.getDefault().getLanguage().getBytes("UTF-8");

            final byte[] text=content.getBytes("UTF-8");
            final int languageSize=language.length;
            final int textLength=language.length;
            final ByteArrayOutputStream payload=new ByteArrayOutputStream(1+languageSize+textLength);

            payload.write((byte)(languageSize &0x1F));
            payload.write(language,0,languageSize);
            payload.write(text,0,textLength);

            return  new NdefRecord(NdefRecord.TNF_WELL_KNOWN,NdefRecord.RTD_TEXT,new byte[0],payload.toByteArray());

        }catch(Exception e){
            Log.e("createTextRecord",e.getMessage());

        }
        return null;
    }


    private NdefMessage createNdefMessage(String content){

        NdefRecord ndefRecord=createTextRecord(content);
        NdefMessage ndefMessage=new NdefMessage(new NdefRecord[]{ndefRecord});
        return ndefMessage;


    }

    public void TglReadWriteOnClick(View view){

        txtTagContent.setText("");

    }
    public  String getTextFromNdefRecord(NdefRecord ndefRecord){

        String tagContent =null;
        try{
            byte[] payload=ndefRecord.getPayload();
            String textEncoding=((payload[0] & 128)== 0) ? "UTF-8" :"UTF-16";
            int languageSize=payload[0] & 0063;
            tagContent =new String(payload,languageSize+1,payload.length-languageSize-1,textEncoding);

        }catch (Exception e){
            Log.e("getTextFromNdefRecord",e.getMessage());

        }
        return  tagContent;
    }

}
