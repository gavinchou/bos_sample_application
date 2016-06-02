package com.baidu.bce.bos.meituapp;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Uri selectedPicUri = null;
    private static final int UPLOAD_FILE_FINISHED = 1;
    private static final int DOWNLOAD_FILE_FINISHED = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Button selectButton = (Button) findViewById(R.id.select_pic_button);
        selectButton.setOnClickListener(this);
        Button uploadButton = (Button) findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(this);
        Button downloadButton = (Button) findViewById(R.id.download_button);
        downloadButton.setOnClickListener(this);
        Button downloadResizeButton = (Button) findViewById(R.id.download_resize_button);
        downloadResizeButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.select_pic_button: {
                Spinner regoinSpinner = (Spinner) findViewById(R.id.region_spinner);
                showToast((String) regoinSpinner.getSelectedItem());
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 1);
                break;
            }
            case R.id.upload_button: {
                showToast("Uploading file");
                uploadPicToBos();
                break;
            }
            case R.id.download_button: {
                showToast("Downloading file");
                downloadFileFromBos();
                break;
            }
            case R.id.download_resize_button: {
                showToast("Downloading resized file");
                downloadResizedFileFromBos();
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            selectedPicUri = data.getData();
            Log.i("uri", selectedPicUri.toString());
            ContentResolver cr = this.getContentResolver();
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(selectedPicUri));
                ImageView imageView = (ImageView) findViewById(R.id.pic_image_view);
                // display selected image
                imageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                Log.e("Exception", e.getMessage(), e);
                showToast("File not found: " + selectedPicUri.getPath());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            byte[] imageContent = data.getByteArray("downloadedImage");
            if (imageContent != null) {
                ImageView imageView = (ImageView) findViewById(R.id.pic_image_view);
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageContent, 0, imageContent.length);
                imageView.setImageBitmap(bitmap);
                return;
            }
            switch (msg.what) {
                case UPLOAD_FILE_FINISHED: {
                    MainActivity.this.showToast("File uploaded");
                    break;
                }
                case DOWNLOAD_FILE_FINISHED: {
                    MainActivity.this.showToast("File downloaded");
                    break;
                }
                default: {
                    break;
                }
            }
        }
    };

    public void uploadPicToBos() {
        // 1. get pic params from ui: file name, file location uri etc
        // 2. send params to app server and get sts, bucket name and region
        // 3. upload selected pic to bos with sts etc, which bos client needs
        if (selectedPicUri == null) {
            showToast("No file selected");
            return;
        }
        EditText et = (EditText) findViewById(R.id.app_server_addr_edittext);
        final String appServerAddr = et.getText().toString();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> bosInfo = AppServer.getBosInfoFromAppServer(appServerAddr, "user-demo",
                        AppServer.BosOperationType.UPLOAD);

                if (bosInfo == null) {
                    return;
                }
                showToast(bosInfo.toString(), Toast.LENGTH_LONG);

                String ak = (String) bosInfo.get("ak");
                String sk = (String) bosInfo.get("sk");
                String stsToken = (String) bosInfo.get("stsToken");
                String endpoint = (String) bosInfo.get("endpoint");
                String bucketName = (String) bosInfo.get("bucketName");
                String objectName = (String) bosInfo.get("objectName");
                String prefix = (String) bosInfo.get("prefix");
                Log.i("UploadFileToBos", bosInfo.toString());

                // specify a object name if the app server does not specify one
                if (objectName == null || objectName.equalsIgnoreCase("")) {
                    objectName = ((EditText) findViewById(R.id.bos_object_name_edittext)).getText().toString();
                    if (prefix != null && !prefix.equalsIgnoreCase("")) {
                        objectName = prefix + "/" + objectName;
                    }
                }

                Bos bos = new Bos(ak, sk, endpoint, stsToken);
                try {
//                    bos.uploadFile(bucketName, objectName, MainActivity.this.getContentResolver().openInputStream(selectedPicUri));
                    byte[] data = Utils.readAllFromStream(MainActivity.this.getContentResolver().openInputStream(selectedPicUri));
                    bos.uploadFile(bucketName, objectName, data);
                } catch (Throwable e) {
                    Log.e("MainActivity/Upload", "Failed to upload file to bos: " + e.getMessage());
                    showToast("Failed to upload file: " + e.getMessage());
                    return;
                }
                // finished uploading file, send a message to inform ui
                handler.sendEmptyMessage(UPLOAD_FILE_FINISHED);
            }
        }).start();
    }

    public void downloadFileFromBos() {
        // 1. get pic params from ui: file name, file location uri etc
        // 2. send params to app server and get sts, bucket name and region
        // 3. down selected pic from bos with sts etc, which bos client needs
        EditText et = (EditText) findViewById(R.id.app_server_addr_edittext);
        final String appServerAddr = et.getText().toString();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> bosInfo = AppServer.getBosInfoFromAppServer(appServerAddr, "user-demo",
                        AppServer.BosOperationType.DOWNLOAD);

                if (bosInfo == null) {
                    return;
                }
                showToast(bosInfo.toString(), Toast.LENGTH_LONG);

                String ak = (String) bosInfo.get("ak");
                String sk = (String) bosInfo.get("sk");
                String stsToken = (String) bosInfo.get("stsToken");
                String endpoint = (String) bosInfo.get("endpoint");
                String bucketName = (String) bosInfo.get("bucketName");
                String objectName = (String) bosInfo.get("objectName");
                String prefix = (String) bosInfo.get("prefix");
                Log.i("DownloadFromBos", bosInfo.toString());

                // specify a object name if the app server does not specify one
                if (objectName == null || objectName.equalsIgnoreCase("")) {
                    objectName = ((EditText) findViewById(R.id.bos_object_name_edittext)).getText().toString();
                    if (prefix != null && !prefix.equalsIgnoreCase("")) {
                        objectName = prefix + "/" + objectName;
                    }
                }

                Bos bos = new Bos(ak, sk, endpoint, stsToken);

                byte [] content = null;
                try {
                    content = bos.downloadFileContent(bucketName, objectName);
                } catch (Exception e) {
                    Log.e("MainActivity/Download", "Failed to download file from bos: " + e.getMessage());
                    showToast("Failed to download file");
                    return;
                }
                // finished uploading file, send a message to inform ui
                Message msg = new Message();
                Bundle data = new Bundle();
                data.putByteArray("downloadedImage", content);
                msg.setData(data);
                handler.sendMessage(msg);
                handler.sendEmptyMessage(DOWNLOAD_FILE_FINISHED);
            }
        }).start();
    }

    public void downloadResizedFileFromBos() {
        // 1. get pic params from ui: file name, file location uri etc
        // 2. send params to app server and get sts, bucket name and region
        // 3. down selected pic with the parameters set by user
        EditText et = (EditText) findViewById(R.id.app_server_addr_edittext);
        final String appServerAddr = et.getText().toString();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> bosInfo = AppServer.getBosInfoFromAppServer(appServerAddr, "user-demo",
                        AppServer.BosOperationType.DOWNLOAD_PROCESSED);

                if (bosInfo == null) {
                    return;
                }
                showToast(bosInfo.toString(), Toast.LENGTH_LONG);

                String ak = (String) bosInfo.get("ak");
                String sk = (String) bosInfo.get("sk");
                String stsToken = (String) bosInfo.get("stsToken");
                String endpoint = (String) bosInfo.get("endpoint");
                String bucketName = (String) bosInfo.get("bucketName");
                Log.i("DownloadResizedFile", bosInfo.toString());

                String objectName = ((EditText) findViewById(R.id.bos_object_name_edittext)).getText().toString();
                objectName += "@w_" + ((EditText) findViewById(R.id.pic_width_edittext)).getText().toString();
                objectName += ",h_" + ((EditText) findViewById(R.id.pic_height_edittext)).getText().toString();
                objectName += ",a_" + ((EditText) findViewById(R.id.pic_rotation_edittext)).getText().toString();

                Bos bos = new Bos(ak, sk, endpoint, stsToken);
                Log.i("DownloadResizedFile", ak);
                Log.i("DownloadResizedFile", sk);
                Log.i("DownloadResizedFile", endpoint);
                Log.i("DownloadResizedFile", stsToken);

                byte [] content = null;
                try {
                    content = bos.downloadFileContent(bucketName, objectName);
                } catch (Exception e) {
                    Log.e("MainActivity/Download", "Failed to download file from bos: " + e.getMessage());
                    showToast("Failed to download file");
                    return;
                }
                // finished uploading file, send a message to inform ui
                Message msg = new Message();
                Bundle data = new Bundle();
                data.putByteArray("downloadedImage", content);
                msg.setData(data);
                handler.sendMessage(msg);
                handler.sendEmptyMessage(DOWNLOAD_FILE_FINISHED);
            }
        }).start();       // 3. upload selected pic to bos with sts etc, which bos client needs
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showToast(final String toast) {
        showToast(toast, Toast.LENGTH_SHORT);
    }

    public void showToast(final String toast, final int period) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, toast, period).show();
            }
        });
    }
}
