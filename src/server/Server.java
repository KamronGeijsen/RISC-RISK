package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import server.Server.ServerGame.Player;
import server.Server.ServerGame.Territory;

public class Server {

	ServerSocket serverSocket;
	ConnectionScheduler scheduler;
	HashMap<String, ServerGame> serverGamesByName = new HashMap<>();

	public Server() throws IOException, InterruptedException {
		SocketAddress address = new InetSocketAddress("192.168.178.29", 4444);
		serverSocket = new ServerSocket();
		serverSocket.bind(address);

		System.out.println(serverSocket);
		logWithTime("Server initialized");

		scheduler = new ConnectionScheduler(100);
		scheduler.start();
		scheduler.join();

		serverSocket.close();

	}

	class ConnectionScheduler extends Thread {
		final int len;
		private short clientIDCounter = 0;

		public ConnectionScheduler(int len) {
			this.len = len;
			connectionAllocator = new PriorityBlockingQueue<>(len);
			connections = new Connection[len];
			for (int i = 0; i < len; i++)
				connectionAllocator.add(i);
		}

		@Override
		public void run() {
			printStatus("scheduler initialized with " + len + " connection slots");
			try {
				while (true) {
					Socket socket = serverSocket.accept();
					Integer emptySlotIndex = scheduler.newSlot();
					if (emptySlotIndex == null) {
						PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
						BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

						out.println("503 Service Unavailable");
						logWithTime("[Scheduler]: connection rejected (scheduler overloaded)");
						Thread.sleep(1000);
						
						out.close();
						in.close();
						socket.close();
					} else {
						Connection client = new Connection(socket, clientIDCounter++, emptySlotIndex);
						scheduler.open(client);
						client.start();
						Thread.sleep(10);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Integer newSlot() {
			return connectionAllocator.peek();
		}

		void open(Connection connection) throws InterruptedException {
			connections[connectionAllocator.take()] = connection;
			printStatus("connection was created");
		}

		void close(int connectionSlot) {
			connectionAllocator.put(connectionSlot);
			printStatus("connection was closed");
		}

		void printStatus(String msg) {
			logWithTime("[Scheduler]: " + msg);

			logWithTime(String.format("[Scheduler]:   currently running: %d/%d",
					scheduler.len - scheduler.connectionAllocator.size(), scheduler.len));
			logWithTime("[Scheduler]:   available slots: " + scheduler.connectionAllocator);
		}

		final PriorityBlockingQueue<Integer> connectionAllocator;
		final Connection[] connections;
	}

	class Connection extends Thread {

		private final Socket socket;
		private final PrintWriter out;
		private final BufferedReader in;
		private final short clientID;
		private final int connectionSlot;
		ServerGame serverGame;

		public Connection(Socket socket, short clientID, int connectionSlot) throws IOException {
			this.socket = socket;
			this.clientID = clientID;
			this.connectionSlot = connectionSlot;
			logWithTime(String.format("[C:%04x] connection established", clientID));
			logWithTime(String.format("[C:%04x]   remote socket address: %s", clientID, socket.getLocalAddress()));
			logWithTime(
					String.format("[C:%04x]   local socket address: %s", clientID, socket.getRemoteSocketAddress()));
			logWithTime(String.format("[C:%04x]   local address: %s", clientID, socket.getLocalSocketAddress()));
			logWithTime(String.format("[C:%04x]   Inet address: %s", clientID, socket.getInetAddress()));
			logWithTime(String.format("[C:%04x]   Inet hostname: %s", clientID, socket.getInetAddress().getHostName()));

			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String init = in.readLine();
			if (init.startsWith("init: ")) {
				init = init.substring("init: ".length());
				logWithTime(String.format("[C:%02x]   client hostname: %s", clientID, init));
				int seq = 0;
				sendMessageAndLog("200 init successful. clientID=" + clientID, seq++);
//					sendMessageAndLog(serverGame.statusString(), seq++);
//				sendMessageAndLog(serverGame.playersString(), seq++);
//				sendMessageAndLog(serverGame.territoriesString(), seq++);
			} else {
				logWithTime("[C:%02x] incorrect connection init");
			}

		}

		@Override
		public void run() {
			int messageSequenceCounter = 0;
			try {
				while (true) {
					String msg = in.readLine();
					if (msg == null) {
						throw new IOException("End of stream");
					}
					int messageSequence = messageSequenceCounter++;
					logWithTime(String.format("[C:%04x S:%08x] received: %s", clientID, messageSequence, msg));
					handleMessage(msg, messageSequenceCounter);
				}
			} catch (SocketException e) {
				if (e.getMessage().contentEquals("Connection reset")) {
					logWithTime(String.format("[C:%04x] connection lost (connection reset)", clientID));
				} else {
					e.printStackTrace();
				}
			} catch (IOException e) {
				if (e.getMessage().contentEquals("End of stream")) {
					logWithTime(String.format("[C:%04x] connection lost (end of datastream)", clientID));
				} else {
					e.printStackTrace();
				}
			}

			try {
				if (!socket.isClosed())
					socket.close();
				out.close();
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			disconnect();
		}

		void disconnect() {
			scheduler.close(connectionSlot);
			serverGame.connections.remove(this);
		}

		void handleMessage(String msg, int seq) {
//				parsePlayers(msg);
//				parsePlayer(msg);
			Matcher lobbiesMatcher = Pattern.compile("lobbies\\((.*)\\)").matcher(msg);
			if (lobbiesMatcher.find()) {
				if (lobbiesMatcher.group(1).contentEquals(""))
					sendMessageAndLog(getLobbies(), seq);
			} else {
				Matcher joinMatcher = Pattern.compile("join\\((.*)\\)").matcher(msg);
			if (joinMatcher.find()) {
				parseJoin(joinMatcher.group(1));
				sendToSubscribers(serverGame.playersString(), seq);
			} else {
				Matcher settingsMatcher = Pattern.compile("settings\\((.*)\\)").matcher(msg);
				if (settingsMatcher.find()) {
					parseSettings(settingsMatcher.group(1));
					sendToSubscribers(serverGame.settingsString(), seq);
			} else {
				Matcher statusMatcher = Pattern.compile("status\\((.*)\\)").matcher(msg);
				if (statusMatcher.find()) {
					parseStatus(statusMatcher.group(1));
					sendToSubscribers(serverGame.statusString(), seq);
			} else {
				Matcher playersMatcher = Pattern.compile("players\\((.*)\\)").matcher(msg);
				if (playersMatcher.find()) {
					parsePlayers(playersMatcher.group(1));
					sendToSubscribers(serverGame.playersString(), seq);
			} else {
				Matcher playerMatcher = Pattern.compile("player\\((.*?)\\)").matcher(msg);
				if (playerMatcher.find()) {
					Player player = parsePlayer(playerMatcher.group(1));
					sendToSubscribers(player.toString(), seq);
			} else {
				Matcher territoriesMatcher = Pattern.compile("territories\\((.*)\\)").matcher(msg);
				if (territoriesMatcher.find()) {
					parseTerritories(territoriesMatcher.group(1));
					sendToSubscribers(serverGame.territoriesString(), seq);
			} else {
				Matcher territoryMatcher = Pattern.compile("territory\\((.*?)\\)").matcher(msg);
				if (territoryMatcher.find()) {
					Territory territory = parseTerritory(territoryMatcher.group(1));
					sendToSubscribers(territory.toString(), seq);
				} else {
				}
			}}}}}}}
		}

		void sendMessage(String msg) {
			out.println(msg);
		}

		String waitForMessage() throws IOException {
			return in.readLine();
		}

		void sendMessageAndLog(String msg, int seq) {
			out.println(msg);
			logWithTime(String.format("[C:%04x S:%08x] sending: %s", clientID, seq, msg));
		}

//			String waitForMessageAndLog() throws IOException {
//				String msg = in.readLine(); 
//				logWithTime("[C:%04x S:%08x] sending: " + msg);
//				return msg;
//			}
//			
		private void parsePlayers(String msg) {
			Matcher playerMatcher = Pattern.compile("player\\((.*?)\\)").matcher(msg);
			serverGame.players.clear();
			serverGame.playerByName.clear();
			while (playerMatcher.find()) {
				String[] parse = playerMatcher.group(1).split("\\,", 2);
				String name = parse[0];
				String cards = parse[1];
				ServerGame.Player player = serverGame.new Player(name, cards);
				serverGame.playerByName.put(name, player);
				serverGame.players.add(player);
			}
		}

		private Player parsePlayer(String msg) {
			String[] parse = msg.split("\\,", 2);
			String name = parse[0];
			String cards = parse[1];

			ServerGame.Player player = serverGame.playerByName.get(name);
			if (player != null) {
				player.cards = cards;
			} else
				System.out.println("Error! Player does not exist: " + name);
			return player;
		}

		private void parseTerritories(String msg) {
			Matcher territoryMatcher = Pattern.compile("territory\\((.*?)\\)").matcher(msg);
			while (territoryMatcher.find()) {
				parseTerritory(territoryMatcher.group(1));
			}
		}

		private Territory parseTerritory(String msg) {
			String[] parse = msg.split("\\,", 3);
			String name = parse[0];
			String player = parse[1];
			String troops = parse[2];
			Territory territory = serverGame.territoriesByName.get(name);
//				System.out.println(territory + "\t" + player + "\t" + playerByName.get(player));
			territory.player = serverGame.playerByName.get(player);
			territory.troops = Integer.parseInt(troops);
			return territory;
		}

		private void parseSettings(String msg) {
			String[] parse = msg.split("\\,", 1);
			String map = parse[0];

			serverGame.map = map;

			serverGame.loadMap(map);
		}

		private void parseStatus(String msg) {
			String[] parse = msg.split("\\,", 2);
			String started = parse[0];
			String turn = parse[1];

			serverGame.started = Boolean.parseBoolean(started);
			serverGame.turn = serverGame.playerByName.get(turn);
		}

		private void parseLobby(String msg) {
			String[] parse = msg.split("\\,");
			String serverName = parse[0];
			String started = parse[1];

			serverGame = serverGamesByName.get(serverName);
			if (serverGame == null) {
				serverGamesByName.put(serverName, serverGame = new ServerGame());
				serverGame.name = serverName;
			}
			serverGame.players.clear();
			serverGame.playerByName.clear();
			for (int i = 2; i < parse.length; i++) {
				String name = parse[i];
				Player player = serverGame.new Player(name, "");
				serverGame.playerByName.put(name, player);
				serverGame.players.add(player);
			}
		}

		private void parseJoin(String msg) {
			String[] parse = msg.split("\\,");
			String serverName = parse[0];

			ServerGame oldServer = serverGame;
			if (oldServer != null) {
				oldServer.connections.remove(this);
			}
			serverGame = serverGamesByName.get(serverName);
			if (serverGame == null) {
				serverGamesByName.put(serverName, serverGame = new ServerGame());
				serverGame.name = serverName;
			}
			serverGame.connections.add(this);

		}

		private String getLobbies() {
			return "lobbies("
					+ serverGamesByName.values().parallelStream().map(Object::toString).collect(Collectors.joining(","))
					+ ")";
		}

		void sendToSubscribers(String msg, int seq) {
			for (Connection connection : serverGame.connections)
				connection.sendMessage(msg);
			logWithTime(String.format("[C:%04x S:%08x] broadcasting: %s", clientID, seq, msg));
		}

		@Override
		public boolean equals(Object arg0) {
			if (arg0 instanceof Connection)
				return ((Connection) arg0).clientID == clientID;
			return false;
		}
	}

	void logWithTime(Object msg) {
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime myDateObj = LocalDateTime.now();
		System.out.println(String.format("[%s.%03d] %s", dtf.format(myDateObj), myDateObj.getNano() / 1000000, msg));
	}

	static class ServerGame {

		String name;
		boolean started = false;
		String map = "default1";
		Player turn;

		ArrayList<Territory> territories = new ArrayList<>();
		ArrayList<Player> players = new ArrayList<>();
		Set<Connection> connections = new HashSet<>();

		HashMap<String, ServerGame.Player> playerByName = new HashMap<>();
		HashMap<String, Territory> territoriesByName = new HashMap<>();

		class Player {
			Player(String name, String cards) {
				this.name = name;
				this.cards = cards;
			}

			String name;
			// 0 - empty
			// i - infantry
			// c - cavalry
			// a - artillery
			String cards;

			@Override
			public String toString() {
				return String.format("player(%s,%s)", name, cards);
			}
		}

		class Territory {
			String name;
			Player player;
			int troops;

			public Territory(String name) {
				this.name = name;
			}

			@Override
			public String toString() {
				return String.format("territory(%s,%s,%d)", name, player == null ? "" : player.name, troops);
			}
		}

		String playersString() {
			return "players(" + players.parallelStream().map(Object::toString).collect(Collectors.joining(",")) + ")";
		}

		String territoriesString() {
			return "territories(" + territories.parallelStream().map(Object::toString).collect(Collectors.joining(","))
					+ ")";
		}

		String statusString() {
			return String.format("status(%b,%s)", started, turn == null ? "" : turn.name);
		}

		String settingsString() {
			return String.format("settings(%s)", map);
		}

		String lobbyString() {
			return "lobby(" + name + "," + started + "," + players.size() + ")";
		}

		public void loadMap(String map) {
			File file = new File("src/maps/" + map + "/data.txt");
			try {
				Scanner sc = new Scanner(file);
				while (sc.hasNext()) {
					String line = sc.nextLine();
					if (line.trim().contentEquals("territory:")) {
						line = sc.nextLine();
						while (!line.trim().contentEquals("territory:")) {
							if (line.trim().startsWith("name:")) {
								String[] parse = line.split("\\:", 2);
								String name = parse[1];
								Territory territory = new Territory(name);
								territories.add(territory);
								territoriesByName.put(name, territory);
								break;
							}
							line = sc.nextLine();
						}
					}
				}
				sc.close();
//				System.out.println(territoriesString());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		try {
			new Server();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
