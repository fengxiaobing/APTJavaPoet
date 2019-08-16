package com.example.mymvpsample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.annotation.ARouter;
import com.example.annotation.BindView;
import com.example.annotation.HelloWorld;
import com.example.library.ButterKnife;

@ARouter(path = "asd")
public class MainActivity extends AppCompatActivity {
    @BindView(R.id.tv)
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

    }
    @HelloWorld
    public void sayHello(){

    }
}
