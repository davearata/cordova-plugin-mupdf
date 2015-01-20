package com.artifex.mupdfdemo;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import android.net.Uri;
import android.content.Intent;
import com.artifex.mupdfdemo.MuPDFActivity;

public class MuPdfPlugin extends CordovaPlugin {
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if ("openPdf".equals(action)) {
      final String fileUrl = args.getString(0);
      final String title = args.getString(1);
      final String headerColor = args.getString(2);
      final CallbackContext delayedCC = callbackContext;
      cordova.getActivity().runOnUiThread(new Runnable(){
        @Override
        public void run() {
          Uri uri = Uri.parse(fileUrl);

          Intent intent = new Intent(cordova.getActivity(), MuPDFActivity.class);

          intent.setAction(Intent.ACTION_VIEW);
          intent.putExtra(MuPDFActivity.KEY_TITLE, title);
          intent.putExtra(MuPDFActivity.KEY_HEADER_COLOR, headerColor);
          intent.setData(uri);

          cordova.getActivity().startActivity(intent);
          delayedCC.success();
        }
      });

      return true;
    }
    return false;  // Returning false results in a "MethodNotFound" error.
  }
}
