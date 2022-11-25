package code;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import code.Board.Territory;
import code.Board.Territory.Border;
import code.Game.Player;
import code.Window.Screen;

public class GameScreen extends Screen {




	
	GameScreen(Window window){
		this.window = window;
		long startTime = System.currentTimeMillis();
		
		game = new Game(this);
		
		System.out.println(System.currentTimeMillis() - startTime + "s");
		System.out.println(System.currentTimeMillis() - startTime + "s");
	}
	static StartScreen startScreen;
	static Game game;
	
	void drawBoard(Graphics2D g) {
		final AffineTransform before = g.getTransform();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		g.translate(view.x, view.y);
		g.scale(view.scale, view.scale);
		g.setColor(Color.black);
		g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] { 9 }, 0));
		for (Territory t : game.board.territories) {
			t.drawOffshoreBorders(g);
		}
		g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (Territory t : game.board.territories)
			t.draw(g);
		if(game.from != null) {
			g.setColor(Color.WHITE);
			g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] { 20, 30 }, 2));
			for(Polygon p : game.from.polygons)
				g.drawPolygon(p);
			for(Polygon p : game.selected.polygons)
				g.drawPolygon(p);
		}
		
//		g.setColor(Color.black);
//		for (Point p : board.vectors)
//			g.drawLine(board.points.get(p.x).x, board.points.get(p.x).y, board.points.get(p.y).x,
//					board.points.get(p.y).y);
//		for(Point p : board.points)
//			g.fillOval(p.x-4, p.y-4, 9, 9);

		if(sidebar.selected == sidebar.attack) {
			if(game.from == game.selected) {
				g.setColor(Color.WHITE);
				for (Border b : game.selected.neighbouring) {
					if(b.bordering.highlight) {
						game.from.drawBorder(g, b.bordering);
					}
				}
			} else {
				g.setColor(Color.WHITE);
				game.from.drawBorder(g, game.selected);
			}
			
		}
		
		g.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (Territory t : game.board.territories)
			t.drawLabels(g);

//		g.setColor(Color.green);
//		for(Territory t : board.territories){
//			for(Border n : t.neighbouring) {
//				Territory from  = t;
//				Territory to  = n.bordering;
//				g.drawLine((int)from.mx, (int)from.my, (int)to.mx, (int)to.my);
//		        
//			}
//		}
		
		g.setTransform(before);
	}

	public void draw(Graphics2D g) {
		if(game.board == null)
			return;
		

		g.setBackground(new Color(0, 128, 255));
		g.clearRect(0, 0, window.WIDTH, window.HEIGHT);
		
		drawBoard(g);

		sidebar.draw(g);
		
		if(dialogueBox != null)
			dialogueBox.draw(g);
		if(details != null && details.visible)
			details.draw(g);
		
		playerDetails.draw(g);
		
	}
	
	final DialogueBox END_TURN_CONFIRM = new DialogueBox(500, 300, 
			"Are you sure you want to\n end your turn?", 
			new String[]{"End turn", "Cancel"}) ;
	final DialogueBox SKIP_REINFORCE_CONFIRM = new DialogueBox(500, 300, 
			"Are you sure you want to\n" +
			"attack? You will not be\n" +
			"able to complete deployments", 
			new String[]{"Continue", "Cancel"}) ;
	final DialogueBox SKIP_ATTACK_CONFIRM = new DialogueBox(500, 300, 
			"Are you sure you want to\n" +
			"move troops? You will not be\n" +
			"able to attack anymore",
			new String[]{"Continue", "Cancel"}) ;
	DialogueBox dialogueBox = null;
	Sidebar sidebar = new Sidebar(300);
	TroopsDetails details = new TroopsDetails(500, 300);
	PlayerDetails playerDetails = new PlayerDetails();
	View view = new View();

	class View {
		double x, y;
		double scale = 0.8;
		
	}
	
	
	class DialogueBox {
		DialogueBox(int w, int h, String dialogue, String[] choices){
			this.x = (window.WIDTH-w)/2;
			this.y = (window.HEIGHT-h)/2;
			this.w = w;
			this.h = h;
			this.dialogue = dialogue;
			this.choices = choices;
		}
		DialogueBox(int x, int y, int w, int h, String dialogue, String[] choices){
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.dialogue = dialogue;
			this.choices = choices;
		}
		int x, y, w, h;

		final String dialogue;
		final String[] choices;
		int sel = -1;
		
		Font font = new Font(Font.DIALOG, Font.PLAIN, 30);
		
		boolean mouseOver(int ax, int ay) {
			final double tile = 60;
			final int len = choices.length;
			sel = -1;
			for(int i = 0; i < len; i++) {
				double tx = x + (w+tile*1.75)/(len+1)*(i+1) - tile*1.25 - tile*1.75/2;
				double ty = y + h-tile*1.75;
				if(ax >= tx && ax <= tx+tile*2.25 && ay >= ty && ay <= ty+tile)
					sel = i;
			}
			return ax >= x && ax <= x+w && ay >= y && ay <= y+h;
		}
		
		String mouseDown(int ax, int ay) {
			if(sel == -1)
				return null;
			else
				return choices[sel];
		}
		
		void draw(Graphics2D g) {
			x = (window.WIDTH-w)/2;
			y = (window.HEIGHT-h)/2;
			g.translate(x, y);
			final int barheight = 30;
			g.setColor(new Color(32,32,32,240));
			g.fillRect(0, 0, w, h);
			g.setColor(new Color(16,16,16));
			g.fillRect(0, 0, w, barheight);
			
			g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, w, h);
			g.drawRect(0, 0, w, barheight);
			
			g.setColor(Color.WHITE);
			g.setFont(font);
			int line = 0;
			for(String s : dialogue.split("\n")) {
				final int dialogueWidth = g.getFontMetrics().stringWidth(s);
				g.drawString(s, (w-dialogueWidth)/2, barheight + (font.getSize()+3) * ++line + 20);
			}
			
			
			final double tile = 60;
			final int len = choices.length;
			for(int i = 0; i < len; i++) {
				double tx = (w+tile*1.75)/(len+1)*(i+1) - tile*1.25 - tile*1.75/2;
				double ty = h-tile*1.75;
				g.setColor(sel==i?new Color(96,96,96,240):new Color(64,64,64,240));
				g.fillRect((int)tx, (int)ty, (int)(tile*2.5), (int)(tile));
				g.setColor(Color.BLACK);
				g.drawRect((int)tx, (int)ty, (int)(tile*2.5), (int)(tile));
				
				g.setColor(Color.WHITE);
				g.setFont(font);
				final int textWidth = g.getFontMetrics().stringWidth(choices[i]);
				g.drawString(choices[i], (int)(tx+(tile*2.5-textWidth)/2), (int)(ty+(tile*1.1+font.getSize()/2)/2));
			}
			
			
			g.translate(-x, -y);
		}
		
	}
	class Sidebar {
		Sidebar(int w){
			this.w = w;
			textBoxEnter.font = new Font(Font.MONOSPACED, Font.BOLD, 30);
			textBoxEnter.active = false;
		}
		final int w;
		
		final static int BUTTON_WIDTH = 200, BUTTON_HEIGHT = 60;
		final static int BUTTON_X = 50;
		final Button reinforce = new Button(BUTTON_X, 80, BUTTON_WIDTH, BUTTON_HEIGHT, "Reinforce");
		final Button attack = new Button(BUTTON_X, 160, BUTTON_WIDTH, BUTTON_HEIGHT, "Attack");
		final Button move = new Button(BUTTON_X, 240, BUTTON_WIDTH, BUTTON_HEIGHT, "Move");
		final Button endTurn = new Button(BUTTON_X, 700, BUTTON_WIDTH, BUTTON_HEIGHT*2, "End Turn");
		final TextBox textBox = new TextBox(BUTTON_X, 380, BUTTON_WIDTH, 50, "-");
		final Button textBoxEnter = new Button(BUTTON_X+(BUTTON_WIDTH-50), textBox.y, 50, 50, ">");
		final Slider slider = new Slider(145, textBox.y + textBox.h + 20, 10, 0);
		int sel = -1;
		Button[] buttons = {reinforce, attack, move, endTurn, textBoxEnter};
		Button selected;
		
		boolean mouseOver(int ax, int ay) {
			final int x = ax - (window.WIDTH-w);
			sel = -1;
			for(int i = 0; i < buttons.length; i++) {
				Button b = buttons[i];
				if(x >= b.x && x <= b.x+b.w && ay >= b.y && ay <= b.y+b.h)
					sel = i;
			}
			
			int y = slider.y + (int)((slider.h - 20) * ((double)(slider.max - slider.slider)/(slider.max - slider.min)));
			if(x >= 125 && x <= 125 + 50 && ay >= y && ay <= y + 20 || slider.drag)
				sel = buttons.length; //6
			
			return x >= 0;
		}
		
		boolean mouseDrag(int ax, int ay) {
			if(slider.drag) {
				System.out.println("weh weh");
				final double s = Math.max(0, Math.min(1, ((double)ay - slider.y - 10)/ (slider.h - 20)));
				final int val = slider.max - (int) Math.round(s * (slider.max-slider.min) + slider.min) + slider.min;
				slider.slider = val;
				textBox.text = val+"";
				sel = buttons.length; //6
			}
			return slider.drag;
		}
		
		Button mousePress(int ax, int ay) {
			
			if(sel >= 0 && sel < buttons.length)
				return buttons[sel];
			return null;
		}
		
		void mouseDown(int ax, int ay) {
			final int x = ax - (window.WIDTH-w);
			int y = slider.y + (int)((slider.h - 20) * ((double)(slider.max - slider.slider)/(slider.max - slider.min)));
			slider.drag = x >= 125 && x <= 125 + 50 && ay >= y && ay <= y + 20 && slider.active;
		}
		
		void updateSize() {
			endTurn.y = window.HEIGHT - (endTurn.h + 50);
			slider.h = endTurn.y-( + 20 + textBox.y + textBox.h + 20);
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
			
			g.setColor(new Color(32,32,32));
			if(!slider.active)
				g.setColor(new Color(96,96,96));
			g.fillRect(textBox.x, textBox.y, textBox.w, textBox.h);
			g.setColor(new Color(64,64,64));
//			g.fillRect(textBox.x+textBox.w-textBox.h, textBox.y, textBox.h, textBox.h);
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
			g.drawRect(textBox.x, textBox.y, textBox.w, textBox.h);
//			g.drawRect(textBox.x+textBox.w-textBox.h, textBox.y, textBox.h, textBox.h);
			g.setColor(Color.WHITE);
			g.setFont(textBox.font);
			g.drawString(textBox.text, textBox.x + 15, textBox.y + (int)(textBox.h*1.15 + textBox.font.getSize()/2)/2);
			
			g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
			for(int i = 0; i < buttons.length; i++) {
				Button b = buttons[i];
				if(b == selected) {
//					g.setColor(sel==i?new Color(128,128,128):new Color(80,80,80));
//					g.fillRect(b.x-(int)(b.w*0.1), b.y-(int)(b.h*0.1), (int)(b.w*1.2), (int)(b.h*1.2));
//					g.setColor(Color.BLACK);
//					g.setStroke(new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
//					g.drawRect(b.x-(int)(b.w*0.1), b.y-(int)(b.h*0.1), (int)(b.w*1.2), (int)(b.h*1.2));
//					g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
				} else {
					g.setColor(sel==i?new Color(96,96,96):new Color(64,64,64));
					if(!b.active)
						g.setColor(new Color(96,96,96));
					g.fillRect(b.x, b.y, b.w, b.h);
					g.setColor(Color.BLACK);
					g.drawRect(b.x, b.y, b.w, b.h);
				}
				
				
				g.setColor(Color.WHITE);
				if(!b.active)
					g.setColor(Color.LIGHT_GRAY);
				g.setFont(b.font);
				final int textWidth = g.getFontMetrics().stringWidth(b.text);
				g.drawString(b.text, b.x + (b.w - textWidth)/2, b.y + (int)(b.h*1.1 + b.font.getSize()/2)/2);
			}
			
			g.setColor(new Color(64,64,64));
			if(!slider.active)
				g.setColor(new Color(96,96,96));
			g.fillRect(145, slider.y, 10, slider.h);
			g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
			g.setColor(Color.BLACK);
			g.drawRect(145, slider.y, 10, slider.h);
			
			
			g.setColor(Color.BLACK);
			if(!slider.active)
				g.setColor(Color.DARK_GRAY);
			g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(115, slider.y+10, 130, slider.y+10);
			g.drawLine(115, slider.y+slider.h-10, 130, slider.y+slider.h-10);
			g.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			if(slider.active) 
			for(int i = 0; i < slider.max - slider.min; i++) {
				g.drawLine(118, slider.y+(int)((slider.h-20)*(double)i/(slider.max - slider.min))+10, 
						125, slider.y+(int)((slider.h-20)*(double)i/(slider.max - slider.min))+10);
			}
			if(slider.active) {
				g.setColor(Color.WHITE);
				g.setFont(slider.font);
				{final int textWidth = g.getFontMetrics().stringWidth(slider.max+"");
				g.drawString(slider.max+"", 115 - textWidth - 10, slider.y + (int)(slider.font.getSize()*0.85));}
				{final int textWidth = g.getFontMetrics().stringWidth(slider.min+"");
				g.drawString(slider.min+"", 115 - textWidth - 10, slider.y + slider.h - 3);}
			}
			
			
			final int sliderY = slider.y + (int)((slider.h - 20) * ((double)(slider.max - slider.slider)/(slider.max - slider.min)));
			g.setColor(sel==buttons.length?new Color(48,48,48):new Color(32,32,32));
			if(!slider.active)
				g.setColor(new Color(128,128,128));
			g.fillRect(125, sliderY, 50, 20);
			g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
			g.setColor(Color.BLACK);
			g.drawRect(125, sliderY, 50, 20);
			g.setColor(Color.WHITE);
			if(!slider.active)
				g.setColor(Color.LIGHT_GRAY);
			g.drawLine(130, sliderY+10, 140, sliderY+10);
			
			
			if(game.turnState.deployableTroops > 0) {
				g.setColor(new Color(32,32,32, 240));
				g.fillRect(-250, 50, 250, 150);
				g.setColor(Color.BLACK);
				g.setStroke(new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
				g.drawRect(-250, 50, 250, 150);
				g.setColor(Color.WHITE);
				{g.setFont(new Font(Font.DIALOG, Font.PLAIN, 30));
				final String text = "Deploy troops:";
				final int textWidth = g.getFontMetrics().stringWidth(text);
				g.drawString(text, -(250 + textWidth)/2, 100);}
				{g.setFont(new Font(Font.DIALOG, Font.PLAIN, 50));
				final String text = game.turnState.deployableTroops+"";
				final int textWidth = g.getFontMetrics().stringWidth(text);
				g.drawString(text, -(250 + textWidth)/2, 170);}
			}
				
			
			
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
				textBox.text = max+"";
				textBox.active = true;
				textBoxEnter.active = true;
			}
			void deactivate() {
				min = 0;
				max = 1;
				slider = max;
				active = false;
				textBox.text = "-";
				textBox.active = false;
				textBoxEnter.active = false;
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
			
			boolean active;
			Font font = new Font(Font.DIALOG, Font.PLAIN, 30);
		}
	}
	class TroopsDetails {
		TroopsDetails(int w, int h){
			this.w = w;
			this.h = h;
		}
		
		int x, y, w, h;
		TerritoryDetails from = new TerritoryDetails(x, y, w, h);
		TerritoryDetails to = new TerritoryDetails(x, y, w, h);
		TerritoryDetails[] territoryDetails = {from, to};
		
		boolean visible;

		public void init(Territory from, Territory to) {
			this.from.init(from);
			this.to.init(to);
			visible = true;
		}
		
		void close() {
			this.visible = false;
		}
		
		void updateSize() {
			final int offset = 50;
			from.x = 50;
			from.y = 50;
			from.w = (w-offset*3)/2;
			from.h = from.w;
			
			to.x = 100+from.w;
			to.y = 50;
			to.w = from.w;
			to.h = from.w;
			System.out.println("w=" + to.w);
		}
		
		void draw(Graphics2D g) {
			x = (window.WIDTH-w)/2;
			y = (window.HEIGHT-h);
			g.translate(x, y);
			
//			final int barheight = 30;
			g.setColor(new Color(32,32,32,240));
			g.fillRect(0, 0, w, h);
//			g.setColor(new Color(16,16,16));
//			g.fillRect(0, 0, w, barheight);
			
			g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, w, h);
//			g.drawRect(0, 0, w, barheight);
			
			for(int i = 0; i < 2; i++) {
				TerritoryDetails t = territoryDetails[i];
				g.setColor(new Color(64,64,64));
				g.fillRect(t.x, t.y, t.w, t.h);
				g.setColor(Color.BLACK);
				g.drawRect(t.x, t.y, t.w, t.h);
				
				g.translate(t.x, t.y);
				g.setColor(t.territory.player.color);
				for(Polygon p : t.polygons)
					g.fillPolygon(p);
				g.setColor(Color.BLACK);
				for(Polygon p : t.polygons)
					g.drawPolygon(p);
				g.translate(-t.x, -t.y);
			}
			
			
			g.translate(-x, -y);
		}
		
		class TerritoryDetails {
			
			TerritoryDetails(int x, int y, int w, int h){
				this.x = x;
				this.y = y;
				this.w = w;
				this.h = h;
			}
			
			void init(Territory territory) {
				this.territory = territory;
				final int len = territory.polygons.length;
				Rectangle2D bounds = territory.polygons[0].getBounds2D();
				double minX = bounds.getMinX(), minY = bounds.getMinY();
				double maxX = bounds.getMaxX(), maxY = bounds.getMaxY();
				for(int i = 1; i < len; i++) {
					bounds = territory.polygons[i].getBounds2D();
					minX = Math.min(minX, bounds.getMinX());
					minY = Math.min(minY, bounds.getMinY());
					maxX = Math.max(maxX, bounds.getMaxX());
					maxY = Math.max(maxY, bounds.getMaxY());
				}
				double scale = Math.min((w-20)/(maxX-minX), (h-20)/(maxY-minY));
				double x = -10+minX*scale;
				double y = -10+minY*scale;
				if((maxX-minX) > (maxY-minY)) {
					y -= ((h-20)-(maxY-minY)*scale)/2;
					System.out.println(h-20 + "\t" + (maxY-minY) + "\t" + scale);
				}
				else {
					x -= ((w-20)-(maxX-minX)*scale)/2;
					System.out.println(w-20 + "\t" + (maxX-minX) + "\t" + scale);
				}
				
				polygons = new Polygon[len];
				for(int i = 0; i < len; i++) {
					int npoints = territory.polygons[i].npoints;
					Polygon tPolygon = territory.polygons[i];
					polygons[i] = new Polygon(new int[npoints], new int[npoints], npoints);
					
					for(int l = 0; l < npoints; l++) {
						polygons[i].xpoints[l] = (int) (tPolygon.xpoints[l] * scale - x);
						polygons[i].ypoints[l] = (int) (tPolygon.ypoints[l] * scale - y);
					}
				}
			}
			
			int x, y, w, h;
			Territory territory;
			Polygon[] polygons;
		}
	}
	class PlayerDetails {
		public PlayerDetails() {
			y = 100;
		}
		
		int y;
		
		void update() {
			playerBars.clear();
			int i = 0;
			for(Player player : game.players) {
				playerBars.add(new PlayerBar(player, i++));
			}
		}
		
		void draw(Graphics2D g){
			g.translate(0, y);
			
			g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
			
			for(PlayerBar p : playerBars) {
				g.setColor(p.player == game.turn?new Color(64,64,64,240):new Color(32,32,32,240));
				g.fillRect(p.x, p.y, p.w, p.h);
				g.setColor(p.player.color);
				g.fillRect(p.x+p.w, p.y, 10, p.h);
				g.setColor(Color.BLACK);
				g.drawRect(p.x, p.y, p.w, p.h);
				g.drawRect(p.x, p.y, p.w+10, p.h);
				g.setFont(new Font(Font.DIALOG, Font.PLAIN, 30));
				g.setColor(Color.WHITE);
				g.drawString(p.player.name, p.x + 15, p.y + 35);
				
				g.setFont(new Font(Font.SERIF, Font.PLAIN, 20));;
				for(int i = 0; i < p.player.cards.size(); i++) {
					final int w = 15, h = 25;
					final int cx = p.x+p.w-i*(w+5)-15, cy = p.y+p.h-15;
					g.setColor(Color.LIGHT_GRAY);
					g.fillRect(cx, cy, w, h);
					g.setColor(Color.BLACK);
					g.drawRect(cx, cy, w, h);
					if(GameScreen.game.player == p.player)
					g.drawString(p.player.cards.get(i).toString().toUpperCase(), cx+2, cy+h-5);
				}
			}
			
			g.translate(0, -y);
		}
		
		ArrayList<PlayerBar> playerBars = new ArrayList<>();
		
		class PlayerBar {
			public PlayerBar(Player player, int index) {
				w = 200;
				if(player == game.player)
					w = 220;
				h = 50;
				x = 0;
				y = index * (h + 20);
				this.player = player;
			}
			int x, y, w, h;
			Player player;
		}
	}
	
	public static Color blend(Color c0, Color c1) {
		double totalAlpha = c0.getAlpha() + c1.getAlpha();
		double weight0 = c0.getAlpha() / totalAlpha;
		double weight1 = c1.getAlpha() / totalAlpha;

		double r = weight0 * c0.getRed() + weight1 * c1.getRed();
		double g = weight0 * c0.getGreen() + weight1 * c1.getGreen();
		double b = weight0 * c0.getBlue() + weight1 * c1.getBlue();
		double a = Math.max(c0.getAlpha(), c1.getAlpha());

		return new Color((int) r, (int) g, (int) b, (int) a);
	}
	public static Color blend(Color c0, Color c1, int alpha) {
	    double totalAlpha = 255 + alpha;
	    double weight0 = 255 / totalAlpha;
	    double weight1 = alpha / totalAlpha;
	    

	    double r = weight0 * c0.getRed() + weight1 * c1.getRed();
	    double g = weight0 * c0.getGreen() + weight1 * c1.getGreen();
	    double b = weight0 * c0.getBlue() + weight1 * c1.getBlue();
	    System.out.println(r + "\t" + g + "\t" + b);
	    return new Color((int) r, (int) g, (int) b, 255);
	  }
	
	

	@Override
	void onResize(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}
}
