package joomidang.papersummary.common.config.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {
    // 교환소
    public static final String EXCHANGE = "paper.direct"; //파싱/요약 등 논문 업로드 관련 이벤트를 라우팅하는 중앙 교환소

    //큐 이름
    public static final String PARSING_QUEUE = "paper.parsing"; //파싱을 처리할 Consumer들이 듣는 큐
    public static final String SUMMARY_QUEUE = "paper.summary"; //OpenApi 요약 요청을 처리할 큐
    public static final String COMPLETE_QUEUE = "paper.complete"; //요약 완료 후 사용자 알림 처리를 위한 큐

    //라우팅 키(이벤트 식별자)
    public static final String ROUTING_KEY_PARSING = "PARSING_REQUESTED";
    public static final String ROUTING_KEY_SUMMARY = "SUMMARY_REQUESTED";
    public static final String ROUTING_KEY_COMPLETE = "SUMMARY_COMPLETED";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public DirectExchange paperExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue parsingQueue() {
        return new Queue(PARSING_QUEUE);
    }

    @Bean
    public Queue summaryQueue() {
        return new Queue(SUMMARY_QUEUE);
    }

    @Bean
    public Queue completeQueue() {
        return new Queue(COMPLETE_QUEUE);
    }

    @Bean
    public Binding parsingBinding() {
        return BindingBuilder.bind(parsingQueue())
                .to(paperExchange())
                .with(ROUTING_KEY_PARSING);
    }

    @Bean
    public Binding summaryBinding() {
        return BindingBuilder.bind(summaryQueue())
                .to(paperExchange())
                .with(ROUTING_KEY_SUMMARY);
    }

    @Bean
    public Binding completeBinding() {
        return BindingBuilder.bind(completeQueue())
                .to(paperExchange())
                .with(ROUTING_KEY_COMPLETE);
    }

    @Bean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setDefaultRequeueRejected(false); // 예외 발생 시 메시지를 재큐하지 않음
        return factory;
    }
}
