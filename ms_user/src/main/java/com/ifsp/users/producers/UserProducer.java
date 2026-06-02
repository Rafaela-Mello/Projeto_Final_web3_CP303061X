package com.ifsp.users.producers;

import com.ifsp.users.dtos.EmailDto;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserProducer {
    private final RabbitTemplate rabbitTemplate;
    private final Queue emailQueue;

    public UserProducer(RabbitTemplate rabbitTemplate, Queue emailQueue) {
        this.rabbitTemplate = rabbitTemplate;
        this.emailQueue = emailQueue;
    }

    public void sendEmail(EmailDto emailDto) {
        rabbitTemplate.convertAndSend(emailQueue.getName(), emailDto);
    }
}
