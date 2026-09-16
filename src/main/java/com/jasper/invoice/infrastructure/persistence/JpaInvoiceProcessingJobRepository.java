package com.jasper.invoice.infrastructure.persistence;

import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaInvoiceProcessingJobRepository
        implements InvoiceProcessingJobRepository {

    private final InvoiceProcessingJobJpaRepository jpaRepository;
    private final InvoiceProcessingJobMapper mapper;

    public JpaInvoiceProcessingJobRepository(
            InvoiceProcessingJobJpaRepository jpaRepository,
            InvoiceProcessingJobMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public InvoiceProcessingJob save(InvoiceProcessingJob job) {
        InvoiceProcessingJobEntity entity =
                mapper.toEntity(job);

        InvoiceProcessingJobEntity saved =
                jpaRepository.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<InvoiceProcessingJob> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<InvoiceProcessingJob> findNextQueuedJobForUpdate() {
        return jpaRepository.findNextQueuedJobForUpdate()
                .map(mapper::toDomain);
    }

    @Override
    public Optional<InvoiceProcessingJob> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<InvoiceProcessingJob> findExpiredProcessingJobs(Instant now) {
        return jpaRepository.findExpiredProcessingJobs(now)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<InvoiceProcessingJob> findAllOrderByCreatedAtDesc() {
        return jpaRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}