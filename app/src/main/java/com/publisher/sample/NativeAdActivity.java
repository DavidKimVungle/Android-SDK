package com.publisher.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.vungle.warren.AdConfig;
import com.vungle.warren.NativeAd;
import com.vungle.warren.NativeAdLayout;
import com.vungle.warren.NativeAdListener;
import com.vungle.warren.NativeAdOptionsView;
import com.vungle.warren.error.VungleException;
import com.vungle.warren.ui.view.MediaView;

import java.util.ArrayList;
import java.util.List;

public class NativeAdActivity extends AppCompatActivity {
    protected static String PACKAGE_NAME;
    private static final String LOG_TAG = "VungleSampleApp";

    private NativeAd vungleNativeAd;
    private NativeAdOptionsView adOptionsView;

    private FrameLayout outerNativeAdContainer;
    private TextView nativeAdLog;
    private TextView titleView;
    private TextView bodyView;
    private TextView sponsoredView;
    private TextView rateView;
    private ViewGroup adChoicesContainer;
    private MediaView adContentView;
    private ImageView iconView;
    private Button ctaButton;
    private Button loadAdButton;
    private Button playAdButton;
    private Button closeAdButton;

    final private String nativeAd = "native_ad";

    public static Intent getIntent(Context ctx) {
        Intent intent = new Intent(ctx, NativeAdActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_ad);

        PACKAGE_NAME = getApplicationContext().getPackageName();
        Log.d(LOG_TAG, PACKAGE_NAME);

        initUIElements();
    }

    private void initUIElements() {
        Log.d(LOG_TAG, "Initialize Native Ad");

        outerNativeAdContainer = findViewById(R.id.outer_native_ad_container);
        outerNativeAdContainer.setVisibility(View.INVISIBLE);

        nativeAdLog = findViewById(R.id.native_ad_status);
        adContentView = findViewById(R.id.native_ad_media);
        titleView = findViewById(R.id.native_ad_title);
        bodyView = findViewById(R.id.native_ad_body);
        rateView = findViewById(R.id.native_ad_rate);
        sponsoredView = findViewById(R.id.native_ad_sponsored);
        iconView = findViewById(R.id.native_ad_icon);
        ctaButton = findViewById(R.id.native_ad_call_to_action);
        adChoicesContainer  = findViewById(R.id.ad_choices_container);

        if (vungleNativeAd == null) {
            vungleNativeAd = new NativeAd(this, getString(R.string.placement_id_native_ad));
        }

        loadAdButton = findViewById(R.id.load_native_ad_button);
        loadAdButton.setOnClickListener(view -> {
//            vungleNativeAd.destroy();
//            vungleNativeAd = new NativeAd(this, getString(R.string.placement_id_native_ad));
            AdConfig adConfig = new AdConfig();
            vungleNativeAd.loadAd(adConfig, vungleNativeAdListener);
        });

        playAdButton = findViewById(R.id.play_native_ad_button);
        playAdButton.setOnClickListener(view -> {
            if (vungleNativeAd != null && vungleNativeAd.canPlayAd()) {
                Log.d(LOG_TAG, Boolean.toString(vungleNativeAd.canPlayAd()));
                populateNativeAdView(vungleNativeAd);
            }
        });

        closeAdButton = findViewById(R.id.finish_native_ad_button);
        closeAdButton.setOnClickListener(view -> {
            if (vungleNativeAd != null) {
                vungleNativeAd.destroy();
            }
        });

//        disableButton(playAdButton);
//        disableButton(closeAdButton);
    }

    private void populateNativeAdView(NativeAd nativeAd) {
        outerNativeAdContainer.setVisibility(View.VISIBLE);
        NativeAdLayout nativeAdLayout;
        View actualView = outerNativeAdContainer.getChildAt(0);
        if (actualView instanceof NativeAdLayout) {
            nativeAdLayout = (NativeAdLayout) actualView;
        } else {
            nativeAdLayout = new NativeAdLayout(this);
            nativeAdLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            outerNativeAdContainer.removeView(actualView);
            nativeAdLayout.addView(actualView);
            outerNativeAdContainer.addView(nativeAdLayout);
        }

        if (adChoicesContainer != null) {
            adOptionsView = new NativeAdOptionsView(this, vungleNativeAd, nativeAdLayout);
            adChoicesContainer.removeAllViews();
            adChoicesContainer.addView(adOptionsView, 0);
        }

        titleView.setText(nativeAd.getAdTitle());
        bodyView.setText(nativeAd.getAdBodyText());
        rateView.setText(Double.toString(nativeAd.getAdStarRating()));
        sponsoredView.setText(nativeAd.getAdSponsoredText());
        ctaButton.setText(nativeAd.getAdCallToActionText());
        ctaButton.setVisibility(nativeAd.hasCallToAction() ? View.VISIBLE : View.INVISIBLE);

        List<View> clickableViews = new ArrayList<>();
        clickableViews.add(iconView);
        clickableViews.add(adContentView);
        clickableViews.add(ctaButton);
        nativeAd.registerViewForInteraction(
                nativeAdLayout, adContentView, iconView, clickableViews);
    }

    private final NativeAdListener vungleNativeAdListener = new NativeAdListener() {

        @Override
        public void onNativeAdLoaded(NativeAd nativeAd) {
            nativeAdLog.append("onNativeAdLoaded\n");
            Log.d(LOG_TAG, "onNativeAdLoaded");
//            enableButton(playAdButton);
//            disableButton(loadAdButton);
        }

        @Override
        public void onAdLoadError(String s, VungleException e) {
            nativeAdLog.append(String.format("onAdLoadError - %s\n%s\n", s, e.getLocalizedMessage()));
            Log.d(LOG_TAG, String.format("onAdLoadError - %s\n%s", s, e.getLocalizedMessage()));
        }

        @Override
        public void onAdPlayError(String s, VungleException e) {
            nativeAdLog.append(String.format("onAdPlayError - %s\n%s\n", s, e.getLocalizedMessage()));
            Log.d(LOG_TAG, String.format("onAdLoadError - %\n%s", s, e.getLocalizedMessage()));
//            disableButton(playAdButton);
//            disableButton(closeAdButton);
//            enableButton(loadAdButton);
        }

        @Override
        public void onAdStart(String s) {
            nativeAdLog.append(String.format("onAdStart - %s\n", s));
            Log.d(LOG_TAG, "onAdStart - " + s);
//            disableButton(playAdButton);
//            enableButton(closeAdButton);
        }

        @Override
        public void onAdViewed(String s) {
            nativeAdLog.append(String.format("onAdViewed - %s\n", s));
            Log.d(LOG_TAG, "onAdViewed - " + s);
//            vungleNativeAd.loadAd(new AdConfig(), vungleNativeAdListener);
//            vungleNativeAd.canPlayAd();
        }

        @Override
        public void onAdClick(String s) {
            nativeAdLog.append(String.format("onAdClick - %s\n", s));
            Log.d(LOG_TAG, "onAdClick - " + s);
        }

        @Override
        public void onAdEnd(String s) {
            nativeAdLog.append(String.format("onAdEnd - %s\n", s));
            Log.d(LOG_TAG, "onAdEnd - " + s);
//            disableButton(playAdButton);
//            disableButton(closeAdButton);
//            enableButton(loadAdButton);
        }

        @Override
        public void onAdLeftApplication(String s) {
            nativeAdLog.append(String.format("onAdLeftApplication - %s\n", s));
            Log.d(LOG_TAG, "onAdLeftApplication - " + s);
        }
    };

    private void enableButton(final Button button) {
        if (button == null) { return; }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setEnabled(true);
                button.setAlpha(1.0f);
            }
        });
    }

    private void disableButton(final Button button) {
        if (button == null) { return; }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setEnabled(false);
                button.setAlpha(0.5f);
            }
        });
    }
}
