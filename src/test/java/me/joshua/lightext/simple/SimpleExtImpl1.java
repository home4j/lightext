package me.joshua.lightext.simple;

/**
 * @author ding.lid
 */
public class SimpleExtImpl1 implements SimpleExt {
	public String echo(String s) {
		return "Ext1Impl1-echo";
	}

	public String yell(String s) {
		return "Ext1Impl1-yell";
	}

	public String bang(int i) {
		return "bang1";
	}
}