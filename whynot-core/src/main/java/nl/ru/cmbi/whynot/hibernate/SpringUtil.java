package nl.ru.cmbi.whynot.hibernate;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringUtil {
	private static final ApplicationContext	context	= new ClassPathXmlApplicationContext(new String[] { "spring.xml" });

	public static ApplicationContext getContext() {
		return context;
	}
}
