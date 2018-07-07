package com.copasso.cocobook.model.type;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import com.copasso.cocobook.App;
import com.copasso.cocobook.R;

/**
 * 发现类型
 */

public enum FindType {
    //排行
    TOP(R.string.fragment_discover_top,R.drawable.ic_section_top),
    //主题书单
    TOPIC(R.string.fragment_discover_topic,R.drawable.ic_section_topic),
    //分类
    SORT(R.string.fragment_discover_sort,R.drawable.ic_section_sort);

    private String typeName;
    private int iconId;

    FindType(@StringRes int typeNameId,@DrawableRes int iconId){
        this.typeName = App.getContext().getResources().getString(typeNameId);
        this.iconId = iconId;
    }

    public String getTypeName(){
        return typeName;
    }

    public int getIconId(){
        return iconId;
    }
}
