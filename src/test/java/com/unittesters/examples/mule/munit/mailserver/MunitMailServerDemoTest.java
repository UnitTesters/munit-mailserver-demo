package com.unittesters.examples.mule.munit.mailserver;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.munit.MailServer;
import org.mule.munit.runner.functional.FunctionalMunitSuite;
import org.mule.transport.email.MailUtils;

public class MunitMailServerDemoTest extends FunctionalMunitSuite {
	
	private MailServer mailServer = new MailServer();
	
	@Override
	protected String getConfigResources() {
		return "munit-mailserver-demo.xml";
	}


	@Before
	public void setUp(){
		mailServer.start();
	}
	
	@After
	public void tearDown(){
		mailServer.stop();
	}

	@Test
	public void shouldSendEmail() throws Exception{
		String payload = "Welcome to UnitTesters.com";
		
		MuleEvent muleEvent = testEvent(payload);
		
		runFlow("subflow-mail-sender", muleEvent);
		
		List<MimeMessage> mails = mailServer.getReceivedMessages();
		
		assertThat(mails)
			.isNotEmpty()
			.hasSize(2);
		
		assertThat(mails)
			.allSatisfy(mimeMessage -> { 
						// Let's inspect our message
						try {
							String toAddr = MailUtils.mailAddressesToString(mimeMessage.getRecipients(RecipientType.TO));
							String ccAddr = MailUtils.mailAddressesToString(mimeMessage.getRecipients(RecipientType.CC));
							
							assertThat(toAddr).as("To Address").isEqualTo("to@example.com");
							assertThat(ccAddr).as("CC Address").isEqualTo("cc@example.com");
							assertThat(mimeMessage.getSubject()).as("Subject").isEqualTo("Welcome to UT");
							assertThat(mimeMessage.getContent().toString().trim()).as("Mail Body").isEqualTo(payload);
							
						} catch (MessagingException | IOException e) {
							fail("Unable to fetch data from Mail", e);
						}
					}
				);
		
	}
	
	@Test
	public void shouldSendEmailWithAttachment() throws Exception{
		String payload = "Welcome to UnitTesters.com";
		
		MuleEvent muleEvent = testEvent(payload);
		
		runFlow("subflow-attachment-mail-sender", muleEvent);
		
		List<MimeMessage> mails = mailServer.getReceivedMessages();
		
		assertThat(mails)
			.isNotEmpty()
			.hasSize(2);
		
		assertThat(mails)
			.allSatisfy(mimeMessage -> { 
						// Verify the attachment
						try {
							
							MimeBodyPart attachment = getAttachments(mimeMessage, "test.txt");
							assertThat(attachment).isNotNull();
							assertThat(attachment.getContentType()).startsWith("text/plain");
							assertThat(attachment.getContent().toString().trim()).isEqualTo(payload);
							
							//Just to test non-existent attachment.
							MimeBodyPart attachment2 = getAttachments(mimeMessage, "test2.txt");
							assertThat(attachment2).isNull();
							
						} catch (MessagingException | IOException e) {
							fail("Unable to fetch data from Mail", e);
						}
					}
				);
		
	}
	
	public MimeBodyPart getAttachments(MimeMessage msg, String attchmentName) throws MessagingException, IOException{
		Map<String, Part> attachments = new HashMap<String, Part>();
		MailUtils.getAttachments((Multipart)msg.getContent(), attachments);
		
		return (MimeBodyPart) attachments.get(attchmentName);
		
	}
	
}
