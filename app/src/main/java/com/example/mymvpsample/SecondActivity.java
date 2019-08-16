package com.example.mymvpsample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import com.example.annotation.BindView;

public class SecondActivity extends AppCompatActivity {
@BindView(R.id.button)
    Button buttonasasd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
    }
}
