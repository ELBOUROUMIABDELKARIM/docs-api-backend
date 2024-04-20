package fr.norsys.docsapi.specifications;

import fr.norsys.docsapi.entity.Document;
import fr.norsys.docsapi.entity.MetaData;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Aymane
 */
public class DocumentSpecifications {
    public static Specification<Document> search(String searchValue) {
        return (root, query, cb) -> {
            Predicate docName = cb.like(root.get("name"), "%" + searchValue + "%");
            Predicate type = cb.like(root.get("type"), "%" + searchValue + "%");
            Join<Document, MetaData> metadataJoin = root.join("metadata"); // Ensure LEFT JOIN for metadata
            Predicate predicateKey = null;
            Predicate predicateValue = null;
            if (searchValue.contains(":")) {
                String[] parts = searchValue.split(":", 2);
                predicateKey = cb.equal(metadataJoin.get("key"), parts[0]);
                predicateValue = cb.equal(metadataJoin.get("value"), parts[1]);
            }
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate localDate = LocalDate.parse(searchValue, formatter);
                Predicate creationDatePredicate = cb.equal(root.get("creationDate").as(LocalDate.class), localDate);
                if (predicateKey != null && predicateValue != null) {
                    return cb.or(docName, type, creationDatePredicate, cb.and(predicateKey, predicateValue));
                } else {
                    return cb.or(docName, type, creationDatePredicate);
                }
            } catch (DateTimeParseException e) {
                if (predicateKey != null && predicateValue != null) {
                    return cb.or(docName, type, cb.and(predicateKey, predicateValue));
                } else {
                    return cb.or(docName, type);
                }
            }
        };
    }

}