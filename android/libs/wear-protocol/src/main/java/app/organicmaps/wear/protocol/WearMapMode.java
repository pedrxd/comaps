package app.organicmaps.wear.protocol;

public enum WearMapMode
{
  // No route is assigned on the phone: the watch shows a black screen with instructions.
  IDLE,
  // A route is assigned: the watch mirrors the phone map and route.
  NAVIGATION
}
