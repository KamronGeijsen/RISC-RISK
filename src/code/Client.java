package code;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import code.Board.Card;
import code.Board.Territory;
import code.Game.Player;



public class Client {
	Socket clientSocket;
	PrintWriter out;
	BufferedReader in;
	
	

	void nonBlockingHandler() {
		try {
			if(in.ready()) {
				String msg = in.readLine();
				logWithTime("Received: " + msg);
//				if(msg.startsWith("settings(")) parseSettings(msg);
//				else if(msg.startsWith("status(")) parseStatus(msg);
//				else if(msg.startsWith("players(")) parsePlayers(msg);
//				else if(msg.startsWith("player(")) parsePlayer(msg);
//				else if(msg.startsWith("territories(")) parseTerritories(msg);
//				else if(msg.startsWith("territory(")) parseTerritory(msg);
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	Thread parallelHandler = new Thread() {
//		public void run() {
//			
//		}
//	};


/**
	// client to server
	// [Game]: join <Player p>
	// [Game]: leave <Player p>
	// [Game]: reinforce <Player p> <Territory to> <amount>
	// [Game]: attack <Player p> <Territory from> <Territory to> <amount att> <amount def>
	// [Game]: move <Player p> <Territory from> <Territory to> <amount>
	// [Game]: endturn <Player p1> <Player p2>
	
	// server to client
	// [Game]: setmap <Map map> <Player p1> <Player p2> ... <Player p_n>
	// [Game]: reinforce <Player p> <Territory to> <amount>
	// [Game]: attack <Player p> <Territory from> <Territory to> <amount att> <amount def>
	// [Game]: move <Player p> <Territory from> <Territory to> <amount>
	// [Game]: endturn <Player p1> <Player p2>
*/
	// client to server
	// [Game]: lobbies(lobby(<name> <started> <count>) .. lobby(<name> <started> <count>))
	// [Game]: join(<name>)
	// [Game]: settings(<Map m>)
	// [Game]: status(<boolean started> <str turn>)
	// [Game]: territories(territory(<name> <Player p> <amount>) .. territory(<name> <Player p> <amount>))
	// [Game]: players(player(<Player p1> <cards> .. <cards>) .. player(<Player p1> <cards> .. <cards>))
	// [Game]: territory(<Territory t> <Player p> <amount>)
	// [Game]: player(<Player p1> <cards> .. <cards>)
	
	// server to client
	// [Game]: lobbies(lobby(<name> <started> <count>) .. lobby(<name> <started> <count>))
	// [Game]: settings(<Map m>)
	// [Game]: status(<boolean started> <str turn>)
	// [Game]: territories(territory(<name> <Player p> <amount>) .. territory(<name> <Player p> <amount>))
	// [Game]: players(player(<Player p1> <cards> .. <cards>) .. player(<Player p1> <cards> .. <cards>))
	// [Game]: territory(<Territory t> <Player p> <amount>)
	// [Game]: player(<Player p1> <cards> .. <cards>)
	

	void attemptConnect() {
		try {
			clientSocket = new Socket("192.168.178.29", 4444);
//			clientSocket = new Socket("83.84.18.28", 4444);
			try {
				System.out.println(clientSocket);
				out = new PrintWriter(clientSocket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

				try {
					String msg = String.format("init: %s", InetAddress.getLocalHost().getHostName());
					sendMessageAndLog(msg);
					String initStatus = waitForMessageAndLog();
					if(initStatus.startsWith("503 ")) {
						throw new IOException("488 Not Acceptable");
					}
				} catch (IOException e) {
					System.err.println("Connection initialization failed");
				}
			} catch (IOException e) {
				System.err.println("I/O streams failed");
			}
		} catch (IOException e) {
			System.err.println("Connection timeout");
		}
		
		
		
	}
	void sendMessage(String msg) {
		out.println(msg);
	}
	String waitForMessage() throws IOException {
		return in.readLine();
	}
	void sendMessageAndLog(String msg) {
		out.println(msg);
		logWithTime("[Client]: " + msg);
	}
	String waitForMessageAndLog() throws IOException {
		String msg = in.readLine(); 
		logWithTime("[Server]: " + msg);
		return msg;
	}
	
//
//	void parseSettings(String msg) {
//		Matcher statusMatcher = Pattern.compile("settings\\((.*?)\\)").matcher(msg);
//		if(statusMatcher.find()) {
//			String[] parse = statusMatcher.group(1).split("\\,");
//			String map = parse[0];
//			File file = new File("src/maps/" + map);
//			try {
//				long startTime = System.currentTimeMillis();
//				BufferedImage image = ImageIO.read(new File(file, "map.png"));
//				String data = new String(Files.readAllBytes(new File(file, "data.txt").toPath()));
//				GameScreen.game.board = new Board(image, data);
//				System.out.println(System.currentTimeMillis() - startTime + "s");
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			GameScreen.startScreen = null;
//		}
//	}
//	private void parseStatus(String msg) {
//		Matcher statusMatcher = Pattern.compile("status\\((.*?)\\)").matcher(msg);
//		if(statusMatcher.find()) {
//			String[] parse = statusMatcher.group(1).split("\\,",2);
//			String started = parse[0];
//			String turn = parse[1];
//			
//			if(!Boolean.parseBoolean(started))
//				return;
////			GUI.game.turn = playerByName.get(turn);
//			GameScreen.game.setTurn(playerByName.get(turn));
//		}
//	}
//	
//	HashMap<String, Player> playerByName = new HashMap<>();
//	
//	void parsePlayers(String msg) {
//		Matcher players = Pattern.compile("players\\((.*)\\)").matcher(msg);
//		if(players.find()) {
//			msg = players.group(1);
//			Matcher playerMatcher = Pattern.compile("player\\((.*?)\\)").matcher(msg);
//			GameScreen.game.players.clear();
//			for(int i = 0; playerMatcher.find(); i++) {
//				String[] parse = playerMatcher.group(1).split("\\,",2);
//				String name = parse[0];
//				String cardsString = parse[1];
//				ArrayList<Card> cards = new ArrayList<>();
//				for(char c : cardsString.toCharArray()) 
//					cards.add(new Card(c));
//				Player player = playerByName.getOrDefault(name, new Player(name, cards, Player.colorPallet[i]));
//				playerByName.put(name, player);
//				GameScreen.game.players.add(player);
//			}
//		}
////		GameScreen.playerDetails.update();
//		
//	}
//	void parsePlayer(String msg) {
//		Matcher playerMatcher = Pattern.compile("player\\((.*?)\\)").matcher(msg);
//		if(playerMatcher.find()) {
//			String[] parse = playerMatcher.group(1).split("\\,",2);
//			String name = parse[0];
//			String cardsString = parse[1];
//			ArrayList<Card> cards = new ArrayList<>();
//			for(char c : cardsString.toCharArray()) 
//				cards.add(new Card(c));
//			
//			Player player = playerByName.get(name);
//			if(player != null) 
//				player.cards = cards;
//			else
//				System.out.println("Error! Player does not exist: " + name);
//		}
//	}
//	
//	private void parseTerritories(String msg) {
//		Matcher territoriesMatch = Pattern.compile("territories\\((.*)\\)").matcher(msg);
//		if(territoriesMatch.find()) {
//			parseTerritory(territoriesMatch.group(1));
//		}
//	}
//	private void parseTerritory(String msg) {
//		Matcher territoryMatcher = Pattern.compile("territory\\((.*?)\\)").matcher(msg);
//		while(territoryMatcher.find()) {
//			String[] parse = territoryMatcher.group(1).split("\\,",3);
//			String name = parse[0];
//			String player = parse[1];
//			String troops = parse[2];
//			Territory territory = GameScreen.game.board.territoriesByName.get(name);
//			territory.player = playerByName.get(player);
//			territory.troops = Integer.parseInt(troops);
//		}
//	}
//	void joinServer(String name) {
//		sendMessageAndLog(String.format("join(%s)", name));
//	}
//	void joinAsPlayer(String name) {
//		Player player = new Player(name, new ArrayList<>(), Player.colorPallet[GameScreen.game.players.size()]);
//		playerByName.put(name, player);
//		GameScreen.game.players.add(player);
//		GameScreen.game.player = player;
//		sendMessageAndLog(playersString());
//		try {
//			waitForMessage();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
////		GameScreen.playerDetails.update();
//	}
//	
//
//
//	public void initBoard() {
//		sendMessageAndLog(String.format("settings(%s)", "default1"));
//		try {
//			waitForMessage();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		sendMessageAndLog(String.format("status(%b,%s)", true, GameScreen.game.player.name));
//		try {
//			waitForMessage();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		sendMessageAndLog(territoriesString());
//		try {
//			waitForMessage();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		for(Player p : GameScreen.game.players) {
//			updatePlayer(p);
//		}
//	}
//	void updateTerritory(Territory territory) {
//		sendMessageAndLog(territory.toString());
//		try {
//			waitForMessage();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	public void endTurn() {
//		int playerIndex = (GameScreen.game.players.indexOf(GameScreen.game.player) + 1) % GameScreen.game.players.size();
//		Player nextPlayerTurn = GameScreen.game.players.get(playerIndex);
//		sendMessageAndLog(String.format("status(%b,%s)", true, nextPlayerTurn.name));
//		GameScreen.game.setTurn(nextPlayerTurn);
//	}
//
//	public void updatePlayer(Player player) {
//		sendMessage(player.toString());
//		try {
//			waitForMessage();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	String playersString() {
//		return "players(" + GameScreen.game.players.parallelStream().map(Object::toString).collect(Collectors.joining(" ")) + ")";
//	}
//	String territoriesString() {
//		return "territories(" + Arrays.stream(GameScreen.game.board.territories).map(Object::toString).collect(Collectors.joining(" ")) + ")";
//	}
	
//	public static void main(String[] args) {
//		System.out.println(Arrays.toString("50,".split("\\,")));
//		System.out.println(new String(new char[] {35,35,35,0,0}).trim());
//		System.out.println("next");
//		parsePlayers("players(player(0,1) player(0,3))");
//		System.out.println("next");
//		parsePlayers("players(player(0,1) player(0,bar,1,2,3) player(0,Hello my name is foo))");
//		System.out.println("next");
//		parsePlayers("");
//	}
	
	void logWithTime(String msg) {
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime myDateObj = LocalDateTime.now();
		System.out.println(String.format("Client: [%s.%03d] %s", dtf.format(myDateObj), myDateObj.getNano()/1000000, msg));
		
	}

//	public static void main(String[] args) {
//		new GameScreen();
//	}
	

}
