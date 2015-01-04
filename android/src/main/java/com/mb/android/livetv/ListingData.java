package com.mb.android.livetv;

import mediabrowser.model.livetv.ProgramInfoDto;
import mediabrowser.model.livetv.TimerInfoDto;

/**
 * Created by Mark on 2014-06-04.
 */
public class ListingData implements IListing {

    public ProgramInfoDto programInfoDto;
    public TimerInfoDto timerInfoDto;

    @Override
    public boolean isHeader() {
        return false;
    }
}
