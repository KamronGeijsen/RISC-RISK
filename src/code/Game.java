package code;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Stack;
import java.util.stream.Collectors;

import code.Board.Card;
import code.Board.Continent;
import code.Board.Territory;
import code.Board.Territory.Border;
import code.GameScreen.Sidebar;
import code.GameScreen.Sidebar.Button;
import code.Game.AttackStrategy.Entry;

public class Game {
	
	GameScreen gameScreen;
	
	ArrayList<Player> players = new ArrayList<>();
	Player turn = null;
	Player player = null;
	
	Territory selected = null;
	Territory from = null;
	Territory highlighted = null;
	Territory mouseOver = null;
	
	TurnState turnState = new TurnState();
	
	Board board;
	
	public Game(GameScreen gameScreen) {
		this.gameScreen = gameScreen;
	}
	
	void setTurn(Player player) {
//		if(turn == player)
//			return;
		turn = player;
		gameScreen.sidebar.endTurn.active = player == this.player;
		selectTerritory(null);
		updateHighlights();
		
		if(player != this.player)
			return;
		
		
		int occupiedTerritories = 0;
		for(Territory t : board.territories)
			if(t.player == player)
				occupiedTerritories++;
		
		turnState.deployableTroops = Math.max(3,occupiedTerritories/3);
		
		lbl: for(Continent c : board.continents) {
			for(Territory t : c.territories)
				if(t.player != player)
					continue lbl;
			turnState.deployableTroops += c.reinforcements;
		}
		turnState.hasAttacked = false;
		turnState.hasDeployed = false;
		turnState.hasMoved = false;
		turnState.earnsTerritory = false;
		turnState.movableTroops = Integer.MAX_VALUE;
		
	}
	
	void initRandom() {
//		for(Territory t : board.territories) {
//			t.player = players.get((int)(Math.random() * players.size()));
//			t.troops = (int) (Math.random()*5 + 1);
//			if(Math.random() > 0.5)
//				t.troops = 1;
//		}
//		GUI.client.initBoard();
//		setTurn(GUI.game.player);

//		GameScreen.client.parseSettings(String.format("settings(%s)", "default1"));
		
		int len = board.territories.length;
		int indeces[] = new int[len];
		for(int i = 0; i < len; i++)
			indeces[i] = i;
		for(int i = 0; i < 1000; i++) {
			int i1 = (int) (Math.random() * len);
			int i2 = (int) (Math.random() * len);
			
			int temp = indeces[i1];
			indeces[i1] = indeces[i2];
			indeces[i2] = temp;
		}
		
		int rr = 0;
		for(int i : indeces) {
			Territory t = board.territories[i];
			t.player = players.get(rr);
			rr++;
			if(rr >= players.size())
				rr = 0;
			t.troops = (int) (Math.random()*5 + 1);
		}
//		GameScreen.client.initBoard();
		setTurn(GameScreen.game.player);
	}
	
	class TurnState {
		int deployableTroops;
		int movableTroops = Integer.MAX_VALUE;
		boolean hasDeployed;
		boolean hasAttacked;
		boolean hasMoved;
		boolean earnsTerritory;
	}
	
	static class Player {
		static final Color[] colorPallet = {
			new Color(32,32,192),
			new Color(192,32,32),
			new Color(192,192,32),
			new Color(32,192,32),
		};
		static final String[] names = {
				"Blue",
				"Red",
				"Yellow",
				"Green",
			};
		
		Player(String name, ArrayList<Card> cards, Color color) {
			this.name = name;
			this.cards = cards;
			this.color = color;
//			cards.add(new Card('a'));
//			cards.add(new Card('i'));
//			cards.add(new Card('c'));
		}
		String name;
		Color color;
		
		ArrayList<Card> cards = new ArrayList<>();
		
		@Override
		public String toString() {
			return String.format("player(%s,%s)", name, cards.parallelStream().map(Object::toString).collect(Collectors.joining("")).trim());
		}

		public void addCard() {
			cards.add(new Card("ica".charAt((int)(Math.random()*3))));
		}
	}
	
	static class Objective {
		
	}
	
	static class AttackStrategy {
		ArrayList<Entry> log = new ArrayList<>();
		
		void battleGeneralCase(int attackerMaxDiceAmount, int defenderMaxDiceAmount) {
			Entry last = log.get(0);
			Random random = new Random();
			while(last.attacker > 0 && last.defender > 0) {
				int attackerDiceAmount = Math.min(last.attacker, attackerMaxDiceAmount);
				int defenderDiceAmount = Math.min(last.defender, defenderMaxDiceAmount);
				int[] attackerDice = random.ints(attackerDiceAmount, 1, 7).toArray();
				int[] defenderDice = random.ints(defenderDiceAmount, 1, 7).toArray();
				Arrays.sort(attackerDice);
				Arrays.sort(defenderDice);
				int attackerWins = 0;
				int defenderWins = 0;
				
				if(attackerDiceAmount > defenderDiceAmount) {
					final int overshoot = attackerDiceAmount-defenderDiceAmount;
					for(int i = 0; i < defenderDiceAmount; i++) {
						if(attackerDice[i + overshoot] > defenderDice[i])
							attackerWins++;
						else
							defenderWins++;
					}
						
				} else {
					int overshoot = defenderDiceAmount-attackerDiceAmount;
					for(int i = 0; i < attackerDiceAmount; i++) {
						if(attackerDice[i] > defenderDice[i + overshoot])
							attackerWins++;
						else
							defenderWins++;
					}
				}
				Entry newEntry = new Entry(last.attacker - defenderWins, last.defender - attackerWins,
						attackerDice, defenderDice);
				log.add(newEntry);
				last = newEntry;
			}
		}
		
		static class Entry {
			public Entry(int attacker, int defender, int[] attackerDice, int[] defenderDice) {
				this.attacker = attacker;
				this.defender = defender;
				this.attackerDice = attackerDice;
				this.defenderDice = defenderDice;
			}
			final int attacker;
			final int defender;
			final int[] attackerDice;
			final int[] defenderDice;
			
			@Override
			public String toString() {
				return "a="+attacker + ",d="+defender + "\t" + 
			Arrays.toString(attackerDice) + "\t" + Arrays.toString(defenderDice);
			}
		}
	}

	void selectTerritory(Territory t) {
		Sidebar s = gameScreen.sidebar;
		if(t != null) {
			if(!t.highlight) {
				selectTerritory(null);
			}
			if((t.player != turn || turn != player) && !t.highlight) {
				selectTerritory(null);
			} else if(s.selected == s.attack) {
				gameScreen.details.init(from, t);
				s.slider.init(1,from.troops-1);
				selected = t;
			} else if(s.selected == s.move) {
				gameScreen.details.init(selected, t);
				s.slider.init(1,selected.troops-1);
				selected = t;
			} else if(t.player == turn && turn == player 
					&& (turnState.deployableTroops > 0 || (t.troops > 1 && turnState.movableTroops > 0))) {
				gameScreen.details.close();
				from = selected = t;
				if(turnState.deployableTroops > 0) {
					s.slider.init(1,turnState.deployableTroops);
					s.selected = s.reinforce;
					boolean canMoveOrAttack = t.troops > 1 && turnState.movableTroops > 0;
					s.reinforce.active = true;
					s.attack.active = canMoveOrAttack;
					s.move.active = canMoveOrAttack;
				} else {
					s.reinforce.active = false;
					if(t.troops > 1) {
						
//						s.slider.init(1,t.troops-1);
						
						s.attack.active = false;
						s.move.active = false;
						for(Border b : selected.neighbouring)
							if(b.bordering.player != turn)
								s.attack.active = true;
						for(Border b : selected.neighbouring)
							if(b.bordering.player == turn)
								s.move.active = true;
						if(s.attack.active)
							s.selected = s.attack;
						else
							s.selected = s.move;
					} else {
						s.attack.active = false;
						s.move.active = false;
					}
					
				}
			}
		} else {
			gameScreen.details.close();
			s.slider.deactivate();
			s.reinforce.active = false;
			s.attack.active = false;
			s.move.active = false;
			s.selected = null;
			from = selected = null;
			
		}
	}
	
	void updateHighlights() {
		Sidebar s = gameScreen.sidebar;
		if (s.selected == null){
				for(Territory t : board.territories)
					t.highlight = t.player == turn && turn == player 
							&& (turnState.deployableTroops > 0 || (t.troops > 1 && turnState.movableTroops > 0));
			
		} else {
			for(Territory t : board.territories) {
				t.highlight = false;
			}
			if(s.selected == s.attack) {
				for(Border b : from.neighbouring) {
					b.bordering.highlight = b.bordering.player != player;
				}
			} else {
				if(s.selected == s.move) {
					Stack<Territory> stack = new Stack<>();
					stack.push(selected);
					while(!stack.empty()) {
						for(Border b : stack.pop().neighbouring) {
							if(!b.bordering.highlight && b.bordering.player == player) {
								b.bordering.highlight = true;
								stack.push(b.bordering);
							}
						}
					}
					
					
				}
			}
		}
		if(turn != player)
			for(Territory t : board.territories) {
				t.highlight = false;
			}
	}

	public void enter() {
		Sidebar s = gameScreen.sidebar;
		Territory t = selected;
		if(s.selected == s.reinforce) {
			
			turnState.deployableTroops -= s.slider.slider;
			t.troops += s.slider.slider;
//			GameScreen.client.updateTerritory(t);
			
			if(t.troops > 1) {
				s.attack.active = true;
				s.move.active = true;
			}

			selectTerritory(null);
			updateHighlights();
			selectTerritory(t);
		} else if(s.selected == s.attack) {
			if(turnState.deployableTroops > 0) {
				gameScreen.dialogueBox = gameScreen.SKIP_REINFORCE_CONFIRM;
				return;
			}
//			System.out.println(from);
//			System.out.println(selected);
			AttackStrategy attack = new AttackStrategy();
			attack.log.add(new Entry(
					s.slider.slider, 
					gameScreen.details.to.territory.troops, null, null));
			attack.battleGeneralCase(3, 2);
			Entry lastEntry = attack.log.get(attack.log.size()-1);
			
			gameScreen.details.from.territory.troops -= s.slider.slider;
			if(lastEntry.attacker > 0) {
				gameScreen.details.to.territory.troops = lastEntry.attacker;
				gameScreen.details.to.territory.player = turn;
				turnState.earnsTerritory = true;
			} else {
				gameScreen.details.to.territory.troops = lastEntry.defender;
			}
			
			for(Entry e : attack.log)
				System.out.println(e);
			
//			GameScreen.client.updateTerritory(gameScreen.details.from.territory);
//			GameScreen.client.updateTerritory(gameScreen.details.to.territory);
			

//			s.slider.deactivate();
//			GUI.details.close();
//			from = selected;
//			s.selected = null;
			selectTerritory(null);
			updateHighlights();
//			System.out.println(t.highlight + "hihhhhhhhhhhhghgh");
			selectTerritory(t);
		} else if(s.selected == s.move){
			if(!turnState.hasMoved) {
				gameScreen.dialogueBox = gameScreen.SKIP_ATTACK_CONFIRM;
				return;
			}
			gameScreen.details.from.territory.troops -= s.slider.slider;
			gameScreen.details.to.territory.troops += s.slider.slider;
//			turnState.movableTroops -= s.slider.slider;
			turnState.movableTroops = 0;
//			GameScreen.client.updateTerritory(gameScreen.details.from.territory);
//			GameScreen.client.updateTerritory(gameScreen.details.to.territory);
			
			
			selectTerritory(null);
			updateHighlights();
		}
	}

	public void selectActionButton(Button choice) {
		Sidebar s = gameScreen.sidebar;
		Territory t = from;
		
		if(choice == s.reinforce) {
			s.selected = choice;
			s.slider.init(1,turnState.deployableTroops);
		} else if(choice == s.attack) {
			s.selected = choice;
			if(from == selected)
				s.slider.deactivate();
			else
				s.slider.init(1,t.troops-1);
			
		} else if(choice == s.move) {
			s.selected = choice;
			if(from == selected)
				s.slider.deactivate();
			else
				s.slider.init(1,t.troops-1);
		}
		
	}

	public void endTurn() {
		if(turnState.earnsTerritory)
			player.addCard();
//		GameScreen.client.endTurn();
		
	}

	public void playCards() {
		if(turn != player || turnState.hasAttacked)
			return;
		int artillary = Collections.frequency(player.cards, new Card('a'));
		int cavalry = Collections.frequency(player.cards, new Card('c'));
		int infantry = Collections.frequency(player.cards, new Card('i'));
//		System.out.println(player.cards);
//		System.out.println(artillary + "\t" + cavalry + "\t" + infantry);
		int count = 0;
		if(artillary >= 1 && cavalry >= 1 && infantry >= 1) {
			player.cards.remove(new Card('a'));
			player.cards.remove(new Card('c'));
			player.cards.remove(new Card('i'));
			count = 10;
		} else if(artillary >= 3) {
			player.cards.remove(new Card('a'));
			player.cards.remove(new Card('a'));
			player.cards.remove(new Card('a'));
			count = 8;
		} else if(cavalry >= 3) {
			player.cards.remove(new Card('c'));
			player.cards.remove(new Card('c'));
			player.cards.remove(new Card('c'));
			count = 6;
		} else if(infantry >= 3) {
			player.cards.remove(new Card('i'));
			player.cards.remove(new Card('i'));
			player.cards.remove(new Card('i'));
			count = 4;
		}
		turnState.deployableTroops += count;
//		if(count > 0)
//			GameScreen.client.updatePlayer(turn);
	}
}
