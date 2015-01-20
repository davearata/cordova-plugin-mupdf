package com.artifex.mupdfdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import android.os.Handler;

import com.theirapp.theemployeeapp.R;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

class ThreadPerTaskExecutor implements Executor {
    @Override
    public void execute(Runnable r) {
        new Thread(r).start();
    }
}

public class MuPDFActivity extends Activity implements FilePicker.FilePickerSupport {
    private final int    PRINT_REQUEST=1;
    private final Handler mHandler = new Handler();
    private MuPDFCore core;
    private String mFileName;
    private MuPDFReaderView mDocView;
    private View mButtonsView;
    private boolean mButtonsVisible;
    private View mTopBarMain;
    private View mNotesButton;
    private View mShareButton;
    private TextView mFilenameView;
    private SeekBar mPageSlider;
    private int mPageSliderRes;
    private TextView mPageNumberView;
    private AlertDialog.Builder mAlertBuilder;
    private boolean mAlertsActive = false;
    private AsyncTask<Void, Void, MuPDFAlert> mAlertTask;
    private AlertDialog mAlertDialog;
    private int currentPageIndex;
    private String title;
    private String headerColor;

    public final static String KEY_TITLE = "com.artifex.mupdfdemo.mupdfactivity.KEY_TITLE";
    public final static String KEY_HEADER_COLOR = "com.artifex.mupdfdemo.mupdfactivity.KEY_HEADER_COLOR";

    public void createAlertWaiter() {
        mAlertsActive = true;
        // All mupdf library calls are performed on asynchronous tasks to avoid stalling
        // the UI. Some calls can lead to javascript-invoked requests to display an
        // alert dialog and collect a reply from the user. The task has to be blocked
        // until the user's reply is received. This method creates an asynchronous task,
        // the purpose of which is to wait of these requests and produce the dialog
        // in response, while leaving the core blocked. When the dialog receives the
        // user's response, it is sent to the core via replyToAlert, unblocking it.
        // Another alert-waiting task is then created to pick up the next alert.
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
        if (mAlertDialog != null) {
            mAlertDialog.cancel();
            mAlertDialog = null;
        }
        mAlertTask = new AsyncTask<Void, Void, MuPDFAlert>() {

            @Override
            protected MuPDFAlert doInBackground(Void... arg0) {
                if (!mAlertsActive)
                    return null;

                return core.waitForAlert();
            }

            @Override
            protected void onPostExecute(final MuPDFAlert result) {
                // core.waitForAlert may return null when shutting down
                if (result == null)
                    return;
                final MuPDFAlert.ButtonPressed pressed[] = new MuPDFAlert.ButtonPressed[3];
                for (int i = 0; i < 3; i++)
                    pressed[i] = MuPDFAlert.ButtonPressed.None;
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mAlertDialog = null;
                        if (mAlertsActive) {
                            int index = 0;
                            switch (which) {
                                case AlertDialog.BUTTON_POSITIVE:
                                    index = 0;
                                    break;
                                case AlertDialog.BUTTON_NEGATIVE:
                                    index = 1;
                                    break;
                                case AlertDialog.BUTTON_NEUTRAL:
                                    index = 2;
                                    break;
                            }
                            result.buttonPressed = pressed[index];
                            // Send the user's response to the core, so that it can
                            // continue processing.
                            core.replyToAlert(result);
                            // Create another alert-waiter to pick up the next alert.
                            createAlertWaiter();
                        }
                    }
                };
                mAlertDialog = mAlertBuilder.create();
                mAlertDialog.setTitle(result.title);
                mAlertDialog.setMessage(result.message);
                switch (result.iconType) {
                    case Error:
                        break;
                    case Warning:
                        break;
                    case Question:
                        break;
                    case Status:
                        break;
                }
                switch (result.buttonGroupType) {
                    case OkCancel:
                        mAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), listener);
                        pressed[1] = MuPDFAlert.ButtonPressed.Cancel;
                    case Ok:
                        mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay), listener);
                        pressed[0] = MuPDFAlert.ButtonPressed.Ok;
                        break;
                    case YesNoCancel:
                        mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.cancel), listener);
                        pressed[2] = MuPDFAlert.ButtonPressed.Cancel;
                    case YesNo:
                        mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), listener);
                        pressed[0] = MuPDFAlert.ButtonPressed.Yes;
                        mAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), listener);
                        pressed[1] = MuPDFAlert.ButtonPressed.No;
                        break;
                }
                mAlertDialog.setOnCancelListener(new OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        mAlertDialog = null;
                        if (mAlertsActive) {
                            result.buttonPressed = MuPDFAlert.ButtonPressed.None;
                            core.replyToAlert(result);
                            createAlertWaiter();
                        }
                    }
                });

                mAlertDialog.show();
            }
        };

        mAlertTask.executeOnExecutor(new ThreadPerTaskExecutor());
    }

    public void destroyAlertWaiter() {
        mAlertsActive = false;
        if (mAlertDialog != null) {
            mAlertDialog.cancel();
            mAlertDialog = null;
        }
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
    }

    private MuPDFCore openFile(String path) {
        int lastSlashPos = path.lastIndexOf('/');
        mFileName = lastSlashPos == -1
                ? path
                : path.substring(lastSlashPos + 1);
        System.out.println("Trying to open " + path);
        try {
            core = new MuPDFCore(this, path);
            // New file: drop the old outline data
            OutlineActivityData.set(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return core;
    }

    private MuPDFCore openBuffer(byte buffer[], String magic) {
        System.out.println("Trying to open byte buffer");
        try {
            core = new MuPDFCore(this, buffer, magic);
            // New file: drop the old outline data
            OutlineActivityData.set(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return core;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlertBuilder = new AlertDialog.Builder(this);

        final Intent intent = getIntent();

        title = intent.getStringExtra(KEY_TITLE);
        headerColor = intent.getStringExtra(KEY_HEADER_COLOR);

        if (core == null) {
            core = (MuPDFCore) getLastNonConfigurationInstance();

            if (savedInstanceState != null && savedInstanceState.containsKey("FileName")) {
                mFileName = savedInstanceState.getString("FileName");
            }
        }
        if (core == null) {
            byte buffer[] = null;

            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri uri = intent.getData();
                System.out.println("URI to open is: " + uri);
                if (uri.toString().startsWith("content://")) {
                    String reason = null;
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        int len = is.available();
                        buffer = new byte[len];
                        is.read(buffer, 0, len);
                        is.close();
                    } catch (OutOfMemoryError e) {
                        System.out.println("Out of memory during buffer reading");
                        reason = e.toString();
                    } catch (Exception e) {
                        System.out.println("Exception reading from stream: " + e);

                        // Handle view requests from the Transformer Prime's file manager
                        // Hopefully other file managers will use this same scheme, if not
                        // using explicit paths.
                        // I'm hoping that this case below is no longer needed...but it's
                        // hard to test as the file manager seems to have changed in 4.x.
                        try {
                            Cursor cursor = getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                            if (cursor.moveToFirst()) {
                                String str = cursor.getString(0);
                                if (str == null) {
                                    reason = "Couldn't parse data in intent";
                                } else {
                                    uri = Uri.parse(str);
                                }
                            }
                        } catch (Exception e2) {
                            System.out.println("Exception in Transformer Prime file manager code: " + e2);
                            reason = e2.toString();
                        }
                    }
                    if (reason != null) {
                        Resources res = getResources();
                        AlertDialog alert = mAlertBuilder.create();
                        setTitle(String.format(res.getString(R.string.cannot_open_document_Reason), reason));
                        alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                });
                        alert.show();
                        return;
                    }
                }
                if (buffer != null) {
                    core = openBuffer(buffer, intent.getType());
                } else {
                    core = openFile(Uri.decode(uri.getEncodedPath()));
                }
                SearchTaskResult.set(null);
            }
            if (core != null && core.countPages() == 0) {
                core = null;
            }
        }
        if (core == null) {
            AlertDialog alert = mAlertBuilder.create();
            alert.setTitle(R.string.cannot_open_document);
            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            alert.setOnCancelListener(new OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
            alert.show();
            return;
        }

        createUI(savedInstanceState);
    }

    public void createUI(Bundle savedInstanceState) {
        if (core == null)
            return;

        // Now create the UI.
        // First create the document view
        mDocView = new MuPDFReaderView(this) {
            @Override
            protected void onMoveToChild(int i) {
                if (core == null)
                    return;
                mPageNumberView.setText(String.format("%d / %d", i + 1,
                        core.countPages()));
                currentPageIndex = i;
                mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
                mPageSlider.setProgress(i * mPageSliderRes);
                super.onMoveToChild(i);
            }

            @Override
            protected void onTapMainDocArea() {
                if (!mButtonsVisible) {
                    showButtons();
                } else {
                    hideButtons();
                }
            }

            @Override
            protected void onDocMotion() {
                hideButtons();
            }

            @Override
            protected void onHit(Hit item) {
            }
        };
        mDocView.setAdapter(new MuPDFPageAdapter(this, this, core));

        // Make the buttons overlay, and store all its
        // controls in variables
        makeButtonsView();

        // Set up the page slider
        int smax = Math.max(core.countPages() - 1, 1);
        mPageSliderRes = ((10 + smax - 1) / smax) * 2;

        // Set the file-name text
        if (title == null) {
            mFilenameView.setText(mFileName);
        } else {
          mFilenameView.setText(title);
        }

        if(headerColor == null) {
          mTopBarMain.setBackgroundColor(Color.parseColor(headerColor));
        }

        // mNotesButton.setVisibility(View.GONE);

        // mShareButton.setVisibility(View.GONE);

        // Activate the seekbar
        mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                mDocView.setDisplayedViewIndex((seekBar.getProgress() + mPageSliderRes / 2) / mPageSliderRes);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                updatePageNumView((progress + mPageSliderRes / 2) / mPageSliderRes);
            }
        });


        // Reenstate last state if it was recorded
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        mDocView.setDisplayedViewIndex(prefs.getInt("page" + mFileName, 0));

        if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false))
            showButtons();

        // Stick the document view and the buttons overlay into a parent view
        RelativeLayout layout = new RelativeLayout(this);
        layout.addView(mDocView);
        layout.addView(mButtonsView);
        setContentView(layout);
    }

    public Object onRetainNonConfigurationInstance() {
        MuPDFCore mycore = core;
        core = null;
        return mycore;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mFileName != null && mDocView != null) {
            outState.putString("FileName", mFileName);

            // Store current page in the prefs against the file name,
            // so that we can pick it up each time the file is loaded
            // Other info is needed only for screen-orientation change,
            // so it can go in the bundle
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page" + mFileName, mDocView.getDisplayedViewIndex());
            edit.apply();
        }

        if (!mButtonsVisible)
            outState.putBoolean("ButtonsHidden", true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mFileName != null && mDocView != null) {
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page" + mFileName, mDocView.getDisplayedViewIndex());
            edit.apply();
        }
    }

    public void onDestroy() {
        if (mDocView != null) {
            mDocView.applyToChildren(new ReaderView.ViewMapper() {
                void applyToView(View view) {
                    ((MuPDFView) view).releaseBitmaps();
                }
            });
        }
        if (core != null)
            core.onDestroy();
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
        core = null;
        super.onDestroy();
    }

    private void showButtons() {
        if (core == null)
            return;
        if (!mButtonsVisible) {
            mButtonsVisible = true;
            // Update page number text and slider
            int index = mDocView.getDisplayedViewIndex();
            updatePageNumView(index);
            mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
            mPageSlider.setProgress(index * mPageSliderRes);

            Animation anim = new TranslateAnimation(0, 0, mPageSlider.getHeight(), 0);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mPageSlider.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mPageNumberView.setVisibility(View.VISIBLE);
                }
            });
            mPageSlider.startAnimation(anim);
        }
    }

    private void hideButtons() {
        if (mButtonsVisible) {
            mButtonsVisible = false;
            hideKeyboard();

            Animation anim = new TranslateAnimation(0, 0, 0, mPageSlider.getHeight());
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mPageNumberView.setVisibility(View.INVISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mPageSlider.setVisibility(View.INVISIBLE);
                }
            });
            mPageSlider.startAnimation(anim);
        }
    }

    private void updatePageNumView(int index) {
        if (core == null)
            return;
        mPageNumberView.setText(String.format("%d / %d", index + 1, core.countPages()));
        currentPageIndex = index;
    }

    private void makeButtonsView() {
      mButtonsView = getLayoutInflater().inflate(R.layout.buttons, null);
      mTopBarMain = mButtonsView.findViewById(R.id.topBar0Main);
      // mShareButton = mButtonsView.findViewById(R.id.shareButton);
      // mNotesButton = mButtonsView.findViewById(R.id.notesButton);
      mFilenameView = (TextView) mButtonsView.findViewById(R.id.docNameText);
      mPageSlider = (SeekBar) mButtonsView.findViewById(R.id.pageSlider);
      mPageNumberView = (TextView) mButtonsView.findViewById(R.id.pageNumber);
      mPageNumberView.setVisibility(View.INVISIBLE);
      mPageSlider.setVisibility(View.INVISIBLE);
    }

//    private void showKeyboard() {
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        if (imm != null)
//            imm.showSoftInput(mSearchText, 0);
//    }

    private void hideKeyboard() {
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        if (imm != null)
//            imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mButtonsVisible) {
            hideButtons();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        if (core != null) {
            core.startAlerts();
            createAlertWaiter();
        }

        super.onStart();
    }

    @Override
    protected void onStop() {
        if (core != null) {
            destroyAlertWaiter();
            core.stopAlerts();
        }

        super.onStop();
    }

    @Override
    public void onBackPressed() {
        // if (core != null && core.hasChanges()) {
        //     DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        //         public void onClick(DialogInterface dialog, int which) {
        //             if (which == AlertDialog.BUTTON_POSITIVE)
        //                 core.save();
        //
        //             finish();
        //         }
        //     };
        //     AlertDialog alert = mAlertBuilder.create();
        //     alert.setTitle("MuPDF");
        //     alert.setMessage(getString(R.string.document_has_changes_save_them_));
        //     alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), listener);
        //     alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), listener);
        //     alert.show();
        // } else {
            super.onBackPressed();
        // }
    }

    @Override
    public void performPickFor(FilePicker picker) {

    }

    // private void showInfo(String message) {
    //   mInfoView.setText(message);
    //
    //   int currentApiVersion = android.os.Build.VERSION.SDK_INT;
    //   if (currentApiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
    //     SafeAnimatorInflater safe = new SafeAnimatorInflater((Activity)this, R.animator.info, (View)mInfoView);
    //   } else {
    //     mInfoView.setVisibility(View.VISIBLE);
    //     mHandler.postDelayed(new Runnable() {
    //       public void run() {
    //         mInfoView.setVisibility(View.INVISIBLE);
    //       }
    //     }, 500);
    //   }
    // }

    public void OnPrintButtonClick(View v) {
      printDoc();
    }

    private void printDoc() {
      if (!core.fileFormat().startsWith("PDF")) {
        // showInfo(getString(R.string.format_currently_not_supported));
        return;
      }

      Intent myIntent = getIntent();
      Uri docUri = myIntent != null ? myIntent.getData() : null;

      if (docUri == null) {
        // showInfo(getString(R.string.print_failed));
      }

      if (docUri.getScheme() == null)
      docUri = Uri.parse("file://"+docUri.toString());

      Intent printIntent = new Intent(this, PrintDialogActivity.class);
      printIntent.setDataAndType(docUri, "aplication/pdf");
      printIntent.putExtra("title", title);
      startActivityForResult(printIntent, PRINT_REQUEST);
    }
}
