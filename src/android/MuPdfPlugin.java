package com.artifex.mupdfdemo;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.net.Uri;
import android.content.Intent;
import com.artifex.mupdfdemo.MuPDFActivity;

public class MuPdfPlugin extends CordovaPlugin {
  private CallbackContext callbackContext;

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if ("openPdf".equals(action)) {
      this.callbackContext = callbackContext;
      final String fileUrl = args.getString(0);
      final String title = args.getString(1);
      final JSONObject options = args.getJSONObject(2);
      final boolean annotationsEnabled = options.getBoolean("annotationsEnabled");
      final boolean isAnnotatedPdf = options.getBoolean("isAnnotatedPdf");
      final String headerColor = options.getString("headerColor");

      Uri uri = Uri.parse(fileUrl);

      Intent intent = new Intent(cordova.getActivity(), MuPDFActivity.class);

      intent.setAction(Intent.ACTION_VIEW);
      intent.putExtra(MuPDFActivity.KEY_TITLE, title);
      intent.putExtra(MuPDFActivity.KEY_HEADER_COLOR, headerColor);
      intent.putExtra(MuPDFActivity.KEY_ANNOTATIONS_ENABLED, annotationsEnabled);
      intent.putExtra(MuPDFActivity.KEY_IS_ANNOTATED_PDF, isAnnotatedPdf);
      intent.setData(uri);

      cordova.startActivityForResult(this, intent, 0);

      return true;
    }
    return false;  // Returning false results in a "MethodNotFound" error.
  }

  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    switch (requestCode) {
    case 0: //integer matching the integer suplied when starting the activity
         if(resultCode == android.app.Activity.RESULT_OK){
             //in case of success return the string to javascript
             String result = intent.getStringExtra(MuPDFActivity.KEY_SAVE_RESULTS);
             if(result != null && result.length() > 0) {
               try {
                 final JSONObject saveResults = new JSONObject(result);
                 this.callbackContext.success(saveResults);
               } catch (JSONException e) {
                    e.printStackTrace();
                }

             } else {
               this.callbackContext.success();
             }
         }
         else{
             //code launched in case of error
             String message = "";
             if(intent != null) {
                 message = intent.getStringExtra("result");
             }
             this.callbackContext.error(message);
         }
         break;
    default:
         break;
    }
}
}
