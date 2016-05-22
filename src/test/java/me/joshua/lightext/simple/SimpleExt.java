package me.joshua.lightext.simple;

import me.joshua.lightext.Default;

/**
 * 简单扩展点。 没有Wrapper。
 *
 * @author ding.lid
 */
@Default("impl1")
public interface SimpleExt {
	String echo(String s);

	String yell(String s);

	String bang(int i);
}