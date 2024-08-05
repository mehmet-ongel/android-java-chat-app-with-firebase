package com.techmania.chatapp.loginpages;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.techmania.chatapp.databinding.ActivitySignupBinding;
import com.techmania.chatapp.models.User;
import com.techmania.chatapp.views.MainActivity;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;

public class SignupActivity extends AppCompatActivity {

    ActivitySignupBinding signupBinding;
    ActivityResultLauncher<String[]> permissionsResultLauncher;

    int deniedPermissionCount = 0;
    ArrayList<String> permissionsList = new ArrayList<>();

    ActivityResultLauncher<Intent> photoPickerResultLauncher;
    ActivityResultLauncher<Intent> cropPhotoResultLauncher;

    Uri croppedImageUri;
    String userName;
    String userEmail;
    String userPassword;

    FirebaseAuth auth = FirebaseAuth.getInstance();

    boolean imageControl = false;

    FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    StorageReference storageReference = firebaseStorage.getReference();

    String userUniqueId;
    String profileImageUrl;

    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference().child("Users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        signupBinding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(signupBinding.getRoot());

        if (Build.VERSION.SDK_INT > 33){
            permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissionsList.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        }else if (Build.VERSION.SDK_INT > 32){
            permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES);
        }else {
            permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        registerActivityForMultiplePermissions();
        registerActivityForPhotoPicker();
        registerActivityForPhotoCrop();

        signupBinding.imageViewProfileSignup.setOnClickListener(v -> {

            if (hasPermission()){
                openPhotoPicker();
            }else {
                shouldShowPermissionRationaleIfNeeded();
            }

        });
        signupBinding.buttonSignup.setOnClickListener(v -> {
            createNewUser();
        });

    }

    public void createNewUser(){

        userName = signupBinding.editTextUserNameSignup.getText().toString().trim();
        userEmail = signupBinding.editTextEmailSignup.getText().toString().trim();
        userPassword = signupBinding.editTextPasswordSignup.getText().toString().trim();

        if (userName.isEmpty() || userEmail.isEmpty() || userPassword.isEmpty()){
            Toast.makeText(this, "Username, email address, and password cannot be empty!", Toast.LENGTH_SHORT).show();
        }else {
            signupBinding.buttonSignup.setEnabled(false);
            signupBinding.progressBarSignup.setVisibility(View.VISIBLE);

            auth.createUserWithEmailAndPassword(userEmail,userPassword).addOnCompleteListener(task -> {

                if (task.isSuccessful()){
                    uploadPhoto();
                }else {
                    Toast.makeText(this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    signupBinding.buttonSignup.setEnabled(true);
                    signupBinding.progressBarSignup.setVisibility(View.INVISIBLE);
                }

            });
        }

    }

    public void uploadPhoto(){

        if (auth.getCurrentUser() != null){
            userUniqueId = auth.getCurrentUser().getUid();

            StorageReference imageRef = storageReference.child("images").child(userUniqueId);

            if (imageControl){

                imageRef.putFile(croppedImageUri).addOnSuccessListener(taskSnapshot -> {

                    StorageReference uploadedImageRef = storageReference.child("images").child(userUniqueId);

                    uploadedImageRef.getDownloadUrl().addOnSuccessListener(uri -> {

                        profileImageUrl = uri.toString();
                        Log.d("profileImageUrl: ", profileImageUrl);
                        saveUserInfoToDatabase();
                    });

                }).addOnFailureListener(e -> {

                    Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();

                });

            }else {
                profileImageUrl = "null";
                saveUserInfoToDatabase();
            }

        }


    }

    public void saveUserInfoToDatabase(){

        User user = new User(userUniqueId,userName,userEmail,profileImageUrl);

        databaseReference.child(userUniqueId).setValue(user).addOnCompleteListener(task -> {

            if (task.isSuccessful()){
                Toast.makeText(this, "Your account has been successfully created.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                startActivity(intent);
                signupBinding.buttonSignup.setEnabled(true);
                signupBinding.progressBarSignup.setVisibility(View.INVISIBLE);
                finish();
            }else {
                Toast.makeText(getApplicationContext(),task.getException().getLocalizedMessage(),Toast.LENGTH_LONG).show();
            }

        });

    }

    public void registerActivityForMultiplePermissions(){

        permissionsResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),result->{

            boolean allGranted = true;
            for (Boolean isAllowed : result.values()){
                if (!isAllowed){
                    allGranted = false;
                    break;
                }
            }

            if (allGranted){
                openPhotoPicker();
            }else {

                deniedPermissionCount++;
                if (deniedPermissionCount < 2){
                    shouldShowPermissionRationaleIfNeeded();
                }else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SignupActivity.this);
                    builder.setTitle("Chat App");
                    builder.setMessage("You can grant the necessary permissions to access the photos from the application settings.");
                    builder.setPositiveButton("Go App Settings",(dialog, which) -> {

                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.parse("package:" + getPackageName());
                        intent.setData(uri);
                        startActivity(intent);

                        dialog.dismiss();

                    });
                    builder.setNegativeButton("Dismiss",(dialog, which) -> {
                       dialog.dismiss();
                    });
                    builder.create().show();
                }


            }

        });

    }

    public void openPhotoPicker(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        photoPickerResultLauncher.launch(intent);
    }

    public void registerActivityForPhotoPicker(){

        photoPickerResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),result -> {

            int resultCode = result.getResultCode();
            Intent data = result.getData();

            if (resultCode == RESULT_OK && data != null){
                Uri uncroppedImageUri = data.getData();
                cropSelectedImage(uncroppedImageUri);
            }

        });

    }

    public void registerActivityForPhotoCrop(){

        cropPhotoResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),cropResult->{

            int resultCode = cropResult.getResultCode();
            Intent data = cropResult.getData();

            if (resultCode == RESULT_OK && data != null){
                croppedImageUri = UCrop.getOutput(data);
                if (croppedImageUri != null){
                    Picasso.get().load(croppedImageUri)
                            .into(signupBinding.imageViewProfileSignup);
                    imageControl = true;
                }
            } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
                Toast.makeText(this, UCrop.getError(data).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }

        });

    }

    public void cropSelectedImage(Uri sourceUri){

        Uri destinationUri = Uri.fromFile(new File(getCacheDir(),"cropped" + System.currentTimeMillis()));
        Intent croppedIntent = UCrop.of(sourceUri,destinationUri)
                .withAspectRatio(1,1)
                .getIntent(SignupActivity.this);
        cropPhotoResultLauncher.launch(croppedIntent);

    }

    public void shouldShowPermissionRationaleIfNeeded(){

        ArrayList<String> deniedPermissions = new ArrayList<>();

        for (String permission : permissionsList){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,permission)){
                deniedPermissions.add(permission);
            }
        }

        if (!deniedPermissions.isEmpty()){

            Snackbar.make(signupBinding.mainSignup,"Please grant necessary permissions to add a profile photo",Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK",v -> {
                        permissionsResultLauncher.launch(deniedPermissions.toArray(new String[0]));
                    }).show();

        }else {
            permissionsResultLauncher.launch(permissionsList.toArray(new String[0]));
        }

    }

    public boolean hasPermission(){
        for (String permission : permissionsList){

            if (ContextCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }

        }
        return true;
    }

}










