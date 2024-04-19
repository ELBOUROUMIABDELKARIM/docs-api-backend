package fr.norsys.docsapi.repository;

import fr.norsys.docsapi.entity.PermissionEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PermissionEntryRepository extends JpaRepository<PermissionEntry, UUID> {
}
