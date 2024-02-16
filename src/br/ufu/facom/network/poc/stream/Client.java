package br.ufu.facom.network.poc.stream;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.TileObserver;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import br.ufu.facom.network.dlop.FinSocket;
import br.ufu.facom.network.dlop.message.DlopMessage;

public class Client {
	//Timer timer; // timer used to receive data
	byte[] buf; // buffer used to store data received

	BufferedReader bufferedReader;
	BufferedWriter bufferedWriter;

	static int MJPEG_TYPE = 26; // RTP payload type for MJPEG video
	
	private FinSocket finSocket;
	private String titleStream;
	
	private Thread streamThread;
	/**
	 * GUI
	 */
	 JFrame f = new JFrame("Client");
	 JPanel mainPanel = new JPanel();
	 JLabel iconLabel = new JLabel();
	 ImageIcon icon;
	
	public Client(String titleClient, String titleStream, boolean viewEnabled) {
		this.titleStream = titleStream;

		// allocate enough memory for the buffer used to receive data from the server
		buf = new byte[15000];
		
		finSocket = new FinSocket();
		if(finSocket.open()){
			finSocket.register(titleClient);
			if(finSocket.joinWorkspace(titleStream)){
			
				if(viewEnabled)
					createView();
			
				startThread(viewEnabled);
			}else{
				System.err.println("Nao possivel se atachar no workspace '"+titleStream+"'");
				finSocket.unregister();
				System.exit(1);
			}
		}else{
			System.err.println("Nao possivel criar FinSocket");
			System.exit(1);
		}
	}

	private void startThread(final boolean viewEnabled) {
		streamThread = new Thread(){
	    	@Override
	    	public void run() {
	    	while(!streamThread.isInterrupted()){
			try {
				// receive the DP from the socket:
				DlopMessage message = finSocket.read();
				
				byte buf[] = message.getPayload();
				int size = buf.length;
	
				// create an RTPpacket object from the DP
				RTPPacket rtp_packet = new RTPPacket(buf, size);
	
				// get the payload bitstream from the RTPpacket object
				int payload_length = rtp_packet.getPayloadLength();
				byte[] payload = new byte[payload_length];
				rtp_packet.getPayload(payload);
	
				if(viewEnabled){
					// get an Image object from the payload bitstream
					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Image image = toolkit.createImage(payload, 0, payload_length);
		
					// display the image as an ImageIcon object
					icon = new ImageIcon(image);
					iconLabel.setIcon(icon);
				}
			}catch (Exception ex) {
				ex.printStackTrace();
				System.out.println("Exception caught: " + ex);
			}
	    	}
	    	}
	    };
	    
	    streamThread.start();
	}

	private void createView() {
		//Image display label
	    iconLabel.setIcon(null);
	    
	    //frame layout
	    mainPanel.setLayout(null);
	    mainPanel.add(iconLabel);
	    iconLabel.setBounds(0,0,380,280);

	    f.getContentPane().add(mainPanel, BorderLayout.CENTER);
	    f.setSize(new Dimension(390,320));
	    f.setVisible(true);
	    f.addWindowListener(new WindowAdapter() {
	        public void windowClosing(WindowEvent e) {
	        	endSocket();
	            System.exit(0);
	     }});
	    
	}

	private void endSocket() {
		if(finSocket != null){
			//finSocket.disjoinWorkspace(titleStream);
			//finSocket.unregister();
			finSocket.close();
		}
	}
	
	public static void main(String[] args) {
		if(args.length == 2){
			new Client(args[0],args[1],true);
		}else if(args.length == 3){
			new Client(args[0],args[1], Boolean.getBoolean(args[2].toLowerCase()));
		}else
			System.err.println("Wrong usage");
	}
}