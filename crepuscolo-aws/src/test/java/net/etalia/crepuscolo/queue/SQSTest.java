package net.etalia.crepuscolo.queue;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.util.StringInputStream;

import net.etalia.crepuscolo.queue.SendQueue.SendBatch;
import net.etalia.jalia.spring.JaliaParametersFilter;

public class SQSTest {

	@Before
	@After
	public void resetFields() {
		JaliaParametersFilter.clean();
	}

	@Test
	public void send() throws Exception {
		AmazonSQS sqs = mock(AmazonSQS.class);
		
		Properties props = new Properties();
		props.put(DummyBean.class.getName(), "email,age");
		
		SQSSendQueue<DummyBean> sender = new SQSSendQueue<DummyBean>();
		sender.setFields(props);
		sender.setSqs(sqs);
		sender.setQueueUrl("http://queue.url/");
		
		DummyBean test = new DummyBean();
		test.setAge(25);
		test.setEmail("simoneg@apache.org");
		test.setName("Simone");
		SendBatch<DummyBean> batch = sender.startBatch();
		batch.put(test);
		batch.send();
		
		verify(sqs).sendMessageBatch(any(SendMessageBatchRequest.class));
		verify(sqs, times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
		
		ArgumentCaptor<SendMessageBatchRequest> captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
		verify(sqs).sendMessageBatch(captor.capture());
		SendMessageBatchRequest req = captor.getValue();
		assertNotNull("Sent a null request", req);
		assertThat(req.getEntries(), hasSize(1));
		SendMessageBatchRequestEntry entry = req.getEntries().get(0);
		String body = entry.getMessageBody();
		assertNotNull("Sent a null body", body);
		
		assertThat(body, startsWith(DummyBean.class.getName() + ";{"));
		assertThat(body, containsString("@entity"));
		assertThat(body, containsString("age"));
		assertThat(body, containsString("email"));
		assertThat(body, not(containsString("name")));
		System.out.println(body);
	}

	@Test
	public void sendWrapped() throws Exception {
		AmazonSQS sqs = mock(AmazonSQS.class);
		
		Properties props = new Properties();
		props.put(DummyBean.class.getName(), "email,age");
		
		SQSSendQueue<WrapDummy> sender = new SQSSendQueue<WrapDummy>();
		sender.setFields(props);
		sender.setSqs(sqs);
		sender.setQueueUrl("http://queue.url/");
		
		DummyBean test = new DummyBean();
		test.setAge(25);
		test.setEmail("simoneg@apache.org");
		test.setName("Simone");
		
		WrapDummy wd = new WrapDummy();
		wd.setWrapped(test);
		SendBatch<WrapDummy> batch = sender.startBatch();
		batch.put(wd);
		batch.send();
		
		verify(sqs).sendMessageBatch(any(SendMessageBatchRequest.class));
		verify(sqs, times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
		
		ArgumentCaptor<SendMessageBatchRequest> captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
		verify(sqs).sendMessageBatch(captor.capture());
		SendMessageBatchRequest req = captor.getValue();
		assertNotNull("Sent a null request", req);
		assertThat(req.getEntries(), hasSize(1));
		SendMessageBatchRequestEntry entry = req.getEntries().get(0);
		String body = entry.getMessageBody();
		assertNotNull("Sent a null body", body);
		
		assertThat(body,containsString("\"@entity\":\"DummyBean\""));
		
		System.out.println(body);
	}

	@Test
	public void read() throws Exception {
		Message msg = new Message()
			.withBody("net.etalia.crepuscolo.queue.DummyBean;{\"@entity\":\"DummyBean\",\"age\":25,\"email\":\"simoneg@apache.org\"}")
			.withReceiptHandle("RECEIPT");

		ReceiveMessageResult result = new ReceiveMessageResult()
			.withMessages(msg);
		
		AmazonSQS sqs = mock(AmazonSQS.class);
		when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(result);
		
		SQSReadQueue<DummyBean> queue = new SQSReadQueue<DummyBean>();
		queue.setSqs(sqs);
		
		int cnt = 0;
		for (DummyBean bean : queue) {
			cnt++;
			assertThat(bean.getAge(), equalTo(25));
			assertThat(bean.getEmail(), equalTo("simoneg@apache.org"));
			assertThat(bean.getName(), nullValue());
		}
		assertThat(cnt, equalTo(1));
		
		verify(sqs, times(1)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
		
		ArgumentCaptor<DeleteMessageBatchRequest> captor = ArgumentCaptor.forClass(DeleteMessageBatchRequest.class);
		verify(sqs).deleteMessageBatch(captor.capture());
		DeleteMessageBatchRequest deleteReq = captor.getValue();
		assertNotNull("Null delete request", deleteReq);
		List<DeleteMessageBatchRequestEntry> entries = deleteReq.getEntries();
		assertNotNull("Delete request without entries", entries);
		assertThat(entries, hasSize(1));
		DeleteMessageBatchRequestEntry entry = entries.get(0);
		assertNotNull(entry);
		assertThat(entry.getReceiptHandle(), equalTo("RECEIPT"));
	}

	@Test
	public void reject() throws Exception {
		Message msg1 = new Message()
			.withBody("net.etalia.crepuscolo.queue.DummyBean;{\"@entity\":\"DummyBean\",\"age\":25,\"email\":\"simoneg@apache.org\"}")
			.withReceiptHandle("RECEIPT");

		Message msg2 = new Message()
			.withBody("net.etalia.crepuscolo.queue.DummyBean;{\"@entity\":\"DummyBean\",\"age\":26,\"email\":\"simoneg2@apache.org\"}")
			.withReceiptHandle("RECEIPT2");
		
		ReceiveMessageResult result = new ReceiveMessageResult()
			.withMessages(msg1, msg2);
		
		AmazonSQS sqs = mock(AmazonSQS.class);
		when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(result);
		
		SQSReadQueue<DummyBean> queue = new SQSReadQueue<DummyBean>();
		queue.setSqs(sqs);
		
		AmazonSQS dlqSqs = mock(AmazonSQS.class);
		SQSSendQueue<MessageError> dlq = new SQSSendQueue<>();
		dlq.setSqs(dlqSqs);
		
		queue.setDlq(dlq);
		queue.setDlqAfter(0);
		
		MessageIterator<DummyBean> iterator = queue.iterator();
		
		int cnt = 0;
		while (iterator.hasNext()) {
			cnt++;
			DummyBean bean = iterator.next();
			if (cnt == 2) {
				iterator.reject(new IllegalStateException());
			} else {
				assertThat(bean.getAge(), equalTo(25));
				assertThat(bean.getEmail(), equalTo("simoneg@apache.org"));
				assertThat(bean.getName(), nullValue());
			}
		}
		assertThat(cnt, equalTo(2));
		
		verify(sqs, times(1)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
		
		{
			ArgumentCaptor<DeleteMessageBatchRequest> captor = ArgumentCaptor.forClass(DeleteMessageBatchRequest.class);
			verify(sqs).deleteMessageBatch(captor.capture());
			DeleteMessageBatchRequest deleteReq = captor.getValue();
			assertNotNull("Null delete request", deleteReq);
			List<DeleteMessageBatchRequestEntry> entries = deleteReq.getEntries();
			assertNotNull("Delete request without entries", entries);
			assertThat(entries, hasSize(1));
			DeleteMessageBatchRequestEntry entry = entries.get(0);
			assertNotNull(entry);
			assertThat(entry.getReceiptHandle(), equalTo("RECEIPT"));
		}
		
		{
			ArgumentCaptor<SendMessageBatchRequest> captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
			verify(dlqSqs).sendMessageBatch(captor.capture());
			SendMessageBatchRequest req = captor.getValue();
			assertNotNull("Sent a null request", req);
			assertThat(req.getEntries(), hasSize(1));
			SendMessageBatchRequestEntry entry = req.getEntries().get(0);
			String body = entry.getMessageBody();
			assertNotNull("Sent a null body", body);
			
			assertThat(body, startsWith(MessageError.class.getName() + ";{"));
			assertThat(body, containsString("\"threadName\":\""));
			assertThat(body, containsString("\"stacktrace\":\"java.lang.IllegalStateException\\n\\tat net.etalia.crepuscolo.queue.SQSTest."));
			assertThat(body, containsString("\"payload\":\"net.etalia.crepuscolo.queue.DummyBean"));
			assertThat(body, containsString("\"lastError\":1"));
			System.out.println(body);
		}
	}

	@Test
	public void failRead() throws Exception {
		Message msg1 = new Message()
			.withBody("net.etalia.crepuscolo.queue.DummyBean;{\"@entity\":\"DummyBean\",\"age\":25,\"email\":\"simoneg@apache.org\"}")
			.withReceiptHandle("RECEIPT");
	
		Message msg2 = new Message()
			.withBody("net.etalia.crepuscolo.queue.DummyBean;{\"@entity\":\"DummyBean\",\"age\":26,\"email\":\"simoneg2@apache.org\"}")
			.withReceiptHandle("RECEIPT2");
		
		ReceiveMessageResult result = new ReceiveMessageResult()
			.withMessages(msg1, msg2);
		
		AmazonSQS sqs = mock(AmazonSQS.class);
		when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(result);
		
		SQSReadQueue<DummyBean> queue = new SQSReadQueue<DummyBean>();
		queue.setSqs(sqs);
	
		MessageIterator<DummyBean> iterator = queue.iterator();
		iterator.hasNext();
		iterator.next();
		// Does not complete reading the iterator
		
		verify(sqs, times(0)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
		
	}

	@Test
	public void sendAndReadLong() throws Exception {
		String body,s3body;
		{
			AmazonSQS sqs = mock(AmazonSQS.class);
			AmazonS3 s3 = mock(AmazonS3.class);
			
			Properties props = new Properties();
			props.put(DummyBean.class.getName(), "email,age");
			
			SQSSendQueue<DummyBean> sender = new SQSSendQueue<DummyBean>();
			sender.setFields(props);
			sender.setSqs(sqs);
			sender.setS3(s3);
			sender.setBucketName("bucket1");
			sender.setBucketPrefix("tests/");
			sender.setQueueUrl("http://queue.url/");
			
			DummyBean test = new DummyBean();
			test.setAge(25);
			
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 257000; i++) {
				sb.append('a');
			}
			
			test.setEmail(sb.toString());
			test.setName("Simone");
			SendBatch<DummyBean> batch = sender.startBatch();
			batch.put(test);
			batch.send();
			
			verify(sqs).sendMessageBatch(any(SendMessageBatchRequest.class));
			verify(sqs, times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
			
			ArgumentCaptor<SendMessageBatchRequest> captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
			verify(sqs).sendMessageBatch(captor.capture());
			SendMessageBatchRequest req = captor.getValue();
			assertNotNull("Sent a null request", req);
			assertThat(req.getEntries(), hasSize(1));
			SendMessageBatchRequestEntry entry = req.getEntries().get(0);
			body = entry.getMessageBody();
			assertNotNull("Sent a null body", body);
			
			assertThat(body, startsWith("s3;bucket1:tests/"));
			//System.out.println(body);
			
			
			ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
			ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
			verify(s3, times(1)).putObject(eq("bucket1"), nameCaptor.capture(), streamCaptor.capture(), any(ObjectMetadata.class));
			
			assertThat(nameCaptor.getValue(), startsWith("tests/"));
			s3body = IOUtils.toString(streamCaptor.getValue());
	
			//System.out.println(s3body);
		}

		{
			Message msg = new Message()
				.withBody(body)
				.withReceiptHandle("RECEIPT");

			S3Object s3obj = new S3Object();
			s3obj.setObjectContent(new StringInputStream(s3body));
			
			ReceiveMessageResult result = new ReceiveMessageResult()
				.withMessages(msg);
			
			AmazonSQS sqs = mock(AmazonSQS.class);
			when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(result);
			
			AmazonS3 s3 = mock(AmazonS3.class);
			when(s3.getObject(eq("bucket1"), any(String.class))).thenReturn(s3obj);
			
			SQSReadQueue<DummyBean> queue = new SQSReadQueue<DummyBean>();
			queue.setSqs(sqs);
			queue.setS3(s3);
			
			int cnt = 0;
			for (DummyBean bean : queue) {
				cnt++;
				assertThat(bean.getAge(), equalTo(25));
				assertThat(bean.getName(), nullValue());
				for (int i = 0; i < bean.getEmail().length(); i++) {
					assertThat(bean.getEmail().charAt(i), equalTo('a'));
				}
			}
			assertThat(cnt, equalTo(1));
			
			verify(sqs, times(1)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
			
			ArgumentCaptor<DeleteMessageBatchRequest> captor = ArgumentCaptor.forClass(DeleteMessageBatchRequest.class);
			verify(sqs).deleteMessageBatch(captor.capture());
			DeleteMessageBatchRequest deleteReq = captor.getValue();
			assertNotNull("Null delete request", deleteReq);
			List<DeleteMessageBatchRequestEntry> entries = deleteReq.getEntries();
			assertNotNull("Delete request without entries", entries);
			assertThat(entries, hasSize(1));
			DeleteMessageBatchRequestEntry entry = entries.get(0);
			assertNotNull(entry);
			assertThat(entry.getReceiptHandle(), equalTo("RECEIPT"));
			
			ArgumentCaptor<DeleteObjectsRequest> dorcaptor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
			verify(s3, times(1)).deleteObjects(dorcaptor.capture());
			
			DeleteObjectsRequest s3delReq = dorcaptor.getValue();
			assertNotNull("Null s3 delete request", s3delReq);
			assertThat(s3delReq.getBucketName(), equalTo("bucket1"));
			assertThat(s3delReq.getKeys(), hasSize(1));
		}
	}

	@Test
	public void skipAndDelete() throws Exception {
		Message msg1 = new Message()
			.withBody("net.etalia.crepuscolo.queue.DummyBean;{\"@entity\":\"DummyBean\",\"age\":25,\"email\":\"simoneg@apache.org\"}")
			.withReceiptHandle("RECEIPT");

		Message msg2 = new Message()
			.withBody("net.etalia.crepuscolo.queue.DummyBean;{\"@entity\":\"DummyBean\",\"age\":26,\"email\":\"simoneg2@apache.org\"}")
			.withReceiptHandle("RECEIPT2");
		
		ReceiveMessageResult result = new ReceiveMessageResult()
			.withMessages(msg1, msg2);
		
		AmazonSQS sqs = mock(AmazonSQS.class);
		when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(result);
		
		SQSReadQueue<DummyBean> queue = new SQSReadQueue<DummyBean>();
		queue.setSqs(sqs);
		
		MessageIterator<DummyBean> iterator = queue.iterator();
		
		ReadReceipt receipt = null;
		int cnt = 0;
		while (iterator.hasNext()) {
			cnt++;
			DummyBean bean = iterator.next();
			if (cnt == 2) {
				receipt = iterator.skip();
				assertThat(receipt, notNullValue());
			} else {
				assertThat(bean.getAge(), equalTo(25));
				assertThat(bean.getEmail(), equalTo("simoneg@apache.org"));
				assertThat(bean.getName(), nullValue());
			}
		}
		assertThat(cnt, equalTo(2));
		
		verify(sqs, times(1)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
		
		{
			ArgumentCaptor<DeleteMessageBatchRequest> captor = ArgumentCaptor.forClass(DeleteMessageBatchRequest.class);
			verify(sqs).deleteMessageBatch(captor.capture());
			DeleteMessageBatchRequest deleteReq = captor.getValue();
			assertNotNull("Null delete request", deleteReq);
			List<DeleteMessageBatchRequestEntry> entries = deleteReq.getEntries();
			assertNotNull("Delete request without entries", entries);
			assertThat(entries, hasSize(1));
			DeleteMessageBatchRequestEntry entry = entries.get(0);
			assertNotNull(entry);
			assertThat(entry.getReceiptHandle(), equalTo("RECEIPT"));
		}
		
		queue.delete(receipt);
		
		verify(sqs, times(1)).deleteMessage(any(DeleteMessageRequest.class));
		
		{
			ArgumentCaptor<DeleteMessageRequest> captor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
			verify(sqs).deleteMessage(captor.capture());
			DeleteMessageRequest deleteReq = captor.getValue();
			assertNotNull("Null delete request", deleteReq);
			assertThat(deleteReq.getReceiptHandle(), equalTo("RECEIPT2"));
		}
		
	}

}
