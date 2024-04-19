package fr.norsys.docsapi.repository;

import fr.norsys.docsapi.entity.MetaData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetaDataRepository extends JpaRepository<MetaData, Long> {
}
