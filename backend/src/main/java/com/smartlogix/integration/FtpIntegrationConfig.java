package com.smartlogix.integration;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizer;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizingMessageSource;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.messaging.MessageChannel;

import java.io.File;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "smartlogix.integration.ftp.enabled", havingValue = "true")
public class FtpIntegrationConfig {

    @Bean
    public DefaultFtpSessionFactory ftpSessionFactory() {
        DefaultFtpSessionFactory factory = new DefaultFtpSessionFactory();
        factory.setHost("localhost");
        factory.setPort(21);
        factory.setUsername("anonymous");
        factory.setPassword("");
        return factory;
    }

    @Bean
    public SessionFactory<FTPFile> cachingFtpSessionFactory(DefaultFtpSessionFactory factory) {
        return new CachingSessionFactory<>(factory);
    }

    @Bean
    public FtpInboundFileSynchronizer ftpInboundFileSynchronizer(SessionFactory<FTPFile> sessionFactory) {
        FtpInboundFileSynchronizer synchronizer = new FtpInboundFileSynchronizer(sessionFactory);
        synchronizer.setDeleteRemoteFiles(false);
        synchronizer.setRemoteDirectory("/tmp/ftp-shipments");
        return synchronizer;
    }

    @Bean
    public FtpInboundFileSynchronizingMessageSource ftpMessageSource(
            FtpInboundFileSynchronizer synchronizer) {
        FtpInboundFileSynchronizingMessageSource source =
                new FtpInboundFileSynchronizingMessageSource(synchronizer);
        source.setLocalDirectory(new File("/tmp/local-ftp"));
        source.setAutoCreateLocalDirectory(true);
        return source;
    }

    @Bean
    public MessageChannel ftpChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow ftpIntegrationFlow(
            FtpInboundFileSynchronizingMessageSource ftpMessageSource,
            MessageChannel ftpChannel) {
        return IntegrationFlow
                .from((MessageSource<File>) ftpMessageSource,
                        c -> c.poller(p -> p.fixedDelay(30_000)))
                .channel(ftpChannel)
                .handle(message -> {
                    File file = (File) message.getPayload();
                    log.info("FTP file received: {}, simulating Kafka push", file.getName());
                })
                .get();
    }
}
