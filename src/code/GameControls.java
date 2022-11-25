package code;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;

import code.Board.Territory;
import code.GameScreen.Sidebar.Button;
import code.Window.Screen;

public class GameControls {

	static int x, y; // Relative field
	static int sx, sy; // Relative field start
	static int lx, ly; // Absolute last
	static int ax, ay; // Absolute
	static int asx, asy; // Absolute start

	static boolean moveOnScreen = false;
	static boolean mouseDown = false;
	static boolean dragged = false;
	static boolean shift = false;
	static boolean ctrl = false;
	static boolean tab = false;

	public static class KA extends KeyAdapter {
		Screen screen;
		public KA(Screen screen) {
			this.screen = screen;
		}
		
		public void keyPressed(KeyEvent e) {
			if(e.getKeyCode() == KeyEvent.VK_SPACE) {
				GameScreen.game.initRandom();
			} else if(e.getKeyCode() == KeyEvent.VK_C) {
				GameScreen.game.playCards();
			}
		}

		public void keyReleased(KeyEvent e) {

		}
	}

	public static class MA extends MouseAdapter {
		
		GameScreen gameScreen;
		public MA(GameScreen gameScreen) {
			this.gameScreen = gameScreen;
		}
		
		public void mouseWheelMoved(MouseWheelEvent e) {
			int mr = e.getWheelRotation();
			System.out.println(mr);
			double scaleDiff = Math.pow(0.75, mr);
			gameScreen.view.scale*=scaleDiff;
			
			gameScreen.view.x = gameScreen.view.x*scaleDiff+ax*(1-scaleDiff);
			gameScreen.view.y = gameScreen.view.y*scaleDiff+ay*(1-scaleDiff);
		}

		public void mousePressed(MouseEvent e) {
			asx = ax = e.getX();
			asy = ay = e.getY();
			sx = x = (int) ((ax-gameScreen.view.x)/gameScreen.view.scale);
			sx = y = (int) ((ay-gameScreen.view.y)/gameScreen.view.scale);
			
			if(gameScreen.dialogueBox != null){
				if(!gameScreen.dialogueBox.mouseOver(ax, ay)) {
					gameScreen.dialogueBox = null;
					moveOnScreen = true;
				}
			} else if(gameScreen.sidebar.mouseOver(ax, ay)) {
				gameScreen.sidebar.mouseDown(ax, ay);
			} else {
				moveOnScreen = true;
				System.out.println(ax + "\t" + ay);
				System.out.println(gameScreen.sidebar.mouseOver(ax, ay));
			}
			gameScreen.game.updateHighlights();
			
			lx = ax;
			ly = ay;
		}

		public void mouseReleased(MouseEvent e) {
			ax = e.getX();
			ay = e.getY();
			x = (int) ((ax-gameScreen.view.x)/gameScreen.view.scale);
			y = (int) ((ay-gameScreen.view.y)/gameScreen.view.scale);
			
			

			gameScreen.sidebar.slider.drag = false;
			System.out.println(asx+"\t"+ asy);
			if(Point.distance(ax, ay, asx, asy) < 20) {
				System.out.println("wahah");
				if(gameScreen.dialogueBox != null){
					String choice = gameScreen.dialogueBox.mouseDown(ax, ay);
					if(gameScreen.dialogueBox == gameScreen.END_TURN_CONFIRM) {
						if(choice != null) {
							if(choice.contentEquals("End turn")) {
								gameScreen.game.endTurn();
							}
							gameScreen.dialogueBox.sel = -1;
							gameScreen.dialogueBox = null;
						}
					}
					if(gameScreen.dialogueBox == gameScreen.SKIP_REINFORCE_CONFIRM) {
						if(choice != null) {
							if(choice.contentEquals("Continue")) {
								gameScreen.game.turnState.deployableTroops = 0;
								gameScreen.game.enter();
							}
							gameScreen.dialogueBox.sel = -1;
							gameScreen.dialogueBox = null;
						}
					}
					if(gameScreen.dialogueBox == gameScreen.SKIP_ATTACK_CONFIRM) {
						if(choice != null) {
							if(choice.contentEquals("Continue")) {
								gameScreen.game.turnState.deployableTroops = 0;
								gameScreen.game.turnState.hasMoved = true;
								gameScreen.game.enter();
							}
							gameScreen.dialogueBox.sel = -1;
							gameScreen.dialogueBox = null;
						}
					}
					
				} else if(gameScreen.sidebar.mouseOver(ax, ay)) {
					System.out.println("bwhweh");
					Button choice = gameScreen.sidebar.mousePress(ax, ay);
					if(choice != null && choice.active) {
						if(choice == gameScreen.sidebar.textBoxEnter) {
							gameScreen.game.enter();
						} else if (choice == gameScreen.sidebar.endTurn) {
							gameScreen.dialogueBox = gameScreen.END_TURN_CONFIRM;
						} else {
							gameScreen.game.selectActionButton(choice);
						}
					}
				} else {
//					GUI.game.selected = null;
					Territory selTerritory = null;
					for(Territory t : gameScreen.game.board.territories)
						for(Polygon p: t.polygons)
							if(p.contains(x, y))
								selTerritory = t;
					gameScreen.game.selectTerritory(selTerritory);
				}
			}
			gameScreen.game.updateHighlights();
			
			moveOnScreen = false;
			
			
			lx = ax;
			ly = ay;
		}
	}

	public static class MMA extends MouseMotionAdapter {
		
		GameScreen gameScreen;
		public MMA(GameScreen gameScreen) {
			this.gameScreen = gameScreen;
		}
		
		
		public void mouseMoved(MouseEvent e) {
			ax = e.getX();
			ay = e.getY();
			x = (int) ((ax-gameScreen.view.x)/gameScreen.view.scale);
			y = (int) ((ay-gameScreen.view.y)/gameScreen.view.scale);
			
			boolean capture = false;
			if(gameScreen.dialogueBox != null)
				capture |= gameScreen.dialogueBox.mouseOver(ax, ay);
			if(!capture)
				capture |= gameScreen.sidebar.mouseOver(ax, ay);
			if(!capture) {
				gameScreen.game.mouseOver = null;
				for(Territory t : gameScreen.game.board.territories) {
					boolean inside = false;
					for(Polygon p: t.polygons) {
						if(p.contains(x, y)) {
							inside = true;
						}
					}
					if(inside) {
						gameScreen.game.mouseOver = t;
					}
				}
			} else {
				gameScreen.game.mouseOver = null;
			}
//			System.out.println(GUI.game.selected);
			
			lx = ax;
			ly = ay;
		}

		public void mouseDragged(MouseEvent e) {
			ax = e.getX();
			ay = e.getY();
			x = (int) ((ax-gameScreen.view.x)/gameScreen.view.scale);
			y = (int) ((ay-gameScreen.view.y)/gameScreen.view.scale);
			
			if(moveOnScreen) {
				gameScreen.view.x += ax-lx;
				gameScreen.view.y += ay-ly;
			}
			
			if(gameScreen.sidebar.slider.drag)
				gameScreen.sidebar.mouseDrag(ax, ay);
			
			boolean capture = false;
			if(gameScreen.dialogueBox != null)
				capture |= gameScreen.dialogueBox.mouseOver(ax, ay);
			if(!capture)
				capture |= gameScreen.sidebar.mouseOver(ax, ay);
			if(!capture) {
				gameScreen.game.mouseOver = null;
				for(Territory t : gameScreen.game.board.territories) {
					boolean inside = false;
					for(Polygon p: t.polygons) {
						if(p.contains(x, y)) {
							inside = true;
						}
					}
					if(inside) {
						gameScreen.game.mouseOver = t;
					}
				}
			} else {
				gameScreen.game.mouseOver = null;
			}
			
//			System.out.println(GUI.view.x + "\t" + GUI.view.y);
			
			lx = ax;
			ly = ay;
		}
	}
}
