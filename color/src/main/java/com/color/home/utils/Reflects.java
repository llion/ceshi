package com.color.home.utils;

import java.lang.reflect.Field;

public class Reflects {
    private static final boolean DBG = false;

    public static <T> Field getFieldFrom(Class<T> clazz, String fieldName) throws NoSuchFieldException {
        Field modifiersField = clazz.getDeclaredField(fieldName);
        modifiersField.setAccessible(true);
        return modifiersField;
    }

    public static <T> Object getFieldValue(Object iv, String fieldName) throws IllegalAccessException, NoSuchFieldException {
        return getFieldFrom(iv.getClass(), fieldName).get(iv);
    }

    public static <T> Object getFieldValueFromClazz(Class<T> clazz, Object iv, String fieldName) throws IllegalAccessException,
            NoSuchFieldException {
        return getFieldFrom(clazz, fieldName).get(iv);
    }

}