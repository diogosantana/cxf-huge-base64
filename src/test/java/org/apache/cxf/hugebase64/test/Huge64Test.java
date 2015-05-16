package org.apache.cxf.hugebase64.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.huge64.data.HugeFile;
import org.apache.cxf.huge64.data.UploadStatus;
import org.apache.cxf.huge64.ws.Huge64PortType;
import org.apache.cxf.huge64.ws.Huge64Service;
import org.apache.cxf.hugebase64.jetty.LocalJettyServer;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.util.StreamUtils;

public class Huge64Test {

	private static final QName SERVICE_NAME = new QName("http://www.example.org/huge64", "huge64Service");

	private static Server server;

	@BeforeClass
	public static void startServer() {
		LocalJettyServer jettyServer = new LocalJettyServer();
		server = jettyServer.starServer();
	}

	@AfterClass
	public static void stopServer() {
		try {
			if (server != null) {
				server.stop();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testSimpleFile() {
		Huge64Service service = new Huge64Service(Huge64Service.WSDL_LOCATION, SERVICE_NAME);

		Huge64PortType huge64Port = service.getHuge64Port();

		HugeFile hugeFile = new HugeFile();
		hugeFile.setFileName("file.xml");

		File file = getFile("file.xml");

		DataSource ds = new FileDataSource(file);

		DataHandler dataHandler = new DataHandler(ds);
		hugeFile.setXml(dataHandler);

		UploadStatus uploadStatus = huge64Port.sendHugeFile(hugeFile);

		assertThat(uploadStatus, is(notNullValue()));
		assertThat(uploadStatus.getStatus(), is("OK"));
	}

	@Test
	public void testSmallGeneratedFile() throws Exception {

		byte[] partBytes = getFileContent("part.xml");
		byte[] headerBytes = getFileContent("soapHeader.txt");
		byte[] footerBytes = getFileContent("soapFooter.txt");

		File tmpDir = new File("target/files").getAbsoluteFile();
		if (!tmpDir.exists()) {
			tmpDir.mkdirs();
		}
		File smallGenFile = new File(tmpDir, "gen_small.xml");
		if (smallGenFile.exists()) {
			smallGenFile.delete();
		}

		OutputStream out = new BufferedOutputStream(new FileOutputStream(smallGenFile));

		out.write(toBytes("<persons>\n"));

		for (int i = 0; i < 1_000_000; i++) {
			out.write(partBytes);
		}

		out.write(toBytes("</persons>"));
		out.close();

		InputStream in = new BufferedInputStream(new FileInputStream(smallGenFile));

		File base64File = new File(smallGenFile.getParentFile(), "base64.txt");
		OutputStream base64FileOutputStream = new BufferedOutputStream(new FileOutputStream(base64File));
		Base64OutputStream base64OutputStream = new Base64OutputStream(base64FileOutputStream, true, -1, null);
		StreamUtils.copy(in, base64OutputStream);
		in.close();
		base64OutputStream.close();

		in = new BufferedInputStream(new FileInputStream(base64File));

		String endpoint = "http://localhost:8080/huge64/cxf/sendHugeFile";
		URL url = new URL(endpoint);
		URLConnection urlConnection = url.openConnection();
		HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
		httpURLConnection.setDoOutput(true);

		OutputStream httpOut = httpURLConnection.getOutputStream();
		httpOut = new BufferedOutputStream(httpOut);

		httpOut.write(headerBytes);

		int bufferSize = 8192;
		byte[] buffer = new byte[bufferSize];
		int reads = 0;
		while ((reads = in.read(buffer, 0, bufferSize)) != -1) {
			httpOut.write(buffer, 0, reads);
		}
		base64OutputStream.flush();
		httpOut.write(footerBytes);
		base64OutputStream.close();
		httpOut.close();
		in.close();

		int responseCode = httpURLConnection.getResponseCode();
		assertThat(responseCode, is(200));
	}

	private byte[] getFileContent(String fileName) throws Exception {
		File file = getFile(fileName);
		InputStream in = new BufferedInputStream(new FileInputStream(file));
		byte[] bytes = IOUtils.readBytesFromStream(in);
		in.close();
		return bytes;
	}

	private byte[] toBytes(String text) {
		return text.getBytes(Charset.forName("UTF-8"));
	}

	private File getFile(String filename) {
		URL url = Huge64Test.class.getClassLoader().getResource(filename);
		File file = new File(url.getFile());
		return file;
	}
}
