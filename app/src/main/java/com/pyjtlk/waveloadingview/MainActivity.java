package com.pyjtlk.waveloadingview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.pyjtlk.waveloadview.WaveLoadingView;

public class MainActivity extends AppCompatActivity {
    private WaveLoadingView waveLoadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        waveLoadingView = findViewById(R.id.loadView);
    }

    public void onClick(View view) {
    }
}
