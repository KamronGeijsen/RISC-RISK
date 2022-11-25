package code;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import code.Board.Territory.Border;
import code.Game.Player;

public class Board {
	
	HashMap<String, Territory> territoriesByName;
	Continent[] continents;
	Territory[] territories;

	static class Card {
		public Card(char code) {
			this.code = code;
		}
		final char code;
		String name;
		
		@Override
		public String toString() {
			return code + "";
		}
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof Card)
				return ((Card) obj).code == code;
			return false;
		}
	}
	
	static class Continent {
		public Continent(String name, int reinforcements, Territory[] territories, int color) {
			this.name = name;
			this.reinforcements = reinforcements;
			this.territories = territories;
			for(Territory t : territories)
				t.continent = this;
			this.color = new Color(color);
		}
		final String name;
		final int reinforcements;
		final Territory[] territories;
		final Color color;
	}
	static class Territory {
		public Territory(String name, Polygon[] polygons) {
			this.name = name;
//			this.neighbouring = neighbouring;
			this.polygons = polygons;
			double mx = 0, my = 0, ma = 0;
			for(Polygon p : polygons) {
				if(p.npoints == 0) {
					System.out.println("weh: " + name);
					continue;
				}
				double area = p.xpoints[p.npoints-1]*p.ypoints[0]-p.xpoints[0]*p.ypoints[p.npoints-1];
				for(int i = 0; i < p.npoints-1; i++) {
					area += p.xpoints[i]*p.ypoints[i+1]-p.xpoints[i+1]*p.ypoints[i];
				}
				area /= 2;
				
				double cx = (p.xpoints[p.npoints-1]+p.xpoints[0])*(p.xpoints[p.npoints-1]*p.ypoints[0]-p.xpoints[0]*p.ypoints[p.npoints-1]), 
					   cy = (p.ypoints[p.npoints-1]+p.ypoints[0])*(p.xpoints[p.npoints-1]*p.ypoints[0]-p.xpoints[0]*p.ypoints[p.npoints-1]);
				
				for(int i = 0; i < p.npoints-1; i++) {
					cx += (p.xpoints[i]+p.xpoints[i+1])*(p.xpoints[i]*p.ypoints[i+1]-p.xpoints[i+1]*p.ypoints[i]);
					cy += (p.ypoints[i]+p.ypoints[i+1])*(p.xpoints[i]*p.ypoints[i+1]-p.xpoints[i+1]*p.ypoints[i]);
				}
				cx /= 6*area;
				cy /= 6*area;
				
				area = Math.abs(area);
				mx+=cx*area;
				my+=cy*area;
				ma+=area;
			}
			mx /= ma;
			my /= ma;
			this.mx = mx;
			this.my = my;
		}
		Player player;
		int troops = 1;
		
		boolean fog;
		boolean highlight;

		final String name;
		final Polygon[] polygons;
		final double mx, my;
		Continent continent;
		ArrayList<Border> neighbouring = new ArrayList<>();
		
		static class Border {
			Border(Territory bordering, boolean offshore, boolean wrap){
				this.bordering = bordering;
				this.offshore = offshore;
				this.wrap = wrap;
			}
			Territory bordering;
			boolean offshore;
			boolean wrap;
			
			@Override
			public boolean equals(Object obj) {
				return ((Border) obj).bordering == bordering;
			}
			
			
		}
		void drawOffshoreBorders(Graphics2D g) {
			
			for(Border n : neighbouring) {
				Territory from  = this;
				Territory to  = n.bordering;
				if(!n.offshore)
					continue;
				if(from.mx > to.mx)
					continue;
				if(!n.wrap) {
					g.drawLine((int)from.mx, (int)from.my, (int)to.mx, (int)to.my);
				} else {
					g.drawLine((int)from.mx, (int)from.my, (int)to.mx-2000, (int)to.my);
					g.drawLine((int)from.mx+2000, (int)from.my, (int)to.mx, (int)to.my);
				}
		        
			}
		}
		void draw(Graphics2D g) {
			Color color;
			if(player == null)
				color = Color.gray;
			else
				color = player.color;
			if (GameScreen.game.mouseOver == this)
				color = GameScreen.blend(color, new Color(255, 255, 255, 20));
			if (highlight)
				color = GameScreen.blend(color, new Color(255, 255, 255, 30));
			if (GameScreen.game.selected == this)
				color = GameScreen.blend(color, new Color(255, 255, 255, 64));
			
			g.setColor(color);
			for(Polygon p : polygons) {
				g.fillPolygon(p);
			}
			g.setColor(Color.BLACK);
			for(Polygon p : polygons) {
				g.drawPolygon(p);
			}
			
			
			
		}
		
		void drawLabels(Graphics2D g) {
//			g.setColor(Color.red);
//			g.fillOval((int)mx-2, (int)my-2, 5, 5);
			g.setColor(Color.WHITE);
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
			final int wtext = g.getFontMetrics().stringWidth(name) + 10;
			g.drawString(name, (int)mx-wtext/2+5, (int)my+25);
			
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
			final String text = troops+"";
			final int w = g.getFontMetrics().stringWidth(text) + 10;
			final int h = g.getFont().getSize() + 6;
			g.setColor(new Color(32,32,32,240));
			g.fillRect((int)mx-w/2, (int)my-h/2, w, h);
			g.setColor(Color.BLACK);
			g.drawRect((int)mx-w/2, (int)my-h/2, w, h);
			
			g.setColor(Color.WHITE);
			g.drawString(text, (int)mx-w/2+5, (int)my+h/2-4);
		}
		
		void drawBorder(Graphics2D g, Territory to) {
			double shortening = 0.8;
			double arrowLen = 10;
			double arrowAng = Math.PI/8;
			double dx = mx + (to.mx - mx) * shortening;
			double dy = my + (to.my - my) * shortening;
			double angle = Math.atan2(to.my - my, to.mx - mx) + Math.PI;
			g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] { 9 }, 0));
			g.drawLine((int)mx, (int)my, (int)dx, (int)dy);
			g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine((int)dx, (int)dy, (int)(dx + arrowLen*Math.cos(angle + arrowAng)), (int)(dy + arrowLen*Math.sin(angle + arrowAng)));
			g.drawLine((int)dx, (int)dy, (int)(dx + arrowLen*Math.cos(angle - arrowAng)), (int)(dy + arrowLen*Math.sin(angle - arrowAng)));
		}

		@Override
		public String toString() {
			return String.format("territory(%s,%s,%d)", name, player.name, troops);
		}
	}
	ArrayList<Point> points;
	ArrayList<Point> vectors;
	
	Board(BufferedImage image, String data){
		final int w = image.getWidth();
		final int h = image.getHeight();
		
		ArrayList<Point> points = new ArrayList<Point>(100);
//		int count = 0;
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				if((image.getRGB(x, y)) == 0xffffffff) {
//					count++;
					boolean found = false;
					for(int i = 0; i < points.size(); i++) {
						Point p = points.get(i);
						if(p.distance(x, y) <= 5) {
//							System.out.println(x + "," + y + "\t" + p.distance(x, y));
							found = true;
							break;
						}
					}
					if(found == false)
						points.add(new Point(x+1,y+3));
				}
			}
		}
//		System.out.println("whitespots" + count);
		final int[] xOffs = {-1,0,1,1,1,0,-1,-1}, yOffs = {1,1,1,0,-1,-1,-1,0}; 
		ArrayList<Point> vectors = new ArrayList<Point>(100);
		System.out.println(points.size());
		for(Point p : points) {
//			System.out.println(p);
			for(int sy = 0; sy < 9; sy++) {
				for(int sx = 0; sx < 9; sx++) {
					if(image.getRGB(p.x-4+sx, p.y-4+sy) == 0xff000000) {
						int x = p.x-4+sx, y = p.y-4+sy;
						while(image.getRGB(x, y) != 0xffffffff) {
							for(int i = 0; i < 8; i++) {
								if(p.distance((double)x + xOffs[i],(double)y + yOffs[i]) >= p.distance((double)x,(double)y) &&
									((image.getRGB(x + xOffs[i], y + yOffs[i])) == 0xff000000 || 
									 (image.getRGB(x + xOffs[i], y + yOffs[i])) == 0xffffffff)) {
									x = x + xOffs[i];
									y = y + yOffs[i];
									
									break;
								}
							}
//							System.out.println(p.x + "," + p.y + "\t" + x + "," + y);
						}
//						System.out.println(p.x + "," + p.y + "\t" + x + "," + y);
						
						// Add the vector to the list if it doesn't exist yet
						for(Point pTo : points) {
							if(pTo.distance(x, y) < 5) {
								// Ignore duplicates, eg (2,5) == (5,2)
								int index1 = points.indexOf(p);
								int index2 = points.indexOf(pTo);
								if(index1 > index2) {
									int temp = index1;
									index1 = index2;
									index2 = temp;
								}
								
								// Add if not already in the list
								if(!vectors.contains(new Point(index1, index2))) {
									vectors.add(new Point(index1, index2));
//									System.out.println(new Point(index1, index2));
								}
							}
						}
					}
				}
			}
		}
		this.points = points;
		this.vectors = vectors;
		
		HashMap<Integer, ArrayList<Point>> meshes = new HashMap<>();
		for(Point v : vectors) {
			Point from = points.get(v.x);
			Point to = points.get(v.y);
			double mx = (from.x + to.x)/2d;
			double my = (from.y + to.y)/2d;
			for(double a : new double[] {Math.PI/2, -Math.PI/2}) {
				double perpendicular_angle = Math.atan2(from.y - to.y, from.x - to.x) + a;
				double perpendicular_x = Math.round(mx + 2*Math.cos(perpendicular_angle));
				double perpendicular_y = Math.round(my + 2*Math.sin(perpendicular_angle));
				int color = image.getRGB((int)perpendicular_x, (int)perpendicular_y);
				if((color&0xff000000) != 0) {
//					System.out.println(color);
					if(color == 0xff000000)
						System.out.println(mx + "\t" + my + "\t" + perpendicular_x + "\t" + perpendicular_y);
					ArrayList<Point> t = meshes.get(color);
					if(t == null) {
						t = new ArrayList<Point>();
						meshes.put(color, t);
					}
					t.add(v);
				}
			}
		}
		System.out.println(meshes.size());
		this.territories = new Territory[meshes.size()];
		
		if(data.isEmpty())
			throw new RuntimeException("Invalid datafile: Empty or cannot read");
		Scanner sc = new Scanner(new ByteArrayInputStream(data.getBytes()));
		
		String format = sc.nextLine();
		System.out.println(format);
		
//		HashMap<String, Object> root = new HashMap<String, Object>();
//		Stack<Object> iterator = new Stack<>();
//		iterator.add(root);
//		int prevIndent = -1;
//		while(sc.hasNext()) {
//			String[] line = sc.nextLine().split("\\:",2);
//			String var = line[0];
//			String value = line.length > 1?line[1]:null;
//			int current_indent = var.lastIndexOf('\t')+1;
//			System.out.println(iterator);
//			while(prevIndent >= current_indent) {
//				prevIndent--;
//				iterator.pop();
//			}
//			prevIndent = current_indent;
//			Object value_obj;
//			if(var.contains("[")) {
//				var = var.substring(0, var.indexOf('[')-1);
//				value_obj = new ArrayList<Object>();
//			} else if(value == null) {
//				value_obj = new HashMap<String, Object>();
//			} else {
//				value_obj = value;
//			}
//			System.out.println(iterator);
//			System.out.println(value_obj);
//			Object top = iterator.peek();
//			System.out.println(top.getClass());
//			if(top.getClass().isInstance(new ArrayList<Object>()) ) {
//				((ArrayList) top).add(value_obj);
//			} else if(top instanceof HashMap<?,?>) {
//				((HashMap) top).put(var, value_obj);
//			}else {
//				System.out.println("errooorrr!");
//			}
//			iterator.add(value_obj);
//			
//			System.out.println(var.trim());
//		}
		
		
//		Stack<String> stack = new Stack<>();
//		while(sc.hasNext()) {
//			String[] line = sc.nextLine().split("\\:",2);
//			String var = line[0];
//			if(line.length > 1) {
//				String value = line.length > 1?line[1]:null;
//			}else {
//				stack.add(var);
//			}
//			
//		}
		ArrayList<Continent> continents = new ArrayList<>();
		territoriesByName = new HashMap<>();
		
		String line = sc.nextLine();
		line = sc.nextLine();
		while(line.contains("continent")) {
			String name = sc.nextLine().trim().split(":")[1];
			String reinforcements = sc.nextLine().trim().split(":")[1];
			line = sc.nextLine();
			String cColor = "000000";
			if(line.contains("color:")) {
				cColor = line.trim().split(":")[1];
				line = sc.nextLine();
			}
			ArrayList<Territory> territories = new ArrayList<>(); 
//			ArrayList<Territory> neighbours = new ArrayList<>(); 
			
			while(line.contains("territory")) {
				String tName = sc.nextLine().trim().split(":")[1];
				String color = sc.nextLine().trim().split(":")[1];
				int colorInt = Integer.parseInt(color, 16)+0xff000000;
				line = sc.nextLine();
				
				
				ArrayList<Polygon> temp = new ArrayList<>();
				ArrayList<Point> v = meshes.get(colorInt);
				
				HashMap<Integer, Integer> maxEdges = new HashMap<>();
				HashMap<Integer, Integer> edges = new HashMap<>();
				for(Point p : v) {
					maxEdges.put(p.x, 0);
					maxEdges.put(p.y, 0);
					edges.put(p.x, 0);
					edges.put(p.y, 0);
				}
				for(Point p : v) {
					maxEdges.put(p.x, maxEdges.get(p.x) + 1);
					maxEdges.put(p.y, maxEdges.get(p.y) + 1);
				}
				
//				System.out.println(maxEdges);
//				System.out.println("Points for: " + color);
//				v.forEach(a -> {System.out.println(a);});
//				System.out.println();
				for(int i = 0; i < v.size(); i++) {
					int last = v.get(i).x;
					int last2 = v.get(i).y;
					if(edges.get(last) == maxEdges.get(last))
						continue;
					temp.add(0,new Polygon());
					while(true) {
						for(Point v2 : v)
							if((v2.x == last || v2.y == last) && v2.x != last2 && v2.y != last2) {
								int val = v2.x == last ? v2.y : v2.x;
								if(edges.get(val) >= maxEdges.get(val))
									continue;
								last2 = last;
								last = val;
								break;
							}
						if(edges.get(last) == maxEdges.get(last))
							break;
						edges.put(last, edges.get(last) + 1);
						edges.put(last2, edges.get(last2) + 1);
						Point p = points.get(last);
						temp.get(0).addPoint(p.x, p.y);
					}
				}
				Polygon[] polygons = new Polygon[temp.size()];
				temp.toArray(polygons);
				Territory territory = new Territory(tName, polygons);
				territoriesByName.put(tName, territory);
				territories.add(territory);
			}
			Territory[] temp2 = new Territory[territories.size()];
			territories.toArray(temp2);
			
			Continent continent = new Continent(name, Integer.parseInt(reinforcements), temp2, Integer.parseInt(cColor, 16));
			continents.add(continent);
		}
		this.continents = new Continent[continents.size()];
		continents.toArray(this.continents);
		
		{
			int i = 0;
			for(Territory t : territoriesByName.values()) {
				territories[i++] = t;
			}
		}
		
		while(line.contains("offshore border:")) {
			String from = sc.nextLine().trim().split(":")[1];
			String to = sc.nextLine().trim().split(":")[1];
			line = sc.nextLine();
			boolean wrap = false;
			if(line.contains("wrap:")) {
				wrap = line.trim().split(":")[1].contentEquals("true");
				line = sc.nextLine();
			}
			territoriesByName.get(from).neighbouring.
				add(new Border(territoriesByName.get(to), true, wrap));
			territoriesByName.get(to).neighbouring.
				add(new Border(territoriesByName.get(from), true, wrap));
//			System.out.println(from + "\t" + to);
		}
		
		for(Territory t1 : territories) {
			for(Polygon p1 : t1.polygons) {
				for(int i1 = 0; i1 < p1.npoints; i1++) {
					for(Territory t2 : territories) {
						for(Polygon p2 : t2.polygons) {
							for(int i2 = 0; i2 < p2.npoints; i2++) {
								if(p1.xpoints[i1] == p2.xpoints[i2] && 
										p1.ypoints[i1] == p2.ypoints[i2]) {
									Border border = new Border(t2, false, false);
									if(!t1.neighbouring.contains(border) && !t1.equals(t2)) {
//												System.out.println(t1.name + "\t" + t2.name);
//												System.out.println(t1.neighbouring);
										t1.neighbouring.add(border);
									}
								}
							}
						}
					}
				}
			}
		}
//		System.out.println(continents);
//		System.out.println(offshoreBorders);
		sc.close();
	}
}
