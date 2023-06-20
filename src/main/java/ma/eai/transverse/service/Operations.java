package ma.eai.transverse.service;

import eu.europa.esig.dss.diagnostic.CertificateWrapper;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.jaxb.XmlObjectModification;
import eu.europa.esig.dss.enumerations.KeyUsageBit;
import eu.europa.esig.dss.enumerations.QCType;
import lombok.SneakyThrows;
import ma.eai.transverse.model.ReturnOperation;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class Operations {

    static Map<List<ReturnOperation>,String> MakeOperations(Iterator<SignatureWrapper> signatureIterator){
        List<Boolean> erreursAllSignatures = new ArrayList<>();
        List<ReturnOperation> listOperationToReturn = new ArrayList<>();
        Map<List<ReturnOperation>,String> listOperationToReturnWithUsagePrevu = new HashMap<>();

        SignatureWrapper signature;
        while (signatureIterator.hasNext()){
            List<String> erreursSingleSign = new ArrayList<>();

            signature = signatureIterator.next();

            String singleSign = signature.getId();

            CertificateWrapper certifSignature = signature.getSigningCertificate();

            BigInteger serialNumber = new BigInteger(signature.getSigningCertificate().getSerialNumber());

            String psoInfo = signature.getSigningCertificate().getCertificateIssuerDN();
            Map<String, String> resultlistpso = new HashMap<>();
            String[] listpso = psoInfo.split(",");
            for (String str : listpso) {
                String[] subelements = str.split("=");
                resultlistpso.put(subelements[0], subelements[1]);
            }

            String signataireInfo = signature.getSigningCertificate().getCertificateDN();
            Map<String, String> resultList = new HashMap<>();
            String[] listSignataire = signataireInfo.split(",");
            for (String str : listSignataire) {
                String[] subList = str.split("=");
                resultList.put(subList[0], subList[1]);
            }

            //hex to ascii
            String asciiIdOrg = null;
            if (resultlistpso.get("2.5.4.97") != null ) {
                String hexString = resultlistpso.get("2.5.4.97");

                // Remove any whitespace and convert the hexadecimal string to uppercase
                hexString = hexString.replaceAll("\\s", "").toUpperCase();
                hexString = hexString.substring(5);

                StringBuilder asciiBuilder = new StringBuilder();
                for (int i = 0; i < hexString.length() - 1; i += 2) {
                    String hexPair = hexString.substring(i, i + 2);
                    int decimalValue = Integer.parseInt(hexPair, 16);
                    char asciiChar = (char) decimalValue;
                    asciiBuilder.append(asciiChar);
                }
                asciiIdOrg = asciiBuilder.toString();
            }
            //fin hex to ascii

            //
            LocalDate currentDate = LocalDate.now();
            LocalTime updateTime = LocalTime.parse("00:00:00"); // Assuming the update time is at midnight
            LocalDateTime currentDateForCheckRevoc = LocalDateTime.of(currentDate, updateTime);

            //
            boolean isRevoked = CheckRevocation.isCertificateRevoked(certifSignature,currentDateForCheckRevoc,resultlistpso.get("CN"));

            String algSign = signature.getSigningCertificate().getDigestAlgoAndValue().getDigestMethod().getName();
            List<CertificateWrapper> certChain = signature.getSigningCertificate().getCertificateChain();
            String algoSignStringWithHash = signature.getSignatureAlgorithm().getName();

            String algoSignString = signature.getDigestAlgorithm().name();
            String longeurAlgoSign = signature.getKeyLengthUsedToSignThisToken();
            if(!algoSignString.equals("SHA256") && !algoSignString.equals("SHA224")){
                erreursSingleSign.add("algorithme de signature invalid : "+algoSignString);
            }else if(!longeurAlgoSign.equals("2048")){
                erreursSingleSign.add("longeur de clé de signature invalide : "+longeurAlgoSign);
            }

            //
            DateFormat dateSignFormat = new SimpleDateFormat("dd/MM/yyy hh:mm:ss XXX");
            Date fisrtDateSign = signature.getClaimedSigningTime();
            String dateSign = dateSignFormat.format(fisrtDateSign);

            //
            boolean qcType=false;
            boolean qcComplianc = signature.getSigningCertificate().isQcCompliance();
            java.util.List<QCType> certQualifie = signature.getSigningCertificate().getQcTypes();
            if(certQualifie.toString().contains("QCT_ESIGN") || certQualifie.toString().contains("QCT_ESEAL")) {
                qcType = true;
            }

            //
            String serialNbr = signature.getSigningCertificate().getSerialNumber();
            BigInteger serialNumberBigInt = new BigInteger(serialNbr);
            String serialNumberHex = serialNumberBigInt.toString(16);

            //
            String integriteRes = null;
            java.util.List<Boolean> modifValid= new ArrayList<>();
            java.util.List<XmlObjectModification> modifications = signature.getPdfSignatureOrFormFillChanges();
            if(!signature.isSignatureValid() || !signature.isSignatureIntact()){
                erreursSingleSign.add("Errer d'integrite , les donnée signé sont modifie");
                //break;
            }else{
                for(XmlObjectModification modif : modifications){
                    if(modif.getAction() != null && modif.getType() !=null &&
                            modif.getAction().toString().equals("CREATION") && modif.getType().toString().equals("Sig")) {
                        modifValid.add(true);
                    } else{
                        modifValid.add(false);
                    }
                }
            }
            if (modifValid.contains(false)) {
                erreursSingleSign.add("Le document a eté modifiées apres la signature ");
                integriteRes = "NON";
            } else {
                integriteRes = "OUI";
            }
            modifValid.clear();

            //
            java.util.List<String> UsagPrevuListElem = new ArrayList();
            java.util.List<KeyUsageBit> keyUsageL = signature.getSigningCertificate().getKeyUsages();
            for (int i = 0; i < keyUsageL.size(); i++) {
                UsagPrevuListElem.add(keyUsageL.get(i).toString());
            }
            if(UsagPrevuListElem.size()!=1 || !UsagPrevuListElem.contains("NON_REPUDIATION") ){
                erreursSingleSign.add("Usage invalide de certificat \"NON REPUDIATION\"");
            }
            String UsagPrevuString = "";
            for (int j = 0; j < UsagPrevuListElem.size(); j++) {
                if (UsagPrevuListElem.get(j) == "NON_REPUDIATION") {
                    UsagPrevuString = UsagPrevuString + "non répudiation";
                } else {
                    UsagPrevuString = UsagPrevuString + UsagPrevuListElem.get(j).toString();
                }
            }

            //
            Date debutValidCert = signature.getSigningCertificate().getNotBefore();
            Date finValidCert = signature.getSigningCertificate().getNotAfter();

            //
            boolean qscsc = signature.getSigningCertificate().isSupportedByQSCD();
            if(!qscsc){
                erreursSingleSign.add("Dispositif de création de signature non qualifiée ");
            }

            //
            java.util.List<CertificateWrapper> certificateChain = signature.getCertificateChain();
            java.util.List<String> validCert = new ArrayList<>();
            validCert.add("85733076978032167898335855330912719962");
            boolean checkValid = false;
            boolean checkIntact = false;
            boolean rootCertificate = false;

            for(CertificateWrapper certW:certificateChain){
                checkValid = certW.isSignatureValid();
                checkIntact = certW.isSignatureIntact();
                if(!checkValid || !checkIntact){
                    erreursSingleSign.add("chaine de certificat invalide");
                    break;
                }
                else if(validCert.contains(certW.getSerialNumber())){
                    rootCertificate = true;
                    break;
                }
                else if(certW.isSelfSigned()){
                    erreursSingleSign.add("Chaine de certificat verfié, mais le prestataire non declaré dans la chaine de confiance nationale ");
                    break;
                }
                else if(certW.getSigningCertificate() == null){
                    erreursSingleSign.add("chaine non complete");
                    certW.getSigningCertificatePublicKey();
                    break;
                }
            }

            //
            String mailString1 = signature.getSigningCertificate().getEmail();
            java.util.List<String> mailList = signature.getSigningCertificate().getSubjectAlternativeNames();

            String mailString2=null;
            if(mailList!=null&&mailList.size()>=1){
                mailString2 = mailList.get(0);
            }

            if(erreursSingleSign.size() == 0){
                erreursAllSignatures.add(true);
            }else{
                erreursAllSignatures.add(false);
            }

            ReturnOperation returnOperation = new ReturnOperation();
            returnOperation.setSignatureId(singleSign);
            returnOperation.setSerialNumber(serialNumber);
            returnOperation.setResultlistpso(resultlistpso);
            returnOperation.setResultList(resultList);
            returnOperation.setIdOrganizationHezadecimal(asciiIdOrg);
            returnOperation.setCurrentDateForCheckRevoc(currentDateForCheckRevoc);
            returnOperation.setRevoked(isRevoked);
            returnOperation.setAlgoSignStringWithHash(algoSignStringWithHash);
            returnOperation.setLongeurAlgoSign(longeurAlgoSign);
            returnOperation.setQcType(qcType);
            returnOperation.setSerialNumberHex(serialNumberHex);
            returnOperation.setIntegriteRes(integriteRes);
            returnOperation.setDateSignature(fisrtDateSign);
            returnOperation.setDebutValidCert(debutValidCert);
            returnOperation.setFinValidCert(finValidCert);
            returnOperation.setQscsc(qscsc);
            returnOperation.setQcComplianc(qcComplianc);
            returnOperation.setCheckIntact(checkIntact);
            returnOperation.setCheckValid(checkValid);
            returnOperation.setRootCertificate(rootCertificate);
            returnOperation.setMail1(mailString1);
            returnOperation.setMail2(mailString2);
            returnOperation.setUsagPrevuListElem(UsagPrevuListElem);
            returnOperation.setErreursSingleSign(erreursSingleSign);
            returnOperation.setErreursAllSignatures(erreursAllSignatures);
            returnOperation.setModifValid(modifValid);
            returnOperation.setUsagPrevuString(UsagPrevuString);


            listOperationToReturn.add(returnOperation);
            //keyUsageL.clear();
        }

        String resultUsagePevu;
        if(!erreursAllSignatures.contains(false)){
            resultUsagePevu="PASSED";
        }else{
            resultUsagePevu="VALIDATION FAILED";
        }

        listOperationToReturnWithUsagePrevu.put(listOperationToReturn,resultUsagePevu);

        return listOperationToReturnWithUsagePrevu;
    }

    @SneakyThrows
    public static String getEmpreintDocument(MultipartFile file){
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = file.getBytes();
        digest.update(bytes, 0, bytes.length);
        byte[] hash = digest.digest();
        String hashString = javax.xml.bind.DatatypeConverter.printHexBinary(hash);
        return hashString;
    }

    public static String getTailleDocument(MultipartFile file){
        long tailleDocVerifie = file.getSize();
        Double taille = tailleDocVerifie / 1024.0;
        DecimalFormat TailleDf = new DecimalFormat("#.##");
        String tailleString = TailleDf.format(taille) + " Ko";
        return tailleString;
    }

    public static Map<String,Integer> getNumerSignatureCachet(Set<SignatureWrapper> allSsignature){
        int NbrSign = 0, NbrSeal = 0;
        Iterator<SignatureWrapper> signSeal = allSsignature.iterator();
        while (signSeal.hasNext()) {
            List<QCType> getsign = signSeal.next().getSigningCertificate().getQcTypes();
            List<String> getsignStr = new ArrayList<>();
            for(QCType s:getsign){
                getsignStr.add(s.toString());
            }
            if (getsignStr.size() != 0) {
                if (getsignStr.contains("QCT_ESIGN")) { NbrSign++;}
                if (getsignStr.contains("QCT_SEAL")) {NbrSeal++;}
            }
        }

        int SignSealInconnue=0;
        if(NbrSign==0 && NbrSeal==0){
            SignSealInconnue = allSsignature.size();
        }

        Map<String,Integer> resultatControle = new HashMap<>();
        resultatControle.put("Signature",NbrSign);
        resultatControle.put("Cachet",NbrSeal);
        resultatControle.put("Inconnue",SignSealInconnue);

        return resultatControle;
    }
}
