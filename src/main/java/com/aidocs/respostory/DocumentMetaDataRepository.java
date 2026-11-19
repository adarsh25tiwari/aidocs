package com.aidocs.respostory;

import com.aidocs.entity.DocumentMetaData;
import org.springframework.ai.document.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentMetaDataRepository extends JpaRepository<DocumentMetaData, UUID> {

    List<DocumentMetaData> findAllByOrderByCreationDateDesc();
}
