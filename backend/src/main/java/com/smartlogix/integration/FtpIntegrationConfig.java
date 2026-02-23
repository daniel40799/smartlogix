package com.smartlogix.integration;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${smartlogix.integration.ftp.host}")
    private String ftpHost;

    @Value("${smartlogix.integration.ftp.port}")
    private int ftpPort;

    @Value("${smartlogix.integration.ftp.username}")
    private String ftpUsername;

    @Value("${smartlogix.integration.ftp.password}")
    private String ftpPassword;

    @Value("${smartlogix.integration.ftp.remote-directory}")
    private String remoteDirectory;

    @Value("${smartlogix.integration.ftp.local-directory}")
    private String localDirectory;

    @Bean
    public DefaultFtpSessionFactory ftpSessionFactory() {
        DefaultFtpSessionFactory factory = new DefaultFtpSessionFactory();
        factory.setHost(ftpHost);
        factory.setPort(ftpPort);
        factory.setUsername(ftpUsername);
        factory.setPassword(ftpPassword);
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
        synchronizer.setRemoteDirectory(remoteDirectory);
        return synchronizer;
    }

    @Bean
    public FtpInboundFileSynchronizingMessageSource ftpMessageSource(
            FtpInboundFileSynchronizer synchronizer) {
        FtpInboundFileSynchronizingMessageSource source =
                new FtpInboundFileSynchronizingMessageSource(synchronizer);
        source.setLocalDirectory(new File(localDirectory));
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
