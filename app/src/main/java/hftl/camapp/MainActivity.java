package hftl.camapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;  // Strutz

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.graphics.Bitmap;
import android.widget.Toast;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.escape.CharEscaper;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Chars;
import com.google.common.primitives.Doubles;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
//import org.opencv.core.Point;

import java.io.File;
import java.lang.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//import.java.Math.toIntExact(long);

import static com.google.common.math.DoubleMath.log2;
import static org.opencv.imgproc.Imgproc.calcHist;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.integral;

public class MainActivity extends AppCompatActivity {
    /* gloabal class variables  */
    public
    static ProgressBar progressbar;
    static final String SAVE_DIR = "/Pictures/CamApp/"; /* where to save pictures    */
    private
    CameraBridgeViewBase cameraView; /* variable for camera */
    Button capture_button, mode_button, option_button, action_settings;
    Display display;
    /* defines of different modes   */
    static final int NONE = 0; /* direct display of camera image    */
    static final int QUANTIZE = 1;  /* do quantisation before displaying the image   */
    static final int SAMPLENHOLD = 2;  /*  make image blocky    */
    static final int VOODOO = 3;  /* do voodoo before displaying the image   */
    static final int BONNKMI14 = 4; //Modusnummer definieren
    /* defines of CooDoo settings   */
    static final int OFFSET_A = 0; /*     */
    static final int OFFSET_B = 1;  /*   */
    /* display and picture/frame size   */
    int displayWidth, displayHeight;
    int maxFrameWidth = 0, maxFrameHeight = 0; /* size of displayed image (from openCV) */
    int mode = 0;   /* selected mode */
    int quant_mode = 4; /* do something visible */
    int pixel_mode = 4;
    int voodoo_mode = OFFSET_A;
    int voodoo_y, voodoo_x, voodoo_diff, voodoo_diff_shift=2;


    //Initialisierung und Aktivierung des Frames zuständig. Den OpenCVManager einbinden.
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    cameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        /* "raw" corresponds to subfolder of res folder    */
        final MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.shuttersound);

        getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView( R.layout.activity_main);
        cameraView = (CameraBridgeViewBase) findViewById( R.id.surface_view);
        // in imageprocessing(): cameraView.setVisibility( SurfaceView.VISIBLE);
        // in imageprocessing(): cameraView.setCvCameraViewListener(this);
        /* get size of display  */

        display = getWindowManager().getDefaultDisplay(); /* get acccess    */
        Point size = new Point();
        display.getSize( size );
        displayWidth = size.x;
        displayHeight = size.y;


        int rotation = display.getRotation();

        /* modify orientation of surface */
        switch (rotation) {
            case Surface.ROTATION_90: // video landscape or upside-down, 1
                if (displayWidth > displayHeight)
                    setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                else
                    setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_180:
                if (displayWidth > displayHeight)
                    setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                else
                    setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case Surface.ROTATION_270: // video upside-down, 3
                if (displayWidth > displayHeight)
                    setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                else
                    setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            default : // phone is upright, 0
                if (displayWidth > displayHeight)
                    setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                else
                    setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
/**
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
/**/

        /* not use yet  */
        progressbar = (ProgressBar) findViewById( R.id.progressbar);
        progressbar.setVisibility(View.GONE);

        /* prepare listener for capture button */
        capture_button = (Button) findViewById( R.id.capture_button);
        capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d(TAG, "playing Sound");
                mediaPlayer.setVolume( (float)1.0, (float)1.0); // maximum volume
                mediaPlayer.start();
                // must be stopped some where
                //progressbar.setVisibility( (View.VISIBLE));
                // capture image from camera
                mode = -(mode + 1);

                //Log.d("Mat Click: ", "höhe: " + previewImage.height() + " breite: " + previewImage.width() + " channels: " + previewImage.channels());
            }
        });

        /* prepare listener for mode button */
        mode_button = (Button) findViewById( R.id.mode_button);
        mode_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createModeMenu();
            }
        });

        /* prepare listener for option button */
        option_button = (Button) findViewById( R.id.option_button);
        option_button.setVisibility(View.GONE);
        option_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createOptionMenu();
            }
        });

        cameraView.setOnTouchListener(new View.OnTouchListener() {
             public boolean onTouch(View v, MotionEvent event) {
                 if (event.getAction() == MotionEvent.ACTION_DOWN) {
                     /* get position of touch, use it for something
                      * position is related to display resolution
                      */
                     if (mode == VOODOO) {
                         // normalise to size of displayed image
                         voodoo_x = (int) (maxFrameWidth * event.getX() / displayWidth);
                         voodoo_y = (int) (maxFrameHeight * event.getY() / displayHeight);
                     }
                }
                return true;
            }
        });

        imageProcessing(); /* initialize the image stuff, camera listener    */
        showWelcomeDialog();
    }
    /* dialog that will be shown after starting the app   */
    private void showWelcomeDialog() {
        new AlertDialog.Builder(this)
                .setTitle( R.string.welcome_title)
                .setMessage( R.string.welcome_msg)
                .setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick( DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon( android.R.drawable.ic_dialog_info)
                .show();
    }

    /**
     * * related to OpenCV camera access,
     * http://docs.opencv.org/2.4/doc/tutorials/introduction/android_binary_package/dev_with_OCV_on_Android.html
     * Override-Methoden, um die OneCvView zu schließen oder zu pausieren.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    /* Laden des OpenCvManagers.  */
    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            /* no access to camera or similar   */
            OpenCVLoader.initAsync( OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    //Setzen der Listener und einbinden der Funktionen, der dann eintretenden Aktionen.
    public void imageProcessing() {
        cameraView.setVisibility( SurfaceView.VISIBLE);

        //Überschreiben des Frames mit dem ausgewählten Verfahren.
        cameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStopped() {
            }

            @Override
            public void onCameraViewStarted(int width, int height) {
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                Boolean save_flag;
                Mat mMat;

                if (mode < 0) {
                    mode = -mode - 1;
                    save_flag = true;
                } else {
                    save_flag = false;
                }


                switch (mode)

                {    /* process image dependent on selected mode   */
                    case QUANTIZE:

                        /* uses globally declared quant_mode    */
                        mMat = getQuantizedImage(inputFrame.rgba());
                        if (save_flag) saveImage(mMat);
                        return mMat;

                    case SAMPLENHOLD: /* sample and hold */
                        /* uses globally declared pixel_mode    */
                        mMat = getPixeledImage(inputFrame.rgba());
                        if (save_flag) saveImage(mMat);
                        return mMat;

                    case VOODOO:
                        /* uses globally declared voodoo_x, _y    */
                        mMat = getVoodooImage(inputFrame.rgba());
                        if (save_flag) saveImage(mMat);
                        return mMat;

                    case BONNKMI14: //Bild mit neuem Modus speichern
                        mMat = getBONNKMI14Image(inputFrame.rgba());
                        if (save_flag) saveImage(mMat);
                        return mMat;

                    case 0:      /* no modification */
                    default:
                        if (save_flag) saveImage(inputFrame.rgba());
                        return inputFrame.rgba();
                }
            }
        });
    }

    /* this handles the settings button on device   */
    @Override
    public boolean onCreateOptionsMenu( Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate( R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        // Handle item selection
        switch (id) {
            case R.id.voodoo_settings:
                //createVoodooMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
      * Popup-SUbMenu for voodoo settings
      */
    private void createVoodooMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder( this);
        final String[] modeS = {"Offset A", "Offset B"};
        builder.setTitle( "Choose Method:");
        builder.setSingleChoiceItems( modeS, voodoo_mode, new DialogInterface.OnClickListener() {
            @Override
            public void onClick( DialogInterface dialog, int item) {
                voodoo_mode = item;
                dialog.dismiss();                     // Nach Auswahl wird das Menü geschlossen
            }
        });
        builder.show();
    }

    /*
      * Popup-Menu for mode selection
      */
    private void createModeMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder( this);
        final String[] modeS = {"None", "Quantizer", "Sample'nHold", "Voodoo", "BONNKMI14"}; //Neuen Modus zum Menü hinzugefügt
        builder.setTitle( "Choose Method:");
        builder.setSingleChoiceItems( modeS, mode, new DialogInterface.OnClickListener() {
            @Override
            public void onClick( DialogInterface dialog, int item) {
                mode = item;

                if (mode == NONE ) hideOptionButton(); /* no options for processing */
                else showOptionButton();
                setPreviewSize( );  /* set size of image dependent on mode */

                dialog.dismiss();  /* close menu after selection    */
            }
        });
        builder.show();
    }

    /* option menu  settings
     *
     */
    private void createOptionMenu() {
        AlertDialog.Builder clusterBuilder = new AlertDialog.Builder(this);
        final String[] quantOptions = {"2 x Bitshift", "3 x Bitshift", "4 x Bitshift",
                 "5 x Bitshift", "6 x Bitshift", "7 x Bitshift"};
        final String[] pixelOptions = { "2 x 2 pix", "4 x 4 pix", "6 x 6 pix", "8 x 8 pix",
                 "10 x 10 pix", "12 x 12 pix", "14 x 14 pix", "16 x 16 pix"};
        final String[] voodooOptions = { "Offset A", "Offset B"};
        final String[] BONNKMI14Options = { "Nase", "Mensch"}; //Optionen des Modus als String bereitstellen
        if (mode == QUANTIZE) {
            clusterBuilder.setTitle( "Choose number of bit shifts:");
            /* quant_mode defines the number of bit shifts, this must be mapped to the
             * dialog item number
             *  ( 2, 3, 4, 5, 6, 7) => ( 0, 1, 2, 3, 4, 5)
             */
            clusterBuilder.setSingleChoiceItems( quantOptions, quant_mode-2,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick( DialogInterface dialog, int item) {
                    quant_mode = item + 2;
                    dialog.dismiss();
                }
            });
        } else if (mode == SAMPLENHOLD) {
            clusterBuilder.setTitle("Choose block size");
            /* pixel_mode defines the size of blocks, this must be mapped to the
             * dialog item number
             *  ( 2, 4, 6, 8) => ( 0, 1, 2, 3)
             */
            clusterBuilder.setSingleChoiceItems( pixelOptions, (pixel_mode >> 1) -1,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            pixel_mode = (item + 1) * 2;
                            dialog.dismiss();
                        }
                    });
        }
        else if (mode == VOODOO) {
            clusterBuilder.setTitle("Choose Offset ");
                /*                  */
            clusterBuilder.setSingleChoiceItems( voodooOptions, voodoo_mode,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick( DialogInterface dialog, int item) {
                            voodoo_mode = item;
                            dialog.dismiss();
                        }
                    });


        }

        else if (mode == BONNKMI14){
            clusterBuilder.setTitle("Nasenlänge auswählen "); //options Menü für neuen Modus
                /*                  */
            clusterBuilder.setSingleChoiceItems( BONNKMI14Options, voodoo_mode,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick( DialogInterface dialog, int item) {
                            voodoo_mode = item;
                            dialog.dismiss();
                        }
                    });


        }
        clusterBuilder.show();
    }    /* show options button  */

    public void showOptionButton() {
        option_button.getHandler().post(new Runnable() {
            @Override
            public void run() {
                option_button.setVisibility(View.VISIBLE);
            }
        });
    }

    /* hide option button */
    public void hideOptionButton() {
        option_button.getHandler().post(new Runnable() {
            @Override
            public void run() {
                option_button.setVisibility(View.GONE);
            }
        });
    }

    /* fit size of displayed image to mode
    * make image smaller if mode is computational complex
    *
    */
    public void setPreviewSize() {
        cameraView.disableView();
        switch (mode) {
            case NONE:
                maxFrameWidth = displayWidth;
                maxFrameHeight = displayHeight;
                break;
            case VOODOO:
//                maxFrameWidth = 352;
//                maxFrameHeight = 288;
                maxFrameWidth = 480;
                maxFrameHeight = 320;
                voodoo_x = maxFrameWidth >> 1; /* initialize */
                voodoo_y = maxFrameHeight >> 1;
                break;
            case QUANTIZE:
            case SAMPLENHOLD:
            default:
                maxFrameWidth = 480;
                maxFrameHeight = 320;
                break;
        }
        cameraView.setMaxFrameSize( maxFrameWidth, maxFrameHeight);
        cameraView.enableView();
    }

    private Mat getQuantizedImage( Mat inputFrame) {
        // inputFrame is a Mat structure in RGBA format
        // wenn eine Quantisierung gewählt wurde, dann wird der aktuelle Inputframe quantisiert
        Long size = inputFrame.total() * inputFrame.channels();
        int isize = size.intValue();
        byte buff[] = new byte[isize];
        int shift = quant_mode;
        int mask = (0xFF >> shift) << shift;

        inputFrame.get(0, 0, buff);

        int pos = 0;
        for (byte b : buff) // four components
        {
            // das byte wird durch eine Bitverschiebung nach rechts und links um eine Bitanzahl gekürzt
            // buff[pos] = (byte) ((b >> shift) << shift);
            buff[pos] = (byte) (b & mask); /* slightly faster  than double shift */
            pos++;
            // ignore Alpha
        }

        inputFrame.put(0, 0, buff);

        return inputFrame;
    }
    /* sample and hold ind 2D   */
    private Mat getPixeledImage( Mat inputFrame) {
        // inputFrame is a Mat structure in RGBA format
        int x, y, m, n;
        int px, py, pm, pos;
        int channels = inputFrame.channels();
        int width = inputFrame.width();
        int height = inputFrame.height();
        int isize = height * width * channels; /* compute total size of required attay  */
        byte buff[] = new byte[isize];      /* allocate memory for image data   */
        int step = pixel_mode; /* (0, 1, 2, 3) ==> (2, 4, 6, 8)   */
        final int stride = width * channels;
        final int stride_step = width * channels * step;
        final int stride_chanstep = step * channels;

        inputFrame.get(0, 0, buff);
        /* for all blocks in vertical direction */
        for ( y = 0, py=0; y < height; y+= step, py+=stride_step)
        {   /* for all blocks in horizontal direction */
            for ( x = 0, px = py; x < width; x+=step, px += stride_chanstep)
            {   /* for all pixels in the blocks in vertical direction
                 * remember that blocks must be cropped at bottom image border */
                for ( m=0, pm = px; (m < step) && (y+m < height); m++, pm += stride)
                {   /* for all pixels in the blocks in horizontal direction
                     * remember that blocks must be cropped at right image border */
                    for ( n=0, pos = pm; (n < step) && (x+n < width); n++, pos+=channels)
                    {
                        buff[pos]= buff[px];
                        buff[pos+1]= buff[px+1];
                        buff[pos+2]= buff[px+2];
                    }
                }
            }
        }

        inputFrame.put( 0, 0, buff);

        return inputFrame;
    }

    /* voodoo   */
    private Mat getVoodooImage( Mat inputFrame) {
        // inputFrame is a Mat structure in RGBA format
        Boolean focus_flag;
        int x, y, x2, y2, diff_x, diff_y, yy, xx;
        int py, pos, pos2;
        int radius2;
        double off;
        double xd, yd, wx, wy;
        int channels = inputFrame.channels();
        int width = inputFrame.width();
        int height = inputFrame.height();
        int isize = height * width * channels; /* compute total size of required attay  */
        byte buff1[] = new byte[isize];      /* allocate memory for image data   */
        byte buff2[] = new byte[isize];      /* allocate memory for image data   */
        final int stride = width * channels;

        /* make a copy when in-place operation is not possible */
        Mat cloneFrame = inputFrame.clone();

         /* make voodoo parameter dependent on displayed size  */
        if (maxFrameWidth < maxFrameHeight) voodoo_diff = maxFrameWidth >> voodoo_diff_shift;
        else  voodoo_diff = maxFrameHeight >> voodoo_diff_shift;
        radius2 = voodoo_diff * voodoo_diff;

        if (voodoo_mode == OFFSET_A) off = voodoo_diff;
        else if (voodoo_mode == OFFSET_B) off = (3 * voodoo_diff) / 2;
        else off = voodoo_diff/2;

        inputFrame.get( 0, 0, buff1); /* get address of original data array  */
        cloneFrame.get( 0, 0, buff2); /* get address of copied data array  */
        /* for all blocks in vertical direction */
        for (y = 0, py = 0; y < height; y++, py += stride) {
            diff_y = y - voodoo_y; /* get vertical focus of effect   */
            yy = diff_y * diff_y;
            /* for all blocks in horizontal direction */
            for (x = 0, pos = py; x < width; x++, pos += channels) {
                diff_x = x - voodoo_x; /* get horizontal focus of effect   */
                xx = diff_x * diff_x;
                if (xx + yy < radius2) {
                    /* get new x position   */
                    xd = (x + off);
                    x2 = (int) Math.floor(xd);
                    x2 = Math.max(0, Math.min(x2, width - 2)); /*  ckeck border  */
                    /* get new y position   */
                    yd = (y );
                    y2 = (int) Math.floor(yd);
                    y2 = Math.max(0, Math.min(y2, height - 2)); /* ckeck border  */
                    pos2 = x2 * channels + y2 * stride;

                    buff1[pos] = buff2[pos2];
                    buff1[pos + 1] = buff2[pos2 + 1];
                    buff1[pos + 2] = buff2[pos2 + 2];
                }
            }
        }

        inputFrame.put( 0, 0, buff1); /* put modified data array back to inputFrame */
        cloneFrame.release();  /* release copied data   */

        return inputFrame;
    }

    /* BONNKMI14Image vorerst mit VODOO Funktion kopiert als Gerüst  */
    private Mat getBONNKMI14Image( Mat inputFrame) {
        // inputFrame is a Mat structure in RGBA format
        Boolean focus_flag;
        int x, y, x2, y2, diff_x, diff_y, yy, xx;
        int py, pos, pos2;
        int radius2;
        double off;
        double xd, yd, wx, wy;
        int channels = inputFrame.channels();
        int width = inputFrame.width();
        int height = inputFrame.height();
        int isize = height * width * channels; /* compute total size of required attay  */
        byte buff1[] = new byte[isize];      /* allocate memory for image data   */
        byte buff2[] = new byte[isize];      /* allocate memory for image data   */
        final int stride = width * channels;

        /* make a copy when in-place operation is not possible */
        Mat cloneFrame = inputFrame.clone();

         /* make voodoo parameter dependent on displayed size  */
        if (maxFrameWidth < maxFrameHeight) voodoo_diff = maxFrameWidth >> voodoo_diff_shift;
        else  voodoo_diff = maxFrameHeight >> voodoo_diff_shift;
        radius2 = voodoo_diff * voodoo_diff;

        if (voodoo_mode == OFFSET_A) off = voodoo_diff;
        else if (voodoo_mode == OFFSET_B) off = (3 * voodoo_diff) / 2;
        else off = voodoo_diff/2;

        inputFrame.get( 0, 0, buff1); /* get address of original data array  */
        cloneFrame.get( 0, 0, buff2); /* get address of copied data array  */
        /* for all blocks in vertical direction */
        for (y = 0, py = 0; y < height; y++, py += stride) {
            diff_y = y - voodoo_y; /* get vertical focus of effect   */
            yy = diff_y * diff_y;
            /* for all blocks in horizontal direction */
            for (x = 0, pos = py; x < width; x++, pos += channels) {
                diff_x = x - voodoo_x; /* get horizontal focus of effect   */
                xx = diff_x * diff_x;
                if (xx + yy < radius2) {
                    /* get new x position   */
                    xd = (x + off);
                    x2 = (int) Math.floor(xd);
                    x2 = Math.max(0, Math.min(x2, width - 2)); /*  ckeck border  */
                    /* get new y position   */
                    yd = (y );
                    y2 = (int) Math.floor(yd);
                    y2 = Math.max(0, Math.min(y2, height - 2)); /* ckeck border  */
                    pos2 = x2 * channels + y2 * stride;

                    buff1[pos] = buff2[pos2];
                    buff1[pos + 1] = buff2[pos2 + 1];
                    buff1[pos + 2] = buff2[pos2 + 2];
                }
            }
        }

        inputFrame.put( 0, 0, buff1); /* put modified data array back to inputFrame */
        cloneFrame.release();  /* release copied data   */

        return inputFrame;
    }


    public String entropy4( Mat inputFrame) {
        // inputFrame is a Mat structure in RGBA format
        int x, y, m, n;
        int px, py, pm, pos;
        int channels = inputFrame.channels();
        int width = inputFrame.width();
        int height = inputFrame.height();
        int isize = height * width * channels; /* compute total size of required array  */
        byte buff[] = new byte[isize];      /* allocate memory for image data   */
        int step = pixel_mode; /* (0, 1, 2, 3) ==> (2, 4, 6, 8)   */
        final int stride = width * channels;
        final int stride_step = width * channels * step;
        final int stride_chanstep = step * channels;
        Multiset<Byte> P = HashMultiset.create();
        Double entr =0.0;
        inputFrame.get(0, 0, buff);
        /* for all blocks in vertical direction */
        for ( y = 0, py=0; y < height; y+= step, py+=stride_step)
        {   /* for all blocks in horizontal direction */
            for ( x = 0, px = py; x < width; x+=step, px += stride_chanstep)
            {   /* for all pixels in the blocks in vertical direction
                 * remember that blocks must be cropped at bottom image border */
                for ( m=0, pm = px; (m < step) && (y+m < height); m++, pm += stride)
                {   /* for all pixels in the blocks in horizontal direction
                     * remember that blocks must be cropped at right image border */
                    for ( n=0, pos = pm; (n < step) && (x+n < width); n++, pos+=channels)
                    {
                        buff[pos]= buff[px];
                        P.add(buff[pos]);
                    }
                }
            }
        }

        for(int c=0; c<=P.size(); c++){
            double sym_occur;
            Log.d("penis"+c,String.valueOf(P.count(c)));
            sym_occur=(P.count(c)/P.size());
            //entr += (sym_occur / size) * (Math.log(size / sym_occur)/Math.log(2));
           if(sym_occur !=0){ entr += (sym_occur * log2(sym_occur))*-1;}

        }
        //Log.d("penis",String.valueOf(entr));
        String H = String.valueOf(entr);

        return H;
    }






    /* generate filename and save image */
    /**
     * Das Speichern des momentan angezeigten Bildes als PNG-File im Ordner QPic unter Pictures.
     *
     * @param mat zu speicherndes Bild
     */
    public void saveImage( Mat mat) {

        Mat mIntermediateMat = new Mat();

        Mat cloneFrame = mat.clone();

        //Strutz Imgproc.cvtColor( mat, mIntermediateMat, Imgproc.COLOR_RGBA2BGRA, 3);
        cvtColor( mat, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);

        File path = new File( Environment.getExternalStorageDirectory() + SAVE_DIR);
        if (!path.exists()) path.mkdirs();

        StringBuilder postFix = new StringBuilder();
        if ( mode == QUANTIZE) {
            postFix.append("_").append( quant_mode).append("_bit");
        }
        else if (mode == SAMPLENHOLD) {
            postFix.append("_").append( pixel_mode).append("_block");
        }
        else if (mode == VOODOO) {
            postFix.append("_").append(pixel_mode).append("_voodoo");
        }
        else if (mode == BONNKMI14) {
                postFix.append("_").append( pixel_mode).append("_SSIO"); // Filename Suffix für neuen Modus
            }

        String filename = new SimpleDateFormat( "yyyy-MM-dd_HH-mm-ss", Locale.GERMAN).format(new Date()) + postFix.toString() + ".png";

        File file = new File( path, filename);

        // initiate media scan and put the new things into the path array to
        // make the scanner aware of the location and the files you want to see
        /* does not always work as expected, Widows does not show these images  */
        MediaScannerConnection.scanFile(this, new String[]{file.toString()}, null, null);


        String ENTROPY = this.entropy4(mIntermediateMat); //Entropymethode aufrufen
        //Double zu String casten weil Core.putText nur String akkzeptiert

        int n = 250;
        int m = 250;
        Core.putText(mIntermediateMat, ENTROPY, new org.opencv.core.Point(n,m), 3, 2, new Scalar(255, 0, 0, 255), 2);

        Boolean saved = Highgui.imwrite( file.toString(), mIntermediateMat); //maximum compression for PNG
        //MainActivity.progressbar.setVisibility( (View.GONE));
/* some problems
        if (saved) {
            Toast.makeText( getApplicationContext(), R.string.msg_saved + "\n" + file.getPath(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText( getApplicationContext(), R.string.msg_save_error, Toast.LENGTH_SHORT).show();
        }
        /**/
        ;
    }



}

