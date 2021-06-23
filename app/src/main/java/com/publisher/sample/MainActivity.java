package com.publisher.sample;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.vungle.warren.BannerAdConfig;
import com.vungle.warren.Banners;
import com.vungle.warren.Vungle;
import com.vungle.warren.AdConfig;              // Custom ad configurations
import com.vungle.warren.InitCallback;          // Initialization callback
import com.vungle.warren.LoadAdCallback;        // Load ad callback
import com.vungle.warren.PlayAdCallback;        // Play ad callback
import com.vungle.warren.VungleApiClient;
import com.vungle.warren.VungleBanner;          // Banner
import com.vungle.warren.VungleNativeAd;        // MREC
import com.vungle.warren.Vungle.Consent;        // GDPR consent
import com.vungle.warren.VungleSettings;
import com.vungle.warren.error.VungleException; // onError message


import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static com.vungle.warren.Vungle.getValidPlacements;
import static java.lang.Thread.sleep;


public class MainActivity extends AppCompatActivity {

    Map<String, String> adMarkUp = new HashMap<>();

    private class VungleAd {
        @NonNull private final String name;
        @NonNull private final String placementReferenceId;
        @NonNull private final TextView titleTextView;
        @NonNull private final Button loadButton;
        @NonNull private final Button playButton;
        @NonNull private boolean nativeAdPlaying;
        @Nullable private final Button pauseResumeButton;
        @Nullable private final Button closeButton;
        @Nullable private final RelativeLayout container;
        @Nullable private final Button bannerListButton;
        @Nullable private final Button bannerMultipleButton;

        private VungleAd(String name) {
            this.name = name;
            this.placementReferenceId = getPlacementReferenceId();
            this.titleTextView = getTextView();
            this.loadButton = getLoadButton();
            this.playButton = getPlayButton();
            this.pauseResumeButton = getPauseResumeButton();
            this.closeButton = getCloseButton();
            this.bannerListButton = getBannerListButton();
            this.bannerMultipleButton = getBannerMultipleButton();
            this.container = getContainer();
            this.nativeAdPlaying = false;
        }

        private String getPlacementReferenceId() {
            int stringId = getResources().getIdentifier("placement_id_" + name, "string", PACKAGE_NAME);
            return getString(stringId);
        }

        private TextView getTextView() {
            int textViewId = getResources().getIdentifier("tv_" + name, "id", PACKAGE_NAME);
            String textViewString = getString(getResources().getIdentifier("title_" + name, "string", PACKAGE_NAME));
            TextView tv = (TextView) findViewById(textViewId);
            tv.setText(textViewString + " - " + placementReferenceId);
            return tv;
        }

        private Button getLoadButton() {
            int buttonId = getResources().getIdentifier("btn_load_" + name, "id", PACKAGE_NAME);
            Button button = (Button) findViewById(buttonId);
            disableButton(button);
            return button;
        }

        private Button getPlayButton() {
            int buttonId = getResources().getIdentifier("btn_play_" + name, "id", PACKAGE_NAME);
            Button button = (Button) findViewById(buttonId);
            disableButton(button);
            return button;
        }

        private Button getPauseResumeButton() {
            int buttonId = getResources().getIdentifier("btn_pause_resume_" + name, "id", PACKAGE_NAME);
            Button button = (Button) findViewById(buttonId);
            if (button != null) {
                return button;
            }
            return null;
        }

        private Button getCloseButton() {
            int buttonId = getResources().getIdentifier("btn_close_" + name, "id", PACKAGE_NAME);
            Button button = (Button) findViewById(buttonId);
            if (button != null) {
                return button;
            }
            return null;
        }

        private RelativeLayout getContainer() {
            int containerId = getResources().getIdentifier("container_" + name, "id", PACKAGE_NAME);
            RelativeLayout container = (RelativeLayout) findViewById(containerId);
            if (container != null) {
                return container;
            }
            return null;
        }

        private Button getBannerListButton() {
            int buttonId = getResources().getIdentifier("btn_list_" + name, "id", PACKAGE_NAME);
            Button button = (Button) findViewById(buttonId);
            if (button != null) {
                return button;
            }
            return null;
        }

        private Button getBannerMultipleButton() {
            int buttonId = getResources().getIdentifier("btn_multiple_" + name, "id", PACKAGE_NAME);
            Button button = (Button) findViewById(buttonId);
            if (button != null) {
                return button;
            }
            return null;
        }
    }

    protected static String PACKAGE_NAME;

    private View nativeAdView;
    private VungleNativeAd vungleNativeAd;
    private VungleBanner vungleBannerAd;

    private List<VungleAd> vungleAds = new ArrayList<>();

    final private String interstitial = "interstitial_legacy";
    final private String interstitialHeaderBidding = "interstitial_dt";
    final private String rewarded = "rewarded_video";
    final private String rewardedHeaderBidding = "rewarded_playable";
    final private String mrec = "mrec";
    final private String banner = "banner";

    final String LOG_TAG = "VungleSampleApp";

    private Consent vungleConsent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PACKAGE_NAME = getApplicationContext().getPackageName();

        vungleAds.add(new VungleAd(interstitial));
        vungleAds.add(new VungleAd(interstitialHeaderBidding));
        vungleAds.add(new VungleAd(rewarded));
        vungleAds.add(new VungleAd(rewardedHeaderBidding));
        vungleAds.add(new VungleAd(mrec));
        vungleAds.add(new VungleAd(banner));

        // CCPA example
        Vungle.updateCCPAStatus(Consent.OPTED_IN);
        Vungle.getCCPAStatus();
        Log.d(LOG_TAG, "CCPA (pre init) - " + Vungle.getCCPAStatus());

        // GDPR example
        Vungle.updateConsentStatus(Consent.OPTED_IN, "1.0.0");
        Vungle.getConsentStatus();
        Vungle.getConsentMessageVersion();
        Log.d(LOG_TAG, "GDPR (pre init) - " + Vungle.getConsentStatus() + " " + Vungle.getConsentMessageVersion());

//        modifyEndPoint();

        initUiElements();
        initSDK();
    }

    private void initSDK() {
        final String appId = getString(R.string.app_id);

        final long MEGABYTE = 1024L * 1024L;
        final VungleSettings vungleSettings =
                new VungleSettings.Builder()
                        .setMinimumSpaceForAd(20 * MEGABYTE)
                        .setMinimumSpaceForInit(21 * MEGABYTE)
                        .setAndroidIdOptOut(false)
                        .build();

        // CCPA example
        Vungle.updateCCPAStatus(Consent.OPTED_OUT);
        Vungle.getCCPAStatus();
        Log.d(LOG_TAG, "CCPA (init success) - " + Vungle.getCCPAStatus());

        // GDPR example
        Vungle.updateConsentStatus(Consent.OPTED_OUT, "2.0.0");
        Vungle.getConsentStatus();
        Vungle.getConsentMessageVersion();
        Log.d(LOG_TAG, "GDPR (init success) - " + Vungle.getConsentStatus() + " " + Vungle.getConsentMessageVersion());

        Vungle.init(appId, getApplicationContext(), new InitCallback() {
            @Override
            public void onSuccess() {
                makeToast("Vungle SDK initialized");

//                getAdMarkUp(getString(R.string.app_id), getString(R.string.placement_id_interstitial_dt));

                Log.d(LOG_TAG, "InitCallback - onSuccess");
                Log.d(LOG_TAG, "Vungle SDK Version - " + com.vungle.warren.BuildConfig.VERSION_NAME);
                Log.d(LOG_TAG, "Valid placement list:");

                for (String validPlacementReferenceIdId : getValidPlacements()) {
                    Log.d(LOG_TAG, validPlacementReferenceIdId);
                }

//                setCustomRewardedFields();

//                Vungle.loadAd("DYNAMIC_TEMPLATE_INTERSTITIAL-6969365", null, vungleLoadAdCallback);
//                Vungle.loadAd("DYNAMIC_TEMPLATE_REWARDED-5271535", null, vungleLoadAdCallback);
//                Vungle.loadAd("MREC-2191415", null, vungleLoadAdCallback);

                // Set button state according to ad playability
                for (VungleAd vungleAd : vungleAds) {
                    String id = vungleAd.placementReferenceId;
//                    getAdMarkUp(id);
                    if (Vungle.canPlayAd(id) || Vungle.canPlayAd(id, adMarkUp.get(id))) {
                        enableButton(vungleAd.playButton);
                    } else {
                        enableButton(vungleAd.loadButton);
                    }
                }
            }

            @Override
            public void onError(VungleException ex) {
                if (ex != null) {
                    Log.d(LOG_TAG, "InitCallback - onError: " + ex.getLocalizedMessage());
                } else {
                    Log.d(LOG_TAG, "Throwable is null");
                }
            }

            @Override
            public void onAutoCacheAdAvailable(final String placementReferenceID) {
                Log.d(LOG_TAG, "InitCallback - onAutoCacheAdAvailable" +
                        "\n\tPlacement Reference ID = " + placementReferenceID);

                VungleAd ad = getVungleAd(placementReferenceID);
                if (ad != null) {
                    enableButton(ad.playButton);
                    disableButton(ad.loadButton);
                }
            }
        }, vungleSettings);
    }

    private final PlayAdCallback vunglePlayAdCallback = new PlayAdCallback() {
        @Override
        public void onAdStart(final String placementReferenceID) {

            // CCPA example
            Vungle.updateCCPAStatus(Consent.OPTED_IN);
            Vungle.getCCPAStatus();
            Log.d(LOG_TAG, "CCPA (onAdStart) - " + Vungle.getCCPAStatus());

            // GDPR example
            Vungle.updateConsentStatus(Consent.OPTED_IN, "3.0.0");
            Vungle.getConsentStatus();
            Vungle.getConsentMessageVersion();
            Log.d(LOG_TAG, "GDPR (onAdStart) - " + Vungle.getConsentMessageVersion() + Vungle.getConsentStatus() + " " + Vungle.getConsentMessageVersion());

            Log.d(LOG_TAG, "PlayAdCallback - onAdStart" +
                    "\n\tPlacement Reference ID = " + placementReferenceID);

            VungleAd ad = getVungleAd(placementReferenceID);
            if (ad != null) {
                disableButton(ad.playButton);
            }
        }

        @Override
        public void creativeId(String creativeId) {
            Log.i(LOG_TAG, "PlayAdCallback - creativeId" +
                    "\n\tCreative ID = " + creativeId);
        }

        @Override
        public void onAdViewed(String placementReferenceID) {
            Log.d(LOG_TAG, "PlayAdCallback - onAdViewed" +
                    "\n\tPlacement Reference ID = " + placementReferenceID);
        }

        // Deprecated
        @Override
        public void onAdEnd(final String placementReferenceID, final boolean completed, final boolean isCTAClicked) {
            Log.d(LOG_TAG, "PlayAdCallback - onAdEnd" +
                    "\n\tPlacement Reference ID = " + placementReferenceID +
                    "\n\tView Completed = " + completed + "" +
                    "\n\tDownload Clicked = " + isCTAClicked);
        }

        @Override
        public void onAdEnd(String placementReferenceID) {
            Log.d(LOG_TAG, "PlayAdCallback - onAdEnd" +
                    "\n\tPlacement Reference ID = " + placementReferenceID);
        }

        @Override
        public void onAdClick(String placementReferenceID) {
            Log.d(LOG_TAG, "PlayAdCallback - onAdClick" +
                    "\n\tPlacement Reference ID = " + placementReferenceID);
        }

        @Override
        public void onAdRewarded(String placementReferenceID) {
            Log.d(LOG_TAG, "PlayAdCallback - onAdRewarded" +
                    "\n\tPlacement Reference ID = " + placementReferenceID);
        }

        @Override
        public void onAdLeftApplication(String placementReferenceID) {
            Log.d(LOG_TAG, "PlayAdCallback - onAdLeftApplication" +
                    "\n\tPlacement Reference ID = " + placementReferenceID);
        }

        @Override
        public void onError(final String placementReferenceID, VungleException ex) {
            Log.d(LOG_TAG, "PlayAdCallback - onError" +
                    "\n\tPlacement Reference ID = " + placementReferenceID +
                    "\n\tError = " + ex.getLocalizedMessage());

            makeToast(ex.getLocalizedMessage());
            checkInitStatus(ex);
        }
    };

    private final LoadAdCallback vungleLoadAdCallback = new LoadAdCallback() {
        @Override
        public void onAdLoad(final String placementReferenceID) {
            Log.d(LOG_TAG, "LoadAdCallback - onAdLoad" +
                    "\n\tPlacement Reference ID = " + placementReferenceID);

            VungleAd ad = getVungleAd(placementReferenceID);
            if (ad != null) {
                enableButton(ad.playButton);
                disableButton(ad.loadButton);
                enableButton(ad.bannerListButton);
            }
        }

        @Override
        public void onError(final String placementReferenceID, VungleException ex) {
            Log.d(LOG_TAG, "LoadAdCallback - onError" +
                    "\n\tPlacement Reference ID = " + placementReferenceID +
                    "\n\tError = " + ex.getLocalizedMessage());

            makeToast(ex.getLocalizedMessage());
            checkInitStatus(ex);
            VungleAd ad = getVungleAd(placementReferenceID);
            if (ad != null) {
                enableButton(ad.loadButton);
            }
        }
    };

    private void checkInitStatus(VungleException ex) {
        try {
            Log.d(LOG_TAG, "CheckInitStatus - " + ex.getLocalizedMessage());

            if (ex.getExceptionCode() == VungleException.VUNGLE_NOT_INTIALIZED) {
                initSDK();
            }
        } catch (ClassCastException cex) {
            Log.d(LOG_TAG, cex.getLocalizedMessage());
        }
    }

    private void setVungleAdUi(final VungleAd ad) {
        if (Vungle.canPlayAd(ad.placementReferenceId)) {
            enableButton(ad.playButton);
        } else {
            disableButton(ad.playButton);
        }

        switch (ad.name) {
            case interstitial:
                setFullscreenAd(ad);
            case interstitialHeaderBidding:
                setFullscreenAd(ad);
//                setFullscreenHeaderBiddingAd(ad);
            case rewarded:
                setFullscreenAd(ad);
            case rewardedHeaderBidding:
                setFullscreenAd(ad);
//                setFullscreenHeaderBiddingAd(ad);
                break;
            case mrec:
                setNativeAd(ad);
//                setAppBiddingBannerAd(ad, AdConfig.AdSize.VUN GLE_MREC);
//                setBannerAd(ad, AdConfig.AdSize.VUNGLE_MREC);
                break;
            case banner:
//                setAppBiddingBannerAd(ad, AdConfig.AdSize.BANNER_LEADERBOARD);
                setBannerAd(ad, AdConfig.AdSize.BANNER);
                break;
            default:
                Log.d(LOG_TAG, "Vungle ad type not recognized");
                break;
        }
    }

    private void setFullscreenAd(final VungleAd ad) {
        // Set custom configuration for rewarded placements
//        if (ad.name.equals(rewarded) || ad.name.equals(rewardedHeaderBidding)) {
//            setCustomRewardedFields();
//        }

//        setCustomRewardedFields();

        ad.loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Vungle.isInitialized()) {
                    // Play Vungle ad
                    Vungle.loadAd(ad.placementReferenceId, vungleLoadAdCallback);
                    // Button UI
                    disableButton(ad.loadButton);
                } else {
                    makeToast("Vungle SDK not initialized");
                }
            }
        });

        ad.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCustomRewardedFields();
                if (Vungle.isInitialized()) {
                    if (Vungle.canPlayAd(ad.placementReferenceId)) {
                        final AdConfig adConfig = getAdConfig();

                        // Play Vungle ad
                        Vungle.playAd(ad.placementReferenceId, adConfig, vunglePlayAdCallback);
                        // Button UI
                        enableButton(ad.loadButton);
                        disableButton(ad.playButton);
                    } else {
                        makeToast("Vungle ad not playable for " + ad.placementReferenceId);
                    }
                } else {
                    makeToast("Vungle SDK not initialized");
                }
            }
        });
    }

    private void setFullscreenHeaderBiddingAd(final VungleAd ad) {
        // Set custom configuration for rewarded placements
        if (ad.name.equals(rewarded) || ad.name.equals(rewardedHeaderBidding)) {
            setCustomRewardedFields();
        }

        final String p = ad.placementReferenceId;
        ad.loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Vungle.isInitialized()) {
                    String adm = adMarkUp.get(p);

//                    Log.d(LOG_TAG, "Vungle.loadAd with null adMarkup");
//                    Vungle.loadAd(p, null, new AdConfig(), vungleLoadAdCallback);
//                    Log.d(LOG_TAG, "Vungle.loadAd with invalid adMarkup");
//                    Vungle.loadAd(p, "INVALID_ADMARKUP", new AdConfig(), vungleLoadAdCallback);

                    if (adm != null) {
                        Log.d(LOG_TAG, adm);
                        Vungle.loadAd(p, adm, new AdConfig(), vungleLoadAdCallback);

                    } else {
                        Log.d(LOG_TAG, "AdMarkup not found");
                        getAdMarkUp(p);
                    }

                    // Button UI
                    disableButton(ad.loadButton);
                } else {
                    makeToast("Vungle SDK not initialized");
                }
            }
        });

        ad.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCustomRewardedFields();
                if (Vungle.isInitialized()) {
                    String adm = adMarkUp.get(p);

//                    Log.d(LOG_TAG, "Vungle.canPlayAd with null adMarkup - " + Vungle.canPlayAd(p, null));
//                    Log.d(LOG_TAG, "Vungle.playAd with null adMarkup");
//                    Vungle.playAd(ad.placementReferenceId, null, new AdConfig(), vunglePlayAdCallback);

//                    Log.d(LOG_TAG, "Vungle.canPlayAd with invalid adMarkup - " + Vungle.canPlayAd(p, "INVALID_ADMARKUP"));
//                    Log.d(LOG_TAG, "Vungle.playAd with invalid adMarkup");
//                    Vungle.playAd(ad.placementReferenceId, "INVALID_ADMARKUP", new AdConfig(), vunglePlayAdCallback);

                    if (Vungle.canPlayAd(p, adm)) {
                        Log.d(LOG_TAG, adm);
                        final AdConfig adConfig = getAdConfig();
                        Vungle.playAd(ad.placementReferenceId, adm, adConfig, vunglePlayAdCallback);
                        adMarkUp.remove(p);
                        // Button UI
                        enableButton(ad.loadButton);
                        disableButton(ad.playButton);
                    } else {
                        makeToast("Vungle ad not playable for " + ad.placementReferenceId);
                    }
                } else {
                    makeToast("Vungle SDK not initialized");
                }
                enableButton(ad.loadButton);
            }
        });
    }

    private void setNativeAd(final VungleAd ad) {
        disableButton(ad.pauseResumeButton);
        disableButton(ad.closeButton);

        final String p = ad.placementReferenceId;

        final AdConfig adConfig = new AdConfig();
        adConfig.setAdSize(AdConfig.AdSize.VUNGLE_MREC);
        adConfig.setMuted(false);

        // Loading VungleNativeAd works similar to fullscreen ad and only requires the placement to be configured properly
        ad.loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Vungle.isInitialized()) {
                    if (adMarkUp.containsKey(p)) {
                        Vungle.loadAd(p, adMarkUp.get(p), adConfig, vungleLoadAdCallback);
                    } else {
                        getAdMarkUp(p);
                    }
                } else {
                    makeToast("Vungle SDK not initialized");
                }
            }
        });

        ad.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Vungle.isInitialized()) {
                    if (Vungle.canPlayAd(p, adMarkUp.get(p))) {
                        if (vungleNativeAd != null) {
                            vungleNativeAd.finishDisplayingAd();
                            vungleNativeAd = null;
                            ad.container.removeView(nativeAdView);
                        }

                        vungleNativeAd = Vungle.getNativeAd(p, adMarkUp.get(p), adConfig, vunglePlayAdCallback);
                        adMarkUp.remove(p);

                        if (vungleNativeAd != null) {
                            nativeAdView = vungleNativeAd.renderNativeView();
                            ad.container.addView(nativeAdView);
                            ad.container.setVisibility(RelativeLayout.VISIBLE);
                        }

                        ad.nativeAdPlaying = true;

                        // Button UI
                        enableButton(ad.loadButton);
                        disableButton(ad.playButton);
                        enableButton(ad.pauseResumeButton);
                        enableButton(ad.closeButton);

                        ad.nativeAdPlaying = true;
                        ad.pauseResumeButton.setText("PAUSE");
//
//                        try {
//                            sleep(5000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//
//                        Log.d(LOG_TAG, "play ad for fullscreen issued before MREC");
//                        Vungle.playAd("DYNAMIC_TEMPLATE_INTERSTITIAL-6969365", null, vunglePlayAdCallback);
                    } else {
                        makeToast("Vungle ad not playable for " + ad.placementReferenceId);
                    }
                } else {
                    makeToast("Vungle SDK not initialized");
                }
            }
        });

        ad.pauseResumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ad.nativeAdPlaying = !ad.nativeAdPlaying;

                if (vungleNativeAd != null) {
                    vungleNativeAd.setAdVisibility(ad.nativeAdPlaying);
                }

                if (ad.nativeAdPlaying) {
                    ad.pauseResumeButton.setText("PAUSE");
                } else {
                    ad.pauseResumeButton.setText("RESUME");
                }
            }
        });

        ad.closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (vungleNativeAd != null) {
                    vungleNativeAd.finishDisplayingAd();
                    vungleNativeAd = null;
                    ad.container.removeView(nativeAdView);
                    ad.container.setVisibility(RelativeLayout.GONE);
                }

                disableButton(ad.pauseResumeButton);
                disableButton(ad.closeButton);
            }
        });
    }

    private void setAppBiddingBannerAd(final VungleAd ad, final AdConfig.AdSize adSize) {
        disableButton(ad.pauseResumeButton);
        disableButton(ad.closeButton);
        disableButton(ad.bannerListButton);

        final String p = ad.placementReferenceId;

        final BannerAdConfig adConfig = new BannerAdConfig();
        adConfig.setAdSize(adSize);

        // Loading Banner ad works similar to fullscreen ad and only requires the placement and AdSize to be configured properly
        ad.loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Vungle.isInitialized()) {
                    if (adMarkUp.containsKey(p)) {
                        Banners.loadBanner(p, adMarkUp.get(p), adConfig, vungleLoadAdCallback);
                    } else {
                        getAdMarkUp(p);
                    }

                    // Button UI
                    disableButton(ad.loadButton);
                } else {
                    makeToast("Vungle SDK not initialized");
                }
            }
        });

        ad.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Vungle.isInitialized()) {
                    adConfig.setAdSize(AdConfig.AdSize.VUNGLE_MREC);
                    if (Banners.canPlayAd(p, adMarkUp.get(p), adConfig.getAdSize())) {
                        if (vungleBannerAd != null) {
                            vungleBannerAd.destroyAd();
                            vungleBannerAd = null;
                            ad.container.removeAllViews();
                        }

                        Log.d(LOG_TAG, "canPlayAd for BANNER" + Banners.canPlayAd(p, adMarkUp.get(p), AdConfig.AdSize.BANNER));
                        Log.d(LOG_TAG, "canPlayAd for VUNGLE_MREC" + Banners.canPlayAd(p, adMarkUp.get(p), AdConfig.AdSize.VUNGLE_MREC));
                        Log.d(LOG_TAG, "canPlayAd for BANNER_LEADERBOARD" + Banners.canPlayAd(p, adMarkUp.get(p), AdConfig.AdSize.BANNER_LEADERBOARD));
                        Log.d(LOG_TAG, "canPlayAd for BANNER_SHORT" + Banners.canPlayAd(p, adMarkUp.get(p), AdConfig.AdSize.BANNER_SHORT));

                        String placement = "1";
                        String admarkup = "1";

                        final BannerAdConfig bac = new BannerAdConfig();
                        bac.setAdSize(AdConfig.AdSize.VUNGLE_MREC);
                        vungleBannerAd = Banners.getBanner(p, adMarkUp.get(p), bac, vunglePlayAdCallback);
                        adMarkUp.remove(p);

                        if (vungleBannerAd != null) {
                            ad.container.addView(vungleBannerAd);
                            ad.container.setVisibility(RelativeLayout.VISIBLE);
                        }

                        ad.nativeAdPlaying = true;

                        // Button UI
                        enableButton(ad.loadButton);
                        disableButton(ad.playButton);
                        enableButton(ad.pauseResumeButton);
                        enableButton(ad.closeButton);

                        ad.nativeAdPlaying = true;
                        ad.pauseResumeButton.setText("PAUSE");
                    } else {
                        makeToast("Vungle ad not playable for " + ad.placementReferenceId);
                    }
                } else {
                    makeToast("Vungle SDK not initialized");
                }
            }
        });

        ad.pauseResumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ad.nativeAdPlaying = !ad.nativeAdPlaying;

                if (vungleBannerAd != null) {
                    vungleBannerAd.setAdVisibility(ad.nativeAdPlaying);
                }

                if (ad.nativeAdPlaying) {
                    ad.pauseResumeButton.setText("PAUSE");
                } else {
                    ad.pauseResumeButton.setText("RESUME");
                }
            }
        });

        ad.closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (vungleBannerAd != null) {
                    vungleBannerAd.destroyAd();
                    vungleNativeAd = null;
                    ad.container.removeView(vungleBannerAd);
                    ad.container.setVisibility(RelativeLayout.GONE);
                }

                disableButton(ad.pauseResumeButton);
                disableButton(ad.closeButton);
            }
        });

        if (ad.bannerListButton != null) {
            ad.bannerListButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(BannerListActivity.getIntent(MainActivity.this, ad.placementReferenceId));
                }
            });
        }

        if (ad.bannerMultipleButton != null) {
            ad.bannerMultipleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(BannerMultipleActivity.getIntent(MainActivity.this));
                }
            });
        }
    }

    private void setBannerAd(final VungleAd ad, final AdConfig.AdSize adSize) {
        disableButton(ad.pauseResumeButton);
        disableButton(ad.closeButton);
        disableButton(ad.bannerListButton);

        final String p = ad.placementReferenceId;

        final BannerAdConfig adConfig = new BannerAdConfig();
        adConfig.setAdSize(adSize);
        adConfig.setMuted(false);

        // Loading Banner ad works similar to fullscreen ad and only requires the placement and AdSize to be configured properly
        ad.loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Vungle.isInitialized()) {
                    Banners.loadBanner(p, adConfig, vungleLoadAdCallback);
                    // Button UI
                    disableButton(ad.loadButton);
                } else {
                    makeToast("Vungle SDK not initialized");
                }
            }
        });

        ad.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Vungle.isInitialized()) {
                    if (Banners.canPlayAd(p, adConfig.getAdSize())) {
                        if (vungleBannerAd != null) {
                            vungleBannerAd.destroyAd();
                            vungleBannerAd = null;
                            ad.container.removeAllViews();
                        }

                        Log.d(LOG_TAG, "NBP canPlayAd for BANNER" + Banners.canPlayAd(p, AdConfig.AdSize.BANNER));
                        Log.d(LOG_TAG, "NBP canPlayAd for VUNGLE_MREC" + Banners.canPlayAd(p, AdConfig.AdSize.VUNGLE_MREC));
                        Log.d(LOG_TAG, "NBP canPlayAd for BANNER_LEADERBOARD" + Banners.canPlayAd(p, AdConfig.AdSize.BANNER_LEADERBOARD));
                        Log.d(LOG_TAG, "NBP canPlayAd for BANNER_SHORT" + Banners.canPlayAd(p, AdConfig.AdSize.BANNER_SHORT));

//                        adConfig.setAdSize(AdConfig.AdSize.VUNGLE_MREC);
                        vungleBannerAd = Banners.getBanner(p, adConfig, vunglePlayAdCallback);
//                        vungleBannerAd = Banners.getBanner(p, adConfig, vunglePlayAdCallback);

//                        adMarkUp.remove(p);

                        if (vungleBannerAd != null) {
                            ad.container.addView(vungleBannerAd);
                            ad.container.setVisibility(RelativeLayout.VISIBLE);
                        }

                        ad.nativeAdPlaying = true;

                        // Button UI
                        enableButton(ad.loadButton);
                        disableButton(ad.playButton);
                        enableButton(ad.pauseResumeButton);
                        enableButton(ad.closeButton);

                        ad.nativeAdPlaying = true;
                        ad.pauseResumeButton.setText("PAUSE");
                    } else {
                        makeToast("Vungle ad not playable for " + ad.placementReferenceId);
                    }
                } else {
                    makeToast("Vungle SDK not initialized");
                }
            }
        });

        ad.pauseResumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ad.nativeAdPlaying = !ad.nativeAdPlaying;

                if (vungleBannerAd != null) {
                    vungleBannerAd.setAdVisibility(ad.nativeAdPlaying);
                }

                if (ad.nativeAdPlaying) {
                    ad.pauseResumeButton.setText("PAUSE");
                } else {
                    ad.pauseResumeButton.setText("RESUME");
                }
            }
        });

        ad.closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (vungleBannerAd != null) {
                    vungleBannerAd.destroyAd();
                    vungleNativeAd = null;
                    ad.container.removeView(vungleBannerAd);
                    ad.container.setVisibility(RelativeLayout.GONE);
                }

                disableButton(ad.pauseResumeButton);
                disableButton(ad.closeButton);
            }
        });

        if (ad.bannerListButton != null) {
            ad.bannerListButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(BannerListActivity.getIntent(MainActivity.this, ad.placementReferenceId));
                }
            });
        }

        if (ad.bannerMultipleButton != null) {
            ad.bannerMultipleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(BannerMultipleActivity.getIntent(MainActivity.this));
                }
            });
        }
    }

    private AdConfig getAdConfig() {
        AdConfig adConfig = new AdConfig();

//        adConfig.setBackButtonImmediatelyEnabled(true);
        adConfig.setAdOrientation(AdConfig.MATCH_VIDEO);

        adConfig.setMuted(true);

        return adConfig;
    }

    private void setCustomRewardedFields() {
        Vungle.setIncentivizedFields("TestUser", "", "RewardedBody", "RewardedKeepWatching", "RewardedClose");
    }

    private void initUiElements() {
        Log.d(LOG_TAG, "initUiElements");

        TextView text_app_id = (TextView) findViewById(R.id.text_app_id);
        text_app_id.setText("App ID - " + getString(R.string.app_id));

        Log.d(LOG_TAG, "!!!VungleAd Test Begins!!!");

        for (VungleAd vungleAd : vungleAds) {
            setVungleAdUi(vungleAd);
        }
    }

    private VungleAd getVungleAd(String placementReferenceId) {
        for (VungleAd vungleAd : vungleAds) {
            if (vungleAd.placementReferenceId.equals(placementReferenceId)) {
                return vungleAd;
            }
        }
        return null;
    }

    private void getAdMarkUp(String placementId) {
        final String a = getString(R.string.app_id);
        final String p = placementId;

        RequestQueue queue = Volley.newRequestQueue(this);

        String url = "https://rtb.api.vungle.com/bid/t/34dec8a";
//        String url = "https://rtb-ext-qa.api.vungle.com/bid/t/34dec8a";
        String requestBody = "{\"app\":{\"cat\":[\"IAB3\",\"business\"],\"id\":\"ac58e7b8f4614177a53f75681fbc104a\",\"name\":\"iOS Advanced Bidding Test App\",\"publisher\":{\"id\":\"1308c11342c349e8a2934d8bb8fd33f6\",\"name\":\"Twitter\"},\"ver\":\"1.0\"},\"at\":1,\"bcat\":[\"IAB25\",\"IAB26\",\"IAB7-39\",\"IAB8-18\",\"IAB8-5\",\"IAB9-9\"],\"device\":{\"connectiontype\":2,\"dnt\":0,\"h\":1136,\"ifa\":\"4423DD36-2738-46DC-84D1-02A47F95320D1\",\"js\":1,\"language\":\"en\",\"os\":\"ios\",\"osv\":\"13\",\"pxratio\":2,\"ua\":\"Mozilla/5.0 (iPhone; CPU iPhone OS 12_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148\",\"w\":640},\"ext\":{\"pchain\":\"74b46c0ea83967ca:1308c11342c349e8a2934d8bb8fd33f6\"},\"id\":\"82317317-7e72-4927-8a0f-28f4b9c41251\",\"imp\":[{\"banner\":{\"api\":[3,5],\"battr\":[3,8,9,10,14],\"btype\":[4],\"h\":480,\"pos\":1,\"w\":320},\"bidfloor\":0.01,\"displaymanager\":\"mopub\",\"displaymanagerver\":\"4.17.0 bidding\",\"ext\":{\"brsrclk\":1,\"dlp\":1,\"metric\":[{\"type\":\"viewability\",\"vendor\":\"ias\"},{\"type\":\"viewability\",\"vendor\":\"moat\"}],\"networkids\":{\"appid\":\"hbappid\",\"placementid\":\"hbplacementid\"}},\"id\":\"1\",\"instl\":0,\"secure\":0,\"tagid\":\"052068b0ef5a463590a634c0b07039ea\",\"video\":{\"api\":[3,5],\"battr\":[3,8,9,10,14],\"companiontype\":[1,2,3],\"h\":480,\"linearity\":1,\"maxduration\":120,\"mimes\":[\"video/mp4\",\"video/3gpp\"],\"minduration\":0,\"protocols\":[2,5,3,6],\"startdelay\":0,\"w\":320}}],\"regs\":{\"ext\":{\"gdpr\":0}},\"tmax\":3000,\"user\":{\"buyeruid\":\"hbsupertoken\"},\"test\":1}";
        requestBody = requestBody.replace("hbsupertoken", Vungle.getAvailableBidTokens(getApplicationContext()));
//        requestBody = requestBody.replace("hbsupertoken", Vungle.getAvailableBidTokensBySize(getApplicationContext(), 0));
        requestBody = requestBody.replace("hbappid", a);
        requestBody = requestBody.replace("hbplacementid", p);

        final String request = requestBody;

        Log.d("iab", "Request is: "+ request);

        StringRequest postRequest =  new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                VungleAd ad = getVungleAd(p);
                String adm = "";

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject seatbid = jsonObject.getJSONArray("seatbid").getJSONObject(0);
                    JSONObject bid = seatbid.getJSONArray("bid").getJSONObject(0);
                    adm = bid.getString("adm");
                    adMarkUp.put(p, adm);
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }

                if (ad.name.contains("mrec")) {
                    final BannerAdConfig adConfig = new BannerAdConfig();
                    adConfig.setAdSize(AdConfig.AdSize.VUNGLE_MREC);
                    adConfig.setMuted(false);
                    Banners.loadBanner(p, adm, adConfig, vungleLoadAdCallback);
                } else if (ad.name.contains("banner")) {
                    final BannerAdConfig adConfig = new BannerAdConfig();
                    adConfig.setAdSize(AdConfig.AdSize.BANNER);
                    Banners.loadBanner(p, adm, adConfig, vungleLoadAdCallback);
                } else {
                    Vungle.loadAd(p, adm, getAdConfig(), vungleLoadAdCallback);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG_TAG, "That didn't work!");
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                return params;
            }
            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return request == null ? null : request.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    Log.d(LOG_TAG, "getBody did not work");
                    return null;
                }
            }
        };

        queue.add(postRequest);
    }

    private void modifyEndPoint() {
        String url = "https://apiqa.vungle.com/api/v5/";

        try {
            Field field = VungleApiClient.class.getDeclaredField("BASE_URL");
            field.setAccessible(true);
            field.set(null, url);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private void enableButton(final Button button) {
        if (button == null)
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setEnabled(true);
                button.setAlpha(1.0f);
            }
        });
    }

    private void disableButton(final Button button) {
        if (button == null)
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setEnabled(false);
                button.setAlpha(0.5f);
            }
        });
    }

    private void makeToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (vungleNativeAd != null) {
            vungleNativeAd.setAdVisibility(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (vungleNativeAd != null) {
            vungleNativeAd.setAdVisibility(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (vungleNativeAd != null) {
            vungleNativeAd.setAdVisibility(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (vungleNativeAd != null) {
            vungleNativeAd.setAdVisibility(false);
        }
    }
}