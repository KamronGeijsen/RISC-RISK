package code;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;

public class StartControls {

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
		StartScreen startScreen;
		public KA(StartScreen startScreen) {
			this.startScreen = startScreen;
		}
		
		public void keyPressed(KeyEvent e) {
			if(e.getKeyCode() != e.VK_ENTER) {
				if(startScreen.sideBar.userTextBox.active) {
					startScreen.sideBar.userTextBox.keyTyped(e);
				}
				if(startScreen.sideBar.passTextBox.active) {
					startScreen.sideBar.passTextBox.keyTyped(e);
				}
			} else {
				startScreen.login();
			}
			
		}

		public void keyReleased(KeyEvent e) {

		}
	}

	public static class MA extends MouseAdapter {
		
		StartScreen startScreen;
		public MA(StartScreen startScreen) {
			this.startScreen = startScreen;
		}
		
		public void mouseWheelMoved(MouseWheelEvent e) {
			int mr = e.getWheelRotation();
			System.out.println(mr);
		}

		public void mousePressed(MouseEvent e) {
			asx = ax = e.getX();
			asy = ay = e.getY();
			
			lx = ax;
			ly = ay;
		}

		public void mouseReleased(MouseEvent e) {
			ax = e.getX();
			ay = e.getY();
			moveOnScreen = false;
			
			
			lx = ax;
			ly = ay;
		}
	}

	public static class MMA extends MouseMotionAdapter {
		
		StartScreen startScreen;
		public MMA(StartScreen startScreen) {
			this.startScreen = startScreen;
		}
		
		
		public void mouseMoved(MouseEvent e) {
			ax = e.getX();
			ay = e.getY();
			
//			System.out.println(GUI.game.selected);
			
			lx = ax;
			ly = ay;
		}

		public void mouseDragged(MouseEvent e) {
			ax = e.getX();
			ay = e.getY();
			
//			System.out.println(GUI.view.x + "\t" + GUI.view.y);
			
			lx = ax;
			ly = ay;
		}
	}
}
