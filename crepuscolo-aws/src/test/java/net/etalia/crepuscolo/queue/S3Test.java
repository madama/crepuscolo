package net.etalia.crepuscolo.queue;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.InputStream;
import java.util.Properties;

import net.etalia.crepuscolo.queue.SendQueue.SendBatch;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.util.StreamUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class S3Test {

	@Test
	public void sendS3() throws Exception {
		AmazonS3 sqs = mock(AmazonS3.class);
		
		Properties props = new Properties();
		props.put(DummyBean.class.getName(), "email,age");
		
		S3SendQueue<DummyBean> sender = new S3SendQueue<DummyBean>();
		sender.setFields(props);
		sender.setS3(sqs);
		sender.setBucketName("test");
		
		DummyBean test = new DummyBean();
		test.setAge(25);
		test.setEmail("simoneg@apache.org");
		test.setName("Simone");
		SendBatch<DummyBean> batch = sender.startBatch();
		batch.put(test);
		batch.send();
		
		verify(sqs).putObject(eq("test"), any(String.class), any(InputStream.class), any(ObjectMetadata.class));
		verify(sqs, times(1)).putObject(eq("test"), any(String.class), any(InputStream.class), any(ObjectMetadata.class));
		
		ArgumentCaptor<InputStream> captor = ArgumentCaptor.forClass(InputStream.class);
		verify(sqs).putObject(eq("test"), any(String.class), captor.capture(), any(ObjectMetadata.class));
		InputStream instr = captor.getValue();
		assertNotNull("Sent a null request", instr);
		String body = new String(StreamUtils.copyToByteArray(instr));
		
		assertThat(body, startsWith(DummyBean.class.getName() + ";{"));
		assertThat(body, containsString("@entity"));
		assertThat(body, containsString("age"));
		assertThat(body, containsString("email"));
		assertThat(body, not(containsString("name")));
		System.out.println(body);
	}

}
