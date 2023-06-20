package ma.eai.transverse.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data @NoArgsConstructor
public class ReturnOperation {
    private String signatureId;
    private BigInteger serialNumber;
    private Map<String, String> resultlistpso;
    private Map<String, String> resultList;
    private String idOrganizationHezadecimal;
    private LocalDateTime currentDateForCheckRevoc;
    private boolean isRevoked;
    private String algoSignStringWithHash;
    private String longeurAlgoSign;
    //private String dateSignature;
    private boolean qcType;
    private String serialNumberHex;
    private String integriteRes;
    private Date dateSignature;
    private Date debutValidCert;
    private Date finValidCert;
    private boolean qscsc;
    private boolean qcComplianc;
    private boolean checkValid;
    private boolean checkIntact;
    private boolean rootCertificate;
    private String mail1;
    private String mail2;
    private List<String> UsagPrevuListElem;
    private List<String> erreursSingleSign;
    private List<Boolean> erreursAllSignatures;
    private List<Boolean> modifValid;
    private String UsagPrevuString;
}
