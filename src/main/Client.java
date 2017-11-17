package main;

public class Client {
	
	public String ip;
	public String localip;
	
	public boolean shotReady;
	public double shotRotation;
	public int shotX;
	public int shotY;
	public String shotWeapon;
	
	public int x;
	public int y;
	public double rotation;
	public boolean hit;
	
	Client(String ipaddr, String localipaddr) {
		this.ip = ipaddr;
		this.localip = localipaddr;
	}
	
}
