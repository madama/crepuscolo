package net.etalia.crepuscolo.queue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

import net.etalia.crepuscolo.json.CrepuscoloObjectMapper;
import net.etalia.crepuscolo.queue.SendQueue.SendBatch;

public class SQSReadQueue<T> implements ReadQueue<T> {

	protected Log log = LogFactory.getLog(SQSReadQueue.class);

	private AmazonSQS sqs;
	private AmazonS3 s3;

	private String defaultClass;
	private String queueUrl;
	private Integer waitSeconds;

	private Integer visibilityTimeout;
	private boolean progressiveTimeout = true;

	private Integer dlqAfter = 10;
	private SendQueue<MessageError> dlq;

	protected CrepuscoloObjectMapper objectMapper = new CrepuscoloObjectMapper();

	public void setSqs(AmazonSQS sqs) {
		this.sqs = sqs;
	}
	public void setS3(AmazonS3 s3) {
		this.s3 = s3;
	}
	public void setQueueUrl(String queueUrl) {
		this.queueUrl = queueUrl;
	}
	public void setWaitSeconds(Integer waitSeconds) {
		this.waitSeconds = waitSeconds;
	}
	public void setVisibilityTimeout(Integer visibilityTimeout) {
		this.visibilityTimeout = visibilityTimeout;
	}
	public void setProgressiveTimeout(boolean progressiveTimeout) {
		this.progressiveTimeout = progressiveTimeout;
	}
	public void setObjectMapper(CrepuscoloObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void setDlqAfter(Integer dlqAfter) {
		this.dlqAfter = dlqAfter;
	}
	public void setDlq(SendQueue<MessageError> dlq) {
		this.dlq = dlq;
	}

	public void setDefaultClass(String defaultClass) {
		this.defaultClass = defaultClass;
	}
	public String getDefaultClass() {
		return defaultClass;
	}

	@Override
	public MessageIterator<T> iterator() {
		ReceiveMessageRequest rmr = new ReceiveMessageRequest();
		rmr.setMaxNumberOfMessages(10);
		rmr.setAttributeNames(Arrays.asList("ApproximateReceiveCount"));
		rmr = rmr.withQueueUrl(queueUrl).withVisibilityTimeout(visibilityTimeout).withWaitTimeSeconds(waitSeconds);
		ReceiveMessageResult result = sqs.receiveMessage(rmr);
		List<Message> messages = result.getMessages();
		if (log.isDebugEnabled()) {
			for (Message msg : messages) {
				log.debug("Received message {}" + msg);
			}
		}
		return new SQSIterator(messages);
	}

	public class SQSReadReceipt implements ReadReceipt {
		private Message message;

		public SQSReadReceipt(Message message) {
			this.message = message;
		}

		@Override
		public String getMessageId() {
			return message.getMessageId();
		}
	}

	public class SQSIterator implements MessageIterator<T> {
		
		private Iterator<Message> iter = null;
		private List<DeleteMessageBatchRequestEntry> deletes = null;
		private List<DeleteObjectRequest> s3deletes = null;
		
		private Message acmsg;
		private DeleteObjectRequest acs3delete;
		
		public SQSIterator(List<Message> messages) {
			this.iter = messages.iterator();
			this.deletes = new ArrayList<DeleteMessageBatchRequestEntry>(messages.size());
			this.s3deletes = new ArrayList<DeleteObjectRequest>(messages.size());
		}
		
		@Override
		public boolean hasNext() {
			boolean hasNext = this.iter.hasNext();
			if (!hasNext) {
				if (this.acmsg != null) {
					DeleteMessageBatchRequestEntry entry = new DeleteMessageBatchRequestEntry(this.acmsg.getMessageId(), this.acmsg.getReceiptHandle());
					deletes.add(entry);
					if (acs3delete != null) {
						log.debug("Queueing SQS and S3 delete");
						s3deletes.add(acs3delete);
					} else {
						log.debug("Queueing SQS delete");
					}
				}
				if (log.isDebugEnabled()) {
					log.debug("Read batch ended, performing " + deletes.size() + " SQS deletes and " + s3deletes.size() + " S3 deletes");
				}
				if (deletes.size() > 0) {
					DeleteMessageBatchRequest req = new DeleteMessageBatchRequest(queueUrl, deletes);
					sqs.deleteMessageBatch(req);
				}
				if (s3deletes.size() > 0) {
					DeleteObjectsRequest req = new DeleteObjectsRequest(s3deletes.get(0).getBucketName());
					List<KeyVersion> keys = new ArrayList<KeyVersion>();
					for (DeleteObjectRequest dor : s3deletes) {
						keys.add(new KeyVersion(dor.getKey()));
					}
					req.setKeys(keys);
					s3.deleteObjects(req);
				}
			}
			return hasNext;
		}
		
		@Override
		public T next() {
			if (this.acmsg != null) {
				DeleteMessageBatchRequestEntry entry = new DeleteMessageBatchRequestEntry(this.acmsg.getMessageId(), this.acmsg.getReceiptHandle());
				deletes.add(entry);
				if (acs3delete != null) {
					log.debug("Queueing SQS and S3 delete");
					s3deletes.add(acs3delete);
				} else {
					log.debug("Queueing SQS delete");
				}
			}
			this.acmsg = this.iter.next();
			log.debug("Fetched SQS message");
			acs3delete = null;
			String body = acmsg.getBody();
			try {
				String clazz = defaultClass;
				if (defaultClass == null) {
					clazz = body.substring(0, body.indexOf(';'));
				}
				if (clazz.equals("s3") || body.startsWith("s3;")) {
					log.debug("Fetching S3 payload from " + body);
					body = body.substring(3);
					String bucketName = body.substring(0, body.indexOf(':'));
					body = body.substring(bucketName.length() + 1);
					acs3delete = new DeleteObjectRequest(bucketName, body);
					S3Object obj = s3.getObject(bucketName, body);
					InputStream instr = obj.getObjectContent();
					try {
						body = IOUtils.toString(instr);
					} catch (IOException e) {
						throw new IllegalStateException("Error reading message payload from s3", e);
					} finally {
						IOUtils.closeQuietly(instr);
					}
					if (defaultClass == null) {
						clazz = body.substring(0, body.indexOf(';'));
					}
				}
				if (defaultClass == null) {
					body = body.substring(clazz.length() + 1);
				}
				return parse(body, clazz);
			} catch (Throwable t) {
				reject(t);
				throw new IllegalStateException("Error parsing SQS payload", t);
			}
		}
		
		@Override
		public void remove() {
		}
		
		@Override
		public void reject() {
			this.reject(null);
		}
		
		@Override
		public void reject(Throwable exception) {
			SQSReadQueue.this.reject(skip(), exception);
		}
		
		@Override
		public SQSReadReceipt skip() {
			Message premsg = this.acmsg;
			this.acmsg = null;
			return new SQSReadReceipt(premsg);
		}
		
		@Override
		public String getMessageId() {
			return acmsg == null ? null : acmsg.getMessageId();
		}
		
	}

	@Override
	public void reject(ReadReceipt receipt, Throwable exception) {
		if (!(receipt instanceof SQSReadQueue.SQSReadReceipt)) throw new IllegalStateException("Can only handle SQSReadReceipt, not " + receipt);
		Message acmsg = ((SQSReadReceipt)receipt).message;
		int reccnt = 0;
		if (progressiveTimeout || dlq != null) {
			Map<String, String> attrs = acmsg.getAttributes();
			String cnt = attrs.get("ApproximateReceiveCount");
			if (cnt != null) {
				reccnt = Integer.parseInt(cnt);
			}
		}
		if (dlq != null && reccnt >= dlqAfter) {
			MessageError me = new MessageError("SQS:" + queueUrl);
			me.setPayload(acmsg.getBody());
			me.setThrowable(exception);
			SendBatch<MessageError> dlqBatch = dlq.startBatch();
			dlqBatch.put(me);
			dlqBatch.send();
			DeleteMessageRequest delreq = new DeleteMessageRequest(queueUrl, acmsg.getReceiptHandle());
			sqs.deleteMessage(delreq);
			log.warn("Rejected SQS message, receive count " + reccnt + ", sent to dlq : " + acmsg.getBody());
		} else if (progressiveTimeout) {
			int newto = 0;
			newto = (int) (Math.pow(reccnt,2) * 60);
			newto = Math.min(newto, 60*60);
			ChangeMessageVisibilityRequest chv = new ChangeMessageVisibilityRequest(queueUrl, acmsg.getReceiptHandle(), newto);
			sqs.changeMessageVisibility(chv);
			log.warn("Rejected SQS message, receive count " + reccnt + ", rescheduled : " + newto + " " + acmsg.getBody());
		} else {
			log.warn("Rejected SQS message, receive count " + reccnt + ", no progressive rescheduling : " + acmsg.getBody());
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void delete(ReadReceipt receipt) {
		if (!(receipt instanceof SQSReadQueue.SQSReadReceipt)) throw new IllegalStateException("Can only handle SQSReadReceipt, not " + receipt);
		Message acmsg = ((SQSReadReceipt)receipt).message;
		DeleteMessageRequest req = new DeleteMessageRequest(queueUrl, acmsg.getReceiptHandle());
		sqs.deleteMessage(req);
	}

	@SuppressWarnings("unchecked")
	protected T parse(String body, String clazz) {
		try {
			return (T)objectMapper.readValue(body, Class.forName(clazz));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
