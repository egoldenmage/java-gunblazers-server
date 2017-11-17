package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Connection extends Thread {
	
	Socket serverSocket;
	private String clientIp;
	private boolean remove = true;
	
	Connection(Socket socket) {
		this.serverSocket = socket;
		clientIp =  serverSocket.getInetAddress().getHostAddress();
	}
	
	public void run() {
		try {
			String data = null;
			BufferedReader br = new BufferedReader(new InputStreamReader(serverSocket.getInputStream())); //Reader voor input vanuit client
			PrintWriter pw = new PrintWriter(serverSocket.getOutputStream(), true); //Writer om naar client te kunnen writen
			while (!serverSocket.isClosed()) {
				try {
					data = br.readLine(); //Code "Blockt" hier, totdat er data binnenkomt.
					clientIp =  serverSocket.getInetAddress().getHostAddress();
					if (data == null) {  //Als een verbinding beindigtd wordt is de data NULL, break dan
						break;
					} else if (data == "-1") {
						serverSocket.close();
						break;
					}
					
					if (Server.logInPackets) {
						System.out.println(data); //Log alle raw data die binnenkomt
					}
					
					//Nieuwe session wordt aangemaakt
					if (data.indexOf("type:create") != -1) {
						pw.println("sessioncreated");
						Server.sessions.add(new Session(extractData(data,"servername"), extractData(data,"serverpass")));
						serverSocket.close();
					} else if (data.indexOf("type:connect") != -1) { //er wordt verbonden met een (hopelijk) bestaande session
						boolean found = false;
						for (Session s : Server.sessions) {
							if (s.username.indexOf(extractData(data, "servername")) != -1) {
								if (s.password.indexOf(extractData(data, "serverpass")) != -1) { //als username en pw correct zijn, gebruiker toevoegen aan session
									found = true;
									if (s.addUser(clientIp, extractData(data, "machineip")) == false) {
										pw.println("duplicate");
										remove = false;
									} else {
										pw.println("connected");
										System.out.println("Client with IP " + clientIp + " (" + extractData(data, "machineip") + ") connected.");
									}
								} else {
									pw.println("wrongpass");
									System.out.println("Client with IP " + clientIp + " (" + extractData(data, "machineip") + ") tried to connect (wrongpass).");
									found = true;
								}
							}
						}
						if (!found) {
							pw.println("nosuchserver");
							System.out.println("Client with IP " + clientIp + " (" + extractData(data, "machineip") + ") tried to connect to unexisting session.");
						}
					} else if (data.indexOf("type:clientdata") != -1) {//er worden gegevens van een client geupdate.
						for (Session s : Server.sessions) {
							if (s.username.indexOf(extractData(data, "servername")) != -1) {
								if (data.indexOf("fire") != -1) {
									for (Client c : s.users) {
										if ((c.ip.indexOf(clientIp) == -1) && (c.localip.indexOf(extractData(data, "machineip")) == -1)) {
											c.shotReady = true;
											c.shotRotation = Double.parseDouble(extractData(data, "rotation"));
											c.shotX = Integer.parseInt(extractData(data, "playerposx"));
											c.shotY = Integer.parseInt(extractData(data, "playerposy"));
											c.shotWeapon = extractData(data, "fire");
										}
									}
								}
								if (data.indexOf("hit") != -1) {
									for (Client c : s.users) {
										if ((c.ip + c.localip).indexOf(extractData(data,"hit")) != -1) {
											c.hit = true;
											c.shotWeapon = extractData(data, "fire");
										}
									}
								}
								s.updatePos(clientIp, extractData(data, "machineip"), extractData(data, "playerposx"), extractData(data, "playerposy"), extractData(data, "rotation"));
								String prntstr = "type:serverdata|" + s.getData(clientIp, extractData(data, "machineip"));
								pw.println(prntstr);
								if (Server.logOutPackets) {
									System.out.println(prntstr);
								}
							}
						}
					}
				} catch (Exception e) {
					//e.printStackTrace();
					System.out.println("Client " + clientIp + " (" + extractData(data, "machineip") + ") disconnected. (timed out)");
					if (remove) {
						for (Session s : Server.sessions) {
							s.removeUser(clientIp, extractData(data, "machineip"));
						}
					}
					serverSocket.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String extractData(String data, String var) {
		return data.substring(data.indexOf(var) + var.length() + 1, data.indexOf("|",(data.indexOf(var) + var.length() + 1)));
	}	
	
}
