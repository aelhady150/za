package ma.eai.transverse.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ma.eai.transverse.model.PdfGeneratedDTO;
import ma.eai.transverse.model.PdfGenerator;

import java.util.List;

@Repository
public interface GeneratePdfRepository extends JpaRepository<PdfGenerator, Long>{
//    @Query("SELECT p.nom_file, p.status_rapp, p.rapport FROM PdfGenerator p")
//    List<PdfGenerator> findAllVerifiedDocuments();
	PdfGenerator findById(long idReport);
}