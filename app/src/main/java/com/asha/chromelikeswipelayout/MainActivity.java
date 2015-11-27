package com.asha.chromelikeswipelayout;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
    }

    public void TestA(View view) {
        Intent i = new Intent(this,AMainActivity.class);
        startActivity(i);
    }

    public void TestB(View view) {
        Intent i = new Intent(this,BMainActivity.class);
        startActivity(i);
    }

    public void OnListViewClicked(View view) {
        Intent i = new Intent(this,ListViewActivity.class);
        startActivity(i);
    }
}
