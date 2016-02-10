package com.asha.chromelikeswipelayout;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.asha.ChromeLikeSwipeLayout;

import static com.asha.ChromeLikeSwipeLayout.dp2px;

/**
 * Created by hzqiujiadi on 15/11/27.
 * hzqiujiadi ashqalcn@gmail.com
 */
public class DisabledExampleActivity extends SubActivity {
    private ChromeLikeSwipeLayout chromeLikeSwipeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listview);

        chromeLikeSwipeLayout = (ChromeLikeSwipeLayout) findViewById(R.id.chrome_like_swipe_layout);
        ChromeLikeSwipeLayout.makeConfig()
                .setMaxHeight(dp2px(30)) // set the max height to be 30dp
                .addIcon(R.drawable.selector_icon_add)
                .addIcon(R.drawable.selector_icon_refresh)
                .addIcon(R.drawable.selector_icon_close)
                .circleColor(0xFF11CCFF)
                .listenItemSelected(new ChromeLikeSwipeLayout.IOnItemSelectedListener() {
                    @Override
                    public void onItemSelected(int index) {
                        Toast.makeText(DisabledExampleActivity.this, "onItemSelected:" + index, Toast.LENGTH_SHORT).show();
                    }
                })
                .setTo(chromeLikeSwipeLayout);

        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(new Adapter());
    }

    @Override
    protected void onResume() {
        super.onResume();
        chromeLikeSwipeLayout.setEnabled(false);
    }

    private class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return 40;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if ( convertView == null ){
                LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
                convertView = layoutInflater.inflate(R.layout.list_item,parent,false);
            }
            return convertView;
        }
    }
}
