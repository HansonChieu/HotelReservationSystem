package com.hanson.hotelreservationsystem.repository;

import com.hanson.hotelreservationsystem.model.WaitlistEntry;
import com.hanson.hotelreservationsystem.model.enums.RoomType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Logger;

public class WaitlistRepository {
    private static final Logger LOGGER = Logger.getLogger(WaitlistRepository.class.getName());
    private static WaitlistRepository instance;
    private EntityManager entityManager;

    private WaitlistRepository() {}

    public static synchronized WaitlistRepository getInstance() {
        if (instance == null) instance = new WaitlistRepository();
        return instance;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void save(WaitlistEntry entry) {
        try {
            entityManager.getTransaction().begin();
            if (entry.getId() == null) {
                entityManager.persist(entry);
            } else {
                entityManager.merge(entry);
            }
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            throw e;
        }
    }

    public List<WaitlistEntry> findAll() {
        return entityManager.createQuery("SELECT w FROM WaitlistEntry w ORDER BY w.addedAt DESC", WaitlistEntry.class)
                .getResultList();
    }

    public void delete(WaitlistEntry entry) {
        try {
            entityManager.getTransaction().begin();
            // Ensure entity is attached before removing
            WaitlistEntry toRemove = entityManager.contains(entry) ? entry : entityManager.merge(entry);
            entityManager.remove(toRemove);
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            throw e;
        }
    }
}