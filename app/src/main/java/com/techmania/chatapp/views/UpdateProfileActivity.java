package com.techmania.chatapp.views;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.techmania.chatapp.R;
import com.techmania.chatapp.databinding.ActivityUpdateProfileBinding;
import com.techmania.chatapp.loginpages.SignupActivity;
import com.techmania.chatapp.models.User;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UpdateProfileActivity extends AppCompatActivity {

    ActivityUpdateProfileBinding updateProfileBinding;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser currentUser;

    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference().child("Users");

    String userName;
    String userEmail;
    String imageUrl;
    String userId;

    ValueEventListener updateValueEventListener;

    ActivityResultLauncher<String[]> permissionsResultLauncher;

    int deniedPermissionCount = 0;
    ArrayList<String> permissionsList = new ArrayList<>();

    ActivityResultLauncher<Intent> photoPickerResultLauncher;
    ActivityResultLauncher<Intent> cropPhotoResultLauncher;

    Uri croppedImageUri;
    boolean imageControl = false;

    FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    StorageReference storageReference = firebaseStorage.getReference().child("images");

    String updatedProfileImageUrl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateProfileBinding = ActivityUpdateProfileBinding.inflate(getLayoutInflater());
        setContentView(updateProfileBinding.getRoot());

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

        updateProfileBinding.toolbarUpdateProfile.setNavigationOnClickListener(v -> {
            finish();
        });

        currentUser = auth.getCurrentUser();

        getAndShowUserInfo();

        updateProfileBinding.imageViewProfileUpdateProfile.setOnClickListener(v -> {

            if (hasPermission()){
                openPhotoPicker();
            }else {
                shouldShowPermissionRationaleIfNeeded();
            }

        });

        updateProfileBinding.buttonUpdateProfile.setOnClickListener(v -> {

            updatePhoto();

        });

    }

    public void updatePhoto(){

        updateProfileBinding.buttonUpdateProfile.setEnabled(false);
        updateProfileBinding.progressBarUpdateProfile.setVisibility(View.VISIBLE);

        StorageReference imageRef = storageReference.child(currentUser.getUid());

        if (imageControl){

            imageRef.putFile(croppedImageUri).addOnSuccessListener(taskSnapshot -> {

                StorageReference uploadedImageRef = storageReference.child(currentUser.getUid());

                uploadedImageRef.getDownloadUrl().addOnSuccessListener(uri -> {

                    updatedProfileImageUrl = uri.toString();
                    updateUserData();

                });

            }).addOnFailureListener(e -> {

                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();

                updateProfileBinding.buttonUpdateProfile.setEnabled(true);
                updateProfileBinding.progressBarUpdateProfile.setVisibility(View.INVISIBLE);

            });

        }else {

            updatedProfileImageUrl = imageUrl;
            updateUserData();

        }

    }

    public void updateUserData(){

        String updatedUserName = updateProfileBinding.editTextUserNameUpdateProfile.getText().toString().trim();

        //databaseReference.child(currentUser.getUid()).child("userName").setValue(updatedUserName); // first way

        //updateChildren() -> second way

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userName",updatedUserName);
        userMap.put("imageUrl",updatedProfileImageUrl);

        databaseReference.child(currentUser.getUid()).updateChildren(userMap).addOnCompleteListener(task -> {

            if (task.isSuccessful()){
                Toast.makeText(this, "Data updated", Toast.LENGTH_SHORT).show();
                finish();
            }else {
                Toast.makeText(this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
            updateProfileBinding.buttonUpdateProfile.setEnabled(true);
            updateProfileBinding.progressBarUpdateProfile.setVisibility(View.INVISIBLE);

        });

    }

    public void getAndShowUserInfo(){

        if (currentUser != null){

            updateValueEventListener = databaseReference.child(currentUser.getUid()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    User user = snapshot.getValue(User.class);
                    if (user != null){

                        userName = user.getUserName();
                        userEmail = user.getUserEmail();
                        userId = user.getUserId();
                        imageUrl = user.getImageUrl();

                        updateProfileBinding.editTextUserNameUpdateProfile.setText(userName);

                        if (imageUrl.equals("null")){
                            updateProfileBinding.imageViewProfileUpdateProfile.setImageResource(R.drawable.default_profile_photo);
                        }else {
                            Picasso.get().load(imageUrl).into(updateProfileBinding.imageViewProfileUpdateProfile);
                        }

                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                    Toast.makeText(UpdateProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();

                }
            });

        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (updateValueEventListener != null){
            databaseReference.child(currentUser.getUid()).removeEventListener(updateValueEventListener);
        }
    }

    public void registerActivityForMultiplePermissions(){

        permissionsResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result->{

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
                    AlertDialog.Builder builder = new AlertDialog.Builder(UpdateProfileActivity.this);
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
                            .into(updateProfileBinding.imageViewProfileUpdateProfile);
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
                .getIntent(UpdateProfileActivity.this);
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

            Snackbar.make(updateProfileBinding.mainUpdateProfile,"Please grant necessary permissions to add a profile photo",Snackbar.LENGTH_INDEFINITE)
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














