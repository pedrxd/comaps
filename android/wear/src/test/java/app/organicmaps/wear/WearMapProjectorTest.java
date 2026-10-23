package app.organicmaps.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WearMapProjectorTest
{
  private static final float EPS = 0.001f;

  @Test
  public void centerProjectsToScreenCenter()
  {
    final WearMapProjector projector = new WearMapProjector(320, 320, 55.75, 37.61, 15, 0f);
    final float[] out = new float[2];

    projector.project(55.75, 37.61, out);

    assertEquals(160f, out[0], EPS);
    assertEquals(160f, out[1], EPS);
  }

  @Test
  public void eastPointMovesRight()
  {
    final WearMapProjector projector = new WearMapProjector(320, 320, 0, 0, 10, 0f);
    final float[] center = new float[2];
    final float[] east = new float[2];

    projector.project(0, 0, center);
    projector.project(0, 0.01, east);

    assertEquals(center[0] + 0.01 / (360.0 / (256.0 * 1024.0)), east[0], 0.5f);
    assertEquals(center[1], east[1], EPS);
  }

  @Test
  public void northPointMovesUp()
  {
    final WearMapProjector projector = new WearMapProjector(320, 320, 0, 0, 10, 0f);
    final float[] center = new float[2];
    final float[] north = new float[2];

    projector.project(0, 0, center);
    projector.project(0.01, 0, north);

    assertEquals(center[0], north[0], EPS);
    // Y grows downwards on screen: a northern point must move up (smaller Y).
    assertTrue(north[1] < center[1]);
  }

  @Test
  public void bearingRotatesProjection()
  {
    final WearMapProjector noRotation = new WearMapProjector(320, 320, 0, 0, 10, 0f);
    final WearMapProjector rotated = new WearMapProjector(320, 320, 0, 0, 10, 90f);
    final float[] unrotated = new float[2];
    final float[] east = new float[2];

    noRotation.project(0, 0.01, unrotated);
    rotated.project(0, 0.01, east);

    // With a 90° bearing the map is rotated so "up" points east: the eastern point moves
    // from "right of center" to "below center" on the watch screen.
    assertEquals(160f, east[0], 1f);
    assertTrue(east[1] > unrotated[1]);
    assertEquals(160f + (unrotated[0] - 160f), east[1], 1f);
  }
}
