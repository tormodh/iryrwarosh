package iryrwarosh;

import java.util.ArrayList;
import java.util.List;

public class World {
	private WorldMap map;
	private Tile[][] tiles;
	
	private List<Creature> creatures;
	public List<Creature> creatures() { return creatures; }
	
	private Item[][] items;
	private List<Item> itemList;
	
	private List<Projectile> projectiles;
	public List<Projectile> projectiles() { return projectiles; }
	
	public int width() { return tiles.length; }
	
	public int height() { return tiles[0].length; }
	
	public World(Tile[][] tiles, WorldMap map){
		this.tiles = tiles;
		this.map = map;
		this.creatures = new ArrayList<Creature>();
		this.items = new Item[tiles.length][tiles[0].length];
		this.itemList = new ArrayList<Item>();
		this.projectiles = new ArrayList<Projectile>();
	}
	
	public WorldMap map(){
		return map;
	}
	
	public Tile tile(int x, int y){
		if (x < 0 || x >= tiles.length || y < 0 || y >= tiles[0].length)
			return Tile.OUT_OF_BOUNDS;
		else
			return tiles[x][y];
 	}
	
	public Item item(int x, int y){
		if (x < 0 || x >= tiles.length || y < 0 || y >= tiles[0].length)
			return null;
		else
			return items[x][y];
 	}
	
	public Creature creature(int x, int y){
		for (Creature c : creatures)
    		if (c.position.x == x && c.position.y == y)
    			return c;
		
		return null;
	}
	
    public void update(){
    	updateLava();
    	updateWater();
    	
    	List<Item> itemsToUpdate = new ArrayList<Item>();
    	itemsToUpdate.addAll(itemList);
    	for (Item item : itemsToUpdate)
    		item.update(this, null);
    	
    	List<Projectile> stillFlying = new ArrayList<Projectile>();
    	for (Projectile p : projectiles){
    		p.checkForCollision(this);
    		if (!p.isDone())
    			stillFlying.add(p);
    	}
    	projectiles = stillFlying;
    	
    	stillFlying = new ArrayList<Projectile>();
    	for (Projectile p : projectiles){
    		p.moveAndCheckForCollision(this);
    		if (!p.isDone())
    			stillFlying.add(p);
    	}
    	projectiles = stillFlying;
    	
    	List<Creature> toUpdate = new ArrayList<Creature>();
    	toUpdate.addAll(creatures);
    	for (Creature c : toUpdate)
    		if (c.hearts() > 0)
    			c.update(this);
    	
    	List<Creature> stillAlive = new ArrayList<Creature>();
    	
    	for (Creature c : creatures)
    		if (c.hearts() > 0)
    			stillAlive.add(c);
    	
    	creatures = stillAlive;
    }
    
    private void updateLava() {
    	for (int x = 0; x < tiles.length; x++)
        for (int y = 0; y < tiles[0].length; y++){
        	if (!tiles[x][y].isLava())
        		continue;
        	
        	if (Math.random() < 0.75)
        		continue;
        	
        	tiles[x][y] = Tile.LAVA1.variation(x, y);
        }
	}

	private void updateWater(){
    	for (int x = 0; x < tiles.length; x++)
        for (int y = 0; y < tiles[0].length; y++){
        	if (!tiles[x][y].isWater())
        		continue;
        	
        	if (x < tiles.length-1 && tiles[x+1][y].isWater())
        		tiles[x][y] = tiles[x+1][y];
        	else
        		tiles[x][y] = Tile.WATER1.variation(x, y);
        }
    }

	public void add(Creature creature) {
		while (creature.position == null){
			int x = (int)(Math.random() * tiles.length);
			int y = (int)(Math.random() * tiles[0].length);
			
			if (creature.isHuman() && map.screen(x/19, y/9).defaultGround == Tile.DESERT_SAND1)
				continue; // too crowded in the deserts
			
			if (creature.canEnter(tile(x,y)) && creature(x,y) == null)
				creature.position = new Point(x, y);
		}
		creatures.add(creature);
	}

	public void addToScreen(Creature creature, int sx, int sy) {
		while (creature.position == null){
			int x = sx * 19 + (int)(Math.random() * 19);
			int y = sy *  9 + (int)(Math.random() * 9);
			
			if (creature.canEnter(tile(x,y)) && creature(x,y) == null)
				creature.position = new Point(x, y);
		}
		creatures.add(creature);
	}

	public void addToScreen(Item item, int sx, int sy) {
		while (true){
			int x = sx * 19 + (int)(Math.random() * 19);
			int y = sy *  9 + (int)(Math.random() * 9);
			
			if (!tile(x,y).isGround())
				continue;
			
			items[x][y] = item;
			itemList.add(item);
			break;
		}
	}

	public void add(Item item, int x, int y){
		if (item == null || x < 0 || y < 0 || x >= items.length || y >= items[0].length)
			return;
		
		removeItem(x, y);
		
        items[x][y] = item;
		itemList.add(item);
        Creature here = creature(x, y);
        if (here != null && here.hearts() > 0)
        	item.onCollide(this, here);
    }

	public void add(Item item) {
		int x = (int)(Math.random() * tiles.length);
		int y = (int)(Math.random() * tiles[0].length);
		
		while (!tile(x,y).isGround()){
			x = (int)(Math.random() * tiles.length);
			y = (int)(Math.random() * tiles[0].length);
		}
		items[x][y] = item;
		itemList.add(item);
	}

	public void removeItem(int x, int y) {
		if (items[x][y] == null)
			return;
		
		itemList.remove(items[x][y]);
		items[x][y] = null;
	}
	
	public List<Point> screensOfType(Tile biome){
		return map.scrensOfType(biome);
	}

	public void setTile(Tile tile, int x, int y) {
		tiles[x][y] = tile;
	}

	public void add(Projectile projectile) {
		projectiles.add(projectile);
	}
	
	public List<Creature> creaturesNear(Creature creature){
		List<Creature> near = new ArrayList<Creature>();
		
		for (Creature other : creatures){
			if (other == creature)
				continue;
			
			if (creature.canSee(other))
				near.add(other);
		}
		
		return near;
	}

	public List<Pair<Item,Point>> itemsNear(Creature creature){
		List<Pair<Item,Point>> near = new ArrayList<Pair<Item,Point>>();
		
		for (int ox = -9; ox < 10; ox++)
		for (int oy = -9; oy < 10; oy++){
			int x = creature.position.x + ox;
			int y = creature.position.y + oy;
			
			if (x < 0 || y < 0 || x >= items.length || y >= items[0].length)
				continue;
			
			if (items[x][y] != null)
				near.add(new Pair<Item,Point>(items[x][y], new Point(x,y)));
		}
		
		return near;
	}
}
