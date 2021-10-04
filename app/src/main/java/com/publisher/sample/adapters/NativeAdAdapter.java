package com.publisher.sample.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.publisher.sample.R;
import com.vungle.warren.AdConfig;
import com.vungle.warren.Banners;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.NativeAd;
import com.vungle.warren.NativeAdLayout;
import com.vungle.warren.NativeAdListener;
import com.vungle.warren.NativeAdOptionsView;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.error.VungleException;
import com.vungle.warren.ui.view.MediaView;

import java.util.ArrayList;
import java.util.List;

public class NativeAdAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String LOG = "VungleNativeFeed";

    private static final int NATIVE_AD_UNIT = R.layout.native_ad_unit;
    private final String placementId;
    private final int adPosition;
    private final RecyclerView.Adapter originalAdapter;
    private NativeAd vungleNativeAd;
    private boolean destroyed;

    public NativeAdAdapter(@NonNull String placementId,
                           int adPosition,
                           @NonNull RecyclerView.Adapter originalAdapter) {
        this.placementId = placementId;
        this.adPosition = adPosition;
        this.originalAdapter = originalAdapter;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
        if (type == NATIVE_AD_UNIT) {
            NativeAdLayout inflatedView = (NativeAdLayout) LayoutInflater
                    .from(viewGroup.getContext())
                    .inflate(R.layout.native_ad_unit, viewGroup, false);
            return new NativeAdHolder(inflatedView);
        }
        return originalAdapter.onCreateViewHolder(viewGroup, type);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == NATIVE_AD_UNIT) {
//            NativeAd vungleNativeAd;
            NativeAdHolder adHolder = (NativeAdHolder) holder;
            adHolder.adChoicesContainer.removeAllViews();

            vungleNativeAd = new NativeAd(adHolder.itemView.getContext(), placementId);

            vungleNativeAd.loadAd(new AdConfig(), new NativeAdListener() {
                @Override
                public void onNativeAdLoaded(NativeAd nativeAd) {
                    NativeAdLayout nativeAdLayout = (NativeAdLayout) adHolder.nativeAdLayout;

                    if (adHolder.adChoicesContainer != null) {
                        adHolder.adOptionsView = new NativeAdOptionsView(adHolder.itemView.getContext(), vungleNativeAd, nativeAdLayout);
                        adHolder.adChoicesContainer.removeAllViews();
                        adHolder.adChoicesContainer.addView(adHolder.adOptionsView, 0);
                    }

                    adHolder.titleView.setText(nativeAd.getAdTitle());
                    adHolder.bodyView.setText(nativeAd.getAdBodyText());
                    adHolder.rateView.setText(Double.toString(nativeAd.getAdStarRating()));
                    adHolder.sponsoredView.setText(nativeAd.getAdSponsoredText());
                    adHolder.ctaButton.setText(nativeAd.getAdCallToActionText());
                    adHolder.ctaButton.setVisibility(nativeAd.hasCallToAction() ? View.VISIBLE : View.INVISIBLE);

                    List<View> clickableViews = new ArrayList<>();
                    clickableViews.add(adHolder.iconView);
                    clickableViews.add(adHolder.adContentView);
                    clickableViews.add(adHolder.ctaButton);
                    nativeAd.registerViewForInteraction(
                            nativeAdLayout, adHolder.adContentView, adHolder.iconView, clickableViews);
                }

                @Override
                public void onAdLoadError(String s, VungleException e) {
                    Log.d(LOG, String.format("onAdLoadError - %s\n%s", s, e.getLocalizedMessage()));
                }

                @Override
                public void onAdPlayError(String s, VungleException e) {
                    Log.d(LOG, String.format("onAdLoadError - %\n%s", s, e.getLocalizedMessage()));
                }

                @Override
                public void onAdStart(String s) {
                    Log.d(LOG, "onAdStart - " + s);
                }

                @Override
                public void onAdViewed(String s) {
                    Log.d(LOG, "onAdViewed - " + s);
                }

                @Override
                public void onAdClick(String s) {
                    Log.d(LOG, "onAdClick - " + s);
                }

                @Override
                public void onAdEnd(String s) {
                    Log.d(LOG, "onAdEnd - " + s);
                }

                @Override
                public void onAdLeftApplication(String s) {
                    Log.d(LOG, "onAdLeftApplication - " + s);
                }
            });
        } else {
            originalAdapter.onBindViewHolder(holder, position < adPosition ? position : position - 1);
        }
    }

    @Override
    public int getItemCount() {
        return 1 + originalAdapter.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == adPosition) {
            return NATIVE_AD_UNIT;
        } else {
            return position < adPosition ?
                    originalAdapter.getItemViewType(position) :
                    originalAdapter.getItemViewType(position + 1);
        }
    }

    //must be called
    public void destroy() {
        destroyed = true;
        if (vungleNativeAd != null) {
            vungleNativeAd.destroy();
        }
    }

    private boolean canStart() {
        return vungleNativeAd == null && !destroyed;
    }

    private static class NativeAdHolder extends RecyclerView.ViewHolder {
        NativeAdLayout nativeAdLayout;
        NativeAdOptionsView adOptionsView;
        TextView titleView;
        TextView bodyView;
        TextView sponsoredView;
        TextView rateView;
        MediaView adContentView;
        ImageView iconView;
        Button ctaButton;
        ViewGroup adChoicesContainer;

        NativeAdHolder(NativeAdLayout layout) {
            super(layout);
            nativeAdLayout = layout;
            titleView = itemView.findViewById(R.id.native_ad_title);
            bodyView = itemView.findViewById(R.id.native_ad_body);
            sponsoredView = itemView.findViewById(R.id.native_ad_sponsored);
            rateView = itemView.findViewById(R.id.native_ad_rate);
            adContentView = itemView.findViewById(R.id.native_ad_media);
            iconView = itemView.findViewById(R.id.native_ad_icon);
            ctaButton = itemView.findViewById(R.id.native_ad_call_to_action);
            adChoicesContainer  = itemView.findViewById(R.id.ad_choices_container);
        }
    }
}
