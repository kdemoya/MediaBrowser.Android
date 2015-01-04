package com.mb.android.exceptions;

/**
 * Created by Mark on 2014-04-04.
 */
public class LoadFileException extends Exception {

    public LoadFileException() { super(); }

    public LoadFileException(String detailMessage) { super(detailMessage); }

    public LoadFileException(Throwable throwable) { super(throwable); }

    public LoadFileException(String detailMessage, Throwable throwable) { super(detailMessage, throwable); }
}
