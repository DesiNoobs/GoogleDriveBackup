package com.ashish.googledrivebackup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ammarptn.debug.gdrive.lib.GDriveDebugViewActivity;
import com.ammarptn.gdriverest.DriveServiceHelper;
import com.ammarptn.gdriverest.GoogleDriveFileHolder;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.util.IOUtils;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static com.ammarptn.debug.gdrive.lib.ui.gdrivedebugview.util.PathUtil.getPath;
import static com.ammarptn.gdriverest.DriveServiceHelper.getGoogleDriveService;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;
    private DriveServiceHelper mDriveServiceHelper;
    private static final String TAG = "MainActivity";
    private Button login;
    private LinearLayout gDriveAction;
    private Button createFolder, upload_doc, upload_video, upload_contact, view_folder;
    private Button uploadFile;
    private TextView email;
    DriveServiceHelper1 driveServiceHelper1;
    private List<String> selectedVideos;


    FileUtils fileUtils;
    final int REQUEST_EXTERNAL_STORAGE = 101;
    final int REQUEST_EXTERNAL_VIDEO_STORAGE = 102;
    final int REQUEST_EXTERNAL_DOC_STORAGE = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        fileUtils = new FileUtils(this);
        createFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }
                // you can provide  folder id in case you want to save this file inside some folder.
                // if folder id is null, it will save file to the root
                final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                View mView = getLayoutInflater().inflate(R.layout.custom_dialog_geofence, null);
                final EditText txt_inputText = (EditText) mView.findViewById(R.id.txt_input);
                Button btn_cancel = (Button) mView.findViewById(R.id.btn_cancel);
                Button btn_okay = (Button) mView.findViewById(R.id.btn_okay);
                alert.setView(mView);
                final AlertDialog alertDialog = alert.create();
                alertDialog.setCanceledOnTouchOutside(false);
                btn_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                    }
                });
                btn_okay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String strUserName = txt_inputText.getText().toString();

                        if (TextUtils.isEmpty(strUserName)) {
                            if (TextUtils.isEmpty(strUserName)) {
                                txt_inputText.setError("Enter Folder Name");
                                return;
                            }
                        } else {

                            String Geo_ID = txt_inputText.getText().toString().trim();
                            mDriveServiceHelper.createFolder(Geo_ID, null)
                                    .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                                        @Override
                                        public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                                            Gson gson = new Gson();
                                            Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));
                                            Toast.makeText(MainActivity.this, "" + gson.toJson(googleDriveFileHolder), Toast.LENGTH_SHORT).show();
                                            try {
                                                JSONObject jsonObject = new JSONObject(gson.toJson(googleDriveFileHolder));
                                                jsonObject.getString("");
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d(TAG, "onFailure: " + e.getMessage());

                                        }
                                    });
                            alertDialog.dismiss();
                        }
                    }
                });
                alertDialog.show();

            }
        });

        uploadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (driveServiceHelper1 == null) {
                    return;
                }
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);
//                    return;
                } else {
                    launchGalleryIntent();
                }
            }
        });


        view_folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openActivity = new Intent(getApplicationContext(), GDriveDebugViewActivity.class);
                startActivity(openActivity);
            }
        });

        upload_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (driveServiceHelper1 == null) {
                    return;
                }
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);
//                    return;
                } else {
                    launchVideoIntent();
                }
            }
        });

        upload_doc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (driveServiceHelper1 == null) {
                    return;
                }
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);
//                    return;
                } else {
                    launchDocIntent();
                }
            }
        });


    }

    private void launchDocIntent() {

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("*/*");
        String[] extraMimeTypes = {"application/pdf", "application/doc"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_EXTERNAL_DOC_STORAGE);
    }

    private void launchVideoIntent() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("video/*");
        startActivityForResult(intent, REQUEST_EXTERNAL_VIDEO_STORAGE);
    }

    public void launchGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_EXTERNAL_STORAGE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

        if (account == null) {

            signIn();

        } else {
            email.setText(account.getEmail());
            mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getApplicationContext(), account, "appName"));
            driveServiceHelper1 = new DriveServiceHelper1(getGoogleDriveService(getApplicationContext(), account, "appName"));
        }
    }

    private void signIn() {

        mGoogleSignInClient = buildGoogleSignInClient();
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .requestEmail()
                        .build();
        return GoogleSignIn.getClient(getApplicationContext(), signInOptions);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    handleSignInResult(resultData);
                }
                break;
            case REQUEST_EXTERNAL_STORAGE:
                if (requestCode == REQUEST_EXTERNAL_STORAGE && resultCode == RESULT_OK) {
                    ClipData clipData = resultData.getClipData();
                    if (clipData != null) {
                        //multiple images selecetd
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            Uri imageUri = clipData.getItemAt(i).getUri();
                            String path = fileUtils.getPath(imageUri);
                            ContentResolver cR = this.getContentResolver();
                            String mime = cR.getType(imageUri);
                            driveServiceHelper1.UploadImage(path, displayName(imageUri), null, mime)
                                    .addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String s) {
                                            Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(MainActivity.this, "NotUpload" + e, Toast.LENGTH_SHORT).show();
                                }
                            });


                        }
                    } else {
                        //single image selected
                        Uri imageUri = resultData.getData();
                        String path = fileUtils.getPath(imageUri);
                        ContentResolver cR = this.getContentResolver();
                        String mime = cR.getType(imageUri);
                        driveServiceHelper1.UploadImage(path, displayName(imageUri), null, mime)
                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String s) {
                                        Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, "NotUpload", Toast.LENGTH_SHORT).show();
                            }
                        });


                    }
                }
                break;

            case REQUEST_EXTERNAL_VIDEO_STORAGE:
                if (resultCode == RESULT_OK) {
                    if (requestCode == REQUEST_EXTERNAL_VIDEO_STORAGE) {

                        try {
                            selectedVideos = getSelectedVideos(requestCode, resultData);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }

                }
                break;

            case REQUEST_EXTERNAL_DOC_STORAGE:
                if (requestCode == REQUEST_EXTERNAL_DOC_STORAGE && resultCode == RESULT_OK) {
                    ClipData clipData = resultData.getClipData();
                    if (clipData != null) {
                        //multiple Doc
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            Uri uri = clipData.getItemAt(i).getUri();
                            String path = fileUtils.getPath(uri);

                            driveServiceHelper1.UploadDoc(path, displayName(uri))
                                    .addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String s) {
                                            Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, "NotUpload", Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    } else {
//Single Doc
                        Uri uri = resultData.getData();
                        ContentResolver cR = this.getContentResolver();
                        String mime = cR.getType(uri);
                        String path = fileUtils.getPath(uri);
                        driveServiceHelper1.UploadDoc(path, displayName(uri))
                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String s) {
                                        Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, "NotUpload", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                }
                break;

        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }

    private List<String> getSelectedVideos(int requestCode, Intent data) throws URISyntaxException{

        List<String> result = new ArrayList<>();
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item videoItem = clipData.getItemAt(i);
                Uri videoURI = videoItem.getUri();
                String filePath = fileUtils.getPath(videoURI);
                ContentResolver cR = this.getContentResolver();
                String mime = cR.getType(videoURI);
                driveServiceHelper1.UploadVideo(filePath, displayName(videoURI))
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String s) {
                                Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "NotUpload", Toast.LENGTH_SHORT).show();
                    }
                });
                result.add(filePath);
            }
        } else {
            //single video
            Uri videoURI = data.getData();
            String filePath = fileUtils.getPath(videoURI);
            ContentResolver cR = this.getContentResolver();
            String mime = cR.getType(videoURI);
            driveServiceHelper1.UploadVideo(filePath, displayName(videoURI))
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String s) {
                            Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MainActivity.this, "NotUpload", Toast.LENGTH_SHORT).show();
                }
            });
            result.add(filePath);
        }

        return result;
    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                        Log.d(TAG, "Signed in as " + googleSignInAccount.getEmail());
                        email.setText(googleSignInAccount.getEmail());

                        mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(getApplicationContext(), googleSignInAccount, "appName"));
                        driveServiceHelper1 = new DriveServiceHelper1(getGoogleDriveService(getApplicationContext(), googleSignInAccount, "appName"));

                        Log.d(TAG, "handleSignInResult: " + mDriveServiceHelper);
                        Log.d(TAG, "handleSignInResult: " + driveServiceHelper1);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Unable to sign in.", e);
                    }
                });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchGalleryIntent();
                } else {
                }
                return;
            }
        }
    }


    private String displayName(Uri uri) {

        Cursor mCursor =
                getApplicationContext().getContentResolver().query(uri, null, null, null, null);
        int indexedname = mCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        mCursor.moveToFirst();
        String filename = mCursor.getString(indexedname);
        mCursor.close();
        return filename;


    }

    private void initView() {

        email = findViewById(R.id.email);
        gDriveAction = findViewById(R.id.g_drive_action);
        createFolder = findViewById(R.id.create_folder);
        uploadFile = findViewById(R.id.upload_file);
        upload_contact = findViewById(R.id.upload_contact);
        upload_doc = findViewById(R.id.upload_doc);
        upload_video = findViewById(R.id.upload_video);
        view_folder = findViewById(R.id.view_folder);
    }
}