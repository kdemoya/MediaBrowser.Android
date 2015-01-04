package com.mb.android.exceptions;

/**
 * Created by Mark on 2014-04-04.
 */
public class SaveFileException extends Exception {

    public SaveFileException() { super(); }

    public SaveFileException(String detailMessage) { super(detailMessage); }

    public SaveFileException(Throwable throwable) { super(throwable); }

    public SaveFileException(String detailMessage, Throwable throwable) { super(detailMessage, throwable); }
}
