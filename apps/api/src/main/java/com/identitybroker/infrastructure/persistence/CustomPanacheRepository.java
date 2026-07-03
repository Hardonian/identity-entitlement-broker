package com.identitybroker.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.UUID;

/**
 * Generic base repository providing EntityManager access for complex queries
 * beyond what Panache offers out of the box.
 *
 * <p>Extends {@link PanacheRepositoryBase} to retain full Panache convenience
 * methods while exposing the EntityManager for custom JPQL or native queries.
 *
 * @param <T> the entity type
 */
public abstract class CustomPanacheRepository<T>
        implements PanacheRepositoryBase<T, UUID> {

    @PersistenceContext
    protected EntityManager entityManager;

    /**
     * Get the underlying EntityManager for custom query execution.
     */
    public EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Refresh the entity state from the database, discarding uncommitted changes.
     */
    public void refresh(T entity) {
        entityManager.refresh(entity);
    }

    /**
     * Detach the entity from the persistence context.
     */
    public void detach(T entity) {
        entityManager.detach(entity);
    }

    /**
     * Merge the entity back into the persistence context.
     */
    public T merge(T entity) {
        return entityManager.merge(entity);
    }
}
