    private android.widget.FrameLayout mNm7Mini;

    private void installNm7MiniPlayer() {
        android.widget.FrameLayout root = findViewById(android.R.id.content);
        if (mNm7Mini != null) {
            root.removeView(mNm7Mini);
            mNm7Mini = null;
        }
        if (!com.liskovsoft.smartyoutubetv2.droid.ui.playback.PlaybackActivity.isNm7MiniPlayerActive()) return;
        float density = getResources().getDisplayMetrics().density;
        mNm7Mini = new android.widget.FrameLayout(this);
        mNm7Mini.setBackgroundColor(android.graphics.Color.BLACK);
        android.widget.FrameLayout.LayoutParams box = new android.widget.FrameLayout.LayoutParams(
                (int) (240 * density), (int) (135 * density), android.view.Gravity.BOTTOM | android.view.Gravity.END);
        box.bottomMargin = (int) (80 * density);
        box.rightMargin = (int) (12 * density);
        android.view.TextureView video = new android.view.TextureView(this);
        mNm7Mini.addView(video, new android.widget.FrameLayout.LayoutParams(-1, -1));
        android.widget.FrameLayout host = mNm7Mini;
        video.setOnClickListener(v -> com.liskovsoft.smartyoutubetv2.droid.ui.playback.PlaybackActivity.restoreNm7Player());
        android.widget.ImageButton close = new android.widget.ImageButton(this);
        close.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        close.setContentDescription("Đóng video YouTube");
        host.addView(close, new android.widget.FrameLayout.LayoutParams((int) (40 * density),
                (int) (40 * density), android.view.Gravity.TOP | android.view.Gravity.END));
        close.setOnClickListener(v -> {
            com.liskovsoft.smartyoutubetv2.droid.ui.playback.PlaybackActivity.stopForNm7Iptv();
            root.removeView(host);
            mNm7Mini = null;
        });
        root.addView(host, box);
        com.liskovsoft.smartyoutubetv2.droid.ui.playback.PlaybackActivity.attachNm7MiniPlayer(video);
    }
