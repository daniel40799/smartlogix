package com.smartlogix.config;

import com.smartlogix.batch.OrderItemProcessor;
import com.smartlogix.domain.entity.Order;
import com.smartlogix.domain.entity.Tenant;
import com.smartlogix.domain.repository.OrderRepository;
import com.smartlogix.domain.repository.TenantRepository;
import com.smartlogix.dto.OrderCsvRecord;
import com.smartlogix.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final OrderRepository orderRepository;
    private final TenantRepository tenantRepository;
    private final OrderItemProcessor orderItemProcessor;

    /**
     * Creates a step-scoped {@link FlatFileItemReader} that reads order records from a CSV file.
     * <p>
     * The reader skips the header row, uses comma-delimited parsing, and maps each data row to
     * an {@link OrderCsvRecord} via a {@link BeanWrapperFieldSetMapper}. The CSV columns are
     * expected in the order: {@code orderNumber, description, destinationAddress, weight}.
     * The file path is injected from the Spring Batch job parameter {@code filePath} at
     * step-execution time.
     * </p>
     *
     * @param filePath the absolute path to the CSV file, provided as a job parameter
     * @return a configured {@link FlatFileItemReader} for {@link OrderCsvRecord} items
     */
    @Bean
    @StepScope
    public FlatFileItemReader<OrderCsvRecord> orderCsvItemReader(
            @Value("#{jobParameters['filePath']}") String filePath) {
        BeanWrapperFieldSetMapper<OrderCsvRecord> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(OrderCsvRecord.class);

        return new FlatFileItemReaderBuilder<OrderCsvRecord>()
                .name("orderCsvItemReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .delimited()
                .names("orderNumber", "description", "destinationAddress", "weight")
                .fieldSetMapper(fieldSetMapper)
                .build();
    }

    /**
     * Creates an {@link ItemWriter} that persists a chunk of processed {@link Order} entities
     * to the database.
     * <p>
     * Before saving, the writer resolves the active {@link com.smartlogix.domain.entity.Tenant}
     * from {@link TenantContext} and associates it with every order in the chunk. This ensures
     * that all batch-imported orders are correctly scoped to the tenant that triggered the import.
     * </p>
     *
     * @return an {@link ItemWriter} that bulk-saves order entities via {@link OrderRepository}
     * @throws IllegalStateException if {@link TenantContext} is not set or the tenant is not active
     */
    @Bean
    public ItemWriter<Order> orderItemWriter() {
        return items -> {
            UUID tenantId = TenantContext.get();
            if (tenantId == null) {
                throw new IllegalStateException(
                        "TenantContext is not set - cannot write batch orders without a tenant");
            }
            Tenant tenant = tenantRepository.findByIdAndActiveTrue(tenantId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Active tenant not found for id: " + tenantId));
            items.forEach(order -> order.setTenant(tenant));
            orderRepository.saveAll(items);
        };
    }

    /**
     * Defines the single {@link Step} used in the order import job.
     * <p>
     * The step processes records in chunks of 10, combining:
     * <ul>
     *   <li>{@link #orderCsvItemReader(String)} — reads CSV rows from the uploaded file</li>
     *   <li>{@link OrderItemProcessor} — validates and maps rows to {@link Order} entities</li>
     *   <li>{@link #orderItemWriter()} — bulk-saves the mapped entities to PostgreSQL</li>
     * </ul>
     * Each chunk is processed within a single transaction, providing restartability: if a chunk
     * fails mid-way the job can be restarted from the last successfully committed chunk.
     * </p>
     *
     * @return the configured import {@link Step}
     */
    @Bean
    public Step orderImportStep() {
        return new StepBuilder("orderImportStep", jobRepository)
                .<OrderCsvRecord, Order>chunk(10, transactionManager)
                .reader(orderCsvItemReader(null))
                .processor(orderItemProcessor)
                .writer(orderItemWriter())
                .build();
    }

    /**
     * Defines the Spring Batch {@link Job} for bulk order imports.
     * <p>
     * The job consists of a single step ({@link #orderImportStep()}) and is triggered by
     * {@code POST /api/orders/import}. Job parameters (e.g. {@code filePath} and a
     * {@code timestamp} to ensure uniqueness) are passed by
     * {@link com.smartlogix.controller.OrderController#importOrders}.
     * </p>
     *
     * @return the configured {@link Job} bean
     */
    @Bean
    public Job orderImportJob() {
        return new JobBuilder("orderImportJob", jobRepository)
                .start(orderImportStep())
                .build();
    }
}
