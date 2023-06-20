package ma.eai.transverse.service;

import eu.europa.esig.dss.diagnostic.CertificateWrapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.cert.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class CheckRevocation {

    private static Map<String,String> pscoWithcrls = new HashMap<>();
    static {
       // pscoWithcrls.put("EAI AC Infrastructures", "http://SRVPKICA:8080/EAI%20AC%20Infrastructures.crl");
        pscoWithcrls.put("AC Utilisateurs Physiques", "http://crl.eurafric-information.com/pkipub/Eurafric_Trust_Root_CA.crl");
        pscoWithcrls.put("AC Utilisateurs Physiques", "http://crl.eurafric-information.com/pkipub/AC_Utilisateurs_Physiques.crl");
        pscoWithcrls.put("Baridesign AC Classe 3", "http://psce.baridesign.ma/ACDeleguees_Externes/crl/Classe3__AC-crl-2.crl");
    }

    private static Map<String,LocalDateTime> crlWithNextUpdateDate = new HashMap<>();


    private static final String crlLien = "http://crl.eurafric-information.com/pkipub/AC_Utilisateurs_Physiques.crl";
    static List<? extends X509CRLEntry> revokedCertificates;
    //static List<LocalDateTime> nextUpdateDateTimeList = new ArrayList<>();
    //static LocalDateTime nextUpdateDateTime;


    @PostConstruct
    public static void initializeCRLlist() {
        try {
            downloadCRL();
            System.out.println("CRL loaded successfully.");
        } catch (IOException | CertificateException | CRLException e) {
            System.err.println("Failed to load CRL: " + e.getMessage());
        }
    }
    public static X509CRL downloadCRL() throws IOException, CertificateException, CRLException {

        X509CRL crl=null;
        for (String lienCRL:pscoWithcrls.values()){
            URL crlURL = new URL(lienCRL);
            InputStream crlStream = crlURL.openStream();
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            crl = (X509CRL) cf.generateCRL(crlStream);

            revokedCertificates = new ArrayList<>(crl.getRevokedCertificates());
            LocalDateTime nextUpdateDateTime = calculateNextUpdateDateTime(crl.getNextUpdate());

            //nextUpdateDateTimeList.add(nextUpdateDateTime);
            crlWithNextUpdateDate.put(lienCRL,nextUpdateDateTime);
            crlStream.close();
        }
        return crl;
    }
    public static X509CRL downloadSingleCRL(String nomPscoCN) throws IOException, CertificateException, CRLException {
        X509CRL crl=null;
        for (String lienCRL:pscoWithcrls.keySet()){
            if(lienCRL.equals(nomPscoCN)){
                URL crlURL = new URL(lienCRL);
                InputStream crlStream = crlURL.openStream();
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                crl = (X509CRL) cf.generateCRL(crlStream);

                revokedCertificates = new ArrayList<>(crl.getRevokedCertificates());

                LocalDateTime nextUpdateDateTime = calculateNextUpdateDateTime(crl.getNextUpdate());

                crlWithNextUpdateDate.put(lienCRL,nextUpdateDateTime);

                crlStream.close();
            }else{
                System.out.println("lienCRL is not equal nom PSCO");
            }
        }
        return crl;
    }

//    @Scheduled(fixedDelay = 60000) // Check for update every minute (adjust the delay as needed)
//    public void checkForUpdate() {
//        if(crlWithNextUpdateDate.values()!=null){
//            if (Collections.min(crlWithNextUpdateDate.values()).isBefore(LocalDateTime.now())) {
//                try {
//                    downloadCRL();
//                    System.out.println("CRL updated successfully.");
//                } catch (IOException | CertificateException | CRLException e) {
//                    System.err.println("Failed to update CRL: " + e.getMessage());
//                }
//            }
//        }
//
//    }
    static LocalDateTime calculateNextUpdateDateTime(Date nextUpdate) {
        LocalDate currentDate = LocalDate.now();
        LocalTime updateTime = LocalTime.parse("00:00:00"); // Assuming the update time is at midnight
        LocalDateTime nextUpdateDateTime = nextUpdate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime nextUpdateDateTimeWithTime = LocalDateTime.of(currentDate, updateTime).with(nextUpdateDateTime.toLocalTime());

        if (nextUpdateDateTimeWithTime.isBefore(LocalDateTime.now())) {
            return nextUpdateDateTimeWithTime.plusDays(1);
        } else {
            return nextUpdateDateTimeWithTime;
        }
    }
    public static boolean isCertificateRevoked(CertificateWrapper certificate,LocalDateTime currentDateForCheckRevoc,String orgName) {

        boolean resultControle = true;
        for (Map.Entry<String, LocalDateTime> entry : crlWithNextUpdateDate.entrySet()) {
            String key = entry.getKey();
            LocalDateTime value = entry.getValue();

            //System.out.println("param :"+orgName);

            if (pscoWithcrls.keySet().contains(orgName)) {
                if (currentDateForCheckRevoc.isBefore(value)) {
                    if(revokedCertificates.contains(certificate)){
                        resultControle = true;
                        //System.out.println("certificat appartient a un prestataire de service de confiance, est revoké");
                    }else{
                        resultControle = false;
                        //System.out.println("certificat appartient a un prestataire de service de confiance, est pas revoké");
                    }
                }
                else {
                    try {
                        downloadSingleCRL(key);
                        System.out.println("CRL updated successfully.");
                    } catch (IOException | CertificateException | CRLException e) {
                        System.err.println("Failed to update CRL: " + e.getMessage());
                    }
                    resultControle = revokedCertificates.contains(certificate);
                }
            }else{
                //System.out.println("local :"+pscoWithcrls.keySet());
                System.out.println("Certificat ne provienne pas d'un prestatire de service de confiance");
            }
        }

        return resultControle;
//        if(currentDateForCheckRevoc.isBefore(Collections.min(nextUpdateDateTimeList))){
//            return revokedCertificates.contains(certificate);
//        }else{
//            try {
//                downloadCRL();
//                System.out.println("CRL updated successfully.");
//            } catch (IOException | CertificateException | CRLException e) {
//                System.err.println("Failed to update CRL: " + e.getMessage());
//            }
//            return revokedCertificates.contains(certificate);
//        }
    }

    public static boolean iscertificateRevokedCRL(BigInteger serialNbr,CertificateWrapper certificat) throws IOException, CertificateException, CRLException {

        List<String> listlinksCRL = certificat.getCRLDistributionPoints();
        Map<BigInteger,X509CRLEntry> resultCRL = new HashMap<>();
        X509CRL crl = null;

        for(String singlCRL : listlinksCRL){

            X509CRLEntry isRevoked = crl.getRevokedCertificate(serialNbr);

            //listRevokedCert = crl.getRevokedCertificates();

            Date dateRev = crl.getRevokedCertificate(serialNbr).getRevocationDate();

            resultCRL.put(serialNbr,isRevoked);

            System.out.println(isRevoked);


        }
        System.out.println(resultCRL);

        return false;
    }

}
