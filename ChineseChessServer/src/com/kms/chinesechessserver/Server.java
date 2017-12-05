package com.kms.chinesechessserver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * ChineseChess 프로그램을 실행하는 클라이언트들끼리 연결시켜주는 서버의 역할을 하는 클래스
 * 동일 네트워크에 지속적으로 브로드캐스트를 실행하고 응답이 오는 클라이언트들을 선착순으로 짝을 연결시켜준다.
 * @author Kwon
 *
 */
public class Server {
	private static final int SERVER_PORT = 10000;		// 본 서버의 포트
	private static final int BROADCAST_PORT = 30000;	// 브로드캐스트용 포트
	
	private DatagramSocket socket;
	
	private Server() {
		try {
			socket = new DatagramSocket( SERVER_PORT );
		} catch(Exception e) {}
		
		new Thread( ()->{ broadcast(); } ).start();
		new Thread( ()->{ matchClient(); } ).start();
	}
	
	private void broadcast() {
		try {
			final InetAddress ADDRESS = InetAddress.getLocalHost();
			final InetAddress BROADCAST = NetworkInterface.getByInetAddress(ADDRESS).getInterfaceAddresses().get(0).getBroadcast();
			
			DatagramPacket packet = new DatagramPacket( new byte[4], 4, BROADCAST, BROADCAST_PORT );
			
			socket.setBroadcast( true );
			
			synchronized( socket ) {
				socket.notify();
			}
			
			while(!socket.isClosed()) {
				Thread.sleep( 1000 );
				
				socket.send( packet );
				System.out.println( "브로드캐스트" );
			}
		} catch(Exception e) {
			if( socket != null )	socket.close();
			socket = null;
		}
	}
	
	private void matchClient() {
		try {
			DatagramPacket packet = new DatagramPacket( new byte[4], 4 );
			
			byte[] gameHostAddr = null;
			byte[] emptyData = { 0, 0, 0, 0 };
			
			synchronized( socket ) {
				socket.wait();
			}
			
			while(!socket.isClosed()) {
				socket.receive( packet );
			//	System.out.println("서버에서 데이터 수신함");
				
				if( gameHostAddr == null || isDuplicated( gameHostAddr, packet.getAddress().getAddress() ) ) {
					gameHostAddr = packet.getAddress().getAddress();
					packet.setData( emptyData );
					socket.send(packet);
			//		System.out.println( "게임 호스트 신호 보냄" );
				} else {
					packet.setData( gameHostAddr );
					socket.send( packet );
					gameHostAddr = null;
			//		System.out.println( "상대방 IP보냄" );
				}
			}
		} catch(Exception e) {
			if( socket != null )	socket.close();
			socket = null;
		}
	}
	
	private boolean isDuplicated( byte[] previous, byte[] current ) {
		for( int index = 0; index < 4; index++ )	
			if( previous[index] != current[index] )	return false;
		
		return true;
	}
	
	public static void main(String[] args) {
		new Server();
	}
}
