package me.joshua.lightext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hamcrest.core.StringContains;
import org.junit.Test;

import me.joshua.lightext.LightExtLoader;
import me.joshua.lightext.generic.BlankDefaultExt;
import me.joshua.lightext.generic.GenericExt;
import me.joshua.lightext.generic.IntegerExt;
import me.joshua.lightext.simple.SimpleExt;
import me.joshua.lightext.simple.SimpleExtImpl1;
import me.joshua.lightext.simple.SimpleExtImpl2;
import me.joshua.lightext.simple.SimpleExtImpl3;

public class LightExtLoaderTests {

	private LightExtLoader<SimpleExt> simpleLoader = LightExtLoader.getExtensionLoader(SimpleExt.class);

	@Test
	public void test() {
		SimpleExt impl1 = simpleLoader.getExtension("impl1");
		assertTrue(impl1 instanceof SimpleExtImpl1);
		SimpleExt impl2 = simpleLoader.getExtension("impl2");
		assertTrue(impl2 instanceof SimpleExtImpl2);
		SimpleExt impl3 = simpleLoader.getExtension("impl3");
		assertTrue(impl3 instanceof SimpleExtImpl3);
		assertEquals(3, simpleLoader.getLoadedExtensions().size());
	}

	@Test(expected = ClassCastException.class)
	public void testGeneric() {
		@SuppressWarnings("rawtypes")
		LightExtLoader<GenericExt> genericLoader = LightExtLoader.getExtensionLoader(GenericExt.class);
		IntegerExt ext = genericLoader.getExtension("integer");
		assertNotNull(ext);
		ext = genericLoader.getExtension("long");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBlankDefault() {
		LightExtLoader.getExtensionLoader(BlankDefaultExt.class);
	}

	@Test
	public void testDefault() {
		assertEquals("impl1", simpleLoader.getDefaultName());
		SimpleExt defaultExt = simpleLoader.getDefaultExtension();
		assertTrue(defaultExt instanceof SimpleExtImpl1);
	}

	@Test
	public void testGetExtensionLoader_Null() throws Exception {
		try {
			LightExtLoader.getExtensionLoader(null);
			fail();
		} catch (IllegalArgumentException expected) {
			assertThat(expected.getMessage(), StringContains.containsString("Extension type == null"));
		}
	}

	@Test
	public void testGetExtensionLoader_NotInterface() throws Exception {
		try {
			LightExtLoader.getExtensionLoader(LightExtLoaderTests.class);
			fail();
		} catch (IllegalArgumentException expected) {
			assertThat(expected.getMessage(), StringContains.containsString(
			        "Extension type(class me.joshua.lightext.LightExtLoaderTests) is not interface!"));
		}
	}
}