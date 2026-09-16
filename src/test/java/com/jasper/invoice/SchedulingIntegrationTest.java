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
        "invoice.processing.mode=poller"
})
class SchedulingIntegrationTest {

    @Autowired
    private InvoiceProcessingPoller processingPoller;

    @Autowired
    private InvoiceProcessingRecoveryPoller recoveryPoller;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldLoadBothProcessingSchedulers() {
        assertNotNull(processingPoller);
        assertNotNull(recoveryPoller);
    }

    @Test
    void shouldLoadPollerModeSchedulers() {
        assertNotNull(processingPoller);
        assertNotNull(recoveryPoller);

        assertThrows(
                NoSuchBeanDefinitionException.class,
                () -> applicationContext.getBean(OutboxPublishingPoller.class)
        );
    }
}