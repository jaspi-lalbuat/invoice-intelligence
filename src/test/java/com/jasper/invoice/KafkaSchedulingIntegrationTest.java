package com.jasper.invoice;

import com.jasper.invoice.application.InvoiceProcessingPoller;
import com.jasper.invoice.application.InvoiceProcessingRecoveryPoller;
import com.jasper.invoice.application.OutboxPublishingPoller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        properties = "spring.task.scheduling.enabled=false"
)
@TestPropertySource(properties = {
        "invoice.processing.mode=kafka"
})
class KafkaSchedulingIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private OutboxPublishingPoller outboxPublishingPoller;

    @Autowired
    private InvoiceProcessingRecoveryPoller recoveryPoller;

    @Test
    void shouldLoadKafkaModeSchedulers() {
        assertNotNull(outboxPublishingPoller);
        assertNotNull(recoveryPoller);

        assertThrows(
                NoSuchBeanDefinitionException.class,
                () -> applicationContext.getBean(InvoiceProcessingPoller.class)
        );
    }
}