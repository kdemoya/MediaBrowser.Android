package com.mb.android.player;

/**
 * Created by Mark on 2014-05-16.
 *
 * Exception that will be thrown when Initialize() has not been called on a MediaPlayerSurfaceView
 * and an attempt is made to load media.
 */
public class PlayerNotInitializedException extends Exception {

    public PlayerNotInitializedException() { super(); }

    public PlayerNotInitializedException(String detailMessage) { super(detailMessage); }

    public PlayerNotInitializedException(Throwable throwable) { super(throwable); }

    public PlayerNotInitializedException(String detailMessage, Throwable throwable) { super(detailMessage, throwable); }
}
