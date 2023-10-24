package org.synyx.urlaubsverwaltung.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.user.UserSettingsService;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailServiceImplTest {

    private MailServiceImpl sut;

    @Mock
    private MessageSource messageSource;
    @Mock
    private ITemplateEngine emailTemplateEngine;
    @Mock
    private MailSenderService mailSenderService;
    @Mock
    private MailProperties mailProperties;
    @Mock
    private UserSettingsService userSettingsService;

    @BeforeEach
    void setUp() {
        when(messageSource.getMessage(any(), any(), any())).thenReturn("subject");
        when(emailTemplateEngine.process(any(String.class), any(Context.class))).thenReturn("emailBody");
        when(mailProperties.getSender()).thenReturn("no-reply@example.org");
        when(mailProperties.getSenderDisplayName()).thenReturn("Urlaubsverwaltung");
        when(mailProperties.getApplicationUrl()).thenReturn("http://localhost:8080");
        sut = new MailServiceImpl(messageSource, emailTemplateEngine, mailSenderService, mailProperties, userSettingsService);
    }

    @Test
    void sendMailToWithPerson() {

        setupMockServletRequest();

        final Person hans = new Person();
        hans.setEmail("hans@example.org");

        final String subjectMessageKey = "subject.overtime.created";
        final String templateName = "overtime_office";

        final Mail mail = Mail.builder()
            .withRecipient(hans)
            .withSubject(subjectMessageKey)
            .withTemplate(templateName, locale -> new HashMap<>())
            .build();

        sut.send(mail);

        verify(mailSenderService).sendEmail("Urlaubsverwaltung <no-reply@example.org>", "hans@example.org", "subject", "emailBody");
    }

    @Test
    void sendMailToEachPerson() {

        setupMockServletRequest();

        final Person hans = new Person();
        hans.setEmail("hans@example.org");

        final Person franz = new Person();
        franz.setEmail("franz@example.org");
        final List<Person> persons = asList(hans, franz);

        final String subjectMessageKey = "subject.overtime.created";
        final String templateName = "overtime_office";

        final Mail mail = Mail.builder()
            .withRecipient(persons)
            .withSubject(subjectMessageKey)
            .withTemplate(templateName, locale -> new HashMap<>())
            .build();

        sut.send(mail);

        verify(mailSenderService).sendEmail("Urlaubsverwaltung <no-reply@example.org>", "hans@example.org", "subject", "emailBody");
        verify(mailSenderService).sendEmail("Urlaubsverwaltung <no-reply@example.org>", "franz@example.org", "subject", "emailBody");
    }

    @Test
    void sendMailWithAttachment() {

        setupMockServletRequest();

        final Person hans = new Person();
        hans.setEmail("hans@example.org");

        final Person franz = new Person();
        franz.setEmail("franz@example.org");
        final List<Person> persons = asList(hans, franz);

        final String subjectMessageKey = "subject.overtime.created";
        final String templateName = "overtime_office";

        final ByteArrayResource iCal = new ByteArrayResource(new byte[]{}, "calendar.ics");

        final Mail mail = Mail.builder()
            .withRecipient(persons)
            .withSubject(subjectMessageKey)
            .withTemplate(templateName, locale -> new HashMap<>())
            .withAttachment("fileName", iCal)
            .build();

        sut.send(mail);

        verify(mailSenderService).sendEmail("Urlaubsverwaltung <no-reply@example.org>", "franz@example.org", "subject", "emailBody", List.of(new MailAttachment("fileName", iCal)));
        verify(mailSenderService).sendEmail("Urlaubsverwaltung <no-reply@example.org>", "hans@example.org", "subject", "emailBody", List.of(new MailAttachment("fileName", iCal)));
    }

    @Test
    void sendMailWithAttachmentToEachPerson() {

        setupMockServletRequest();

        final Person hans = new Person();
        hans.setEmail("hans@example.org");

        final Person franz = new Person();
        franz.setEmail("franz@example.org");
        final List<Person> persons = asList(hans, franz);

        final String subjectMessageKey = "subject.overtime.created";
        final String templateName = "overtime_office";

        final ByteArrayResource iCal = new ByteArrayResource(new byte[]{}, "calendar.ics");

        final Mail mail = Mail.builder()
            .withRecipient(persons)
            .withSubject(subjectMessageKey)
            .withTemplate(templateName, locale -> new HashMap<>())
            .withAttachment("fileName", iCal)
            .build();

        sut.send(mail);

        verify(mailSenderService).sendEmail("Urlaubsverwaltung <no-reply@example.org>", "hans@example.org", "subject", "emailBody", List.of(new MailAttachment("fileName", iCal)));
        verify(mailSenderService).sendEmail("Urlaubsverwaltung <no-reply@example.org>", "franz@example.org", "subject", "emailBody", List.of(new MailAttachment("fileName", iCal)));
    }

    @Test
    void sendTechnicalMail() {

        setupMockServletRequest();

        final String subjectMessageKey = "subject.overtime.created";
        final String templateName = "overtime_office";
        String to = "admin@example.org";
        when(mailProperties.getAdministrator()).thenReturn(to);

        final Mail mail = Mail.builder()
            .withTechnicalRecipient(true)
            .withSubject(subjectMessageKey)
            .withTemplate(templateName, locale -> new HashMap<>())
            .build();
        sut.send(mail);

        verify(mailSenderService).sendEmail("Urlaubsverwaltung <no-reply@example.org>", to, "subject", "emailBody");
    }

    @Test
    void sendMailToWithPersonsAndAdministrator() {

        when(mailProperties.getAdministrator()).thenReturn("admin@example.org");

        setupMockServletRequest();

        final Person franz = new Person();
        franz.setEmail("franz@example.org");

        final String subjectMessageKey = "subject.overtime.created";
        final String templateName = "overtime_office";

        final Mail mail = Mail.builder()
            .withTechnicalRecipient(true)
            .withRecipient(List.of(franz))
            .withSubject(subjectMessageKey)
            .withTemplate(templateName, locale -> new HashMap<>())
            .build();

        sut.send(mail);

        verify(mailSenderService).sendEmail("Urlaubsverwaltung <no-reply@example.org>", "franz@example.org", "subject", "emailBody");
        verify(mailSenderService).sendEmail("Urlaubsverwaltung <no-reply@example.org>", "admin@example.org", "subject", "emailBody");
    }

    @Test
    void ensureDistinctRecipientsForSendMail() {

        setupMockServletRequest();

        final Person franz = new Person();
        franz.setEmail("franz@example.org");

        final String subjectMessageKey = "subject.overtime.created";
        final String templateName = "overtime_office";

        final Mail mail = Mail.builder()
            .withRecipient(List.of(franz, franz))
            .withSubject(subjectMessageKey)
            .withTemplate(templateName, locale -> new HashMap<>())
            .build();

        sut.send(mail);

        verify(mailSenderService).sendEmail("Urlaubsverwaltung <no-reply@example.org>", "franz@example.org", "subject", "emailBody");
        verifyNoMoreInteractions(mailSenderService);
    }

    private void setupMockServletRequest() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
