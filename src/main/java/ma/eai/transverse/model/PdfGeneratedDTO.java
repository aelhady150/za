package ma.eai.transverse.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data @NoArgsConstructor @AllArgsConstructor

public class PdfGeneratedDTO {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idPdfGenerated;
    private String nomFile;
    private String resultatVerif;
    private byte[] bytesDoc;

}
