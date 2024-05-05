package fr.norsys.docsapi.repository;

import fr.norsys.docsapi.entity.Document;
import fr.norsys.docsapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID>, PagingAndSortingRepository<Document, UUID>, JpaSpecificationExecutor<Document> {
    Optional<Document> findByChecksum(String checksum);
    Optional<Document> findByName(String filename);
    Optional<List<Document>> findByUser(User user);
    @Query("SELECT d FROM Document d JOIN d.permissions p WHERE p.user = :user AND d.user <> :user")
    Optional<List<Document>> findSharedDocumentsForUser(User user);
}
