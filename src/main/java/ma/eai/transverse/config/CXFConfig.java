package ma.eai.transverse.config;

import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import eu.europa.esig.dss.ws.cert.validation.common.RemoteCertificateValidationService;
import eu.europa.esig.dss.ws.cert.validation.rest.RestCertificateValidationServiceImpl;
import eu.europa.esig.dss.ws.cert.validation.rest.client.RestCertificateValidationService;
import eu.europa.esig.dss.ws.signature.common.RemoteDocumentSignatureService;
import eu.europa.esig.dss.ws.signature.rest.RestDocumentSignatureServiceImpl;
import eu.europa.esig.dss.ws.signature.rest.client.RestDocumentSignatureService;
import eu.europa.esig.dss.ws.timestamp.remote.RemoteTimestampService;
import eu.europa.esig.dss.ws.timestamp.remote.rest.RestTimestampServiceImpl;
import eu.europa.esig.dss.ws.timestamp.remote.rest.client.RestTimestampService;
import eu.europa.esig.dss.ws.validation.common.RemoteDocumentValidationService;
import eu.europa.esig.dss.ws.validation.rest.RestDocumentValidationServiceImpl;
import eu.europa.esig.dss.ws.validation.rest.client.RestDocumentValidationService;
import ma.eai.transverse.exception.ExceptionRestMapper;

@Configuration
@ImportResource({ "classpath:META-INF/cxf/cxf.xml" })
public class CXFConfig {

	public static final String SOAP_SIGNATURE_ONE_DOCUMENT = "/soap/signature/one-document";
	public static final String SOAP_SIGNATURE_MULTIPLE_DOCUMENTS = "/soap/signature/multiple-documents";
	public static final String SOAP_SIGNATURE_TRUSTED_LIST = "/soap/signature/trusted-list";
	public static final String SOAP_VALIDATION = "/soap/validation";
	public static final String SOAP_CERTIFICATE_VALIDATION = "/soap/certificate-validation";
	public static final String SOAP_SERVER_SIGNING = "/soap/server-signing";
	public static final String SOAP_TIMESTAMP_SERVICE = "/soap/timestamp-service";

	public static final String REST_SIGNATURE_ONE_DOCUMENT = "/rest/signature/one-document";
	public static final String REST_SIGNATURE_MULTIPLE_DOCUMENTS = "/rest/signature/multiple-documents";
	public static final String REST_SIGNATURE_TRUSTED_LIST = "/rest/signature/trusted-list";
	public static final String REST_VALIDATION = "/rest/validation";
	public static final String REST_CERTIFICATE_VALIDATION = "/rest/certificate-validation";
	public static final String REST_SERVER_SIGNING = "/rest/server-signing";
	public static final String REST_TIMESTAMP_SERVICE = "/rest/timestamp-service";

	@Value("${cxf.debug:false}")
	private boolean cxfDebug;

	@Value("${cxf.mtom.enabled:true}")
	private boolean mtomEnabled;

	@Value("${dssVersion:1.0}")
	private String dssVersion;

	@Autowired
	private Bus bus;

	@Autowired
	private RemoteDocumentSignatureService remoteSignatureService;

	@Autowired
	private RemoteDocumentValidationService remoteValidationService;

	@Autowired
	private RemoteCertificateValidationService remoteCertificateValidationService;

	@Autowired
	private RemoteTimestampService timestampService;

	@PostConstruct
	private void addLoggers() {
		if (cxfDebug) {
			LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
			bus.getInInterceptors().add(loggingInInterceptor);
			bus.getInFaultInterceptors().add(loggingInInterceptor);

			LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
			bus.getOutInterceptors().add(loggingOutInterceptor);
			bus.getOutFaultInterceptors().add(loggingOutInterceptor);
		}
	}

	// --------------- REST

	@Bean
	public RestDocumentSignatureService restSignatureService() {
		RestDocumentSignatureServiceImpl service = new RestDocumentSignatureServiceImpl();
		service.setService(remoteSignatureService);
		return service;
	}

	@Bean
	public RestDocumentValidationService restValidationService() {
		RestDocumentValidationServiceImpl service = new RestDocumentValidationServiceImpl();
		service.setValidationService(remoteValidationService);
		return service;
	}

	@Bean
	public RestCertificateValidationService restCertificateValidationService() {
		RestCertificateValidationServiceImpl service = new RestCertificateValidationServiceImpl();
		service.setValidationService(remoteCertificateValidationService);
		return service;
	}

	@Bean
	public RestTimestampService restTimestampService() {
		RestTimestampServiceImpl restTimestampService = new RestTimestampServiceImpl();
		restTimestampService.setTimestampService(timestampService);
		return restTimestampService;
	}

	@Bean
	public Server createServerValidationRestService() {
		JAXRSServerFactoryBean sfb = new JAXRSServerFactoryBean();
		sfb.setServiceBean(restValidationService());
		sfb.setAddress(REST_VALIDATION);
		sfb.setProvider(jacksonJsonProvider());
		sfb.setProvider(exceptionRestMapper());
		sfb.setFeatures(Arrays.asList(createOpenApiFeature()));
		return sfb.create();
	}

	@Bean
	public Server createServerCertificateValidationRestService() {
		JAXRSServerFactoryBean sfb = new JAXRSServerFactoryBean();
		sfb.setServiceBean(restCertificateValidationService());
		sfb.setAddress(REST_CERTIFICATE_VALIDATION);
		sfb.setProvider(jacksonJsonProvider());
		sfb.setProvider(exceptionRestMapper());
		sfb.setFeatures(Arrays.asList(createOpenApiFeature()));
		return sfb.create();
	}

	@Bean
	public Server createRemoteTimestampRestService() {
		JAXRSServerFactoryBean sfb = new JAXRSServerFactoryBean();
		sfb.setServiceBean(restTimestampService());
		sfb.setAddress(REST_TIMESTAMP_SERVICE);
		sfb.setProvider(jacksonJsonProvider());
		sfb.setProvider(exceptionRestMapper());
		sfb.setFeatures(Arrays.asList(createOpenApiFeature()));
		return sfb.create();
	}

    @Bean
    public OpenApiFeature createOpenApiFeature() {
        final OpenApiFeature openApiFeature = new OpenApiFeature();
		openApiFeature.setCustomizer(openApiCustomizer());
        openApiFeature.setPrettyPrint(true);
        openApiFeature.setScan(true);
		openApiFeature.setUseContextBasedConfig(true);
        openApiFeature.setTitle("DSS WebServices");
		openApiFeature.setVersion(dssVersion);
        return openApiFeature;
    }

	@Bean
	public OpenApiCustomizer openApiCustomizer() {
		OpenApiCustomizer customizer = new OpenApiCustomizer();
		customizer.setDynamicBasePath(true);
		return customizer;
	}

	@Bean
	public JacksonJsonProvider jacksonJsonProvider() {
		JacksonJsonProvider jsonProvider = new JacksonJsonProvider();
		jsonProvider.setMapper(objectMapper());
		return jsonProvider;
	}
    
	/**
	 * ObjectMappers configures a proper way for (un)marshalling of json data
	 *
	 * @return {@link ObjectMapper}
	 */
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		// true value allows processing of {@code @IDREF}s cycles
		JaxbAnnotationIntrospector jai = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
		objectMapper.setAnnotationIntrospector(jai);
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		return objectMapper;
	}
	
	@Bean
	public ExceptionRestMapper exceptionRestMapper() {
		return new ExceptionRestMapper();
	}

}
