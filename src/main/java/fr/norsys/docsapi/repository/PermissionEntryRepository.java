package fr.norsys.docsapi.repository;

import fr.norsys.docsapi.entity.Document;
import fr.norsys.docsapi.entity.PermissionEntry;
import fr.norsys.docsapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PermissionEntryRepository extends JpaRepository<PermissionEntry, UUID> {
    void deleteByDocumentAndUser(Document document, User user);
}
