package code;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;

public class Window extends JFrame {
	
	/**
	 * default serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	
	static int WIDTH = 800, HEIGHT = 500;
	static int x = WIDTH / 2, y = HEIGHT / 2;
	
	
	class CA extends ComponentAdapter {
		
		public void componentResized(ComponentEvent e) {
			WIDTH = getWidth();
			HEIGHT = getHeight();
			
			screen.onResize(e);
		}

		public void componentMoved(ComponentEvent e) {

		}
	}
	
	

	Client client;
	Window(){
		
		try {
			client = new Client();
			client.attemptConnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		StartScreen startScreen = new StartScreen(this); 
		GameScreen gameScreen = new GameScreen(this);
		screen = startScreen;
		startScreen.autofillLogin();
		addKeyListener(new StartControls.KA(startScreen));
		addMouseListener(new StartControls.MA(startScreen));
		addMouseWheelListener(new StartControls.MA(startScreen));
		addMouseMotionListener(new StartControls.MMA(startScreen));
		addComponentListener(new CA());
		

		timer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				repaint();
			}
		}, 0, (int)(1000/TARGET_FPS));
		
		
		setTitle("RISC RISK");
		setSize(WIDTH, HEIGHT);
		setResizable(true);
//		setFocusTraversalKeysEnabled(false);

//		try {
//			setIconImage((Image) ImageIO.read(new File("src/main/resources/InvDot.png")));
//		} catch (Exception e) {
//
//		}
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBackground(new Color(0, 128, 255));
		setLocation(1920 / 2 - WIDTH / 2, 1080 / 2 - HEIGHT / 2);
		
		setVisible(true);
	}
	
	Screen screen;

	Timer timer = new Timer();
	
	final double TARGET_FPS = 60.0;
	long lastFrame = 0;
	@Override
	public void paint(Graphics g) {
		BufferedImage bf = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D gf = bf.createGraphics();
		screen.draw(gf);
		g.drawImage(bf, 0, 0, null);
		gf.dispose();
		g.dispose();
//		if(lastFrame + 1000/TARGET_FPS > System.currentTimeMillis())
//			repaint();
	}
	
	
	static abstract class Screen {
		Window window;
		abstract void onResize(ComponentEvent e);
		
		abstract void draw(Graphics2D g);
	}

	public static void main(String[] args) {
		new Window();
	}
}
