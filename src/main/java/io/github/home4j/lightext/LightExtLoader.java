package io.github.home4j.lightext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 扩展加载器，从Dubbo的扩展实现简化而来（<a href=
 * "https://github.com/alibaba/dubbo/blob/master/dubbo-common/src/main/java/com/alibaba/dubbo/common/extension/ExtensionLoader.java">
 * com.alibaba.dubbo.common.extension.ExtensionLoader</a>）。
 * <p>
 * LightExt保留了ExtensionLoader中基于SPI的扩展，让SPI可以通过扩展实现的名字来直接获取扩展实例。
 * 
 * 
 * @author Joshua Zhan
 */
public class LightExtLoader<T> {

	private static final Logger logger = LoggerFactory.getLogger(LightExtLoader.class);

	private static final String SERVICES_DIRECTORY = "META-INF/lightext/";

	private static final ConcurrentMap<Class<?>, LightExtLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<Class<?>, LightExtLoader<?>>();
	private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<Class<?>, Object>();

	private final Class<?> type;

	/**
	 * 默认扩展的名称
	 */
	private final String defaultName;

	private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();
	private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<String, Holder<Object>>();

	@SuppressWarnings("unchecked")
	public static <T> LightExtLoader<T> getExtensionLoader(Class<T> type) {
		if (type == null)
			throw new IllegalArgumentException("Extension type == null");
		if (!type.isInterface()) {
			throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");
		}

		LightExtLoader<T> loader = (LightExtLoader<T>) EXTENSION_LOADERS.get(type);
		if (loader == null) {
			Default anno = type.getAnnotation(Default.class);
			String defaultName = null == anno ? null : anno.value();
			EXTENSION_LOADERS.putIfAbsent(type, new LightExtLoader<T>(type, defaultName));
			loader = (LightExtLoader<T>) EXTENSION_LOADERS.get(type);
		}
		return loader;
	}

	/**
	 * 构造ExtensionLoader
	 * 
	 * @param type
	 * @param defaultName
	 *            可以为空表示没有默认扩展，但不允许为空字符串
	 */
	private LightExtLoader(Class<?> type, String defaultName) {
		this.type = type;
		if (null != defaultName && StringUtils.isBlank(defaultName)) {
			throw new IllegalArgumentException("Default extension name for type(" + type + ") is blank!");
		}
		this.defaultName = StringUtils.trimToNull(defaultName);
	}

	/**
	 * 返回默认的扩展点实例，通过{@link Default}来配置。
	 */
	public T getDefaultExtension() {
		if (null == defaultName) {
			throw new IllegalStateException("No default extension for " + type.getName());
		}

		return getExtension(defaultName);
	}

	/**
	 * 返回缺省的扩展点名。
	 */
	public String getDefaultName() {
		return defaultName;
	}

	/**
	 * 返回已经加载的扩展点的名字。
	 * <p>
	 * 一般应该调用{@link #getSupportedExtensions()}方法获得扩展，这个方法会返回所有的扩展点。
	 *
	 * @see #getSupportedExtensions()
	 */
	public Set<String> getLoadedExtensions() {
		return Collections.unmodifiableSet(new TreeSet<String>(cachedInstances.keySet()));
	}

	/**
	 * 返回指定名字的扩展。
	 *
	 * @param name
	 * @return
	 * @exception ClassCastException
	 *                如果泛型参数的类型S和实际的扩展类不匹配，则抛异常
	 * @exception IllegalStateException
	 *                如果指定名字的扩展不存在，则抛异常
	 */
	@SuppressWarnings("unchecked")
	public <S extends T> S getExtension(String name) {
		if (StringUtils.isBlank(name)) {
			throw new IllegalArgumentException("Extension name == null");
		}
		Holder<Object> holder = cachedInstances.get(name);
		if (holder == null) {
			cachedInstances.putIfAbsent(name, new Holder<Object>());
			holder = cachedInstances.get(name);
		}
		Object instance = holder.get();
		if (instance == null) {
			synchronized (holder) {
				instance = holder.get();
				if (instance == null) {
					instance = createExtension(name);
					holder.set(instance);
				}
			}
		}
		return (S) instance;
	}

	/**
	 * 判断某个名字的扩展是否存在
	 * 
	 * @param name
	 * @return
	 */
	public boolean hasExtension(String name) {
		if (StringUtils.isBlank(name))
			throw new IllegalArgumentException("Extension name == null");
		try {
			return getExtensionClass(name) != null;
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * 
	 * @return
	 */
	public Set<String> getSupportedExtensions() {
		Map<String, Class<?>> clazzes = getExtensionClasses();
		return Collections.unmodifiableSet(new TreeSet<String>(clazzes.keySet()));
	}

	@SuppressWarnings("unchecked")
	private T createExtension(String name) {
		Class<?> clazz = getExtensionClasses().get(name);
		if (clazz == null) {
			throw new IllegalStateException("No such extension " + type.getName() + " by name " + name);
		}
		try {
			T instance = (T) EXTENSION_INSTANCES.get(clazz);
			if (instance == null) {
				EXTENSION_INSTANCES.putIfAbsent(clazz, (T) clazz.newInstance());
				instance = (T) EXTENSION_INSTANCES.get(clazz);
			}
			return instance;
		} catch (Throwable t) {
			throw new IllegalStateException("Extension instance(name: " + name + ", class: " + type
			        + ")  could not be instantiated: " + t.getMessage(), t);
		}
	}

	private Class<?> getExtensionClass(String name) {
		if (type == null)
			throw new IllegalArgumentException("Extension type == null");
		if (name == null)
			throw new IllegalArgumentException("Extension name == null");
		Class<?> clazz = getExtensionClasses().get(name);
		if (clazz == null)
			throw new IllegalStateException("No such extension \"" + name + "\" for " + type.getName() + "!");
		return clazz;
	}

	private Map<String, Class<?>> getExtensionClasses() {
		Map<String, Class<?>> classes = cachedClasses.get();
		if (classes == null) {
			synchronized (cachedClasses) {
				classes = cachedClasses.get();
				if (classes == null) {
					classes = loadExtensionClasses();
					cachedClasses.set(classes);
				}
			}
		}
		return classes;
	}

	// 此方法已经getExtensionClasses方法同步过。
	private Map<String, Class<?>> loadExtensionClasses() {
		Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
		loadFile(extensionClasses, SERVICES_DIRECTORY);
		return extensionClasses;
	}

	private void loadFile(Map<String, Class<?>> extensionClasses, String dir) {
		String fileName = dir + type.getName();
		try {
			Enumeration<java.net.URL> urls;
			ClassLoader classLoader = findClassLoader();
			if (classLoader != null) {
				urls = classLoader.getResources(fileName);
			} else {
				urls = ClassLoader.getSystemResources(fileName);
			}
			if (urls != null) {
				while (urls.hasMoreElements()) {
					java.net.URL url = urls.nextElement();
					try {
						BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
						try {
							String line = null;
							while ((line = reader.readLine()) != null) {
								final int ci = line.indexOf('#');
								if (ci >= 0)
									line = line.substring(0, ci);
								line = line.trim();
								if (line.length() > 0) {
									String name = null;
									int i = line.indexOf('=');
									if (i > 0) {
										name = line.substring(0, i).trim();
										line = line.substring(i + 1).trim();
									}
									if (line.length() > 0) {
										Class<?> clazz = Class.forName(line, true, classLoader);
										if (!type.isAssignableFrom(clazz)) {
											throw new IllegalStateException(
											        "Error when load extension class(interface: " + type
											                + ", class line: " + clazz.getName() + "), class "
											                + clazz.getName() + "is not subtype of interface.");
										}

										Class<?> c = extensionClasses.get(name);
										if (c == null) {
											extensionClasses.put(name, clazz);
										} else {
											throw new IllegalStateException(
											        "Duplicate extension " + type.getName() + " name " + name + " on "
											                + c.getName() + " and " + clazz.getName());
										}

									}
								}
							} // end of while read lines
						} finally {
							reader.close();
						}
					} catch (Throwable t) {
						logger.error("Exception when load extension class(interface: " + type + ", class file: " + url
						        + ") in " + url, t);
					}
				} // end of while urls
			}
		} catch (Throwable t) {
			logger.error(
			        "Exception when load extension class(interface: " + type + ", description file: " + fileName + ").",
			        t);
		}
	}

	private static ClassLoader findClassLoader() {
		return LightExtLoader.class.getClassLoader();
	}

	@Override
	public String toString() {
		return this.getClass().getName() + "[" + type.getName() + "]";
	}

}