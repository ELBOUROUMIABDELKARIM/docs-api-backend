package fr.norsys.docsapi.repository;

import fr.norsys.docsapi.entity.Document;
import fr.norsys.docsapi.entity.PermissionEntry;
import fr.norsys.docsapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PermissionEntryRepository extends JpaRepository<PermissionEntry, UUID> {
    Optional<PermissionEntry> findByDocumentAndUser(Document document, User user);
    boolean existsByDocumentAndUserAndPermission(Document document, User user, PermissionEntry permission);
}
