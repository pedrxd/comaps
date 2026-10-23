package app.organicmaps.wear.protocol;

/**
 * Wire contract shared by the phone bridge and the watch app: Data Layer paths and payload keys,
 * plus a protocol version so the two independently-updatable APKs can detect a mismatch.
 */
public final class WearMapData
{
  public static final String CAPABILITY_PHONE_APP = "comaps_phone_app";
  public static final String PATH_MAP_STATE = "/comaps/map/state";
  public static final int VERSION = 1;

  public static final String KEY_VERSION = "version";
  public static final String KEY_MODE = "mode";
  public static final String KEY_CENTER_LAT = "center_lat";
  public static final String KEY_CENTER_LON = "center_lon";
  public static final String KEY_ZOOM = "zoom";
  public static final String KEY_BEARING = "bearing";
  public static final String KEY_MY_POSITION_LAT = "my_position_lat";
  public static final String KEY_MY_POSITION_LON = "my_position_lon";
  public static final String KEY_SNAPSHOT = "snapshot";
  public static final String KEY_POINTS_COUNT = "points_count";
  public static final String KEY_POINTS = "points";

  private WearMapData() {}
}
