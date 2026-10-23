package app.organicmaps.wear.protocol;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WearMapStateCodecTest
{
  private static final double[] ROUTE = {55.0, 37.0, 55.001, 37.001, 55.002, 37.002};

  @Test
  public void roundTripIdle()
  {
    final byte[] bytes = WearMapStateCodec.encode(WearMapState.idle());
    final WearMapState decoded = WearMapStateCodec.decode(bytes);
    assertNotNull(decoded);
    assertEquals(WearMapMode.IDLE, decoded.getMode());
    assertFalse(decoded.isNavigating());
    assertFalse(decoded.hasMyPosition());
    assertNull(decoded.getSnapshot());
    assertNull(decoded.getRoutePoints());
  }

  @Test
  public void roundTripNavigation()
  {
    final byte[] snapshot = pngHeader();
    final WearMapState state = WearMapState.navigation(55.75, 37.61, 15, 90f,
                                                      true, 55.751, 37.611, snapshot, ROUTE);
    final WearMapState decoded = WearMapStateCodec.decode(WearMapStateCodec.encode(state));

    assertNotNull(decoded);
    assertEquals(WearMapMode.NAVIGATION, decoded.getMode());
    assertTrue(decoded.isNavigating());
    assertEquals(55.75, decoded.getCenterLat(), 0);
    assertEquals(37.61, decoded.getCenterLon(), 0);
    assertEquals(15, decoded.getZoom());
    assertEquals(90f, decoded.getBearing(), 0);
    assertTrue(decoded.hasMyPosition());
    assertEquals(55.751, decoded.getMyPositionLat(), 0);
    assertEquals(37.611, decoded.getMyPositionLon(), 0);
    assertArrayEquals(snapshot, decoded.getSnapshot());
    assertArrayEquals(ROUTE, decoded.getRoutePoints(), 0);
  }

  @Test
  public void roundTripNavigationWithoutPositionSnapshotOrRoute()
  {
    final WearMapState state = WearMapState.navigation(10, 20, 5, 0f, false, 0, 0, null, null);
    final WearMapState decoded = WearMapStateCodec.decode(WearMapStateCodec.encode(state));

    assertNotNull(decoded);
    assertTrue(decoded.isNavigating());
    assertFalse(decoded.hasMyPosition());
    assertNull(decoded.getSnapshot());
    assertNull(decoded.getRoutePoints());
  }

  @Test
  public void decodeNullOrEmptyReturnsNull()
  {
    assertNull(WearMapStateCodec.decode(null));
    assertNull(WearMapStateCodec.decode(new byte[0]));
  }

  @Test
  public void decodeVersionMismatchReturnsNull()
  {
    final byte[] payload = WearMapStateCodec.encode(WearMapState.idle());
    payload[0] = 0x7F; // Corrupt the protocol version (little-endian int).
    assertNull(WearMapStateCodec.decode(payload));
  }

  @Test
  public void decodeUnknownModeReturnsNull()
  {
    final byte[] payload = WearMapStateCodec.encode(WearMapState.idle());
    payload[4] = (byte) 0x7F; // Mode ordinal out of range.
    assertNull(WearMapStateCodec.decode(payload));
  }

  @Test
  public void decodeGarbageReturnsNull()
  {
    assertNull(WearMapStateCodec.decode("not a payload".getBytes()));
  }

  @Test
  public void decodeTruncatedReturnsNull()
  {
    final byte[] payload = WearMapStateCodec.encode(WearMapState.idle());
    final byte[] truncated = new byte[payload.length / 2];
    System.arraycopy(payload, 0, truncated, 0, truncated.length);
    assertNull(WearMapStateCodec.decode(truncated));
  }

  @Test
  public void decodeOversizedReturnsNull()
  {
    assertNull(WearMapStateCodec.decode(new byte[97 * 1024]));
  }

  @Test
  public void decodeOddRouteLengthReturnsNull()
  {
    final byte[] payload = WearMapStateCodec.encode(
        WearMapState.navigation(0, 0, 1, 0, false, 0, 0, null, new double[] {1, 2, 3}));
    assertNull(WearMapStateCodec.decode(payload));
  }

  private static byte[] pngHeader()
  {
    return new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
  }
}
