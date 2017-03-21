package kartaskup.samuki.me.kartaskup;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class CardAddActivity extends AppCompatActivity {
    EditText loginText;
    EditText passwordText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card);

        loginText = (EditText)findViewById(R.id.login);
        passwordText = (EditText)findViewById(R.id.password);
    }

    public void back(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void memberCard(View view) {
        if(!loginText.getText().toString().equals("") && !passwordText.getText().toString().equals("")) {
            SharedPreferences.Editor editor = MainActivity.prefs.edit();
            editor.putString("Login", loginText.getText().toString());
            editor.putString("Password", passwordText.getText().toString());
            editor.putBoolean("CardWasPin", true);
            editor.putString("Cookies", null);
            editor.apply();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        else
            Toast.makeText(this, "Podaj dane do logowania", Toast.LENGTH_LONG).show();
    }
}
