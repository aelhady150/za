package ma.eai.transverse.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import eu.europa.esig.dss.diagnostic.*;
import eu.europa.esig.dss.diagnostic.jaxb.XmlObjectModification;
import eu.europa.esig.dss.enumerations.*;
import eu.europa.esig.dss.validation.*;
import ma.eai.transverse.model.PdfGeneratedDTO;
import ma.eai.transverse.model.PdfGenerator;
import ma.eai.transverse.service.CallVerifiedDocs;
import org.apache.commons.collections.ArrayStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.text.DocumentException;

import eu.europa.esig.dss.diagnostic.jaxb.XmlDiagnosticData;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.MimeType;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.CommonCertificateSource;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.executor.ValidationLevel;
import eu.europa.esig.dss.validation.reports.Reports;
import ma.eai.transverse.WebAppUtils;
import ma.eai.transverse.editor.EnumPropertyEditor;
import ma.eai.transverse.exception.SourceNotFoundException;
import ma.eai.transverse.model.ValidationForm;
import ma.eai.transverse.service.FOPService;
import ma.eai.transverse.service.GenerateReportService;

@RestController
@SessionAttributes({ "simpleReportXml", "detailedReportXml", "diagnosticDataXml" })
@RequestMapping(value = "/validation")
//@CrossOrigin("*")
@CrossOrigin(origins = "http://localhost:4200")
public class ValidationController extends AbstractValidationController {

	private static final Logger LOG = LoggerFactory.getLogger(ValidationController.class);

	private static final String VALIDATION_TILE = "validation";
	private static final String VALIDATION_RESULT_TILE = "validation-result";

	private static final String[] ALLOWED_FIELDS = { "signedFile", "originalFiles[*].*", "digestToSend", "validationLevel", "defaultPolicy",
			"policyFile", "signingCertificate", "adjunctCertificates", "includeCertificateTokens", "includeTimestampTokens", "includeRevocationTokens",
			"includeUserFriendlyIdentifiers", "includeSemantics" };
	
	@Autowired
	private GenerateReportService generateReportService;
	@Autowired
	private CallVerifiedDocs callVerifiedDocs;
	
	@Autowired
	private FOPService fopService;
	static ByteArrayInputStream fileFromService;
	
	String tailleDocVerif;

	@Autowired
	private Resource defaultPolicy;

	@Autowired
	protected SignaturePolicyProvider signaturePolicyProvider;	

	@InitBinder
	public void initBinder(WebDataBinder webDataBinder) {
		webDataBinder.registerCustomEditor(ValidationLevel.class, new EnumPropertyEditor(ValidationLevel.class));
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder webDataBinder) {
		webDataBinder.setAllowedFields(ALLOWED_FIELDS);
	}

	@RequestMapping(method = RequestMethod.GET)
	public String showValidationForm(Model model, HttpServletRequest request) {
		ValidationForm validationForm = new ValidationForm();
		validationForm.setValidationLevel(ValidationLevel.ARCHIVAL_DATA);
		validationForm.setDefaultPolicy(true);
		model.addAttribute("validationForm", validationForm);
		return VALIDATION_TILE;
	}

	@PostMapping("/rest-validate")
	public Reports validate(@RequestParam(value = "file", required = false) MultipartFile file, 
						   HttpServletRequest request) throws IOException, NoSuchAlgorithmException, DocumentException  {
		LOG.trace("Validation BEGINS...");
		ValidationForm validationForm = new ValidationForm();
		validationForm.setDefaultPolicy(true);
		validationForm.setValidationLevel(ValidationLevel.ARCHIVAL_DATA);
		validationForm.setSignedFile(file);

		SignedDocumentValidator documentValidator = SignedDocumentValidator
				.fromDocument(WebAppUtils.toDSSDocument(validationForm.getSignedFile()));
		documentValidator.setCertificateVerifier(getCertificateVerifier(validationForm));
		documentValidator.setTokenExtractionStrategy(TokenExtractionStrategy.fromParameters(validationForm.isIncludeCertificateTokens(),
				validationForm.isIncludeTimestampTokens(), validationForm.isIncludeRevocationTokens()));
		documentValidator.setIncludeSemantics(validationForm.isIncludeSemantics());
		documentValidator.setSignaturePolicyProvider(signaturePolicyProvider);

		TokenIdentifierProvider identifierProvider = validationForm.isIncludeUserFriendlyIdentifiers() ?
				new UserFriendlyIdentifierProvider() : new OriginalIdentifierProvider();
		documentValidator.setTokenIdentifierProvider(identifierProvider);

		setSigningCertificate(documentValidator, validationForm);
		setDetachedContents(documentValidator, validationForm);

		Locale locale = request.getLocale();
		LOG.trace("Requested locale : {}", locale);
		if (locale == null) {
			locale =  Locale.getDefault();
			LOG.warn("The request locale is null! Use the default one : {}", locale);
		}
		documentValidator.setLocale(locale);

		Reports reports = validate(documentValidator, validationForm);

		return reports;
	}

	@CrossOrigin(origins = "http://localhost:4200")
	@PostMapping("/pdf-generate")
	public PdfGeneratedDTO generatePdf(@RequestParam(value = "file", required = false) MultipartFile file,
									HttpServletRequest request) throws IOException, NoSuchAlgorithmException, DocumentException, CertificateException, CRLException {
		Reports reports = validate(file, request);

		PdfGenerator pdfRes = this.generateReportService.export(reports,file);

		fileFromService = new ByteArrayInputStream(pdfRes.getRapport());

//		Set<SignatureWrapper> allSignatures = reports.getDiagnosticData().getAllSignatures();
//		Iterator<SignatureWrapper> iterator = allSignatures.iterator();
//		while (iterator.hasNext()){
//			SignatureWrapper signature = iterator.next();
//			System.out.println(signature.getSigningCertificate().getKeyUsages());
//			System.out.println(signature.isSignatureIntact()+" "+signature.isSignatureValid());
//			System.out.println(signature.getPdfSignatureOrFormFillChanges());
//		}

		PdfGeneratedDTO pdfG = new PdfGeneratedDTO();
        pdfG.setNomFile(pdfRes.getNom_file());
        pdfG.setResultatVerif(pdfRes.getStatus_rapp());
		pdfG.setBytesDoc(pdfRes.getRapport());

        return pdfG;
	}

	@CrossOrigin(origins = "http://localhost:4200")
	@GetMapping("/call-verified-docs")
	public List<PdfGeneratedDTO> getAllVerifierdDocs(){
		return callVerifiedDocs.getReportsOfVerifiedDocs();
	}
	
	@CrossOrigin(origins = "http://localhost:4200")
	@GetMapping("/download-report/{idReport}")
	public PdfGenerator downloadReport(@PathVariable String idReport) {
		return callVerifiedDocs.getReportToDownload(Long.parseLong(idReport));
	}

	private void setDetachedContents(DocumentValidator documentValidator, ValidationForm validationForm) {
		List<DSSDocument> originalFiles = WebAppUtils.originalFilesToDSSDocuments(validationForm.getOriginalFiles());
		if (Utils.isCollectionNotEmpty(originalFiles)) {
			documentValidator.setDetachedContents(originalFiles);
		}
		documentValidator.setValidationLevel(validationForm.getValidationLevel());
	}

	private void setSigningCertificate(DocumentValidator documentValidator, ValidationForm validationForm) {
		CertificateToken signingCertificate = WebAppUtils.toCertificateToken(validationForm.getSigningCertificate());
		if (signingCertificate != null) {
			CertificateSource signingCertificateSource = new CommonCertificateSource();
			signingCertificateSource.addCertificate(signingCertificate);
			documentValidator.setSigningCertificateSource(signingCertificateSource);
		}

	}

	private CertificateVerifier getCertificateVerifier(ValidationForm certValidationForm) {
		CertificateSource adjunctCertSource = WebAppUtils.toCertificateSource(certValidationForm.getAdjunctCertificates());

		CertificateVerifier cv;
		if (adjunctCertSource == null) {
			// reuse the default one
			cv = certificateVerifier;
		} else {
			cv = new CertificateVerifierBuilder(certificateVerifier).buildCompleteCopy();
			cv.setAdjunctCertSources(adjunctCertSource);
		}

		return cv;
	}

	private Reports validate(DocumentValidator documentValidator, ValidationForm validationForm) {
		Reports reports = null;

		Date start = new Date();
		DSSDocument policyFile = WebAppUtils.toDSSDocument(validationForm.getPolicyFile());
        if (!validationForm.isDefaultPolicy() && (policyFile != null)) {
            try (InputStream is = policyFile.openStream()) {
                reports = documentValidator.validateDocument(is);
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
		} else if (defaultPolicy != null) {
            try (InputStream is = defaultPolicy.getInputStream()) {
                reports = documentValidator.validateDocument(is);
            } catch (IOException e) {
                LOG.error("Unable to parse policy : " + e.getMessage(), e);
            }
		} else {
			LOG.error("Not correctly initialized");
		}

		Date end = new Date();
		long duration = end.getTime() - start.getTime();
		LOG.info("Validation process duration : {}ms", duration);

		return reports;
	}

	@RequestMapping(value = "/download-simple-report")
	public void downloadSimpleReport(HttpSession session, HttpServletResponse response) {
		String simpleReport = (String) session.getAttribute(XML_SIMPLE_REPORT_ATTRIBUTE);
		if (simpleReport == null) {
			throw new SourceNotFoundException("Simple report not found");
		}

		try {
			response.setContentType(MimeType.PDF.getMimeTypeString());
			response.setHeader("Content-Disposition", "attachment; filename=DSS-Simple-report.pdf");

			fopService.generateSimpleReport(simpleReport, response.getOutputStream());
		} catch (Exception e) {
			LOG.error("An error occurred while generating pdf for simple report : " + e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/download-detailed-report")
	public void downloadDetailedReport(HttpSession session, HttpServletResponse response) {
		String detailedReport = (String) session.getAttribute(XML_DETAILED_REPORT_ATTRIBUTE);
		if (detailedReport == null) {
			throw new SourceNotFoundException("Detailed report not found");
		}

		try {
			response.setContentType(MimeType.PDF.getMimeTypeString());
			response.setHeader("Content-Disposition", "attachment; filename=DSS-Detailed-report.pdf");

			fopService.generateDetailedReport(detailedReport, response.getOutputStream());
		} catch (Exception e) {
			LOG.error("An error occurred while generating pdf for detailed report : " + e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/download-diagnostic-data")
	public void downloadDiagnosticData(HttpSession session, HttpServletResponse response) {
		String diagnosticData = (String) session.getAttribute(XML_DIAGNOSTIC_DATA_ATTRIBUTE);
		if (diagnosticData == null) {
			throw new SourceNotFoundException("Diagnostic data not found");
		}

		try {
			response.setContentType(MimeType.XML.getMimeTypeString());
			response.setHeader("Content-Disposition", "attachment; filename=DSS-Diagnotic-data.xml");
			Utils.copy(new ByteArrayInputStream(diagnosticData.getBytes()), response.getOutputStream());
		} catch (IOException e) {
			LOG.error("An error occurred while downloading diagnostic data : " + e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/diag-data.svg")
	public @ResponseBody ResponseEntity<String> downloadSVG(HttpSession session, HttpServletResponse response) {
		String diagnosticData = (String) session.getAttribute(XML_DIAGNOSTIC_DATA_ATTRIBUTE);
		if (diagnosticData == null) {
			throw new SourceNotFoundException("Diagnostic data not found");
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.valueOf(MimeType.SVG.getMimeTypeString()));
		ResponseEntity<String> svgEntity = new ResponseEntity<>(xsltService.generateSVG(diagnosticData), headers,
				HttpStatus.OK);
		return svgEntity;
	}

	@RequestMapping(value = "/download-certificate")
	public void downloadCertificate(@RequestParam(value = "id") String id, HttpSession session, HttpServletResponse response) {
		DiagnosticData diagnosticData = getDiagnosticData(session);
		CertificateWrapper certificate = diagnosticData.getUsedCertificateById(id);
		if (certificate == null) {
			String message = "Certificate " + id + " not found";
			LOG.warn(message);
			throw new SourceNotFoundException(message);
		}
		String pemCert = DSSUtils.convertToPEM(DSSUtils.loadCertificate(certificate.getBinaries()));
		String filename = DSSUtils.getNormalizedString(certificate.getReadableCertificateName()) + ".cer";

		addTokenToResponse(response, filename, MimeType.CER, pemCert.getBytes());
	}

	@RequestMapping(value = "/download-revocation")
	public void downloadRevocationData(@RequestParam(value = "id") String id, @RequestParam(value = "format") String format, HttpSession session,
									   HttpServletResponse response) {
		DiagnosticData diagnosticData = getDiagnosticData(session);
		RevocationWrapper revocationData = diagnosticData.getRevocationById(id);
		if (revocationData == null) {
			String message = "Revocation data " + id + " not found";
			LOG.warn(message);
			throw new SourceNotFoundException(message);
		}
		String filename = revocationData.getId();
		MimeType mimeType;
		byte[] binaries;

		if (RevocationType.CRL.equals(revocationData.getRevocationType())) {
			mimeType = MimeType.CRL;
			filename += ".crl";

			if (Utils.areStringsEqualIgnoreCase(format, "pem")) {
				String pem = "-----BEGIN CRL-----\n";
				pem += Utils.toBase64(revocationData.getBinaries());
				pem += "\n-----END CRL-----";
				binaries = pem.getBytes();
			} else {
				binaries = revocationData.getBinaries();
			}
		} else {
			mimeType = MimeType.BINARY;
			filename += ".ocsp";
			binaries = revocationData.getBinaries();
		}

		addTokenToResponse(response, filename, mimeType, binaries);
	}

	@RequestMapping(value = "/download-timestamp")
	public void downloadTimestamp(@RequestParam(value = "id") String id, @RequestParam(value = "format") String format, HttpSession session,
								  HttpServletResponse response) {
		DiagnosticData diagnosticData = getDiagnosticData(session);
		TimestampWrapper timestamp = diagnosticData.getTimestampById(id);
		if (timestamp == null) {
			String message = "Timestamp " + id + " not found";
			LOG.warn(message);
			throw new SourceNotFoundException(message);
		}
		TimestampType type = timestamp.getType();

		byte[] binaries;
		if (Utils.areStringsEqualIgnoreCase(format, "pem")) {
			String pem = "-----BEGIN TIMESTAMP-----\n";
			pem += Utils.toBase64(timestamp.getBinaries());
			pem += "\n-----END TIMESTAMP-----";
			binaries = pem.getBytes();
		} else {
			binaries = timestamp.getBinaries();
		}

		String filename = type.name() + ".tst";
		addTokenToResponse(response, filename, MimeType.TST, binaries);
	}

	protected DiagnosticData getDiagnosticData(HttpSession session) {
		String diagnosticDataXml = (String) session.getAttribute(XML_DIAGNOSTIC_DATA_ATTRIBUTE);
		if (diagnosticDataXml == null) {
			throw new SourceNotFoundException("Diagnostic data not found");
		}
		try {
			XmlDiagnosticData xmlDiagData = DiagnosticDataFacade.newFacade().unmarshall(diagnosticDataXml);
			return new DiagnosticData(xmlDiagData);
		} catch (Exception e) {
			LOG.error("An error occurred while generating DiagnosticData from XML : " + e.getMessage(), e);
		}
		return null;
	}

	protected void addTokenToResponse(HttpServletResponse response, String filename, MimeType mimeType, byte[] binaries) {
		response.setContentType(MimeType.TST.getMimeTypeString());
		response.setHeader("Content-Disposition", "attachment; filename=" + filename);
		try (InputStream is = new ByteArrayInputStream(binaries); OutputStream os = response.getOutputStream()) {
			Utils.copy(is, os);
		} catch (IOException e) {
			LOG.error("An error occurred while downloading a file : " + e.getMessage(), e);
		}
	}

	@ModelAttribute("validationLevels")
	public ValidationLevel[] getValidationLevels() {
		return new ValidationLevel[] { ValidationLevel.BASIC_SIGNATURES, ValidationLevel.LONG_TERM_DATA, ValidationLevel.ARCHIVAL_DATA };
	}

	@ModelAttribute("displayDownloadPdf")
	public boolean isDisplayDownloadPdf() {
		return true;
	}

	@ModelAttribute("digestAlgos")
	public DigestAlgorithm[] getDigestAlgorithms() {
		// see https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/digest
		return new DigestAlgorithm[] { DigestAlgorithm.SHA1, DigestAlgorithm.SHA256, DigestAlgorithm.SHA384,
				DigestAlgorithm.SHA512 };
	}

}