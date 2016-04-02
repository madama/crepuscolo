package net.etalia.crepuscolo.test;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.etalia.crepuscolo.test.db.SchemaCreator;

@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class BaseTestSpring implements ApplicationContextAware, InitializingBean {

	protected Log log = LogFactory.getLog(BaseTestSpring.class);

	protected ApplicationContext applicationContext;

	@Override
	public void afterPropertiesSet() throws Exception {
	}

	protected void recreateSchema() throws SQLException, IOException {
		SchemaCreator bean = applicationContext.getBean(SchemaCreator.class);
		bean.recreateSchema();
	}

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		this.applicationContext = ctx;
	}

	protected <T> T getBean(Class<T> clazz) {
		T bean = applicationContext.getBean(clazz);
		Assert.assertNotNull("Cannot find a bean of class " + clazz + " in current application context", bean);
		return bean;
	}

}
