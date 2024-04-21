package fr.norsys.docsapi.repository;

import fr.norsys.docsapi.entity.Document;
import fr.norsys.docsapi.entity.PermissionEntry;
import fr.norsys.docsapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionEntryRepository extends JpaRepository<PermissionEntry, UUID> {
    Optional<List<PermissionEntry>> findByDocumentAndUser(Document document, User user);
    void deleteByDocumentAndUser(Document document, User user);
}
