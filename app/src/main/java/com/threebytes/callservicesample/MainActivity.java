package com.threebytes.callservicesample;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.threebytes.callapi.CallService;

public class MainActivity extends AppCompatActivity {
    private static final String EXTRA_REMOTE_USER_ID = "remoteId";

    /*
     * Update with your Google Cloud Project ID, visit https://console.developers.google.com/project
     */
    private static final String GOOGLE_CLOUD_PROJECT_ID = "933691616050";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnRegister = (Button) findViewById(R.id.register);

        /*
         * allow only alphanumeric user Ids
         */
        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!Character.isLetterOrDigit(source.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }
        };
        ((EditText)findViewById(R.id.userId)).setFilters(new InputFilter[]{filter});
        ((EditText)findViewById(R.id.remoteId)).setFilters(new InputFilter[]{filter});

        /*
         * check for registration
         */
        String userId = CallService.getDefaultInstance().getUserId(getApplicationContext());
        if(userId != null) {
            ((EditText)findViewById(R.id.userId)).setText(userId);
            findViewById(R.id.userId).setEnabled(false);
            btnRegister.setEnabled(false);
            findViewById(R.id.call).setEnabled(true);
        }

        btnRegister.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                findViewById(R.id.register).setEnabled(false);

                Toast.makeText(MainActivity.this, "registering...", Toast.LENGTH_SHORT).show();

                String userId = ((EditText) findViewById(R.id.userId)).getText().toString();

                CallService.getDefaultInstance().register(userId, userId, GOOGLE_CLOUD_PROJECT_ID, MainActivity.this, new CallService.Callback() {
                    @Override
                    public void onError(Exception error) {
                        error.printStackTrace();

                        Toast.makeText(MainActivity.this, "failed to register!", Toast.LENGTH_SHORT).show();

                        findViewById(R.id.register).setEnabled(true);
                    }

                    @Override
                    public void onSuccess() {
                        findViewById(R.id.call).setEnabled(true);
                        findViewById(R.id.userId).setEnabled(false);

                        Toast.makeText(MainActivity.this, "registered!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        Button btnCall = (Button) findViewById(R.id.call);
        btnCall.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                String remoteUserId = ((EditText) findViewById(R.id.remoteId)).getText().toString();

                Intent outgoingCallIntent = new Intent(MainActivity.this, OutgoingCallActivity.class);
                outgoingCallIntent.putExtra(EXTRA_REMOTE_USER_ID, remoteUserId);

                outgoingCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(outgoingCallIntent);
            }
        });

        TextView legalsView = (TextView) findViewById(R.id.textLegals);
        legalsView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
