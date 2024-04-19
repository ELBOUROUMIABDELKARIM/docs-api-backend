package fr.norsys.docsapi.repository;

import fr.norsys.docsapi.entity.Document;
import fr.norsys.docsapi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID>, PagingAndSortingRepository<Document, UUID>, JpaSpecificationExecutor<Document> {
    Optional<Document> findByChecksum(String checksum);
    Optional<Document> findByName(String filename);
    List<Document> findByUser(User user);
    Page<Document> findByUser(User user, Pageable pageable);
}
