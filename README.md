# ChromeLikeSwipeLayout
Pull down, and execute more action!

## Screenshot
![DemoPreview](https://raw.githubusercontent.com/ashqal/ChromeLikeSwipeLayout/master/screenshot/DemoPreview.gif)
</br>

## Usage
```java
<com.asha.ChromeLikeSwipeLayout
    android:id="@+id/chrome_like_swipe_layout"
    app:clwl_circleColor="#89d999"
    app:clwl_gap="0dp"
    app:clwl_radius="45dp"
    app:clwl_maxHeight="30dp"
    app:clwl_collapseDuration="300"
    app:clwl_rippleDuration="300"
    app:clwl_gummyDuration="300"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <LinearLayout
        android:orientation="vertical"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="#DDDDDD">
       ...	
    </LinearLayout>
</com.asha.ChromeLikeSwipeLayout>
```

* item selected callback
```java
ChromeLikeSwipeLayout chromeLikeSwipeLayout = (ChromeLikeSwipeLayout) findViewById(R.id.chrome_like_swipe_layout);
ChromeLikeSwipeLayout.makeConfig()
        .addIcon(R.drawable.selector_icon_add)
        .addIcon(R.drawable.selector_icon_refresh)
        .addIcon(R.drawable.selector_icon_close)
	.maxHeight(dp2px(40))
        .radius(dp2px(35))
        .gap(dp2px(5))
        .circleColor(0xFF11CCFF)
        .gummyDuration(1000)
        .rippleDuration(1000)
        .collapseDuration(1000)
        .listenItemSelected(new ChromeLikeSwipeLayout.IOnItemSelectedListener() {
            @Override
            public void onItemSelected(int index) {
                Toast.makeText(RecyclerViewActivity.this, "onItemSelected:" + index, Toast.LENGTH_SHORT).show();
            }
        })
        .setTo(chromeLikeSwipeLayout);
```

## Supported
* All ViewGroup

## Download
```
   repositories { 
        jcenter()
        maven { url "https://jitpack.io" }
   }
   dependencies {
         compile 'com.github.ashqal:ChromeLikeSwipeLayout:<version>'
   }
```

## Release Note
0.3 add multi touch support

##LICENSE
```
Copyright 2015 Asha

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
