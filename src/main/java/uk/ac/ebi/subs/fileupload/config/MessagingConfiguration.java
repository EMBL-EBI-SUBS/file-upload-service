package uk.ac.ebi.subs.fileupload.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import uk.ac.ebi.subs.messaging.ExchangeConfig;
import uk.ac.ebi.subs.messaging.Queues;
import uk.ac.ebi.subs.messaging.Topics;

/**
 * This is a {@link Configuration} class responsible for configuring message related {@link Bean}s.
 */
@Configuration
@ComponentScan(basePackageClasses = ExchangeConfig.class)
public class MessagingConfiguration {

    public static final String USI_FILE_DELETION_QUEUE = "usi-file-deletion";
    private static final String EVENT_FILE_DELETION = "usi.file.deletion";

    public static final String FU_GLOBUS_SUB_UNREGISTER = "usi-fu-globus-sub-unregister";

    @Bean
    public MessageConverter messageConverter() {
        return jackson2Converter();
    }

    @Bean
    public MappingJackson2MessageConverter jackson2Converter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();

        ObjectMapper objectMapper = converter.getObjectMapper();

        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        return converter;
    }

    @Bean
    public RabbitMessagingTemplate rabbitMessagingTemplate(RabbitTemplate rabbitTemplate) {
        RabbitMessagingTemplate rmt = new RabbitMessagingTemplate(rabbitTemplate);
        rmt.setMessageConverter(this.jackson2Converter());
        return rmt;
    }

    /**
     * Instantiate a {@link Queue} for file deletion messages.
     *
     * @return an instance of a {@link Queue} for file deletion messages.
     */
    @Bean
    Queue fileDeletionQueue() {
        return Queues.buildQueueWithDlx(USI_FILE_DELETION_QUEUE);
    }

    /**
     * Create a {@link Binding} between the exchange and file deletion queue
     * using its routing key.
     *
     * @param fileDeletionQueue {@link Queue} for generating file checksum
     * @param submissionExchange {@link TopicExchange}
     * @return a {@link Binding} between the exchange and the file deletion queue
     * using the routing key of the file deletion message.
     */
    @Bean
    Binding fileDeletionBinding(Queue fileDeletionQueue, TopicExchange submissionExchange) {
        return BindingBuilder.bind(fileDeletionQueue).to(submissionExchange)
                .with(EVENT_FILE_DELETION);
    }

    @Bean
    Queue fuGlobusSubUnregisterQueue() {
        return Queues.buildQueueWithDlx(FU_GLOBUS_SUB_UNREGISTER);
    }

    @Bean
    Binding submissionSubmittedQueueBinding(Queue fuGlobusSubUnregisterQueue, TopicExchange submissionExchange) {
        return BindingBuilder.bind(fuGlobusSubUnregisterQueue).to(submissionExchange)
                .with(Topics.EVENT_SUBMISSION_SUBMITTED);
    }
}
