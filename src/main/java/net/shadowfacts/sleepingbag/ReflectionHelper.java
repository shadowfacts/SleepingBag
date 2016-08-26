package net.shadowfacts.sleepingbag;

import java.lang.reflect.Field;

/**
 * @author shadowfacts
 */
public class ReflectionHelper {

	public static <T> void set(Class<T> clazz, T instance, Object value, String... names) {
		for (String name : names) {
			try {
				Field f = clazz.getDeclaredField(name);
				f.set(instance, value);
				return;
			} catch (ReflectiveOperationException ignored) {}
		}
		throw new RuntimeException();
	}

}
