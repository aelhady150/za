package ma.eai.transverse.model;

import java.util.List;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.springframework.web.multipart.MultipartFile;

import eu.europa.esig.dss.enumerations.ASiCContainerType;
import ma.eai.transverse.WebAppUtils;

public class SignatureMultipleDocumentsForm extends AbstractSignatureForm implements ContainerDocumentForm {

	@NotNull(message = "{error.container.type.mandatory}")
	private ASiCContainerType containerType;

	private List<MultipartFile> documentsToSign;

	@Override
	public ASiCContainerType getContainerType() {
		return containerType;
	}

	public void setContainerType(ASiCContainerType containerType) {
		this.containerType = containerType;
	}

	public List<MultipartFile> getDocumentsToSign() {
		return documentsToSign;
	}

	public void setDocumentsToSign(List<MultipartFile> documentsToSign) {
		this.documentsToSign = documentsToSign;
	}

	@AssertTrue(message = "{error.to.sign.files.mandatory}")
	public boolean isDocumentsToSign() {
        return WebAppUtils.isCollectionNotEmpty(documentsToSign);
	}

}
