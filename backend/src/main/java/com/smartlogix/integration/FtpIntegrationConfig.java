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

/**
 * Spring Integration configuration for polling an FTP server for incoming shipment manifests.
 * <p>
 * This configuration is activated only when the property
 * {@code smartlogix.integration.ftp.enabled=true} is set in the application environment.
 * It establishes a fixed-delay FTP polling flow that downloads new files from a remote
 * directory to a local staging directory and forwards them to the internal event pipeline.
 * </p>
 */
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

    /**
     * Creates the underlying FTP session factory using the configured host, port, and credentials.
     *
     * @return a {@link DefaultFtpSessionFactory} configured with the FTP server connection details
     */
    @Bean
    public DefaultFtpSessionFactory ftpSessionFactory() {
        DefaultFtpSessionFactory factory = new DefaultFtpSessionFactory();
        factory.setHost(ftpHost);
        factory.setPort(ftpPort);
        factory.setUsername(ftpUsername);
        factory.setPassword(ftpPassword);
        return factory;
    }

    /**
     * Wraps the {@link DefaultFtpSessionFactory} in a {@link CachingSessionFactory} to reuse
     * FTP connections across polling cycles instead of reconnecting on every poll.
     *
     * @param factory the underlying {@link DefaultFtpSessionFactory} to wrap
     * @return a connection-caching {@link SessionFactory} for {@link FTPFile}
     */
    @Bean
    public SessionFactory<FTPFile> cachingFtpSessionFactory(DefaultFtpSessionFactory factory) {
        return new CachingSessionFactory<>(factory);
    }

    /**
     * Configures the {@link FtpInboundFileSynchronizer} that downloads files from the remote
     * FTP directory to the local staging directory.
     * <p>
     * Remote files are not deleted after synchronisation ({@code deleteRemoteFiles = false})
     * so the source system retains its copies.
     * </p>
     *
     * @param sessionFactory the session factory used to open FTP connections
     * @return a configured {@link FtpInboundFileSynchronizer}
     */
    @Bean
    public FtpInboundFileSynchronizer ftpInboundFileSynchronizer(SessionFactory<FTPFile> sessionFactory) {
        FtpInboundFileSynchronizer synchronizer = new FtpInboundFileSynchronizer(sessionFactory);
        synchronizer.setDeleteRemoteFiles(false);
        synchronizer.setRemoteDirectory(remoteDirectory);
        return synchronizer;
    }

    /**
     * Declares the {@link FtpInboundFileSynchronizingMessageSource} that generates a Spring
     * Integration {@link org.springframework.messaging.Message} for each file downloaded by
     * the synchroniser.
     * <p>
     * The local staging directory is created automatically if it does not exist.
     * </p>
     *
     * @param synchronizer the synchroniser responsible for downloading remote files
     * @return a message source emitting {@link File} payload messages for each new file
     */
    @Bean
    public FtpInboundFileSynchronizingMessageSource ftpMessageSource(
            FtpInboundFileSynchronizer synchronizer) {
        FtpInboundFileSynchronizingMessageSource source =
                new FtpInboundFileSynchronizingMessageSource(synchronizer);
        source.setLocalDirectory(new File(localDirectory));
        source.setAutoCreateLocalDirectory(true);
        return source;
    }

    /**
     * Creates the {@link MessageChannel} that carries downloaded FTP file messages to the
     * integration flow handler.
     *
     * @return a {@link DirectChannel} for synchronous, single-subscriber message delivery
     */
    @Bean
    public MessageChannel ftpChannel() {
        return new DirectChannel();
    }

    /**
     * Assembles the end-to-end Spring Integration flow for FTP file ingestion.
     * <p>
     * The flow:
     * <ol>
     *   <li>Polls the FTP message source every 30 seconds.</li>
     *   <li>Sends the downloaded {@link File} to {@code ftpChannel}.</li>
     *   <li>Handles each file — currently logs the file name and simulates a Kafka push.
     *       This is the extension point where real parsing and event production would occur
     *       in a production implementation.</li>
     * </ol>
     * </p>
     *
     * @param ftpMessageSource the polled FTP message source
     * @param ftpChannel       the channel connecting the source to the handler
     * @return the fully wired {@link IntegrationFlow}
     */
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
