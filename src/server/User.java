package server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

public class User {
	static final MessageDigest passMessageDigest = getHash("SHA3-512");
	static final MessageDigest nameMessageDigest = getHash("MD5");
	static final SecureRandom rng = new SecureRandom();
	static final ReentrantLock lock = new ReentrantLock();
	
	public static final HashMap<Integer, User> userDB = new HashMap<Integer, User>();
	static final HashMap<String, User> usernameLookup = new HashMap<String, User>();
	
	int pk;
	String name;
	byte[] passhash;
	long createdTime;
	long lastLoginTime;
	
	public User(int pk, String name, byte[] passhash, long createdTime, long lastLoginTime) {
		this.pk = pk;
		this.name = name;
		this.passhash = passhash;
		this.createdTime = createdTime;
		this.lastLoginTime = lastLoginTime;
	}
	
	@Override
	public String toString() {
		return String.format("%s\t%s\t%s\t%d\t%d", Integer.toHexString(pk), name, new String(Base64.getEncoder().encode(passhash)), createdTime, lastLoginTime);
	}
	
	
	public static void loadDB() throws IOException {
		lock.lock();
		try {
			Scanner sc = new Scanner(new File("server/database/player.txt"));
			userDB.clear();
			usernameLookup.clear();
			while(sc.hasNext()) {
				String s = sc.nextLine();
				
				String[] parse = s.split("\t");
				User user = new User(
						Integer.parseUnsignedInt(parse[0], 16),
						parse[1],
						Base64.getDecoder().decode(parse[2]),
						Long.parseLong(parse[3]),
						Long.parseLong(parse[4])
						);
				userDB.put(user.pk, user);
				usernameLookup.put(user.name, user);
				
			}
			sc.close();
		} finally {
			lock.unlock();
		}
	}
	public static void saveDB() throws IOException {
		lock.lock();
		try {
			PrintWriter out = new PrintWriter(new File("server/database/player.txt"));
			for(User user : userDB.values()) {
				out.println(user);
			}
			out.flush();
			out.close();
		} finally {
			lock.unlock();
		}
	}
	
	public static User createUser(String username, String passhash) {
		byte[] doubleHash = serverHash(passhash);
		
		lock.lock();
		try {
			if(usernameLookup.containsKey(username))
				throw new NoSuchElementException("Username '" + username + "' already exists");
			
			Integer pk;
			do {
				pk = rng.nextInt();
			} while(userDB.containsKey(pk));
			
			User user = new User(
				pk,
				username,
				doubleHash,
				System.currentTimeMillis(),
				System.currentTimeMillis()
				);
			userDB.put(user.pk, user);
			usernameLookup.put(username, user);
			return user;
		} finally {
			lock.unlock();
		}
	}
	
	public static boolean checkPass(String username, String passhash) {
		byte[] doubleHash = serverHash(passhash);
		
		lock.lock();
		try {
			User user = usernameLookup.get(username);
			if(user == null)
				throw new NoSuchElementException("User '" + username + "' does not exist");
			return Arrays.equals(doubleHash, user.passhash);
		} finally {
			lock.unlock();
		}
		
	}
	public static String clientHash(String password, String username) {
		passMessageDigest.update(nameMessageDigest.digest(username.getBytes()));
		return new String(Base64.getEncoder().encode(passMessageDigest.digest(password.getBytes())));
	}
	public static byte[] serverHash(String passhash) {
		return passMessageDigest.digest(Base64.getDecoder().decode(passhash));
	}
	
	
	public static void main(String[] args) {
//		System.out.println(clientHash("wachtwoord123", "kamron"));
//		System.out.println(clientHash("password123", "kamron"));
//		System.out.println(new String(Base64.getEncoder().encode(serverHash(clientHash("kamron33", "kamron")))));
//		
		try {
			loadDB();
//			System.out.println(userDB);
//			createUser("kamron2", clientHash("wachtwoord123", "kamron2"));
//			System.out.println(userDB);
			
			System.out.println(checkPass("kamron2", clientHash("wachtwoord123", "kamron2")));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		msgRegister(new Message("register", "kamron2", clientHash("wachtwoord123", "kamron2")));
//		msgLogin(new Message("login", Map.of("num", "5"), "Parker", clientHash("wachtwoord123", "Parker")));
		
	}
	
	static void msgRegister(Message msg) {
		String username = msg.get(0);
		String passhash = msg.get(1);
		
		try {
			User usr = createUser(username, passhash);
			System.out.println("Successfully created user");
			System.out.println(usr);
		} catch(NoSuchElementException e) {
			System.out.println("Failed, user already existed");
		}
		
		
		
	}
	
	public static void msgLogin(Message msg) {
		String username = msg.get(0);
		String passhash = msg.get(1);
		String num = msg.get("num");
		
		try {
			boolean check = checkPass(username, passhash);
			
			if(check) {
				System.out.println("Successfully logged in");
			} else {
				System.out.println("Wrong username/password");
			}
		} catch(NoSuchElementException e) {
			System.out.println("Wrong username/password");
		}
		
	}

	static MessageDigest getHash(String s) {
		try {
			return MessageDigest.getInstance(s);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new Error(e.getCause());
		}
	}
	
}
