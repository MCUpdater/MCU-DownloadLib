package test.mcupdater;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.mcupdater.downloadlib.Version;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

public class TestForm {
	private final JButton btnRaw;
	private final JButton btnMF;
	private JPanel panel1;
		private JTextArea txtContent;
		private JTextField txtURL;
		private JLabel lblURL;
		private JButton btnGo;
		private JScrollPane scrollContent;

	public static void main(String[] args){
		new TestForm();
	}

	public TestForm() {
		final JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(600, 400);
		panel1 = new JPanel();
		panel1.setLayout(new BorderLayout());
		frame.setContentPane(panel1);
		txtContent=new JTextArea();
		scrollContent = new JScrollPane(txtContent);
		panel1.add(scrollContent, BorderLayout.CENTER);
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());
		panel1.add(topPanel, BorderLayout.NORTH);
		lblURL = new JLabel("URL:");
		txtURL = new JTextField();
		lblURL.setLabelFor(txtURL);
		btnGo = new JButton("Adf.ly");
		btnMF = new JButton("MediaFire");
		btnRaw = new JButton("Get Raw");
		topPanel.add(lblURL, BorderLayout.WEST);
		topPanel.add(txtURL, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout());
		buttons.add(btnGo);
		buttons.add(btnMF);
		buttons.add(btnRaw);
		topPanel.add(buttons, BorderLayout.EAST);
		btnGo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				txtContent.setText("");
				try {
					URLConnection cn = new URL(txtURL.getText()).openConnection();
					cn.setRequestProperty("User-Agent","MCU-DownloadLib/" + Version.API_VERSION);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					IOUtils.copy(cn.getInputStream(), baos);
					byte[] bytes = baos.toByteArray();
					Reader in = new InputStreamReader(new ByteArrayInputStream(bytes));
					char[] buffer = new char[819200];
					StringBuilder content = new StringBuilder();
					while (in.read(buffer) != -1) {
						//System.out.print(String.copyValueOf(buffer));
//						txtContent.append(String.copyValueOf(buffer));
						content.append(buffer);
					}
					in.close();
					int key = content.toString().indexOf("'", content.toString().indexOf("ysmm"));
					int after = content.toString().indexOf("'", key + 1);
					String raw = content.toString().substring(key + 1, after);
					StringBuilder forward = new StringBuilder();
					StringBuilder backward = new StringBuilder();
					for (int i = 0; i < raw.length(); i++) {
						if (i % 2 == 0) {
							forward.append(raw.charAt(i));
						} else {
							backward.insert(0, raw.charAt(i));
						}
					}
					System.out.println(forward.toString());
					System.out.println(backward.toString());
					String rebuilt = forward.toString() + backward.toString();
					txtContent.append(raw + "\n======\n");
					txtContent.append(rebuilt + "\n");
					byte[] decode = Base64.decodeBase64(rebuilt);
					byte[] decode2 = new byte[decode.length - 2];
					System.arraycopy(decode, 2, decode2, 0, decode.length - 2);
					txtContent.append(new String(decode2) + "\n");
				} catch (java.io.IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		btnRaw.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				txtContent.setText("");
				try {
					URLConnection cn = new URL(txtURL.getText()).openConnection();
					cn.setRequestProperty("User-Agent","MCU-DownloadLib/" + Version.API_VERSION);
					for (Map.Entry entry : cn.getHeaderFields().entrySet()) {
						System.out.println(entry.getKey() + ": " + entry.getValue());
					}
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					IOUtils.copy(cn.getInputStream(), baos);
					byte[] bytes = baos.toByteArray();
					Reader in = new InputStreamReader(new ByteArrayInputStream(bytes));
					char[] buffer = new char[819200];
					while (in.read(buffer) != -1) {
						txtContent.append(String.copyValueOf(buffer));
					}
					in.close();
				} catch (java.io.IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		btnMF.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				txtContent.setText("");
				try {
					URLConnection cn = new URL(txtURL.getText()).openConnection();
					cn.setRequestProperty("User-Agent","MCU-DownloadLib/" + Version.API_VERSION);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					IOUtils.copy(cn.getInputStream(), baos);
					byte[] bytes = baos.toByteArray();
					Reader in = new InputStreamReader(new ByteArrayInputStream(bytes));
					char[] buffer = new char[819200];
					StringBuilder content = new StringBuilder();
					while (in.read(buffer) != -1) {
						content.append(buffer);
					}
					in.close();
					int key = content.toString().indexOf("\"", content.toString().indexOf("kNO"));
					int after = content.toString().indexOf("\"", key + 1);
					String raw = content.toString().substring(key + 1, after);
					txtContent.append(raw);
				} catch (java.io.IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		frame.setVisible(true);
	}
}
