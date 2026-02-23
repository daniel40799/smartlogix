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

    @Bean
    public Step orderImportStep() {
        return new StepBuilder("orderImportStep", jobRepository)
                .<OrderCsvRecord, Order>chunk(10, transactionManager)
                .reader(orderCsvItemReader(null))
                .processor(orderItemProcessor)
                .writer(orderItemWriter())
                .build();
    }

    @Bean
    public Job orderImportJob() {
        return new JobBuilder("orderImportJob", jobRepository)
                .start(orderImportStep())
                .build();
    }
}
