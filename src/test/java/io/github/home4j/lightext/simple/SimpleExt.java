package io.github.home4j.lightext.simple;

import io.github.home4j.lightext.Default;

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