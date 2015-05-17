package org.apache.cxf.hugebase64.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

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

	private byte[] partBytes;

	private byte[] headerBytes;

	private byte[] footerBytes;

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

	public Huge64Test() {
		try {
			partBytes = getFileContent("part.xml");
			headerBytes = getFileContent("soapHeader.txt");
			footerBytes = getFileContent("soapFooter.txt");
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
	public void testGeneratedFile() throws Exception {
		test("gen_10K.xml", 10_000);
		test("gen_100K.xml", 100_000);
		test("gen_500K.xml", 500_000);
		test("gen_1M.xml", 1_000_000);
	}

	private void test(String fileName, int numberOfPersonElements) throws Exception {

		printTestMessage(numberOfPersonElements);

		printMemory();

		File generaredFile = generateFile(fileName, numberOfPersonElements);

		File base64File = convertToBase64(generaredFile);

		int responseCode = sendFile(base64File);

		printMemory();

		assertThat(responseCode, is(200));
	}

	private int sendFile(File file) throws Exception {
		InputStream in = new BufferedInputStream(new FileInputStream(file));

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
		httpOut.write(footerBytes);
		httpOut.flush();
		in.close();

		return httpURLConnection.getResponseCode();
	}

	private File generateFile(String fileName, int numberOfPersonElements) throws Exception {

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

		for (int i = 0; i < numberOfPersonElements; i++) {
			out.write(partBytes);
		}

		out.write(toBytes("</persons>"));
		out.close();

		return smallGenFile;
	}

	private File convertToBase64(File file) throws Exception {
		InputStream in = new BufferedInputStream(new FileInputStream(file));

		File base64File = new File(file.getParentFile(), "base64.txt");

		OutputStream base64FileOutputStream = new BufferedOutputStream(new FileOutputStream(base64File));
		Base64OutputStream base64OutputStream = new Base64OutputStream(base64FileOutputStream, true, -1, null);

		StreamUtils.copy(in, base64OutputStream);

		in.close();
		base64OutputStream.close();

		return base64File;
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

	private String humanReadableByteCount(long bytes) {
		return humanReadableByteCount(bytes, true);
	}

	/**
	 * Original: http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java/3758880#3758880
	 */
	private String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	private Memory getMemory() {
		Runtime runtime = Runtime.getRuntime();
		Memory memory = new Memory();
		memory.maxMemory = runtime.maxMemory();
		memory.totalMemory = runtime.totalMemory();
		memory.freeMemory = runtime.freeMemory();
		return memory;
	}

	private void printTestMessage(int numberOfPersonElements) {
		String message = "Testing " + numberOfPersonElements + " of person elements";
		StringBuilder sb = new StringBuilder(message.length());
		for (int i = 0; i < message.length(); i++) {
			sb.append("-");
		}
		System.out.println(sb.toString());
		System.out.println(message);
	}

	private void printMemory() {
		Memory memory = getMemory();
		System.out.println(memory);
	}

	class Memory {
		long maxMemory;
		long totalMemory;
		long freeMemory;

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Memory: ");
			sb.append(humanReadableByteCount(totalMemory - freeMemory));
			sb.append("/");
			sb.append(humanReadableByteCount(totalMemory));
			sb.append(" - Max: ");
			sb.append(humanReadableByteCount(maxMemory));
			return sb.toString();
		}
	}
}
