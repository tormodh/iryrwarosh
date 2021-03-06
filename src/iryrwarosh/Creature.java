package iryrwarosh;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import asciiPanel.AsciiPanel;

public class Creature {
	public Point position;
	public Point homeScreenPosition;
	
	public Point lastMovedDirection = new Point(0,0);
	public Point lastMovedDirection() { return lastMovedDirection.copy(); }
	
	private int regenerateCounter;
	private int poisonCounter;
	private Creature lastPoisonedBy;
	public boolean isPoisoned() { return poisonCounter > 0; }
	
	private String causeOfDeath;
	public String causeOfDeath() { return causeOfDeath; }
	
	private String name;
	public String name() { return name; }
	
	private char glyph;
	public char glyph() { return glyph; }
	
	private Color color;
	public Color color(){ return color; }

	private int hearts;
	public int hearts() { return hearts; }

	private int maxHearts;
	public int maxHearts() { return maxHearts + (hasTrait(Trait.EXTRA_HP) ? 3 : 0); }
	public void increaseMaxHearts(int amount) { maxHearts += amount; }

	private int rupees;
	public int rupees() { return rupees; }

	private boolean isMiniboss;
	public boolean isMiniboss() { return isMiniboss; }
	public void setIsMiniboss(boolean value) { isMiniboss = value; }
	
	private int lastWanderX;
	private int lastWanderY;
	private boolean hasMovedThisTurn = false;
	private boolean hasAttackedThisTurn = false;
	private boolean hasDoubleMovedThisTurn = false;
	private boolean hasDoubleAttackedThisTurn = false;
	
	private List<Trait> traits = new ArrayList<Trait>();

	public void addTrait(Trait trait) { traits.add(trait); }

	public boolean hasTrait(Trait trait) {
		return traits.contains(trait)
				|| leftHand != null && leftHand.hasTrait(trait)
				|| rightHand != null && rightHand.hasTrait(trait);
	}
	
	public boolean isPlayer(){
		return name.equals("player");
	}
	
	public String description(){
		String text = "";
		
		if (hasTrait(Trait.MYSTERIOUS))
			text += ", mysterious";
		else {
			for (Trait trait : traits)
				text += ", " + trait.description();
		}
		
		text = name + " (" + text.substring(2) + ")";

		if (leftHand != null)
			text += " " + leftHand.name();
		
		if (rightHand != null)
			text += " " + rightHand.name();
		
		if (loot != null)
			text += " (drops " + loot.name() + ")";
		
		return text;
	}
	
	private Item leftHand;
	public Item leftHand() { return leftHand; }

	private Item rightHand;
	public Item rightHand() { return rightHand; }
	
	private Item loot;
	public Item loot() { return loot; }
	public void setLoot(Item loot) { this.loot = loot; }
	
	private int reducedEvasionCounter;
	
	public Creature(String name, char glyph, Color color, int maxHp){
		this.name = name;
		this.glyph = glyph;
		this.color = color;
		this.maxHearts = maxHp;
		this.hearts = maxHp;
	}
	
	public boolean canEnter(Tile tile){
		if (hasTrait(Trait.WALKER) && tile.isGround())
			return true;
		
		if (hasTrait(Trait.SWIMMER) && tile.isSwimmable())
			return true;
		
		if (hasTrait(Trait.FLIER) && tile.isFlyable())
			return true;
		
		return false;
	}

	public boolean isHuman(){
		return glyph == '@';
	}
	
	public void jumpBy(World world, int dx, int dy){
		if (dx == 0 && dy == 0 || isHidden())
			return;
		
		boolean didJump = false;
		
		for (int i = 0; i < 3; i++){
			if (isHuman() && rupees < 1)
				break;
			
			Tile tile = world.tile(position.x+dx, position.y+dy);
			if (!tile.isGround() && !tile.isSwimmable() && !tile.isLava())
				break;

			Creature other = world.creature(position.x+dx, position.y+dy);
			if (other != null){
				other.loseHearts(world, this, 1, null, "A " + name() + " bumped into you while jumping");
				break;
			}
			
			didJump = true;
			position.x += dx;
			position.y += dy;
			
			loseRupees(world, 1);
		}
		
		if (didJump) {
			hasMovedThisTurn = true;
			publishMessage(new Jumped(world, this));
		}
	}
	
	public void moveBy(World world, int x, int y) {
		if (x==0 && y==0 || isHidden())
			return;
		
		if (hasMovedThisTurn && !hasTrait(Trait.DOUBLE_MOVE))
			return;
		
		if (hasTrait(Trait.TERRITORIAL)
				&& ((position.x+x) / 19 != homeScreenPosition.x || (position.y+y) / 9 != homeScreenPosition.y))
			return;
		
		if (hasTrait(Trait.JUMPER) && !hasTrait(Trait.TERRITORIAL) && Math.random() < 0.25) 
			jumpBy(world, x, y);
		
		Creature other = world.creature(position.x+x, position.y+y);
		
		if (other == null) {
			lastMovedDirection.x = x;
			lastMovedDirection.y = y;
			if (canEnter(world.tile(position.x+x, position.y+y))){
				position.x += x;
				position.y += y;
				hasMovedThisTurn = true;
				publishMessage(new Moved(world, this));
			} else {
				lastWanderX = (int)(Math.random() * 3) - 1;
				lastWanderY = (int)(Math.random() * 3) - 1;
				publishMessage(new BumpedIntoObsticle(world, this, position.x+x, position.y+y));
			}
		} else if (isFriendlyTo(other)) {
			lastWanderX = (int)(Math.random() * 3) - 1;
			lastWanderY = (int)(Math.random() * 3) - 1;
			return;
		} else if (hasDoubleAttackedThisTurn == false 
				&& other.evadeCheck(world)){
			other.evade(world, this);
		} else if (hasDoubleAttackedThisTurn == false){
			attack(world, other, "");
		}
	}
	
	public boolean isFriendlyTo(Creature other){
		if (isHuman() && other.isHuman())
			return this == other;
		else
			return Character.toLowerCase(other.glyph) == Character.toLowerCase(this.glyph);
	}
	
	public void attack(World world, Creature other, String specialType){
		if (other.hearts < 1 || hearts < 1 || isHidden() || other.isHidden())
			return;
		
		if (hasDoubleAttackedThisTurn
				|| hasAttackedThisTurn && !hasTrait(Trait.DOUBLE_ATTACK))
			return;
			
		hasAttackedThisTurn = true;
		
		if (hasTrait(Trait.DEFLECT_MELEE) && Math.random() < 0.33){
			publishMessage(new DeflectedMelee(world, other, this));
			return;
		}

		if (other.hasTrait(Trait.SPIKED)){
			publishMessage(new HitSpikes(world, this, other));
			loseHearts(world, other, 1, null, "You impaled youself on the spikes of a " + other.name());
			
			if (hearts < 1)
				return;
		}
			
		if (hasTrait(Trait.SLOWING_ATTACK) && other.hearts > 0)
			reduceEvasion(world, other);
		
		other.loseHearts(world, this, hasTrait(Trait.STRONG_ATTACK) ? 2 : 1, specialType, getSlainByText());

		if (hasTrait(Trait.POISONOUS) && other.hearts > 0)
			poison(world, other);
		
		if (hasTrait(Trait.DOUBLE_ATTACK)
				&& !hasDoubleAttackedThisTurn
				&& other.hearts > 0) {
			hasDoubleAttackedThisTurn = true;
			attack(world, other, "again");
		}
	}
	
	private String getSlainByText() {
		String text = "You were slain by ";
		char first = name().charAt(0);
		
		if (Character.isUpperCase(first))
			text += name();
		else if ("aeiou".contains(""+first)) 
			text += "an " + name();
		else 
			text += "a " + name();
		
		return text;
	}
	
	private void reduceEvasion(World world, Creature other) {
		other.reducedEvasionCounter += 10;
		
		if (other.reducedEvasionCounter == 10)
			publishMessage(new ReducedEvasion(world, this, other));
	}
	
	private void poison(World world, Creature other){
		other.poisonCounter += 10;
		other.lastPoisonedBy = this;
		publishMessage(new Poisoned(world, this, other));
	}
	
	public void curePoison() {
		poisonCounter = 0;
	}
	
	private int evasionPerOpenSpace = 5;
	
	public void modifyEvasion(int amount) {
		evasionPerOpenSpace += amount;
	}
	
	private List<Point> evasionCandidates(World world){
		List<Point> candidates = new ArrayList<Point>();
		
		for (Point p : position.neighbors()) {
			if (canEnter(world.tile(p.x, p.y)) && world.creature(p.x, p.y) == null)
				candidates.add(p);
		}
		
		Collections.shuffle(candidates);
		return candidates;
	}
	
	public int evadePercent(World world){
		int perOpenSpace = evasionPerOpenSpace;
		
		if (hasTrait(Trait.EXTRA_EVADE))
			perOpenSpace += 5;
		
		if (leftHand != null)
			perOpenSpace += leftHand.evasionModifier();
		
		if (rightHand != null)
			perOpenSpace += rightHand.evasionModifier();
		
		if (reducedEvasionCounter > 0)
			perOpenSpace /= 3;
		
		return evasionCandidates(world).size() * perOpenSpace;
	}
	
	public boolean evadeCheck(World world){
		return Math.random() * 100 < evadePercent(world);
	}
	
	public void evade(World world, Creature other){
		position = evasionCandidates(world).get(0);
		publishMessage(new Evaded(world, other, this));
	}
	
	private Point positionBeforeHiding;
	private int hiddenCounter;
	private int projectileCooldown;
	
	public void update(World world){
		hasMovedThisTurn = false;
		hasAttackedThisTurn = false;
		hasDoubleAttackedThisTurn = false;
		hasDoubleMovedThisTurn = false;
		
		if (homeScreenPosition == null)
			homeScreenPosition = new Point(position.x / 19, position.y / 9); 
		
		if (leftHand != null)
			leftHand.update(world, this);

		if (rightHand != null)
			rightHand.update(world, this);
		
		if (poisonCounter > 0 && poisonCounter-- % 5 == 0)
			loseHearts(world, lastPoisonedBy, 1, null, "You died of poison from a " + lastPoisonedBy.name());
		
		if (reducedEvasionCounter > 0)
			reducedEvasionCounter--;
		
		if (hasTrait(Trait.REGENERATES) && regenerateCounter-- < 1){
			regenerateCounter = 10;
			if (hearts < maxHearts)
				recoverHearts(1);
		}
		
		if (hasTrait(Trait.HIDER) && hiddenCounter-- < 1){
			hiddenCounter = 5 + (int)(Math.random() * 5);
			
			if (positionBeforeHiding == null)
				hide(world);
			else
				unhide(world);
		}
		
		if (!isHidden() && !canEnter(world.tile(position.x, position.y)))
			loseHearts(world, this, 1, null, "You died from standing in " + world.tile(position.x, position.y).description());
		
		if (hasTrait(Trait.ROCK_SPITTER) 
				&& projectileCooldown-- < 1 
				&& Math.random() < 0.1)
			spitRock(world);
	}

	private void spitRock(World world) {
		while (lastWanderX == 0 && lastWanderY == 0){ // don't spit rock at yourself
			lastWanderX = (int)(Math.random() * 3) - 1;
			lastWanderY = (int)(Math.random() * 3) - 1;
		}
		
		projectileCooldown += 10 + (int)(Math.random() * 10);
		world.add(new Projectile("rock", this,   7, AsciiPanel.brightYellow, 1, position.plus(lastWanderX, lastWanderY), new Point(lastWanderX, lastWanderY)));
		world.add(new Projectile("", this, 250, AsciiPanel.brightYellow, 0, position.copy(), new Point(lastWanderX, lastWanderY)));
	}

	private void unhide(World world) {
		int x = 0;
		int y = 0;
		
		while (positionBeforeHiding != null){
			if (hasTrait(Trait.TERRITORIAL)){
				x = homeScreenPosition.x * 19 + (int)(Math.random() * 19);
				y = homeScreenPosition.y *  9 + (int)(Math.random() * 9);	
			} else {
				x = positionBeforeHiding.x + (int)(Math.random() * 7) - 3;
				y = positionBeforeHiding.y + (int)(Math.random() * 7) - 3;
			}
			
			if (!canEnter(world.tile(x, y)) || world.creature(x, y) != null)
				continue;
			
			position.x = x;
			position.y = y;
			positionBeforeHiding = null;
		}

		publishMessage(new Unhid(world, this));
		
		if (hasTrait(Trait.ROCK_SPITTER))
			spitRock(world);
	}

	private void hide(World world) {
		if (!world.tile(position.x, position.y).canHideIn())
			return;
		
		positionBeforeHiding = position.copy();
		publishMessage(new Hid(world, this));
		position.x = -100;
		position.y = -100;
	}

	public boolean isHidden(){
		return position.x == -100 && position.y == -100;
	}

	public void wander(World world){
		if (prey != null && (prey.hearts < 1 || prey.isHidden() || huntCounter-- < 1))
			prey = null;
		else if (prey != null && prey.hearts > 0)
			moveTo(world, prey);

		if (preditor != null && (preditor.hearts < 1 || preditor.isHidden() || fleeCounter-- < 1))
			preditor = null;
		else if (preditor != null && preditor.hearts > 0)
			moveFrom(world, preditor);
		
		else if (hasTrait(Trait.AGGRESSIVE))
			fightNearby(world);
		else
			wanderForReal(world);
	}

	private void moveTo(World world, Creature target) {
		int mx = (int)Math.signum(target.position.x - position.x);
		int my = (int)Math.signum(target.position.y - position.y);
		
		if (canEnter(world.tile(position.x+mx, position.y+my)))
			moveBy(world, mx, my);
		else
			wanderForReal(world);
		
		if (hasTrait(Trait.DOUBLE_MOVE) && !hasDoubleMovedThisTurn){
			hasDoubleMovedThisTurn = true;
			moveTo(world, target);
		}
	}
	
	private void moveFrom(World world, Creature target) {
		int mx = -(int)Math.signum(target.position.x - position.x);
		int my = -(int)Math.signum(target.position.y - position.y);
		
		if (canEnter(world.tile(position.x+mx, position.y+my)))
			moveBy(world, mx, my);
		else
			wanderForReal(world);
		
		if (hasTrait(Trait.DOUBLE_MOVE) && !hasDoubleMovedThisTurn){
			hasDoubleMovedThisTurn = true;
			moveFrom(world, target);
		}
	}
	
	private void wanderForReal(World world) {
		if (Math.random() < 0.33)
			lastWanderX = (int)(Math.random() * 3) - 1;
		if (Math.random() < 0.33)
			lastWanderY = (int)(Math.random() * 3) - 1;
		
		moveBy(world, lastWanderX, lastWanderY);
		
		if (hasTrait(Trait.DOUBLE_MOVE) && !hasDoubleMovedThisTurn){
			hasDoubleMovedThisTurn = true;
			wanderForReal(world);
		}
	}
	
	public void fightNearby(World world){
		List<Creature> candidates = new ArrayList<Creature>();
		
		for (Point p : position.neighbors()){
			Creature other = world.creature(p.x, p.y);
			if (other != null && !isFriendlyTo(other))
				candidates.add(other);
		}

		if (candidates.size() == 0)
			wanderForReal(world);
		else {
			Creature other = candidates.get((int)(Math.random() * candidates.size()));
			hunt(other);
			attack(world, other, "aggressively");
		}
	}

	public void recoverHearts(int amount) {
		hearts = Math.min(hearts + amount, maxHearts);
	}

	public void loseHearts(World world, Creature attacker, int amount, String specialType, String causeOfDeath) {
		if (hearts < 1)
			return;
		
		if (amount > 1 && hasTrait(Trait.EXTRA_DEFENSE)){
			amount--;
			publishMessage(new BlockSomeDamage(world, attacker, this, rightHand));
		}
		
		hearts -= amount;

		if (specialType != null)
			publishMessage(new Attacked(world, attacker, this, specialType));
		
		if (hearts < 1) {
			this.causeOfDeath = causeOfDeath;
			publishMessage(new Killed(world, attacker, this));
		} else if (hasTrait(Trait.SOCIAL))
			publishMessage(new CallForHelp(world, this, attacker));
	}

	private Creature prey;
	private int huntCounter;
	public void hunt(Creature prey) {
		this.prey = prey;
		huntCounter = 6;
	}

	private Creature preditor;
	private int fleeCounter;
	public void fleeFrom(Creature preditor) {
		this.preditor = preditor;
		fleeCounter = 4;
	}
	
	public void gainRupees(int amount) {
		rupees = Math.min(rupees + amount, 255);
	}
	
	public void loseRupees(World world, int amount) {
		if (!isPlayer())
			return;
		
		rupees -= amount;
		
		if (rupees < 0) {
			int debt = 0 - rupees;
			rupees = 0;
			loseHearts(world, this, debt, null, "You spent you last heart after running out of rupees.");
		}
	}

	public void swapLeftHand(World world, Item item) {
		if (world != null){
			world.removeItem(position.x, position.y);
			world.add(leftHand, position.x, position.y);
			if (leftHand != null)
				publishMessage(new DroppedWeapon(world, this, leftHand));
			publishMessage(new EquipedItem(world, this, item));
		}
		leftHand = item;
	}

	public void swapRightHand(World world, Item item) {
		if (world != null){
			world.removeItem(position.x, position.y);
			world.add(rightHand, position.x, position.y);
			if (rightHand != null)
				publishMessage(new DroppedWeapon(world, this, rightHand));
			publishMessage(new EquipedItem(world, this, item));
		}
		rightHand = item;
	}
	
	public boolean canSee(Creature other){
		int viewDistance = 9;
		
		if (other.hasTrait(Trait.CAMOUFLAGED) && !hasTrait(Trait.DETECT_CAMOUFLAGED))
			viewDistance = 5;
		
		return position.distanceTo(other.position) < viewDistance;
	}
	
	public boolean canHear(Creature other) {
		return position.distanceTo(other.position) < 12;
	}
	
	private RivalAi ai;
	public void registerAiForEvents(RivalAi rivalAi) {
		ai = rivalAi;
	}
	
	private void publishMessage(Message message){
		MessageBus.publish(message);
		
		if (ai != null)
			ai.handle(this, message);
	}
}
