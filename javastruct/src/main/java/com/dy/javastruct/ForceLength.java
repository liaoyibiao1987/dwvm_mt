package com.dy.javastruct;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Author by pingping, Email 327648349@qq.com, Date on 2018/6/28.
 * PS: Not easy to write code, please indicate.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ForceLength {
    int forceLen();
}
