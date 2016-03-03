# Audio Widget Overlay View #

![Demo image](https://trello-attachments.s3.amazonaws.com/56c1a2d7384163114caddfe6/800x600/758bbb56f5c0ad476a06b581fbf4d419/Untitled-2.gif)

To use Audio Widget Overlay first add dependency to your project:
 
```groovy
dependencies {
    compile 'com.cleveroad:audiowidget:0.9.0'
}
``` 
 
Then you can create new instance of widget using builder:
```JAVA
AudioWidget audioWidget = new AudioWidget.Builder(context)
        .lightColor(...)
        .darkColor(...)
        .expandWidgetColor(...)
        .progressColor(...)
        .progressStrokeWidth(...)
        .crossColor(...)
        .crossOverlappedColor(...)
        .crossStrokeWidth(...)
        .buttonPadding(...)
        .bubblesMinSize(...)
        .bubblesMaxSize(...)
        .shadowColor(...)
        .shadowRadius(...)
        .shadowDx(...)
        .shadowDy(...)
        .playDrawable(...)
        .pauseDrawable(...)
        .playlistDrawable(...)
        .prevTrackDrawale(...)
        .nextTrackDrawable(...)
        .defaultAlbumDrawable(...)
        .build();
```

Or you can use default configuration. Just call:
```JAVA
AudioWidget audioWidget = new AudioWidget.Builder(context).build();
```

Then you can use audio widget's controller to listen for events:

```JAVA
// media buttons' click listener
audioWidget.controller().onControlsClickListener(new AudioWidget.OnControlsClickListener() {
    @Override
    public void onPlaylistClicked() {
        // playlist icon clicked
    }

    @Override
    public void onPreviousClicked() {
        // previous track button clicked
    }

    @Override
    public boolean onPlayPauseClicked() {
        // return true to change playback state of widget and play button click animation (in collapsed state)
        return true;
    }

    @Override
    public void onNextClicked() {
        // next track button clicked
    }

    @Override
    public void onAlbumClicked() {
        // album cover clicked
    }
});

// widget's state listener
audioWidget.controller().onWidgetStateChangedListener(new AudioWidget.OnWidgetStateChangedListener() {
    @Override
    public void onWidgetStateChanged(@NonNull AudioWidget.State state) {
        // widget state changed (COLLAPSED, EXPANDED, REMOVED)
    }

    @Override
    public void onWidgetPositionChanged(int cx, int cy) {
        // widget position change. Save coordinates here to reuse them next time AudioWidget.show(int, int) called.
    }
});
```

Using AudioWidget.Controller, you can set track's duration, current position or album cover. Also you can set current playback state using start(), pause() or stop() methods. See **MusicService** class for more info on how to use controller.

To show audio widget on screen call **AudioWidget.show(int, int)** method. To hide it call **AudioWidget.hide()** method. Very simple!
```JAVA
audioWidget.show(100, 100); // coordinates in pixels on screen from top left corner
...
audioWidget.hide();
```

<br />
#### Support ####
* * *
If you have any other questions regarding the use of this library, please contact us for support at info@cleveroad.com (email subject: "Android loading animation view. Support request.") 
or 

Use our contacts: 

* [Official site](https://www.cleveroad.com/?utm_source=github&utm_medium=link&utm_campaign=contacts)
* [Facebook account](https://www.facebook.com/cleveroadinc)
* [Twitter account](https://twitter.com/CleveroadInc)
* [Google+ account](https://plus.google.com/+CleveroadInc/)

<br />
#### License ####
* * *
    The MIT License (MIT)
    
    Copyright (c) 2016 Cleveroad Inc.
    
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.