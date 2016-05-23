package me.joshua.lightext;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.lang3.StringUtils;

/**
 * 默认扩展点配置注解
 *
 * @author <a href="mailto:daonan.zhan@gmail.com">Joshua Zhan</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Default {

	/**
	 * 缺省扩展点名。
	 */
	String value() default StringUtils.EMPTY;

}