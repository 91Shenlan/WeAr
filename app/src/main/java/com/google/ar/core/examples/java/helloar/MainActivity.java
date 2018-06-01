package com.google.ar.core.examples.java.helloar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.webank.mbank.ar.HelloArActivity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(MainActivity.this, HelloArActivity.class);
        startActivity(intent);

    }
}
