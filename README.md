# FastScroll 

FastScroll UI for ScrollView and EditText

Use androidx

## File
[aar](https://github.com/NenkaLab/FastScroll/raw/master/FastScroll.aar)

## Usage

### Xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
        xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        xmlns:tools="http://schemas.android.com/tools"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="horizontal"
        tools:context=".activity.Main">

    <androidx.core.widget.NestedScrollView
            android:id="@+id/scroll"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:clipChildren="false"
            android:fillViewport="true"
            android:clipToPadding="true">
        <LinearLayout
                android:id="@+id/content"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="vertical"/>
    </androidx.core.widget.NestedScrollView>

    <io.nenkalab.fastscroll.FastScroll
            android:id="@+id/handle"
            android:layout_width="30dp"
            android:layout_height="match_parent"
            android:layout_gravity="end|center"
            app:scroll="@+id/scroll"/>
</FrameLayout>
```

### Attrs
```xml
<io.nenkalab.fastscroll.FastScroll
    app:scroll="reference"
    app:scrollThumb="reference"
    app:scrollThumbTint="color"
    app:scrollThumbTintMode="enum[PorterDuff.Mode]"
    app:scrollThumbColor="color"
    app:autoHide="boolean"
    app:scrollThumbRotation="enum[start, end]" />
```

### Image

![Scroll](https://raw.githubusercontent.com/NenkaLab/FastScroll/master/20191216_151107.jpg)

### License

[MIT](https://github.com/NenkaLab/FastScroll/blob/master/LICENSE)
