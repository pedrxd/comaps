package app.organicmaps.wear;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.view.PixelCopy;
import android.view.SurfaceView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.MwmApplication;
import app.organicmaps.sdk.Framework;
import app.organicmaps.sdk.location.LocationHelper;
import app.organicmaps.sdk.location.LocationState;
import app.organicmaps.sdk.routing.JunctionInfo;
import app.organicmaps.sdk.routing.RouteGeometry;
import app.organicmaps.sdk.routing.RoutingController;
import app.organicmaps.sdk.util.log.Logger;
import app.organicmaps.sdk.wear.WearBridge;
import app.organicmaps.sdk.wear.WearSnapshotThrottler;
import app.organicmaps.wear.protocol.WearMapState;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Publishes the current map viewport, route polyline and rendered map snapshot to a paired Wear OS
 * device. The watch is strictly read-only: this class only pushes state, never receives commands.
 */
public final class WearMapSnapshotPublisher implements RoutingController.NavigationStateListener,
                                                      RoutingController.RouteChangedListener
{
  private static final String TAG = WearMapSnapshotPublisher.class.getSimpleName();

  // Snapshots are throttled: a watch does not need more than one update per second.
  private static final long SNAPSHOT_MIN_INTERVAL_MS = 1000;
  // The watch screen is small; a square snapshot keeps the Data Layer payload well under its limit.
  private static final int SNAPSHOT_SIZE_PX = 320;

  @NonNull
  private final SurfaceView mMapView;
  @NonNull
  private final Handler mMainHandler = new Handler(Looper.getMainLooper());
  @NonNull
  private final ExecutorService mEncodeExecutor = Executors.newSingleThreadExecutor();
  @NonNull
  private final WearSnapshotThrottler mThrottler =
      new WearSnapshotThrottler(SNAPSHOT_MIN_INTERVAL_MS, this::captureAndPublish);

  private volatile boolean mActive;

  public WearMapSnapshotPublisher(@NonNull SurfaceView mapView)
  {
    mMapView = mapView;
  }

  public void start()
  {
    if (mActive)
      return;
    mActive = true;
    final RoutingController routing = RoutingController.get();
    routing.addNavigationStateListener(this);
    routing.addRouteChangedListener(this);
    onNavigationStateChanged(routing.isNavigating());
  }

  public void stop()
  {
    if (!mActive)
      return;
    mActive = false;
    final RoutingController routing = RoutingController.get();
    routing.removeNavigationStateListener(this);
    routing.removeRouteChangedListener(this);
    mEncodeExecutor.shutdown();
    WearBridge.publishNavigating(false);
  }

  @Override
  public void onNavigationStateChanged(boolean active)
  {
    if (active)
      mThrottler.request();
    else
      WearBridge.publishNavigating(false);
  }

  @Override
  public void onRouteChanged()
  {
    if (mActive && RoutingController.get().isNavigating())
      mThrottler.request();
  }

  /** Called on location updates from the host activity while navigation is active. */
  public void onLocationUpdated(@NonNull Location location)
  {
    if (mActive && RoutingController.get().isNavigating())
      mThrottler.request();
  }

  private void captureAndPublish()
  {
    if (!mActive || !RoutingController.get().isNavigating())
      return;

    final int width = mMapView.getWidth();
    final int height = mMapView.getHeight();
    if (width <= 0 || height <= 0)
      return;

    final Bitmap snapshot = Bitmap.createBitmap(SNAPSHOT_SIZE_PX, SNAPSHOT_SIZE_PX, Bitmap.Config.ARGB_8888);
    // Scale the copied region down to the snapshot size.
    final Rect src = new Rect(0, 0, width, height);
    final Rect dst = new Rect(0, 0, SNAPSHOT_SIZE_PX, SNAPSHOT_SIZE_PX);
    PixelCopy.request(mMapView, src, dst, snapshot, copyResult -> {
      if (copyResult != PixelCopy.SUCCESS)
      {
        Logger.w(TAG, "PixelCopy failed: " + copyResult);
        snapshot.recycle();
        return;
      }
      mEncodeExecutor.execute(() -> encodeAndPublish(snapshot));
    }, mMainHandler);
  }

  private void encodeAndPublish(@NonNull Bitmap snapshot)
  {
    try
    {
      final ByteArrayOutputStream stream = new ByteArrayOutputStream();
      final boolean compressed = snapshot.compress(Bitmap.CompressFormat.PNG, 100, stream);
      snapshot.recycle();
      if (!compressed)
        return;

      WearBridge.publishNavigation(buildState(stream.toByteArray()));
    }
    catch (Throwable t)
    {
      Logger.w(TAG, "Failed to encode Wear snapshot", t);
    }
  }

  @NonNull
  private WearMapState buildState(@Nullable byte[] snapshot)
  {
    final double[] center = Framework.nativeGetScreenRectCenter();
    final int zoom = Framework.nativeGetDrawScale();
    final float bearing = currentBearing();

    final LocationHelper locationHelper = MwmApplication.from(mMapView.getContext()).getLocationHelper();
    final Location location = locationHelper.getSavedLocation();
    final boolean hasPosition = location != null;

    final RouteGeometry geometry = RoutingController.get().getRouteGeometry();
    final double[] route = toFlatArray(geometry);

    return WearMapState.navigation(center[0], center[1], zoom, bearing,
                                   hasPosition,
                                   hasPosition ? location.getLatitude() : 0,
                                   hasPosition ? location.getLongitude() : 0,
                                   snapshot, route);
  }

  private float currentBearing()
  {
    try
    {
      if (LocationState.getMode() == LocationState.FOLLOW_AND_ROTATE)
      {
        final Location location = MwmApplication.from(mMapView.getContext()).getLocationHelper().getSavedLocation();
        if (location != null && location.hasBearing())
          return location.getBearing();
      }
    }
    catch (IllegalStateException e)
    {
      // Location mode is undefined until the engine is created.
    }
    return 0f;
  }

  @Nullable
  private static double[] toFlatArray(@NonNull RouteGeometry geometry)
  {
    final JunctionInfo[] junctions = geometry.mJunctions;
    if (junctions == null || junctions.length == 0)
      return null;

    final double[] flat = new double[junctions.length * 2];
    for (int i = 0; i < junctions.length; i++)
    {
      flat[i * 2] = junctions[i].mLat;
      flat[i * 2 + 1] = junctions[i].mLon;
    }
    return flat;
  }
}
