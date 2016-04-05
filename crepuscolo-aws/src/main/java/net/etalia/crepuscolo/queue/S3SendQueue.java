package net.etalia.crepuscolo.queue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.Map.Entry;

import net.etalia.crepuscolo.check.CheckMode;
import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.json.CrepuscoloObjectMapper;
import net.etalia.crepuscolo.queue.SQSSendQueue.SQSSendBatch;
import net.etalia.crepuscolo.queue.SendQueue.SendBatch;
import net.etalia.jalia.OutField;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.util.StringInputStream;

public class S3SendQueue<T> implements SendQueue<T> {

	protected Log log = LogFactory.getLog(SQSSendQueue.class);

	public static long SQS_LENGTH = 256000;

	protected AmazonS3 s3;
	protected Properties fields;
	protected String bucketName;
	protected String bucketPrefix = "";
	
	protected boolean withClassPrefix = false;
	protected CrepuscoloObjectMapper om = null;

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}
	public void setBucketPrefix(String bucketPrefix) {
		this.bucketPrefix = bucketPrefix;
	}
	public void setS3(AmazonS3 s3) {
		this.s3 = s3;
	}
	public void setFields(Properties fields) {
		this.fields = fields;
	}
	// TODO autowire?
	public void setObjectManager(CrepuscoloObjectMapper om) {
		this.om = om;
	}
	public void setWithClassPrefix(boolean withClassPrefix) {
		this.withClassPrefix = withClassPrefix;
	}
	public void setFieldsResource(String resource) {
		log.info("Loading S3 queue properties from " + resource);
		InputStream str = getClass().getResourceAsStream(resource);
		if (str == null) throw new IllegalArgumentException("Cannot find SS3QS queue fields resource file " + resource);
		fields = new Properties();
		try {
			fields.load(str);
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot load S3 queue fields resource file " + resource, e);
		}
		Enumeration<?> e = fields.propertyNames();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			log.info("Using S3 queue property " + key + "=" + fields.getProperty(key));
		}
	}

	public class S3SendBatch implements SendBatch<T> {
		
		private Map<T, String> payloads = new HashMap<T, String>(); 
		
		@Override
		public void put(T object) {
			payloads.put(object, null);
		}
		
		@Override
		public void prepare() {
			for (Map.Entry<T, String> entry : payloads.entrySet()) {
				if (entry.getValue() == null) {
					entry.setValue(getPayload(entry.getKey()));
				}
			}
		}
		
		@Override
		public void send() {
			prepare();
			do {
				Iterator<Map.Entry<T, String>> iter = payloads.entrySet().iterator();
				while (iter.hasNext()) {
					Entry<T, String> next = iter.next();
					String body = next.getValue();
					String fname = getName(next.getKey());
					ObjectMetadata omd = new ObjectMetadata();
					log.debug("Pushed on S3 " + bucketName + " " + bucketPrefix + fname + " " + omd);
					try {
						s3.putObject(bucketName, bucketPrefix + fname, new StringInputStream(body), omd);
					} catch (UnsupportedEncodingException e) {}
					iter.remove();
				}
			} while (payloads.size() > 0); 
		}
		
	}

	@Override
	public SendBatch<T> startBatch() {
		return new S3SendBatch();
	}

	@CheckMode("SQS")
	protected String getPayload(T object) {
		String[] fields = findFieldsFor(object, object.getClass());
		OutField root = new OutField(null);
		if (fields != null) {
			root.getCreateSubs(fields[1].split(","));
		} else {
			root = null;
			fields = new String[] { object.getClass().getName() };
		}
		if (om == null) om = new CrepuscoloObjectMapper();
		String payload;
		try {
			payload = om.writeValueAsString(object, root);
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot serialize to SQS queue : " + object, e);
		}
		if (this.withClassPrefix) {
			payload = fields[0] + ";" + payload;
		}
		if (log.isDebugEnabled()) {
			log.debug("Serializing " + Arrays.toString(fields));
			log.debug("Resulting " + payload);
		}
		return payload;
	}
	
	protected String getName(Object object) {
		if (object instanceof BaseEntity) return ((BaseEntity)object).getId();
		return UUID.randomUUID().toString();
	}

	public String[] findFieldsFor(Object o, Class<?> clazz) {
		if (clazz == null || fields == null) return null;
		String props = fields.getProperty(clazz.getName());
		if (props != null) return new String[] { clazz.getName(), props };
		props = fields.getProperty(clazz.getSimpleName());
		if (props != null) return new String[] { clazz.getName(), props };
		return findFieldsFor(o, clazz.getSuperclass());
	}


}
