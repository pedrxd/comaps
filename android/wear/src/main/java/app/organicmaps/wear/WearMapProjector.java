package app.organicmaps.wear;

/**
 * Projects lat/lon coordinates onto the watch screen using the viewport parameters received from
 * the phone (Web Mercator, screen center, zoom, bearing).
 *
 * <p>The phone renders with 256px tiles, so at zoom {@code z} one screen pixel covers
 * {@code 360 / (256 * 2^z)} degrees in Mercator X. Y uses the standard Mercator transform.
 */
final class WearMapProjector
{
  private static final double TILE_SIZE = 256.0;
  private static final double DEGREES_PER_RADIANS = 180.0 / Math.PI;

  private final int mScreenWidth;
  private final int mScreenHeight;
  private final double mCenterX;
  private final double mCenterY;
  private final double mDegreesPerPixel;
  private final double mBearingRad;

  WearMapProjector(int screenWidth, int screenHeight, double centerLat, double centerLon, int zoom,
                   float bearingDegrees)
  {
    mScreenWidth = screenWidth;
    mScreenHeight = screenHeight;
    mCenterX = lonToX(centerLon);
    mCenterY = latToY(centerLat);
    mDegreesPerPixel = 360.0 / (TILE_SIZE * Math.pow(2.0, zoom));
    mBearingRad = Math.toRadians(bearingDegrees);
  }

  /** Projects a lat/lon point to screen pixels, writing {@code [x, y]} into {@code out}. */
  void project(double lat, double lon, float[] out)
  {
    final double gx = (lonToX(lon) - mCenterX) / mDegreesPerPixel;
    final double gy = (latToY(lat) - mCenterY) / mDegreesPerPixel;

    // Rotate by -bearing so that the map's "up" direction points to the bearing.
    final double cos = Math.cos(-mBearingRad);
    final double sin = Math.sin(-mBearingRad);
    final double rx = gx * cos - gy * sin;
    final double ry = gx * sin + gy * cos;

    out[0] = (float) (mScreenWidth / 2.0 + rx);
    out[1] = (float) (mScreenHeight / 2.0 - ry);
  }

  private static double lonToX(double lon)
  {
    return lon;
  }

  private static double latToY(double lat)
  {
    final double clamped = Math.max(-86.0, Math.min(86.0, lat));
    final double sin = Math.sin(clamped / DEGREES_PER_RADIANS);
    return Math.log((1.0 + sin) / (1.0 - sin)) / 2.0 * DEGREES_PER_RADIANS;
  }

  private WearMapProjector() {}
}
