package ma.eai.transverse.repositories;

import ma.eai.transverse.model.PdfGeneratedDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PdfGeneratedDTORepository extends JpaRepository<PdfGeneratedDTO,Long> {
	
}