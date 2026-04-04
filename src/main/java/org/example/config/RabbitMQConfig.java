package org.example.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.exchange.dlx}")
    private String dlxExchange;

    @Value("${app.rabbitmq.queue.inventory}")
    private String inventoryQueue;

    @Value("${app.rabbitmq.queue.payment}")
    private String paymentQueue;

    @Value("${app.rabbitmq.queue.notification}")
    private String notificationQueue;

    @Value("${app.rabbitmq.queue.dlq}")
    private String dlq;

    @Value("${app.rabbitmq.routing-key.inventory}")
    private String inventoryRoutingKey;

    @Value("${app.rabbitmq.routing-key.payment}")
    private String paymentRoutingKey;

    @Value("${app.rabbitmq.routing-key.notification}")
    private String notificationRoutingKey;

    @Value("${app.rabbitmq.routing-key.inventory-rollback}")
    private String inventoryRollbackKey;

    @Value("${app.rabbitmq.routing-key.order-failed}")
    private String orderFailedKey;

    @Bean
    public DirectExchange sagaExchange(){
        return new DirectExchange(exchange,true,false);
    }

    @Bean
    public DirectExchange deadLetterExchange(){
        return new DirectExchange(dlxExchange,true,false);
    }

    @Bean
    public Queue inventoryQueue(){
        return QueueBuilder
                .durable(inventoryQueue)
                .withArgument("x-dead-letter-exchange", dlxExchange)
                .withArgument("x-dead-letter-queue", dlq)
                .build();
    }

    @Bean
    public Queue paymentQueue(){
        return QueueBuilder
                .durable(paymentQueue)
                .withArgument("x-dead-letter-exchange", dlxExchange)
                .withArgument("x-dead-letter-queue", dlq)
                .build();
    }

    @Bean
    public Queue notificationQueue(){
        return QueueBuilder
                .durable(notificationQueue)
                .withArgument("x-dead-letter-exchange", dlxExchange)
                .withArgument("x-dead-letter-queue", dlq)
                .build();
    }

    @Bean
    public Queue deadLetterQueue(){
        return QueueBuilder.durable(dlq).build();
    }

    @Bean
    public Binding inventoryBinding(){
        return BindingBuilder
                .bind(inventoryQueue())
                .to(sagaExchange())
                .with(inventoryRoutingKey);
    }

    @Bean
    public Binding paymentBinding(){
        return BindingBuilder
                .bind(paymentQueue())
                .to(sagaExchange())
                .with(paymentRoutingKey);
    }

    @Bean
    public Binding notificationBinding(){
        return BindingBuilder
                .bind(notificationQueue())
                .to(sagaExchange())
                .with(notificationRoutingKey);
    }

    @Bean
    public Binding inventoryRollbackBinding(){
        return BindingBuilder
                .bind(inventoryQueue())
                .to(sagaExchange())
                .with(inventoryRollbackKey);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(dlq);
    }

    @Bean
    public MessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }

    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }

}
