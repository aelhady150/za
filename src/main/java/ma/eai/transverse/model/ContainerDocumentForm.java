package ma.eai.transverse.model;

import eu.europa.esig.dss.enumerations.ASiCContainerType;

/**
 * This interface is used for signature creation forms supporting container creation (i.e. ASiC)
 *
 */
public interface ContainerDocumentForm {

    ASiCContainerType getContainerType();

}
