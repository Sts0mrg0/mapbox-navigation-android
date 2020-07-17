package com.mapbox.navigation.examples.core;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.*;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.navigation.base.internal.extensions.MapboxRouteOptionsUtils;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback;
import com.mapbox.navigation.core.replay.MapboxReplayer;
import com.mapbox.navigation.core.replay.ReplayLocationEngine;
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver;
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent;
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.core.trip.session.TripSessionState;
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver;
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver;
import com.mapbox.navigation.examples.R;
import com.mapbox.navigation.examples.utils.Utils;
import com.mapbox.navigation.examples.utils.extensions.LocationPointLatLngUtils;
import com.mapbox.navigation.ui.NavigationButton;
import com.mapbox.navigation.ui.NavigationConstants;
import com.mapbox.navigation.ui.SoundButton;
import com.mapbox.navigation.ui.camera.NavigationCamera;
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheet;
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheetListener;
import com.mapbox.navigation.ui.feedback.FeedbackItem;
import com.mapbox.navigation.ui.instruction.InstructionView;
import com.mapbox.navigation.ui.internal.utils.BitmapEncodeOptions;
import com.mapbox.navigation.ui.internal.utils.ViewUtils;
import com.mapbox.navigation.ui.map.NavigationMapboxMap;
import com.mapbox.navigation.ui.voice.NavigationSpeechPlayer;
import com.mapbox.navigation.ui.voice.SpeechPlayerProvider;
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader;

import com.mapbox.navigator.BannerComponent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Cache;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;

@SuppressLint("MissingPermission")
public class InstructionViewActivityJava extends AppCompatActivity
    implements OnMapReadyCallback, FeedbackBottomSheetListener {

  private static final String VOICE_INSTRUCTION_CACHE = "voice-instruction-cache";
  private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;

  private MapboxNavigation mapboxNavigation;
  private NavigationMapboxMap navigationMapboxMap;
  private NavigationSpeechPlayer speechPlayer;
  private MapboxReplayer mapboxReplayer = new MapboxReplayer();

  private MapboxMap mapboxMap;
  private NavigationButton feedbackButton;
  private NavigationButton soundButton;
  private DirectionsRoute directionsRoute;

  private FeedbackItem feedbackItem;
  private String feedbackEncodedScreenShot;

  @BindView(R.id.mapView)
  MapView mapView;

  @BindView((R.id.startNavigation))
  Button startNavigation;

  @BindView(R.id.instructionView)
  InstructionView instructionView;

  @BindView(R.id.screenshotView)
  ImageView screenshotView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_instruction_view_layout);
    ButterKnife.bind(this);

    initViews();

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);

    NavigationOptions navigationOptions = MapboxNavigation
        .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
        .locationEngine(getLocationEngine())
        .build();

    mapboxNavigation = new MapboxNavigation(navigationOptions);
    mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver);
    mapboxNavigation.registerRouteProgressObserver(routeProgressObserver);
    mapboxNavigation.registerBannerInstructionsObserver(bannerInstructionsObserver);
    mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver);

    initListeners();
    initializeSpeechPlayer();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    stopLocationUpdates();
    mapView.onStop();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapboxReplayer.finish();
    if (mapboxNavigation != null) {
      mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver);
      mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver);
      mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionsObserver);
      mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver);
      mapboxNavigation.stopTripSession();
      mapboxNavigation.onDestroy();
    }

    speechPlayer.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);

    // This is not the most efficient way to preserve the route on a device rotation.
    // This is here to demonstrate that this event needs to be handled in order to
    // redraw the route line after a rotation.
    if (directionsRoute != null) {
      outState.putString(Utils.PRIMARY_ROUTE_BUNDLE_KEY, directionsRoute.toJson());
    }
  }

  @Override
  protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    directionsRoute = Utils.getRouteFromBundle(savedInstanceState);
  }

  @Override
  public void onMapReady(@NonNull MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;

    mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
      mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0));
      navigationMapboxMap = new NavigationMapboxMap(mapView, mapboxMap, this, true);

      if (directionsRoute != null) {
        restoreNavigation();
      } else if (mapboxNavigation != null) {
        if (shouldSimulateRoute()) {
          mapboxNavigation.registerRouteProgressObserver(
              new ReplayProgressObserver(mapboxReplayer)
          );
          mapboxReplayer.pushRealLocation(this, 0.0);
          mapboxReplayer.play();
        }

        mapboxNavigation.getNavigationOptions().getLocationEngine().getLastLocation(
            locationListenerCallback
        );

        Snackbar.make(
            mapView,
            R.string.msg_long_press_map_to_place_waypoint,
            LENGTH_SHORT)
            .show();
      }
    });

    mapboxMap.addOnMapLongClickListener(latLng -> {
      if (mapboxNavigation != null && mapboxMap.getLocationComponent().getLastKnownLocation() != null) {
        mapboxNavigation.requestRoutes(MapboxRouteOptionsUtils.coordinates(
            MapboxRouteOptionsUtils.applyDefaultParams(RouteOptions.builder()),
            LocationPointLatLngUtils.toPoint(mapboxMap.getLocationComponent().getLastKnownLocation()),
            null,
            LocationPointLatLngUtils.toPoint(latLng))
            .accessToken(Objects.requireNonNull(Utils.getMapboxAccessToken(getApplicationContext())))
            .alternatives(true)
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .build(), routesReqCallback);
      }
      return true;
    });
  }

  @Override
  public void onFeedbackDismissed() {
    // do nothing
  }

  @Override
  public void onFeedbackSelected(FeedbackItem feedbackItem) {
    if (feedbackItem != null) {
      this.feedbackItem = feedbackItem;
      sendFeedback();
    }
  }

  private void encodeSnapshot(Bitmap snapshot) {
    screenshotView.setVisibility(VISIBLE);
    screenshotView.setImageBitmap(snapshot);
    mapView.setVisibility(INVISIBLE);
    feedbackEncodedScreenShot = ViewUtils.encodeView(
        ViewUtils.captureView(mapView),
        new BitmapEncodeOptions.Builder().width(400).compressQuality(40).build()
    );
    screenshotView.setVisibility(INVISIBLE);
    mapView.setVisibility(VISIBLE);

    sendFeedback();
  }

  private void sendFeedback() {
    if (feedbackItem == null || TextUtils.isEmpty(feedbackEncodedScreenShot)) {
      return;
    }

    MapboxNavigation.postUserFeedback(feedbackItem.getFeedbackType(),
        feedbackItem.getDescription(),
        FeedbackEvent.UI,
        feedbackEncodedScreenShot,
        feedbackItem.getFeedbackSubType().toArray(new String[0]));
    FeedbackButtonActivityKt.showFeedbackSentSnackBar(this, mapView, R.string.feedback_reported, LENGTH_SHORT, false);
  }

  private void initViews() {
    startNavigation.setVisibility(VISIBLE);
    startNavigation.setEnabled(false);
    instructionView.setVisibility(GONE);
    feedbackButton = instructionView.retrieveFeedbackButton();
    feedbackButton.hide();
    feedbackButton.addOnClickListener(view -> {
      feedbackItem = null;
      feedbackEncodedScreenShot = null;
      if (mapboxMap != null) {
        mapboxMap.snapshot(this::encodeSnapshot);
      }
      FeedbackBottomSheet.newInstance(
          this,
          NavigationConstants.FEEDBACK_BOTTOM_SHEET_DURATION)
          .show(getSupportFragmentManager(), FeedbackBottomSheet.TAG);
    });

    soundButton = instructionView.retrieveSoundButton();
    soundButton.hide();
    soundButton.addOnClickListener(view -> speechPlayer.setMuted(((SoundButton) soundButton).toggleMute()));
  }

  private void initListeners() {
    startNavigation.setOnClickListener(view -> {
      updateCameraOnNavigationStateChange(true);
      if (navigationMapboxMap != null && mapboxNavigation != null) {
        navigationMapboxMap.addProgressChangeListener(mapboxNavigation);
        if (!mapboxNavigation.getRoutes().isEmpty()) {
          navigationMapboxMap.startCamera(mapboxNavigation.getRoutes().get(0));
        }
        mapboxNavigation.startTripSession();
      }
    });
  }

  private void initializeSpeechPlayer() {
    Cache cache = new Cache(
        new File(this.getApplication().getCacheDir(), VOICE_INSTRUCTION_CACHE),
        10 * 1024 * 1024);
    VoiceInstructionLoader voiceInstructionLoader = new VoiceInstructionLoader(
        this.getApplication(), Mapbox.getAccessToken(), cache);
    SpeechPlayerProvider speechPlayerProvider = new SpeechPlayerProvider(
        this.getApplication(),
        Locale.US.getLanguage(),
        true,
        voiceInstructionLoader);

    speechPlayer = new NavigationSpeechPlayer(speechPlayerProvider);
  }

  private boolean shouldSimulateRoute() {
    return PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext())
        .getBoolean(getString(R.string.simulate_route_key), false);
  }

  private LocationEngine getLocationEngine() {
    if (shouldSimulateRoute()) {
      return new ReplayLocationEngine(mapboxReplayer);
    } else {
      return LocationEngineProvider.getBestLocationEngine(this);
    }
  }

  private void startLocationUpdates() {
    if (!shouldSimulateRoute() && mapboxNavigation != null) {
      LocationEngineRequest locationEngineRequest =
          new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
              .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
              .setMaxWaitTime(BasicNavigationActivity.DEFAULT_MAX_WAIT_TIME)
              .build();

      mapboxNavigation.getNavigationOptions().getLocationEngine().requestLocationUpdates(
          locationEngineRequest,
          locationListenerCallback,
          getMainLooper()
      );
    }
  }

  private void stopLocationUpdates() {
    if (!shouldSimulateRoute() && mapboxNavigation != null) {
      mapboxNavigation.getNavigationOptions().getLocationEngine().removeLocationUpdates(locationListenerCallback);
    }
  }

  private void updateViews(TripSessionState tripSessionState) {
    if (tripSessionState == TripSessionState.STARTED) {
      startNavigation.setVisibility(GONE);
      instructionView.setVisibility(VISIBLE);
      feedbackButton.show();
      soundButton.show();
    } else {
      startNavigation.setVisibility(VISIBLE);
      startNavigation.setEnabled(false);
      instructionView.setVisibility(GONE);
      feedbackButton.hide();
      soundButton.hide();
    }
  }

  private void updateCameraOnNavigationStateChange(boolean navigationStarted) {
    if (navigationMapboxMap == null) {
      return;
    }

    if (navigationStarted) {
      navigationMapboxMap.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
      navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.GPS);
    } else {
      navigationMapboxMap.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);
      navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.COMPASS);
    }
  }

  private void restoreNavigation() {
    if (directionsRoute != null && mapboxNavigation != null && navigationMapboxMap != null) {
      List<DirectionsRoute> routes = new ArrayList<>();
      routes.add(directionsRoute);
      mapboxNavigation.setRoutes(routes);
      navigationMapboxMap.addProgressChangeListener(mapboxNavigation);
      navigationMapboxMap.startCamera(mapboxNavigation.getRoutes().get(0));
      updateCameraOnNavigationStateChange(true);
      mapboxNavigation.startTripSession();
    }
  }

  private RoutesRequestCallback routesReqCallback = new RoutesRequestCallback() {
    @Override
    public void onRoutesReady(@NotNull List<? extends DirectionsRoute> routes) {
      if (routes.size() > 0 && navigationMapboxMap != null) {
        directionsRoute = routes.get(0);
        navigationMapboxMap.drawRoute(directionsRoute);
        startNavigation.setVisibility(VISIBLE);
        startNavigation.setEnabled(true);
      } else {
        startNavigation.setEnabled(false);
      }
    }

    @Override
    public void onRoutesRequestFailure(@NotNull Throwable throwable, @NotNull RouteOptions routeOptions) {

    }

    @Override
    public void onRoutesRequestCanceled(@NotNull RouteOptions routeOptions) {

    }
  };

  private MyLocationEngineCallback locationListenerCallback = new MyLocationEngineCallback(this);

  private TripSessionStateObserver tripSessionStateObserver = tripSessionState -> {
    updateViews(tripSessionState);
    if (tripSessionState == TripSessionState.STARTED) {
      stopLocationUpdates();
    } else {
      startLocationUpdates();
      if (navigationMapboxMap != null) {
        navigationMapboxMap.removeRoute();
      }
      updateCameraOnNavigationStateChange(false);
    }
  };

  private RouteProgressObserver routeProgressObserver =
      routeProgress -> instructionView.updateDistanceWith(routeProgress);

  private BannerInstructionsObserver bannerInstructionsObserver = bannerInstructions -> {
    //===THIS IS CALLED APPROX. EVERY SECOND
    Timber.i("$$$ MAPBOX : ENTER $$$$$$$$$$");

    try {
      if (bannerInstructions != null) {
        //=== THIS DOES NOT APPEAR TO WORK, TKV 26MAY20
        BannerInstructions newBannerInstructions = cleanupBannerInstructions(bannerInstructions);
        Timber.i("$$$ MAPBOX : updating BannerInstructions with = \n" + newBannerInstructions.toJson());
        ///// fixed in 1.1.0-beta.6 ???
        instructionView.updateBannerInstructionsWith(newBannerInstructions);
      } else {
        Timber.i("$$$ MAPBOX : bannerInstructions is NULL");
      }
    } catch (Exception e) {
      Timber.i("$$$ MAPBOX : EXCEPTION = " + e.getMessage());
    }

    Timber.i("$$$ MAPBOX : EXIT $$$$$$$$$$");
    //instructionView.updateBannerInstructionsWith(bannerInstructions);
  };

  private VoiceInstructionsObserver voiceInstructionsObserver =
      voiceInstructions -> {
        VoiceInstructions newAnnouncement = null;//voiceInstructions;
        String theAnnouncement = voiceInstructions.announcement();
        Timber.i("$$$ MAPBOX : ENTER $$$$$$$$$$$$$$$$$$$$$$$$$$$$ " +
            theAnnouncement);

        //=== CHECK FOR SPECIAL CHARACTER DENOTING CUSTOMER SERVICES PRESENT
        //if(theAnnouncement.contains("\u00A0")){
        //  Timber.i("$$$ MAPBOX : voiceInstructions has SPLIT CHAR");

          //=== REMOVE SOME OF MAPBOX VERBAGE ***** DOES NOT SEEM TO WORK, 12FEB20
          VoiceInstructions.Builder announcementBuilder = VoiceInstructions.builder();

          theAnnouncement = theAnnouncement.replace("\u00A0", " ")
              .replace("\u002c", " ")
              .replace("then you have arrived at", "to")
              .replace("You have arrived at", "Arrival")
              .replace("left", "go go left")
              .replace("right", "go go right")
              .replace("turn", "turn turn turn");

          //theAnnouncement = theAnnouncement.replace("\u00A0", " ");
          //theAnnouncement = theAnnouncement.replace("\u002c", " ");
          //theAnnouncement = theAnnouncement.replace("then you have arrived at", "to");
          ////				theAnnouncement = theAnnouncement.replace(", then you have arrived at", " to");
          //theAnnouncement = theAnnouncement.replace("You have arrived at", "Arrival");
          ////Head northwest on Station Road, then you have arrived at 8016 Station Rd delivery   front porch
          ////You have arrived at 8036 Station Rd must deliver

          newAnnouncement = announcementBuilder.announcement(theAnnouncement)
              //.distanceAlongGeometry(voiceInstructions.distanceAlongGeometry())
              //adding ssmlAnnouncement seems to have the voice modification working 30jun20
              //.ssmlAnnouncement(theAnnouncement)
              .build();

          String newAnnouncementText = newAnnouncement.announcement();
          Timber.i("$$$ MAPBOX : $$$$$$$$$$$$$$$$$$$$$$$$$$$$ new voiceInstructions = " +
              newAnnouncementText);

          ////=== SOUND ALERT BELL IF CUSTOMER INSTRUCTIONS ARE PRESENT
          //try{
          //  soundPool.play(sound1, 1, 1, 0, 0, 1);
          //}
          //catch(Exception e){
          //  Timber.i("$$$ MAPBOX : EXCEPTION : alert bell $$$$$$$$$$$$$$$$$$$$$$$$$$$$");
          //}
        //}

        speechPlayer.play(newAnnouncement);
        Timber.i("$$$ MAPBOX : EXIT $$$$$$$$$$$$$$$$$$$$$$$$$$$$ ");
        //speechPlayer.play(voiceInstructions);
      };

  private static class MyLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {
    private WeakReference<InstructionViewActivityJava> activityWeakReference;

    MyLocationEngineCallback(InstructionViewActivityJava activity) {
      activityWeakReference = new WeakReference<>(activity);
    }

    @Override
    public void onSuccess(LocationEngineResult result) {
      if (activityWeakReference != null && activityWeakReference.get() != null) {
        InstructionViewActivityJava activity = activityWeakReference.get();
        if (activity.navigationMapboxMap != null) {
          activity.navigationMapboxMap.updateLocation(result.getLastLocation());
        }
      }
    }

    @Override
    public void onFailure(@NonNull Exception exception) {

    }
  }

  public BannerInstructions cleanupBannerInstructions(@NotNull BannerInstructions bannerInstructions) {
    List<BannerComponents> components = bannerInstructions.primary().components();
    List<BannerComponents> newComponents = new ArrayList<>();
    for (BannerComponents component : components) {
      newComponents.add(component.toBuilder().text(component.text() + "Test; ").build());
    }

    BannerText primaryBannerText =
        bannerInstructions.primary().toBuilder().components(newComponents).build();
    return bannerInstructions.toBuilder().primary(primaryBannerText).build();

    /**
     Timber.i("$$$ MAPBOX : ENTER $$$$$$$$$$");
     BannerInstructions newBannerInstructions = bannerInstructions.toBuilder().build();

     try {
     if (bannerInstructions != null) {
     Timber.i("$$$ MAPBOX : $$$$$$$$$$ INCOMING $$$$$$$$$$$$$$$$" +
     "\nPRIMARY " + ((newBannerInstructions == null || newBannerInstructions.primary() == null) ? " is NULL"
     : newBannerInstructions.primary().text()) +
     "\nSECONDARY" + ((newBannerInstructions == null || newBannerInstructions.secondary() == null) ? " is NULL"
     : newBannerInstructions.secondary().text()) +
     "\nSUB" + ((newBannerInstructions == null || newBannerInstructions.sub() == null) ? " is NULL"
     : newBannerInstructions.sub().text()));

     if (newBannerInstructions.primary() == null) {
     Timber.i("$$$ MAPBOX : newBannerInstructions.primary() is NULL ");
     } else {
     //===HANDLING DELIVERY INSTRUCTIONS IN A DIFFERENT WINDOW
     //===SO EXTRACT IT HERE AND PASS IT ON.
     if (!newBannerInstructions.primary().text().contains("\u00A0")) {
     Timber.i("$$$ MAPBOX : newBannerInstructions.primary().text() DOES NOT CONTAIN split char");
     } else {
     String[] splits = newBannerInstructions.primary().text().split("\u00A0");
     //Timber.i("$$$ MAPBOX : newBannerInstructions primary splits count is " + splits.length);
     //for(int i = 0; i < splits.length; i++){
     //	Timber.i("$$$ MAPBOX : newBannerInstructions primary split[" + i + "] = " + splits[i]);
     //}

     if (splits.length <= 3) {
     Timber.i("$$$ MAPBOX : newBannerInstructions primary splits count is TOO LOW");
     } else {
     List bannerComponents = newBannerInstructions.primary().components();
     if (bannerComponents == null || bannerComponents.size() < 1) {
     Timber.i("$$$ MAPBOX : primary bannerComponents.size() is TOO LOW");
     } else {
     String addressText = splits[0] == null ? "" : splits[0];
     //							String jobTypeText = splits[1] == null ? "" : splits[1];
     //							String productsText = splits[2] == null ? "" : splits[2].equals("\u002c") ? "" : splits[2];
     //							String instructionsText = splits[3] == null ? "" : splits[3].equals("\u002c") ? "" : splits[3];
     //
     //							String deliveryText = jobTypeText + " at " + addressText + " " + (productsText.isEmpty() ? "" : productsText) + (instructionsText.isEmpty() ? "\n" : " " + instructionsText + "\n");
     String newBannerText = addressText;

     BannerText primaryBannerText = newBannerInstructions.primary();
     Timber.i("$$$ MAPBOX : primaryBannerText = " + (primaryBannerText == null ? "NULL"
     : primaryBannerText.text()));
     BannerText.Builder primaryBannerTextBuilder = primaryBannerText.toBuilder();
     //=== FORCE TO DRIVING SIDE RIGHT AS THIS IS NOT IN bannerInstructions, 25 MAY 20
     Timber.i("$$$ MAPBOX : primaryBannerTextBuilder = " + (primaryBannerTextBuilder == null ? "NULL"
     : "NOT NULL"));
     primaryBannerTextBuilder.text(newBannerText);
     //Timber.i("$$$ MAPBOX : FORCE TO DRIVING SIDE RIGHT AS THIS IS NOT IN bannerInstructions");
     //primaryBannerTextBuilder.drivingSide("right");

     BannerText newPrimaryBannerText = primaryBannerTextBuilder.build();

     //===PRIMARY TEXT IS NEVER ALTERED/CHANGED, TKV, 3MAR20
     BannerInstructions.Builder newBannerInstructionsBuilder = newBannerInstructions.toBuilder();
     newBannerInstructions = newBannerInstructionsBuilder.primary(newPrimaryBannerText).build();

     Timber.i("$$$ MAPBOX : $$$$$$$$$ OUTGOING $$$$$$$$$$$$$$$$$" +
     "\nPRIMARY " + ((newBannerInstructions == null || newBannerInstructions.primary() == null)
     ? " is NULL" : newBannerInstructions.primary().text()) +
     "\nSECONDARY" + ((newBannerInstructions == null || newBannerInstructions.secondary() == null)
     ? " is NULL" : newBannerInstructions.secondary().text()) +
     "\nSUB" + ((newBannerInstructions == null || newBannerInstructions.sub() == null) ? " is NULL"
     : newBannerInstructions.sub().text()));

     //							Timber.i("$$$ MAPBOX : new deliveryText = \n" + (deliveryText == null ? "NULL" : deliveryText));
     //							MyMapboxNavigationActivity.this.handleDeliveryInstructions(deliveryText);

     //=== THIS DOES NOT APPEAR TO WORK, TKV 26MAY20
     Timber.i("$$$ MAPBOX : newBannerInstructions json = \n" + newBannerInstructions.toJson());
     //							myBannerInstructionView.updateBannerInstructionsWith(newBannerInstructions);
     }
     }
     }
     }
     } else {
     Timber.i("$$$ MAPBOX : newBannerInstructions is NULL");
     }
     } catch (Exception e) {
     Timber.i("$$$ MAPBOX : EXCEPTION = " + e.getMessage());
     }

     Timber.i("$$$ MAPBOX : EXIT $$$$$$$$$$");
     return newBannerInstructions;
     **/
  }
}