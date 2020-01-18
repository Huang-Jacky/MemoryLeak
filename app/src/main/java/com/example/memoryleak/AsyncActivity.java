package com.example.memoryleak;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 非静态内部类导致内存泄漏：比如AsyncTask，Handler
 */
public class AsyncActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeTask();
            }
        });
    }

    //会导致内存泄漏
    private void executeTask() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                for (int i = 0; i < Long.MAX_VALUE; i++) {
                    Log.e("test", String.valueOf(i));
                }
                return null;
            }
        }.execute();
    }
}
