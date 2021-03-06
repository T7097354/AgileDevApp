package com.agiledev.agiledevapp;

import android.app.AlertDialog;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.app.PendingIntent.getActivity;

public class FeedbackDialogFragment extends DialogFragment
{
    private static final String TAG = "FeedbackDialogFragment";

    //wigets
    private Button mActionSubmit, mActionCancel;
    public EditText mName, mEmail, mMessage;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.dialogfrag_feedback, container, false);
        mActionSubmit = view.findViewById(R.id.feedbackSubmitbutton);
        mActionCancel = view.findViewById(R.id.feedbackcancelbutton);
        mMessage = view.findViewById(R.id.feedbackmessagetext);
        sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        final AlertDialog noTextDialog = SimpleDialog.create(DialogOption.OkOnlyDismiss, getActivity(),
                "Error!", "Feedback needs to have text!");

        mActionCancel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                getDialog().dismiss();
                mMessage.setText("");
            }
        });

        mActionSubmit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(mMessage.getText().toString().length() < 5)
                {
                    noTextDialog.show();
                }
                else
                {
                    String username = sharedPref.getString(getString(R.string.prefs_loggedin_username), null);
                    final String messageS = mMessage.getText().toString();
                    final FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("UserDetails").document(username).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot doc = task.getResult();
                                if (doc != null && doc.exists()) {
                                    final String email = doc.get("email").toString();
                                    db.collection("Feedback").document(email).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            HashMap<String, Object> feedbackIssuesMap = new HashMap<>();
                                            HashMap<String, ArrayList<String>> feedbackMap = new HashMap<>();
                                            ArrayList<String> feedbackMessages = new ArrayList<>();
                                            if (documentSnapshot.exists()) {
                                                feedbackIssuesMap = (HashMap<String, Object>)documentSnapshot.getData();
                                                feedbackMap = (HashMap<String, ArrayList<String>>)feedbackIssuesMap.get("Feedback");
                                                if (feedbackIssuesMap.get("Feedback") != null)
                                                {
                                                    feedbackMessages = feedbackMap.get("Feedback");
                                                }
                                            }
                                            feedbackMessages.add(messageS);
                                            feedbackMap.put("Feedback", feedbackMessages);
                                            feedbackIssuesMap.put("Feedback", feedbackMap);
                                            db.collection("Feedback").document(email).set(feedbackIssuesMap);
                                        }
                                    });
                                }
                            }
                        }
                    });
                    dismiss();
                    final AlertDialog dialog = SimpleDialog.create(DialogOption.OkOnlyDismiss, getActivity(),
                            "Feedback sent", "Thank you for sending feedback");
                    dialog.show();
                }
                mMessage.setText("");
            }
        });

        return view;
    }
}