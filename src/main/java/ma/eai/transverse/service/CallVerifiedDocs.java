package ma.eai.transverse.service;

import ma.eai.transverse.model.PdfGeneratedDTO;
import ma.eai.transverse.model.PdfGenerator;
import ma.eai.transverse.repositories.GeneratePdfRepository;
import ma.eai.transverse.repositories.PdfGeneratedDTORepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CallVerifiedDocs {

    @Autowired
    private PdfGeneratedDTORepository pdfGeneratedDTORepository;
    @Autowired
    private GeneratePdfRepository generatePdfRepository;

    //@GetMapping
    public List<PdfGeneratedDTO> getReportsOfVerifiedDocs(){
        List<PdfGenerator> allVerifiedDocs = generatePdfRepository.findAll();
        
        List<PdfGeneratedDTO> verifiedDocsDTO = new ArrayList<>();
        
        for(PdfGenerator doc:allVerifiedDocs) {
        	PdfGeneratedDTO a = new PdfGeneratedDTO();
        	a.setIdPdfGenerated(doc.getId());
        	a.setNomFile(doc.getNom_file());
        	a.setResultatVerif(doc.getStatus_rapp());
        	a.setBytesDoc(null);
        	
        	verifiedDocsDTO.add(a);
        }
        
        /*List<PdfGeneratedDTO> verifiedDocsDTO = allVerifiedDocs.stream().map(
            pdfGenerator -> {
                // Create a PdfGeneratedDTO object and set the desired variables
                PdfGeneratedDTO dto = new PdfGeneratedDTO();
                dto.setNomFile(pdfGenerator.getNom_file());
                dto.setResultatVerif(pdfGenerator.getStatus_rapp());
                dto.setBytesDoc(pdfGenerator.getRapport());
                return dto;
            }).collect(Collectors.toList());*/
        return verifiedDocsDTO;
    }
    
    public PdfGenerator getReportToDownload(long idReport) {
    	
    	PdfGenerator allData = generatePdfRepository.findById(idReport);
    	byte[] rapport;

    	rapport = allData.getRapport();
    	
    	return allData;
    }

//    public List<PdfGeneratedDTO> getReportsOfVerifiedDocs(){
//        return pdfGeneratedDTORepository.findAll();
//    }
}