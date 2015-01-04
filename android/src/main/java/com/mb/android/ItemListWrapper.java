package com.mb.android;

import mediabrowser.model.dto.BaseItemDto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mark on 2014-05-08.
 */
public class ItemListWrapper implements Serializable {

    public List<BaseItemDto> Items;

    public ItemListWrapper() {
        Items = new ArrayList<>();
    }
}
