package app.organicmaps.wear.protocol;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Encodes/decodes {@link WearMapState} for the Wear Data Layer using a compact binary layout
 * (fixed header + optional route polyline + optional PNG snapshot), so large payloads stay small
 * enough for the Data Layer's per-item size limit.
 *
 * <p>{@link #decode} returns {@code null} on any malformed or version-mismatched payload, so
 * incompatible peers degrade gracefully instead of misreading the bytes.
 *
 * <p>No Android dependencies here on purpose: wear-protocol is a contract module shared by the
 * phone bridge and the watch app.
 */
public final class WearMapStateCodec
{
  private static final int HEADER_SIZE = 4 + 1 + 8 + 8 + 4 + 4 + 4 + 8 + 8;
  private static final int HAS_MY_POSITION_FLAG = 0x01;
  private static final int HAS_SNAPSHOT_FLAG = 0x02;
  private static final int HAS_ROUTE_FLAG = 0x04;
  private static final int MAX_PAYLOAD_SIZE = 96 * 1024;
  private static final int MAX_ROUTE_POINTS = 4096;

  public static byte[] encode(WearMapState state)
  {
    final byte[] snapshot = state.getSnapshot();
    final double[] route = state.getRoutePoints();

    final int flags = (state.hasMyPosition() ? HAS_MY_POSITION_FLAG : 0)
        | (snapshot != null ? HAS_SNAPSHOT_FLAG : 0)
        | (route != null ? HAS_ROUTE_FLAG : 0);

    final ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + 4 + (route != null ? route.length * 8 : 0)
        + (snapshot != null ? snapshot.length : 0));
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    buffer.putInt(WearMapData.VERSION);
    buffer.put((byte) state.getMode().ordinal());
    buffer.putDouble(state.getCenterLat());
    buffer.putDouble(state.getCenterLon());
    buffer.putInt(state.getZoom());
    buffer.putFloat(state.getBearing());
    buffer.putInt(flags);
    if (state.hasMyPosition())
    {
      buffer.putDouble(state.getMyPositionLat());
      buffer.putDouble(state.getMyPositionLon());
    }
    else
    {
      buffer.putDouble(0);
      buffer.putDouble(0);
    }

    if (route != null)
    {
      buffer.putInt(route.length);
      for (final double coordinate : route)
        buffer.putDouble(coordinate);
    }
    else
    {
      buffer.putInt(0);
    }

    if (snapshot != null)
      buffer.put(snapshot);

    return buffer.array();
  }

  public static WearMapState decode(byte[] payload)
  {
    if (payload == null || payload.length < HEADER_SIZE + 4 || payload.length > MAX_PAYLOAD_SIZE)
      return null;

    try
    {
      final ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

      final int version = buffer.getInt();
      if (version != WearMapData.VERSION)
        return null;

      final int modeOrdinal = buffer.get() & 0xFF;
      final WearMapMode[] modes = WearMapMode.values();
      if (modeOrdinal >= modes.length)
        return null;
      final WearMapMode mode = modes[modeOrdinal];

      final double centerLat = buffer.getDouble();
      final double centerLon = buffer.getDouble();
      final int zoom = buffer.getInt();
      final float bearing = buffer.getFloat();
      final int flags = buffer.getInt();
      final boolean hasMyPosition = (flags & HAS_MY_POSITION_FLAG) != 0;
      final double myPositionLat = buffer.getDouble();
      final double myPositionLon = buffer.getDouble();

      final int routeLength = buffer.getInt();
      final boolean hasRoute = (flags & HAS_ROUTE_FLAG) != 0;
      if (routeLength < 0 || routeLength > MAX_ROUTE_POINTS || routeLength % 2 != 0
          || (routeLength > 0) != hasRoute)
        return null;
      if (buffer.remaining() < routeLength * 8L)
        return null;

      double[] route = null;
      if (hasRoute)
      {
        route = new double[routeLength];
        for (int i = 0; i < routeLength; i++)
          route[i] = buffer.getDouble();
      }

      byte[] snapshot = null;
      if ((flags & HAS_SNAPSHOT_FLAG) != 0)
      {
        if (!buffer.hasRemaining())
          return null;
        snapshot = new byte[buffer.remaining()];
        buffer.get(snapshot);
        if (!isPng(snapshot))
          return null;
      }
      else if (buffer.hasRemaining())
      {
        // Trailing bytes without the snapshot flag: malformed payload.
        return null;
      }

      return new WearMapState(mode, centerLat, centerLon, zoom, bearing, hasMyPosition,
                              myPositionLat, myPositionLon, snapshot, route);
    }
    catch (BufferUnderflowException | IllegalArgumentException e)
    {
      return null;
    }
  }

  private static boolean isPng(byte[] data)
  {
    return data.length > 8
        && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G';
  }

  private WearMapStateCodec() {}
}
