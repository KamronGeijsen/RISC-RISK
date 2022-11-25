package code;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import code.Window.Screen;
import server.Message;
import server.User;

public class StartScreen extends Screen {
	
	
	Sidebar sideBar = new Sidebar(300);
	LobbyList lobbyList = new LobbyList(100, 100, window.WIDTH - 100 - sideBar.w, window.HEIGHT- 100);
	
	public StartScreen(Window window) {
		this.window = window;
		
	}
	
	void onResize(ComponentEvent e) {
		lobbyList.resize();
	}
	
	void autofillLogin() {
		File configFile = new File("client/config.txt");
		try {
			Scanner sc = new Scanner(configFile);
			
			HashMap<String, String> map = new HashMap<String, String>();
			while(sc.hasNextLine()) {
				String line = sc.nextLine();
				String[] parse = line.split(":", 2);
				if(parse.length == 2) {
//					return;
					map.put(parse[0], parse[1]);
				}
				
			}
			
			System.out.println(map);
			if(map.containsKey("user")) {
				sideBar.userTextBox.text = map.get("user");
			}
			if(map.containsKey("pass")) {
				sideBar.passTextBox.text = map.get("pass");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		
	}
	
	void login() {
		String user = sideBar.userTextBox.text;
		String passHash;
		if(sideBar.passTextBox.text.length() > 80) {
			passHash = sideBar.passTextBox.text;
		} else {
			passHash = User.clientHash(sideBar.passTextBox.text, sideBar.userTextBox.text);
		}
		
		try {
			if(User.userDB.size() == 0)
				User.loadDB();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(user + "\t" + passHash);
		User.msgLogin(new Message("login", user, passHash));
	}

	public void draw(Graphics2D g) {
		g.setBackground(new Color(96, 96, 96));
		g.clearRect(0, 0, window.WIDTH, window.HEIGHT);
		
		sideBar.draw(g);
		lobbyList.draw(g);
	}
	
	/**
	New single player
	Load single player
	New multi player friends
	Join multi player friends
	(Load multi player friends)
	Join multi player ranked
	
	
	 */
	
	class Sidebar {
		Sidebar(int w){
			this.w = w;
			passTextBox.password = true;
		}
		final int w;
		
		final static int BUTTON_WIDTH = 200, BUTTON_HEIGHT = 60;
		final static int BUTTON_X = 50;
		final TextBox userTextBox = new TextBox(BUTTON_X, 80, BUTTON_WIDTH, 50, "");
		final TextBox passTextBox = new TextBox(BUTTON_X, 150, BUTTON_WIDTH, 50, "");
		int sel = -1;
		
		boolean mouseOver(int ax, int ay) {
			final int x = ax - (window.WIDTH-w);
			
			return x >= 0;
		}
		
		boolean mouseDrag(int ax, int ay) {
			return false;
		}
		
		Button mousePress(int ax, int ay) {
			return null;
		}
		
		void mouseDown(int ax, int ay) {
			final int x = ax - (window.WIDTH-w);
		}
		
		void updateSize() {
			
		}

		// Changes with: 
		//  WIDTH, HEIGHT
		//  w (constant)
		//  slider
		//    init()/deactivate()
		//    slide
		//    mouseover
		//  textBox
		//    type
		//    mouseover
		//  buttons
		//    active/select
		//    mouseover
		
		void draw(Graphics2D g) {

			
			final int x = window.WIDTH-w;
			g.translate(x, 0);
			
			g.setColor(new Color(32,32,32,240));
			g.fillRect(0, 0, w, window.HEIGHT);
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
			g.drawLine(0, 0, 0, window.HEIGHT);
			
			
			userTextBox.draw(g);
			passTextBox.draw(g);
			
			
			
				
			
			
			g.translate(-x, 0);
		}
		
		class Button {
			Button(int x, int y, int w, int h, String text){
				this.x = x;
				this.y = y;
				this.w = w;
				this.h = h;
				this.text = text;
				
			}
			
			int x, y, w, h;
			final String text;
			
			boolean active;
			Font font = new Font(Font.DIALOG, Font.PLAIN, 30);
		}
		
		class Slider {
			Slider(int x, int y, int w, int h){
				this.x = x;
				this.y = y;
				this.w = w;
				this.h = h;
			}
			int x, y, w, h;
			
			boolean active;
			boolean drag;
			int min = 3, max = 10;
			int slider = max;
			
			Font font = new Font(Font.DIALOG, Font.PLAIN, 20);
			
			void init(int min, int max) {
				this.min = min; 
				this.max = max;
				slider = max;
				active = true;
				userTextBox.text = max+"";
				userTextBox.active = true;
				passTextBox.text = max+"";
				passTextBox.active = true;
			}
			void deactivate() {
				min = 0;
				max = 1;
				slider = max;
				active = false;
				userTextBox.text = "";
				userTextBox.active = true;
				passTextBox.text = "";
				passTextBox.active = true;
			}
		}
		
		class TextBox {
			TextBox(int x, int y, int w, int h, String text){
				this.x = x;
				this.y = y;
				this.w = w;
				this.h = h;
				this.text = text;
			}
			
			int x, y, w, h;
			String text;
			
			boolean active = true;
			boolean password = false;
			Font font = new Font(Font.DIALOG, Font.PLAIN, 30);
			int cursorS, cursorEnd;
			
			void draw(Graphics2D g) {

				g.setColor(new Color(32,32,32));
				g.fillRect(x, y, w, h);

				g.setColor(Color.BLACK);
				g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
				g.drawRect(x, y, w, h);
//				g.drawRect(textBox.x+textBox.w-textBox.h, textBox.y, textBox.h, textBox.h);
				String text = this.text;
				if(password)
					text = text.replaceAll(".", "*");
				g.setColor(Color.WHITE);
				g.setFont(font);
				g.drawString(text, x + 15, y + (int)(h*1.15 + font.getSize()/2)/2);
			}
			
			void keyTyped(KeyEvent e) {
				int k = e.getKeyCode();
				char c = e.getKeyChar();
				
				int mask = e.getModifiers();
				final int MASK_SHIFT = 1;
				final int MASK_CTRL = 2;
				final int MASK_HOME = 4;
				final int MASK_ALT = 8;
				
				System.out.println(mask);
				if(e.isControlDown()) {
//					if(k == KeyEvent.VK_C) {
//						StringSelection selection = new StringSelection("asdf");
//						Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//						clipboard.setContents(selection, selection);
//					} else if (k == KeyEvent.VK_V) {
//						try {
//							Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//							Transferable t = clipboard.getContents(null);
//							String data = ((String) t.getTransferData(DataFlavor.stringFlavor));
//							for(char cd : data.toCharArray()){
//								System.out.println(cd);
//							}
//							if(cursorS == cursorEnd) {
//								text = text.substring(0, cursorS) + data + text.substring(cursorS);
//								updateCursorSel(cursorS + data.length());
//							}
//						} catch (UnsupportedFlavorException | IOException e1) {
//							e1.printStackTrace();
//						}
////						}
//					}
//					
				}
				else {
					if(k == KeyEvent.VK_HOME) {
						cursorEnd = cursorS = 0;
					} else if(k == KeyEvent.VK_END) {
						cursorEnd = cursorS = text.length();
					} else if(k == KeyEvent.VK_LEFT || k == KeyEvent.VK_RIGHT) {
						if(cursorS == cursorEnd) {
							cursorEnd = cursorS = cursorS + k - 38;
						}
					} else if(k == KeyEvent.VK_BACK_SPACE) {
						if(cursorS == cursorEnd) {
							if(cursorS > 0) {
								text = text.substring(0, cursorS-1) + text.substring(cursorS);
								cursorEnd = cursorS = cursorS - 1;
							}
						}
					} else if(k == KeyEvent.VK_DELETE) {
						if(cursorS == cursorEnd) {
							if(cursorS < text.length()) {
								text = text.substring(0, cursorS) + text.substring(cursorS+1);
							}
						}
					} else if(k != KeyEvent.VK_CAPS_LOCK
							&& k != KeyEvent.VK_SHIFT
							&& k != KeyEvent.VK_CONTROL
							&& k != KeyEvent.VK_ALT){
						if(cursorS == cursorEnd) {
//							if((c+"").matches("[a-zA-Z0-9_\\,\\.]"))
							if(0x21 <= c && c <= 0x7e) {
								text = text.substring(0, cursorS) + c + text.substring(cursorS);
								cursorS++;
								cursorEnd=cursorS;
							}
							
						}
						
					}
				}
				
			}
		}
	}

	class LobbyList {
		int x, y, w, h;
		Entry[] entries;
		
		public LobbyList(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}
		
		void resize() {
			w = window.WIDTH-100 - x - sideBar.w;
			h = window.HEIGHT-100 - y;
		}
		
		void draw(Graphics2D g) {
			g.setColor(Color.black);
			g.drawRect(x, y, w, h);
		}
		
		class Entry {
			String name;
			String map;
			String gamemode;
			String status;
			
			void draw(Graphics2D g) {

			}
		}
	}
}
