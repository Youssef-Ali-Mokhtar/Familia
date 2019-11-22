package com.example.friendlychat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private String mUsername;

    //one: The entry point for the database. (video 17)
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference; //refers to a specific part of the database (messages portion)
    //one
    //four: handling reading messages
    private ChildEventListener mChildEventListener;
    //four
    //Sixteen add firebase storage instance variable and storage reference object
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;
    //Sixteen
    //six To show you the part of the app related to the state (logged in/out)
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListenenr;
    //six
    //nine:
    public static int RC_SIGN_IN = 1;
    //nine
    //Fifteen
    private static final int RC_PHOTO_PICKER =  2;
    //Fifteen
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;

        //two: The entry point for the database.
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        //seven
        mFirebaseAuth = FirebaseAuth.getInstance();
        //seven
        //Sixteen: initialize the storage instance variable
        mFirebaseStorage = FirebaseStorage.getInstance();
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");
        //Sixteen
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");
        //two

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        //Fifteen: ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);

            }
        });
        //Fifteen

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click

                //three: The object that contains the message, Pushing the message to the database
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null);
                mMessagesDatabaseReference.push().setValue(friendlyMessage);
                //three
                // Clear input box
                mMessageEditText.setText("");
            }
        });

        //four and five was here but now they are down there inside onSignedInInitialized

        //seven: a listener for the states
        mAuthStateListenenr = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                //nine: checking the user's state

                FirebaseUser user = firebaseAuth.getCurrentUser();


                if(user != null)    //signed in
                {
                    //ten: what happens when the user is signed in
                    onSignedInInitialize(user.getDisplayName());
                    //ten
                    MessageAdapter.setNameMessageAdapter(user.getDisplayName());
                }
                else{               //signed out
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.GoogleBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
        //seven
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    //Fourteen: signing out
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //Fourteen

    //Eight attaching the authStateListener with onPause and onResume
    @Override
    protected void onPause() {
        super.onPause();
        if(mAuthStateListenenr != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListenenr);
        }
        deattachDatabaseReadListener();
        mMessageAdapter.clear();
    }
    //Thirteen: problems with going back from the login form
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            } else if(requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK){//Seventeen: get image URL and it's last path segment
                Uri selectedImageUri = data.getData();
                final StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());
                photoRef.putFile(selectedImageUri);
                Toast.makeText(this, "WORKS", Toast.LENGTH_SHORT).show();
                //Eighteen: upload photo to the database.
                //photoRef.putFile(selectedImageURI);

                //Eighteen
            }//Seventeen
        }
    }
    //Thirteen

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListenenr);
    }
    //Eight

    //Ten: attaching name while signed in
    private void onSignedInInitialize(String username){
        mUsername = username;
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanup(){
        //twelve: tearing down the ui after signing out
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        //twelve
    }
    //Ten


    //Eleven: put four and five inside a method and then call the method inside onResume because this is where the authentication happens
    // (its for what happens when the user signs in)
    private void attachDatabaseReadListener(){
        if(mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override //new message inserted
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    //deserializing the very recent message and putting it into mFriendlyMessage and adds it to the messageAdapter
                    FriendlyMessage mFriendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(mFriendlyMessage);
                }
                @Override //content of an existing message gets changed
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
                @Override // when an existing message is deleted
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { }
                @Override // one of the messages changed position in the list
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
                @Override
                // some sort of error occured when you are trying to make changes, for ex: you don't have permission to read the data
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            };
            //four
            //five: the reference defines what exactly I azm listening to and the object defines what exactly will happen to the data
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }
    //Eleven
    //Twelve
    private void deattachDatabaseReadListener(){

        if(mChildEventListener != null){
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }

    }
    //Twelve


}

