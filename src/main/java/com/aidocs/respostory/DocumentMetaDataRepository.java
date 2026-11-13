package com.aidocs.respostory;

import com.aidocs.entity.DocumentMetaData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface DocumentMetaDataRepository extends JpaRepository<DocumentMetaData, UUID> {

}
