package com.ifsp.email.consumers;

import com.ifsp.email.dtos.EmailRecordDto;
import com.ifsp.email.services.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class EmailConsumer {

    private final EmailService emailService;

    public EmailConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "${broker.queue.email.name}")
    public void listenEmailQueue(@Payload EmailRecordDto emailRecordDto) {
        emailService.sendEmail(emailRecordDto);
        System.out.println("Email enviado e registrado: " + emailRecordDto.emailTo());
    }
}
