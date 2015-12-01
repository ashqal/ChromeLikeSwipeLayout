package com.asha.chromelikeswipelayout;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.asha.ChromeLikeSwipeLayout;

import static com.asha.ChromeLikeSwipeLayout.dp2px;

/**
 * Created by hzqiujiadi on 15/11/27.
 * hzqiujiadi ashqalcn@gmail.com
 */
public class RecyclerViewActivity extends SubActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recyclerview);

        ChromeLikeSwipeLayout chromeLikeSwipeLayout = (ChromeLikeSwipeLayout) findViewById(R.id.chrome_like_swipe_layout);
        ChromeLikeSwipeLayout.makeConfig()
                .addIcon(R.drawable.selector_icon_add)
                .addIcon(R.drawable.selector_icon_refresh)
                .addIcon(R.drawable.selector_icon_refresh)
                .addIcon(R.drawable.selector_icon_close)
                .radius(dp2px(35))
                .gap(dp2px(5))
                .circleColor(0xFF11CCFF)
                .listenItemSelected(new ChromeLikeSwipeLayout.IOnItemSelectedListener() {
                    @Override
                    public void onItemSelected(int index) {
                        Toast.makeText(RecyclerViewActivity.this, "onItemSelected:" + index, Toast.LENGTH_SHORT).show();
                    }
                })
                .setTo(chromeLikeSwipeLayout);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new Adapter());
    }

    private class VH extends RecyclerView.ViewHolder{

        public VH(View itemView) {
            super(itemView);
        }
    }
    private class Adapter extends RecyclerView.Adapter<VH> {

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater  layoutInflater = LayoutInflater.from(parent.getContext());
            View v = layoutInflater.inflate(R.layout.list_item,parent,false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 40;
        }
    }
}
