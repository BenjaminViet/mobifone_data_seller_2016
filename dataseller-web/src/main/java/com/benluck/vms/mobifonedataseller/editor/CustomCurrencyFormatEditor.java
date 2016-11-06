package com.benluck.vms.mobifonedataseller.editor;

import org.apache.commons.lang.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * Created with IntelliJ IDEA.
 * User: Quoc Viet
 * Date: 10/26/13
 * Time: 2:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class CustomCurrencyFormatEditor extends PropertyEditorSupport{

    /**
     * Remove commas in submitted value of currency for converting.
     * @param text
     * @throws IllegalArgumentException
     */
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if(StringUtils.isNotBlank(text)) {
            setValue(text.replaceAll("\\,", ""));
        }else{
            setValue(null);
        }
    }
}
