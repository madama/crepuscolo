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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.util.StringInputStream;

import net.etalia.crepuscolo.check.CheckMode;
import net.etalia.crepuscolo.json.CrepuscoloObjectMapper;
import net.etalia.jalia.OutField;

public class SQSSendQueue<T> implements SendQueue<T> {

	protected Log log = LogFactory.getLog(SQSSendQueue.class);

	public static long SQS_LENGTH = 256000;

	protected AmazonSQS sqs;
	protected AmazonS3 s3;
	protected Properties fields;
	protected String queueUrl;
	protected String bucketName;
	protected String bucketPrefix = "";

	protected CrepuscoloObjectMapper om = null;

	public void setQueueUrl(String queueUrl) {
		this.queueUrl = queueUrl;
	}
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}
	public void setBucketPrefix(String bucketPrefix) {
		this.bucketPrefix = bucketPrefix;
	}
	public void setSqs(AmazonSQS sqs) {
		this.sqs = sqs;
	}
	public void setS3(AmazonS3 s3) {
		this.s3 = s3;
	}
	public void setFields(Properties fields) {
		this.fields = fields;
	}
	public void setFieldsResource(String resource) {
		log.info("Loading SQS queue properties from " + resource);
		InputStream str = getClass().getResourceAsStream(resource);
		if (str == null) throw new IllegalArgumentException("Cannot find SQS queue fields resource file " + resource);
		fields = new Properties();
		try {
			fields.load(str);
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot load SQS queue fields resource file " + resource, e);
		}
		Enumeration<?> e = fields.propertyNames();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			log.info("Using SQS queue property " + key + "=" + fields.getProperty(key));
		}
	}

	public class SQSSendBatch implements SendBatch<T> {

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
				List<SendMessageBatchRequestEntry> entries = new ArrayList<SendMessageBatchRequestEntry>();
				Iterator<Map.Entry<T, String>> iter = payloads.entrySet().iterator();
				int length = 0;
				while (iter.hasNext()) {
					Entry<T, String> next = iter.next();
					String body = next.getValue();
					if (body.length() > SQS_LENGTH) {
						log.debug("Using S3 for payload of " + body.length());
						UUID fname = UUID.randomUUID();
						String nbody = "s3;" + bucketName + ":" + bucketPrefix + fname.toString(); 
						if (length + nbody.length() > SQS_LENGTH) break;							
						ObjectMetadata omd = new ObjectMetadata();
						log.debug("Pushed on S3 " + bucketName + " " + bucketPrefix + fname.toString() + " " + omd);
						try {
							s3.putObject(bucketName, bucketPrefix + fname.toString(), new StringInputStream(body), omd);
						} catch (UnsupportedEncodingException e) {}
						body = nbody;
					}
					length += body.length();
					if (length > SQS_LENGTH) break;
					SendMessageBatchRequestEntry req = new SendMessageBatchRequestEntry(UUID.randomUUID().toString(),body);
					log.debug("Added message to batch SQS send");
					entries.add(req);
					iter.remove();
					if (entries.size() >= 10) break;
				}
				if (entries.size() == 0) {
					log.debug("Spurious empty SQS batch");
				} else {
					log.debug("Sending SQS batch");
					SendMessageBatchRequest bt = new SendMessageBatchRequest(queueUrl, entries);
					sqs.sendMessageBatch(bt);
				}
			} while (payloads.size() > 0); 
		}
		
	}

	@Override
	public SendBatch<T> startBatch() {
		return new SQSSendBatch();
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
		payload = fields[0] + ";" + payload;
		if (log.isDebugEnabled()) {
			log.debug("Serializing " + Arrays.toString(fields));
			log.debug("Resulting " + payload);
		}
		return payload;
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
