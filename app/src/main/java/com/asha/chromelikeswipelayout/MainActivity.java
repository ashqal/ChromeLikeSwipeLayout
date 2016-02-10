package com.asha.chromelikeswipelayout;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

/**
 * Created by hzqiujiadi on 15/11/27.
 * hzqiujiadi ashqalcn@gmail.com
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
    }

    public void OnListViewClicked(View view) {
        Intent i = new Intent(this,ListViewActivity.class);
        startActivity(i);
    }

    public void OnScrollViewClicked(View view) {
        Intent i = new Intent(this,ScrollViewActivity.class);
        startActivity(i);
    }

    public void OnLinearLayoutClicked(View view) {
        Intent i = new Intent(this,LinearLayoutActivity.class);
        startActivity(i);
    }

    public void OnRecyclerViewClicked(View view) {
        Intent i = new Intent(this,RecyclerViewActivity.class);
        startActivity(i);
    }

    public void OnNestedRecyclerViewClicked(View view) {
        Intent i = new Intent(this,NestedRecyclerViewActivity.class);
        startActivity(i);
    }

    public void OnCustomViewWithHeightClicked(View view) {
        Intent i = new Intent(this,CustomHeightListViewActivity.class);
        startActivity(i);
    }

    public void OnDisabledExampleClicked(View view) {
        Intent i = new Intent(this, DisabledExampleActivity.class);
        startActivity(i);
    }
}
