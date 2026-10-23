package app.organicmaps.wear.protocol;

/**
 * Map/route state mirrored from the phone to the watch.
 *
 * <p>When a route is assigned ({@link WearMapMode#NAVIGATION}), the payload carries the viewport
 * (center, zoom, bearing), the user's current position, the rendered map snapshot and the route
 * polyline. When no route is assigned ({@link WearMapMode#IDLE}), only the mode is relevant and the
 * watch shows a black screen with instructions to create a route on the phone.
 *
 * <p>No nullability annotations here on purpose: wear-protocol is a dependency-free contract module
 * shared by the phone bridge and the watch app.
 */
public final class WearMapState
{
  private final WearMapMode mMode;
  private final double mCenterLat;
  private final double mCenterLon;
  private final int mZoom;
  private final float mBearing;
  private final boolean mHasMyPosition;
  private final double mMyPositionLat;
  private final double mMyPositionLon;
  private final byte[] mSnapshot;
  private final double[] mRoutePoints;

  // Package-private: construct via the factories below or WearMapStateCodec.decode.
  WearMapState(WearMapMode mode, double centerLat, double centerLon, int zoom, float bearing,
               boolean hasMyPosition, double myPositionLat, double myPositionLon,
               byte[] snapshot, double[] routePoints)
  {
    mMode = mode;
    mCenterLat = centerLat;
    mCenterLon = centerLon;
    mZoom = zoom;
    mBearing = bearing;
    mHasMyPosition = hasMyPosition;
    mMyPositionLat = myPositionLat;
    mMyPositionLon = myPositionLon;
    mSnapshot = snapshot;
    mRoutePoints = routePoints;
  }

  public static WearMapState idle()
  {
    return new WearMapState(WearMapMode.IDLE, 0, 0, 0, 0, false, 0, 0, null, null);
  }

  public static WearMapState navigation(double centerLat, double centerLon, int zoom, float bearing,
                                        boolean hasMyPosition, double myPositionLat, double myPositionLon,
                                        byte[] snapshot, double[] routePoints)
  {
    return new WearMapState(WearMapMode.NAVIGATION, centerLat, centerLon, zoom, bearing,
                            hasMyPosition, myPositionLat, myPositionLon, snapshot, routePoints);
  }

  public WearMapMode getMode()
  {
    return mMode;
  }

  public boolean isNavigating()
  {
    return mMode == WearMapMode.NAVIGATION;
  }

  public double getCenterLat()
  {
    return mCenterLat;
  }

  public double getCenterLon()
  {
    return mCenterLon;
  }

  public int getZoom()
  {
    return mZoom;
  }

  public float getBearing()
  {
    return mBearing;
  }

  public boolean hasMyPosition()
  {
    return mHasMyPosition;
  }

  public double getMyPositionLat()
  {
    return mMyPositionLat;
  }

  public double getMyPositionLon()
  {
    return mMyPositionLon;
  }

  /** PNG-encoded map snapshot, or {@code null} when not available. */
  public byte[] getSnapshot()
  {
    return mSnapshot;
  }

  /**
   * Route polyline as a flat {@code [lat0, lon0, lat1, lon1, ...]} array, or {@code null} when no
   * route is available.
   */
  public double[] getRoutePoints()
  {
    return mRoutePoints;
  }
}
