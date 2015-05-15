
package org.apache.cxf.huge64.ws;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;
import org.apache.cxf.huge64.data.HugeFile;
import org.apache.cxf.huge64.data.UploadStatus;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.apache.cxf.huge64.ws package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _SendHugeFileResponse_QNAME = new QName("http://www.example.org/huge64", "SendHugeFileResponse");
    private final static QName _SendHugeFileRequest_QNAME = new QName("http://www.example.org/huge64", "SendHugeFileRequest");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.apache.cxf.huge64.ws
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UploadStatus }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.example.org/huge64", name = "SendHugeFileResponse")
    public JAXBElement<UploadStatus> createSendHugeFileResponse(UploadStatus value) {
        return new JAXBElement<UploadStatus>(_SendHugeFileResponse_QNAME, UploadStatus.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HugeFile }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.example.org/huge64", name = "SendHugeFileRequest")
    public JAXBElement<HugeFile> createSendHugeFileRequest(HugeFile value) {
        return new JAXBElement<HugeFile>(_SendHugeFileRequest_QNAME, HugeFile.class, null, value);
    }

}
