package iryrwarosh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Worldgen {
	private WorldScreen[][] cells;
	private Tile[][] tiles;
	private int screenWidth = 19;
	private int screenHeight = 9;
	private String dirs = "NSWE";
	
	public Worldgen(int width, int height) {
		this.cells = new WorldScreen[width][height];
		this.tiles = new Tile[width * screenWidth][height * screenHeight];

		for (int x = 0; x < cells.length; x++)
		for (int y = 0; y < cells[0].length; y++)
			this.cells[x][y] = new WorldScreen();
	}

	public World build(){
		makePerfectMazeWithCells();
		addExtraConnectionsToCells();
		addThemesToCells();
		addDesertToCells();
		setTiles();
		addExtraQuarterScreenWallsToTiles();
		addShoreLineToTiles();
		addShoreLineToTiles();
		addLakeToTiles();
		addLakeToTiles();
		return new World(tiles, new WorldMap(cells));
	}

	private void makePerfectMazeWithCells(){
		int width = cells.length;
		int height = cells[0].length;
		boolean[][] connected = new boolean[cells.length][cells[0].length];
		
		List<Point> path = new ArrayList<Point>();
		path.add(new Point((int)(Math.random() * cells.length), (int)(Math.random() * cells[0].length)));
		
		while (!path.isEmpty()) {
			Point p = path.remove((int) (Math.random() * path.size()));

			String possibleDirections = "";

			if (p.x + 1 < width && !connected[p.x + 1][p.y]) 
				possibleDirections += 'E';

			if (p.x - 1 >= 0 && !connected[p.x - 1][p.y])
				possibleDirections += 'W';

			if (p.y + 1 < height && !connected[p.x][p.y + 1])
				possibleDirections += 'S';

			if (p.y - 1 >= 0 && !connected[p.x][p.y - 1])
				possibleDirections += 'N';

			if (possibleDirections.length() > 0) {
				char direction = possibleDirections.charAt((int) (Math.random() * possibleDirections.length()));

				if (possibleDirections.length() > 1)
					path.add(p.copy());

				connected[p.x][p.y] = true;

				connectScreens(p, direction);

				connected[p.x][p.y] = true;
				path.add(p.copy());
			}
		}
	}

	private void addExtraConnectionsToCells(){
		int width = cells.length;
		int height = cells[0].length;
		int total = Math.max(width, height);
		
		while (total-- > 0) {
			Point p = new Point((int)(Math.random() * width),
					            (int)(Math.random() * height));

			String possibleDirections = "";

			if (p.x + 1 < width && cells[p.x][p.y].eEdge == WorldScreen.WALL) 
				possibleDirections += 'E';

			if (p.x - 1 >= 0 && cells[p.x][p.y].wEdge == WorldScreen.WALL)
				possibleDirections += 'W';

			if (p.y + 1 < height && cells[p.x][p.y].sEdge == WorldScreen.WALL)
				possibleDirections += 'S';

			if (p.y - 1 >= 0 && cells[p.x][p.y].nEdge == WorldScreen.WALL)
				possibleDirections += 'N';

			if (possibleDirections.length() > 0) {
				char direction = possibleDirections.charAt((int) (Math.random() * possibleDirections.length()));
				
				connectScreens(p, direction);
			}
		}
	}

	private void connectScreens(Point p, char direction) {
		int pathType = WorldScreen.CENTER;
		switch ((int)(Math.random() * 7)){
		case 0: pathType = WorldScreen.TOP_LEFT; break;
		case 1: pathType = WorldScreen.BOTTOM_RIGHT; break;
		case 2: pathType = WorldScreen.WIDE; break;
		}
		
		// make sure the edges are clear for the shore line
		if (p.x == 0 && pathType == WorldScreen.TOP_LEFT)
			pathType = WorldScreen.CENTER;
		else if (p.y == 0 && pathType == WorldScreen.TOP_LEFT)
			pathType = WorldScreen.CENTER;
		else if (p.x == cells.length-1 && pathType == WorldScreen.BOTTOM_RIGHT)
			pathType = WorldScreen.CENTER;
		else if (p.y == cells[0].length-1 && pathType == WorldScreen.BOTTOM_RIGHT)
			pathType = WorldScreen.CENTER;
		
		switch (direction){
		case 'N':
			cells[p.x][p.y-1].sEdge = pathType;
			cells[p.x][p.y].nEdge = pathType;
			p.y--;
			break;
		case 'S':
			cells[p.x][p.y+1].nEdge = pathType;
			cells[p.x][p.y].sEdge = pathType;
			p.y++;
			break;
		case 'W':
			cells[p.x-1][p.y].eEdge = pathType;
			cells[p.x][p.y].wEdge = pathType;
			p.x--;
			break;
		case 'E':
			cells[p.x+1][p.y].wEdge = pathType;
			cells[p.x][p.y].eEdge = pathType;
			p.x++;
			break;
		}
	}
	
	public void addThemesToCells(){
		Tile[][] themes = new Tile[cells.length][cells[0].length];
		for (Tile theme : new Tile[]{ 
				Tile.BROWN_ROCK, Tile.BROWN_TREE1, Tile.BROWN_TREE4, 
				Tile.GREEN_ROCK, Tile.GREEN_TREE1, Tile.PINE_TREE1,
				Tile.BROWN_ROCK, Tile.BROWN_TREE1, Tile.BROWN_TREE4, 
				Tile.GREEN_ROCK, Tile.GREEN_TREE1, Tile.PINE_TREE1, 
				Tile.WHITE_ROCK, Tile.WHITE_TREE1 }){
		
			while (true){
				int x = (int)(Math.random() * themes.length);
				int y = (int)(Math.random() * themes[0].length);
				
				if (themes[x][y] == null){
					themes[x][y] = theme;
					break;
				}
			}
		}
		
		spreadThemesUntilComplete(themes);
		setThemeGround();
	}

	private void spreadThemesUntilComplete(Tile[][] themes) {
		int unthemedCount = 0;

		do {
			themes = spreadThemesOnce(themes);

			unthemedCount = 0;
			for (int x = 0; x < themes.length; x++)
			for (int y = 0; y < themes[0].length; y++){
				if (themes[x][y] == null)
					unthemedCount++;
			}
			
		} while (unthemedCount > 0);
		
		placeThemes(themes);
	}
	
	private Tile[][] spreadThemesOnce(Tile[][] themes) {
		Tile[][] themes2 = new Tile[themes.length][themes[0].length];
		List<Character> directions = Arrays.asList('N', 'S', 'W', 'E');
		
		for (int x = 0; x < themes.length; x++)
		for (int y = 0; y < themes[0].length; y++){
			if (themes[x][y] != null) {
				themes2[x][y] = themes[x][y];
			} else {
				Collections.shuffle(directions);
				
				for (Character direction : directions){
					switch (direction){
					case 'N':
						if (cells[x][y].nEdge != WorldScreen.WALL && themes[x][y-1] != null)
							themes2[x][y] = themes[x][y-1];
						break;
					case 'S':
						if (cells[x][y].sEdge != WorldScreen.WALL && themes[x][y+1] != null)
							themes2[x][y] = themes[x][y+1];
						break;
					case 'W':
						if (cells[x][y].wEdge != WorldScreen.WALL && themes[x-1][y] != null)
							themes2[x][y] = themes[x-1][y];
						break;
					case 'E':
						if (cells[x][y].eEdge != WorldScreen.WALL && themes[x+1][y] != null)
							themes2[x][y] = themes[x+1][y];
						break;
					}
				}
			}
		}
	
		return themes2;
	}

	private void placeThemes(Tile[][] themes) {
		for (int x = 0; x < themes.length; x++)
		for (int y = 0; y < themes[0].length; y++){
			if (themes[x][y] != null){
				cells[x][y].defaultWall = themes[x][y];
			}
		}
	}
	
	private void setThemeGround(){
		for (int x = 0; x < cells.length; x++)
		for (int y = 0; y < cells[0].length; y++){
			switch (cells[x][y].defaultWall){
			case BROWN_ROCK:
			case BROWN_TREE1:
			case BROWN_TREE4:
				cells[x][y].defaultGround = Tile.BROWN_DIRT;
				break;
			case GREEN_ROCK:
			case GREEN_TREE1:
			case PINE_TREE1:
				cells[x][y].defaultGround = Tile.GREEN_DIRT;
				break;
			case WHITE_ROCK:
			case WHITE_TREE1:
				cells[x][y].defaultGround = Tile.WHITE_DIRT;
				break;
			}
		}
	}
	
	private void addDesertToCells(){
		int x = (int)(Math.random() * cells.length - 2) + 1;
		int y = (int)(Math.random() * cells[0].length - 2) + 1;
		
		cells[x][y].canAddQuarterSection = false;
		cells[x][y].defaultGround = Tile.DESERT_SAND1;
		cells[x][y].defaultWall = Tile.BROWN_ROCK;
		cells[x][y].sEdge = WorldScreen.WIDE;
		cells[x][y].eEdge = WorldScreen.WIDE;

		cells[x+1][y].canAddQuarterSection = false;
		cells[x+1][y].defaultGround = Tile.DESERT_SAND1;
		cells[x+1][y].defaultWall = Tile.BROWN_ROCK;
		cells[x+1][y].sEdge = WorldScreen.WIDE;
		cells[x+1][y].wEdge = WorldScreen.WIDE;

		cells[x][y+1].canAddQuarterSection = false;
		cells[x][y+1].defaultGround = Tile.DESERT_SAND1;
		cells[x][y+1].defaultWall = Tile.BROWN_ROCK;
		cells[x][y+1].nEdge = WorldScreen.WIDE;
		cells[x][y+1].eEdge = WorldScreen.WIDE;

		cells[x+1][y+1].canAddQuarterSection = false;
		cells[x+1][y+1].defaultGround = Tile.DESERT_SAND1;
		cells[x+1][y+1].defaultWall = Tile.BROWN_ROCK;
		cells[x+1][y+1].nEdge = WorldScreen.WIDE;
		cells[x+1][y+1].wEdge = WorldScreen.WIDE;
	}
	
	private void setTiles(){
		for (int x = 0; x < cells.length; x++)
		for (int y = 0; y < cells[0].length; y++)
			setTilesForScreen(x, y);

		for (int x = 0; x < cells.length; x++)
		for (int y = 0; y < cells[0].length; y++)
			addBorderOpenings(x, y);
	}

	private void setTilesForScreen(int sx, int sy){
		addMap(sx, sy, 
			    "###################"
			  + "#.................#"
			  + "#.................#"
			  + "#.................#"
			  + "#.................#"
			  + "#.................#"
			  + "#.................#"
			  + "#.................#"
			  + "###################");
		
		if (cells[sx][sy].defaultGround == Tile.DESERT_SAND1)
			return;
		
		if (isDeadEnd(sx, sy) && Math.random() < 0.5){
			setDeadEndTiles(sx,sy);
			return;
		}
		
		switch ((int)(Math.random() * 7)){
		case 0:
			setTilesFullScreen(sx, sy);
			break;
		case 1:
		case 2:
			setTilesOuterScreen(sx, sy);
			setTilesInnerScreen(sx, sy);
			break;
		case 3:
		case 4:
			setTilesLeftScreen(sx, sy);
			setTilesRightScreen(sx, sy);
			break;
		case 5:
		case 6:
			setTilesTopOfScreen(sx, sy);
			setTilesBottomOfScreen(sx, sy);
			break;
		}

		switch ((int)(Math.random() * 60)){
		case 0: setTilesOuterScreen(sx, sy); break;
		case 1: setTilesInnerScreen(sx, sy); break;
		case 2: setTilesLeftScreen(sx, sy); break;
		case 3: setTilesRightScreen(sx, sy); break;
		case 4: setTilesTopOfScreen(sx, sy); break;
		case 5: setTilesBottomOfScreen(sx, sy); break;
		}
	}

	private boolean isDeadEnd(int sx, int sy) {
		int openings = 0;
		if (cells[sx][sy].nEdge != WorldScreen.WALL) openings++;
		if (cells[sx][sy].eEdge != WorldScreen.WALL) openings++;
		if (cells[sx][sy].sEdge != WorldScreen.WALL) openings++;
		if (cells[sx][sy].wEdge != WorldScreen.WALL) openings++;
		return openings == 1;
	}
	
	private void setDeadEndTiles(int sx, int sy){
		Tile liquid = Math.random() < 0.66 ? Tile.WATER1 : Tile.LAVA1;
		
		if (Math.random() < 0.33){
			cells[sx][sy].defaultGround = Tile.WHITE_TILE1;
			cells[sx][sy].defaultWall = Tile.WHITE_WALL;
		}
		
		Tile random = getRandomWall();
		switch ((int)(Math.random() * 10)){
		case 0:
		case 1: random = Tile.WATER1; break;
		case 2:
		case 3: random = Tile.LAVA1; break;
		case 4: random = Tile.STATUE; break;
		}
		switch ((int)(Math.random() * 20)){
		case 0:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#.#.ttttt=ttttt.#.#"
				  + "#...tt.......tt...#"
				  + "#.ttt.........ttt.#"
				  + "#...tt.......tt...#"
				  + "#.#.ttttt=ttttt.#.#"
				  + "#.................#"
				  + "###################", liquid);
			break;
		case 1:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#.ttttttt.ttttttt.#"
				  + "#.t.............t.#"
				  + "#.t.............t.#"
				  + "#.t.............t.#"
				  + "#.ttttttt.ttttttt.#"
				  + "#.................#"
				  + "###################", random);
			break;
		case 2:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "# .tttttt=tttttt..#"
				  + "#.tttttt...tttttt.#"
				  + "#.=.............=.#"
				  + "#.tttttt...tttttt.#"
				  + "#..tttttt=tttttt..#"
				  + "#.................#"
				  + "###################", liquid);
			break;
		case 3:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#..ttt.......ttt..#"
				  + "#.tt...........tt.#"
				  + "#.......t.t.......#"
				  + "#.tt...........tt.#"
				  + "#..ttt.......ttt..#"
				  + "#.................#"
				  + "###################", random);
			break;
		case 4:
			addMap(sx, sy, 
				    "##ttttttttttttttt##"
				  + "#ttttt.......ttttt#"
				  + "tt.....t...t.....tt"
				  + "t..t..tt...tt..t..t"
				  + "t.ttt.........ttt.t"
				  + "t..t..tt...tt..t..t"
				  + "tt.....t...t.....tt"
				  + "#ttttt.......ttttt#"
				  + "##ttttttttttttttt##", liquid);
			break;
		case 5:
			addMap(sx, sy, 
				    "###################"
				  + "#####.........#####"
				  + "###.............###"
				  + "##...............##"
				  + "#.......&.&.......#"
				  + "##...............##"
				  + "###.............###"
				  + "#####.........#####"
				  + "###################");
			break;
		case 6:
			addMap(sx, sy, 
				    "###################"
				  + "####...........####"
				  + "##....t..t..t....##"
				  + "#...t.........t...#"
				  + "#.t.....t.t.....t.#"
				  + "#...t.........t...#"
				  + "##....t..t..t....##"
				  + "####...........####"
				  + "###################", random);
			break;
		case 7:
			cells[sx][sy].defaultGround = Tile.DESERT_SAND1;
			cells[sx][sy].defaultWall = Tile.BROWN_ROCK;
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#.#.............#.#"
				  + "#.................#"
				  + "#.................#"
				  + "#.................#"
				  + "#.#.............#.#"
				  + "#.................#"
				  + "###################");
			break;
		case 8:
			addMap(sx, sy, 
				    "###################"
				  + "#tttttttt.tttttttt#"
				  + "#tttttttt=tttttttt#"
				  + "#ttttttt...ttttttt#"
				  + "#......=...=......#"
				  + "#ttttttt...ttttttt#"
				  + "#tttttttt=tttttttt#"
				  + "#tttttttt.tttttttt#"
				  + "###################", liquid);
			break;
		case 9:
			addMap(sx, sy, 
				    "##ttttttttttttttt##"
				  + "#ttt...........ttt#"
				  + "tt...............tt"
				  + "t......ttttt......t"
				  + "t.....ttttttt.....t"
				  + "t......ttttt......t"
				  + "tt...............tt"
				  + "#ttt...........ttt#"
				  + "##ttttttttttttttt##", liquid);
			break;
		case 10:
			addMap(sx, sy, 
				    "###################"
				  + "###.............###"
				  + "##.....ttttt.....##"
				  + "#.....tt#t#tt.....#"
				  + "#.....ttttttt.....#"
				  + "#.....tt#t#tt.....#"
				  + "##.....ttttt.....##"
				  + "###.............###"
				  + "###################", liquid);
			break;
		case 11:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#.tt=ttttttttt=tt.#"
				  + "#.t.............t.#"
				  + "#.t.&.........&.t.#"
				  + "#.t.............t.#"
				  + "#.tt=ttttttttt=tt.#"
				  + "#.................#"
				  + "###################", liquid);
			break;
		case 12:
			addMap(sx, sy, 
				    "###################"
				  + "##...............##"
				  + "#...ttttttttttt...#"
				  + "#.tttttt...tttttt.#"
				  + "#.tttttt...tttttt.#"
				  + "#.tttttt...tttttt.#"
				  + "#...ttttt=ttttt...#"
				  + "##...............##"
				  + "###################", liquid);
			break;
		case 13:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#.&.&.&.&.&.&.&.&.#"
				  + "#..tttttt=tttttt..#"
				  + "#.&t...........t&.#"
				  + "#..tttttt=tttttt..#"
				  + "#.&.&.&.&.&.&.&.&.#"
				  + "#.................#"
				  + "###################", liquid);
			break;
		case 14:
			addMap(sx, sy, 
				    "###################"
				  + "####x.........x####"
				  + "##x.............x##"
				  + "#x...............x#"
				  + "#x.....x...x.....x#"
				  + "#x...............x#"
				  + "##x.............x##"
				  + "####x.........x####"
				  + "###################");
			break;
		case 15:
			addMap(sx, sy, 
				    "###################"
				  + "#tt.............tt#"
				  + "#tt.ttt.....ttt.tt#"
				  + "#...txt.....txt...#"
				  + "#.tttxt.....txttt.#"
				  + "#...txt.....txt...#"
				  + "#tt.ttt.....ttt.tt#"
				  + "#tt.............tt#"
				  + "###################", liquid);
			break;
		case 16:
			addMap(sx, sy, 
				    "#ttttttttttttttttt#"
				  + "t...t...t..t..t...t"
				  + "t...=...t..t..=...t"
				  + "t...t...t==t..t...t"
				  + "ttt=ttt==..==tt=ttt"
				  + "t...t...t==t..t...t"
				  + "t...=...t..t..=...t"
				  + "t...t...t..t..t...t"
				  + "#ttttttttttttttttt#", liquid);
			break;
		case 17:
			addMap(sx, sy, 
				    "###################"
				  + "##tt.....=.....tt##"
				  + "#tttttttt=tttttttt#"
				  + "#ttttttt...ttttttt#"
				  + "#=======...=======#"
				  + "#ttttttt...ttttttt#"
				  + "#tttttttt=tttttttt#"
				  + "##tt.....=.....tt##"
				  + "###################", liquid);
			break;
		case 18:
			addMap(sx, sy,
				    "###################"
				  + "#..&...&...&...&..#"
				  + "#&...x...x...x...&#"
				  + "#..x...x...x...x..#"
				  + "#&...x.......x...&#"
				  + "#..x...x...x...x..#"
				  + "#&...x...x...x...&#"
				  + "#..&...&...&...&..#"
				  + "###################");
			break;
		case 19:
			addMap(sx, sy, 
				    "###################"
				  + "##.x.x.x...x.x.x.##"
				  + "#.ttttttt=ttttttt.#"
				  + "#xt.............tx#"
				  + "#.=.............=.#"
				  + "#xt.............tx#"
				  + "#.ttttttt=ttttttt.#"
				  + "##.x.x.x...x.x.x.##"
				  + "###################");
			break;
		}
		
		cells[sx][sy].canAddQuarterSection = false;
		convertEdgesToCenter(sx, sy);
	}

	private void convertEdgesToCenter(int sx, int sy) {
		if (cells[sx][sy].nEdge != WorldScreen.WALL) {
			cells[sx][sy].nEdge = WorldScreen.CENTER;
			cells[sx][sy-1].sEdge = WorldScreen.CENTER;
		}
		if (cells[sx][sy].eEdge != WorldScreen.WALL) {
			cells[sx][sy].eEdge = WorldScreen.CENTER;
			cells[sx+1][sy].wEdge = WorldScreen.CENTER;
		}
		if (cells[sx][sy].sEdge != WorldScreen.WALL) {
			cells[sx][sy].sEdge = WorldScreen.CENTER;
			cells[sx][sy+1].nEdge = WorldScreen.CENTER;
		}
		if (cells[sx][sy].wEdge != WorldScreen.WALL) {
			cells[sx][sy].wEdge = WorldScreen.CENTER;
			cells[sx-1][sy].eEdge = WorldScreen.CENTER;
		}
	}
	
	private void setTilesFullScreen(int sx, int sy){
		switch ((int)(Math.random() * 10)){
		case 0:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#.................#"
				  + "#.................#"
				  + "#.................#"
				  + "#.................#"
				  + "#.................#"
				  + "#.................#"
				  + "###################");
			break;
		case 1:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#.x.............x.#"
				  + "#.................#"
				  + "#.................#"
				  + "#.................#"
				  + "#.x.............x.#"
				  + "#.................#"
				  + "###################");
			break;
		case 2:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#.xxxxxxxxxxxxxxx.#"
				  + "#.xxxxxxxxxxxxxxx.#"
				  + "#.xxxxxxxxxxxxxxx.#"
				  + "#.xxxxxxxxxxxxxxx.#"
				  + "#.xxxxxxxxxxxxxxx.#"
				  + "#.................#"
				  + "###################");
			cells[sx][sy].canAddQuarterSection = false;
			break;
		case 3:
			addMap(sx, sy, 
				    "###################"
				  + "#..x...x...x...x..#"
				  + "#x...x...x...x...x#"
				  + "#..x...x...x...x..#"
				  + "#x...x...x...x...x#"
				  + "#..x...x...x...x..#"
				  + "#x...x...x...x...x#"
				  + "#..x...x...x...x..#"
				  + "###################");
			break;
		case 4:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#.x.x.x.x.x.x.x.x.#"
				  + "#.................#"
				  + "#..x.x.x.x.x.x.x..#"
				  + "#.................#"
				  + "#.x.x.x.x.x.x.x.x.#"
				  + "#.................#"
				  + "###################");
			break;
		case 5:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#.~~~~~~~~~~~~~~~.#"
				  + "#.~~~~~~~~~~~~~~~.#"
				  + "#.~~~~~~~~~~~~~~~.#"
				  + "#.~~~~~~~~~~~~~~~.#"
				  + "#.~~~~~~~~~~~~~~~.#"
				  + "#.................#"
				  + "###################");
			cells[sx][sy].canAddQuarterSection = false;
			break;
		case 6:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#......&.&.&......#"
				  + "#.................#"
				  + "#......&.&.&......#"
				  + "#.................#"
				  + "#......&.&.&......#"
				  + "#.................#"
				  + "###################");
			cells[sx][sy].canAddQuarterSection = false;
			break;
		case 7:
			addMap(sx, sy, 
				    "###################"
				  + "##...............##"
				  + "#.................#"
				  + "#.......xxx.......#"
				  + "#......&xxx&......#"
				  + "#.......xxx.......#"
				  + "#.................#"
				  + "##...............##"
				  + "###################");
			cells[sx][sy].canAddQuarterSection = false;
			break;
		case 8:
			addMap(sx, sy, 
				    "###################"
				  + "####...#####...####"
				  + "##...&...&...&...##"
				  + "#.................#"
				  + "#.&...xx...xx...&.#"
				  + "#.................#"
				  + "##...&...&...&...##"
				  + "####...#####...####"
				  + "###################");
			break;
		case 9:
			addMap(sx, sy, 
				    "###################"
				  + "###################"
				  + "##...............##"
				  + "##...x.~~~~~.x...##"
				  + "##.....~~~~~.....##"
				  + "##...x.~~~~~.x...##"
				  + "##...............##"
				  + "###################"
				  + "###################");
			cells[sx][sy].canAddQuarterSection = false;
			break;
		}
	}
	
	private void setTilesInnerScreen(int sx, int sy){
		switch ((int)(Math.random() * 10)){
		case 0:
			addMap(sx, sy, 
					"                   "
				  + "                   "
				  + "                   "
				  + "   .............   "
				  + "   ..&.&.&.&.&..   "
				  + "   .............   "
				  + "                   "
				  + "                   "
				  + "                   ");
			break;
		case 1:
			addMap(sx, sy, 
					"                   "
				  + "                   "
				  + "                   "
				  + "   &...........&   "
				  + "   .............   "
				  + "   &...........&   "
				  + "                   "
				  + "                   "
				  + "                   ");
			break;
		case 2:
			addMap(sx, sy, 
					"                   "
				  + "                   "
				  + "                   "
				  + "   x...x...x...x   "
				  + "   ..x...x...x..   "
				  + "   x...x...x...x   "
				  + "                   "
				  + "                   "
				  + "                   ");
			break;
		case 3:
			addMap(sx, sy, 
					"                   "
				  + "                   "
				  + "                   "
				  + "   xxxxxxxxxxxxx   "
				  + "   xxxxxxxxxxxxx   "
				  + "   xxxxxxxxxxxxx   "
				  + "                   "
				  + "                   "
				  + "                   ");
			cells[sx][sy].canAddQuarterSection = false;
			break;
		case 4:
			addMap(sx, sy, 
					"                   "
				  + "                   "
				  + "                   "
				  + "   .............   "
				  + "   .xxxx...xxxx.   "
				  + "   .............   "
				  + "                   "
				  + "                   "
				  + "                   ");
			break;
		case 5:
			addMap(sx, sy, 
					"                   "
				  + "                   "
				  + "   .~~~~~~~~~~~.   "
				  + "   ~~~~~~~~~~~~~   "
				  + "   ~~~~~~~~~~~~~   "
				  + "   ~~~~~~~~~~~~~   "
				  + "   .~~~~~~~~~~~.   "
				  + "                   "
				  + "                   ");
			cells[sx][sy].canAddQuarterSection = false;
			break;
		case 6:
			addMap(sx, sy, 
					"                   "
				  + "                   "
				  + "                   "
				  + "   .............   "
				  + "   .&xxx###xxx&.   "
				  + "   .............   "
				  + "                   "
				  + "                   "
				  + "                   ");
			cells[sx][sy].canAddQuarterSection = false;
			break;
		case 7:
			addMap(sx, sy, 
					"                   "
				  + "                   "
				  + "                   "
				  + "   x.x.x.x.x.x.x   "
				  + "   .x.x.x.x.x.x.   "
				  + "   x.x.x.x.x.x.x   "
				  + "                   "
				  + "                   "
				  + "                   ");
			break;
		case 8:
			addMap(sx, sy, 
					"                   "
				  + "                   "
				  + "                   "
				  + "   .&.&.&.&.&.&.   "
				  + "   &.#.#.#.#.#.&   "
				  + "   .&.&.&.&.&.&.   "
				  + "                   "
				  + "                   "
				  + "                   ");
			break;
		case 9:
			addMap(sx, sy, 
					"                   "
				  + "                   "
				  + "                   "
				  + "   .&.x.#.#.x.&.   "
				  + "   .............   "
				  + "   .&.x.#.#.x.&.   "
				  + "                   "
				  + "                   "
				  + "                   ");
			break;
		}
	}
	
	private void setTilesOuterScreen(int sx, int sy){
		switch ((int)(Math.random() * 10)){
		case 0:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#.................#"
				  + "#..             ..#"
				  + "#..             ..#"
				  + "#..             ..#"
				  + "#.................#"
				  + "#.................#"
				  + "###################");
			break;
		case 1:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#.x.x.x.x.x.x.x.x.#"
				  + "#..             ..#"
				  + "#.x             x.#"
				  + "#..             ..#"
				  + "#.x.x.x.x.x.x.x.x.#"
				  + "#.................#"
				  + "###################");
			break;
		case 2:
			addMap(sx, sy, 
				    "###################"
				  + "###.............###"
				  + "##...............##"
				  + "#..             ..#"
				  + "#..             ..#"
				  + "#..             ..#"
				  + "##...............##"
				  + "###.............###"
				  + "###################");
			break;
		case 3:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#.xxxx.......xxxx.#"
				  + "#.x             x.#"
				  + "#..             ..#"
				  + "#.x             x.#"
				  + "#.xxxx.......xxxx.#"
				  + "#.................#"
				  + "###################");
			break;
		case 4:
			addMap(sx, sy,  
				    "###################"
				  + "#.................#"
				  + "#...x.x.....x.x...#"
				  + "#.x             x.#"
				  + "#..             ..#"
				  + "#.x             x.#"
				  + "#...x.x.....x.x...#"
				  + "#.................#"
				  + "###################");
			break;
		case 5:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#..x...........x..#"
				  + "#..             ..#"
				  + "#.x             x.#"
				  + "#..             ..#"
				  + "#..x...........x..#"
				  + "#.................#"
				  + "###################");
			break;
		case 6:
			addMap(sx, sy, 
				    "###################"
				  + "###.............###"
				  + "##...............##"
				  + "#..             ..#"
				  + "#..             ..#"
				  + "#..             ..#"
				  + "##...............##"
				  + "###.............###"
				  + "###################");
			break;
		case 7:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#..&.&.&.&.&.&.&..#"
				  + "#..             ..#"
				  + "#..             ..#"
				  + "#..             ..#"
				  + "#..&.&.&.&.&.&.&..#"
				  + "#.................#"
				  + "###################");
			break;
		case 8:
			addMap(sx, sy, 
				    "###################"
				  + "##...............##"
				  + "#...x.x.x.x.x.x...#"
				  + "#..             ..#"
				  + "#..             ..#"
				  + "#..             ..#"
				  + "#...x.x.x.x.x.x...#"
				  + "##...............##"
				  + "###################");
			break;
		case 9:
			addMap(sx, sy, 
				    "###################"
				  + "#.................#"
				  + "#.~~~~~~~.~~~~~~~.#"
				  + "#.~             ~.#"
				  + "#..             ..#"
				  + "#.~             ~.#"
				  + "#.~~~~~~~.~~~~~~~.#"
				  + "#.................#"
				  + "###################");
			break;
		}
	}

	private void setTilesLeftScreen(int sx, int sy){
		switch ((int)(Math.random() * 10)){
		case 0:
			addMap(sx, sy, 
					"##########         "
			      + "##........         "
			      + "##&.......         "
			      + "#.........         "
			      + "#.........         "
			      + "#.........         "
			      + "##&.......         "
			      + "##........         "
			      + "##########         ");
			break;
		case 1:
			addMap(sx, sy, 
					"##########         "
			      + "####...###         "
			      + "##......#.         "
			      + "#.........         "
			      + "#.........         "
			      + "#.........         "
			      + "##......#.         "
			      + "####...###         "
			      + "##########         ");
			break;
		case 2:
			addMap(sx, sy, 
					"##########         "
			      + "#.........         "
			      + "#.xxx.....         "
			      + "#.xxx.....         "
			      + "#.........         "
			      + "#.xxx.....         "
			      + "#.xxx.....         "
			      + "#.........         "
			      + "##########         ");
			break;
		case 3:
			addMap(sx, sy, 
					"##########         "
			      + "#.........         "
			      + "#.........         "
			      + "#.&.......         "
			      + "#.........         "
			      + "#.&.......         "
			      + "#.........         "
			      + "#.........         "
			      + "##########         ");
			break;
		case 4:
			addMap(sx, sy, 
					"##########         "
			      + "#.........         "
			      + "#..xxxxx..         "
			      + "#.........         "
			      + "#..xxxxx..         "
			      + "#.........         "
			      + "#..xxxxx..         "
			      + "#.........         "
			      + "##########         ");
			break;
		case 5:
			addMap(sx, sy, 
					"##########         "
			      + "#.........         "
			      + "#.....xxx.         "
			      + "#.xxx.....         "
			      + "#.....xxx.         "
			      + "#.xxx.....         "
			      + "#.....xxx.         "
			      + "#.........         "
			      + "##########         ");
			break;
		case 6:
			addMap(sx, sy, 
					"##########         "
			      + "#.........         "
			      + "#.xxx.....         "
			      + "#.....xxx.         "
			      + "#.xxx.....         "
			      + "#.....xxx.         "
			      + "#.xxx.....         "
			      + "#.........         "
			      + "##########         ");
			break;
		case 7:
			addMap(sx, sy, 
					"##########         "
			      + "#.........         "
			      + "#.xxxxxx..         "
			      + "#.xxxxxx..         "
			      + "#.xxxxxx..         "
			      + "#.xxxxxx..         "
			      + "#.xxxxxx..         "
			      + "#.........         "
			      + "##########         ");
			break;
		case 8:
			addMap(sx, sy, 
					"##########         "
			      + "#.........         "
			      + "#.x.....x.         "
			      + "#...xxx...         "
			      + "#.x.....x.         "
			      + "#...xxx...         "
			      + "#.x.....x.         "
			      + "#.........         "
			      + "##########         ");
			break;
		case 9:
			addMap(sx, sy, 
					"##########         "
			      + "#..xxxxx..         "
			      + "#.........         "
			      + "#.........         "
			      + "#...xxx...         "
			      + "#.........         "
			      + "#.........         "
			      + "#..xxxxx..         "
			      + "##########         ");
			break;
		}
	}

	private void setTilesRightScreen(int sx, int sy){
		switch ((int)(Math.random() * 10)){
		case 0:
			addMap(sx, sy,
					"          #########"
			      + "          .......##"
			      + "          ......&##"
			      + "          ........#"
			      + "          ........#"
			      + "          ........#"
			      + "          ......&##"
			      + "          .......##"
			      + "          #########");
			break;
		case 1:
			addMap(sx, sy, 
					"          #########"
			      + "          ##...####"
			      + "          #......##"
			      + "          ........#"
			      + "          ........#"
			      + "          ........#"
			      + "          #......##"
			      + "          ##...####"
			      + "          #########");
			break;
		case 2:
			addMap(sx, sy, 
					"          #########"
			      + "          ........#"
			      + "          ....xxx.#"
			      + "          ....xxx.#"
			      + "          ........#"
			      + "          ....xxx.#"
			      + "          ....xxx.#"
			      + "          ........#"
			      + "          #########");
			break;
		case 3:
			addMap(sx, sy, 
					"          #########"
			      + "          ........#"
			      + "          ........#"
			      + "          ......&.#"
			      + "          ........#"
			      + "          ......&.#"
			      + "          ........#"
			      + "          ........#"
			      + "          #########");
			break;
		case 4:
			addMap(sx, sy, 
					"          #########"
			      + "          ........#"
			      + "          ..xxxx..#"
			      + "          ........#"
			      + "          ..xxxx..#"
			      + "          ........#"
			      + "          ..xxxx..#"
			      + "          ........#"
			      + "          #########");
			break;
		case 5:
			addMap(sx, sy, 
					"          #########"
			      + "          ........#"
			      + "          ....xxx.#"
			      + "          xxx.....#"
			      + "          ....xxx.#"
			      + "          xxx.....#"
			      + "          ....xxx.#"
			      + "          ........#"
			      + "          #########");
			break;
		case 6:
			addMap(sx, sy, 
					"          #########"
			      + "          ........#"
			      + "          xxx.....#"
			      + "          ....xxx.#"
			      + "          xxx.....#"
			      + "          ....xxx.#"
			      + "          xxx.....#"
			      + "          ........#"
			      + "          #########");
			break;
		case 7:
			addMap(sx, sy, 
					"          #########"
			      + "          ........#"
			      + "          .xxxxxx.#"
			      + "          .xxxxxx.#"
			      + "          .xxxxxx.#"
			      + "          .xxxxxx.#"
			      + "          .xxxxxx.#"
			      + "          ........#"
			      + "          #########");
			break;
		case 8:
			addMap(sx, sy, 
					"          #########"
			      + "          ........#"
			      + "          x.....x.#"
			      + "          ..xxx...#"
			      + "          x.....x.#"
			      + "          ..xxx...#"
			      + "          x.....x.#"
			      + "          ........#"
			      + "          #########");
			break;
		case 9:
			addMap(sx, sy, 
					"          #########"
			      + "          .xxxxx..#"
			      + "          ........#"
			      + "          ........#"
			      + "          ..xxx...#"
			      + "          ........#"
			      + "          ........#"
			      + "          .xxxxx..#"
			      + "          #########");
			break;
		}
	}

	private void setTilesTopOfScreen(int sx, int sy){
		switch ((int)(Math.random() * 5)){
		case 0:
			addMap(sx, sy,
					"###################"
			      + "#.................#"
			      + "#.......&.&.......#"
			      + "#.................#"
			      + "#.................#"
			      + "                   "
			      + "                   "
			      + "                   "
			      + "                   ");
			break;
		case 1:
			addMap(sx, sy,
					"###################"
			      + "####...........####"
			      + "##....x.....x....##"
			      + "#..x...........x..#"
			      + "#.................#"
			      + "                   "
			      + "                   "
			      + "                   "
			      + "                   ");
			break;
		case 2:
			addMap(sx, sy,
					"###################"
			      + "###.............###"
			      + "#...&...xxx...&...#"
			      + "#.......xxx.......#"
			      + "#.......xxx.......#"
			      + "                   "
			      + "                   "
			      + "                   "
			      + "                   ");
			break;
		case 3:
			addMap(sx, sy,
					"###################"
			      + "#.................#"
			      + "#.x.x.x.x.x.x.x.x.#"
			      + "#.................#"
			      + "#.................#"
			      + "                   "
			      + "                   "
			      + "                   "
			      + "                   ");
			break;
		case 4:
			addMap(sx, sy,
					"###################"
				  + "#.................#"
				  + "#.x.x.x.x.x.x.x.x.#"
				  + "#.x.x.x.x.x.x.x.x.#"
			      + "#.................#"
			      + "                   "
			      + "                   "
			      + "                   "
			      + "                   ");
			break;
		}
	}

	private void setTilesBottomOfScreen(int sx, int sy){
		switch ((int)(Math.random() * 5)){
		case 0:
			addMap(sx, sy,
				    "                   "
				  + "                   "
				  + "                   "
				  + "                   "
			      + "#.................#"
			      + "#.................#"
			      + "#.......&.&.......#"
			      + "#.................#"
				  + "###################");
			break;
		case 1:
			addMap(sx, sy,
				    "                   "
				  + "                   "
				  + "                   "
				  + "                   "
			      + "#.................#"
			      + "#..x...........x..#"
			      + "##....x.....x....##"
			      + "####...........####"
				  + "###################");
			break;
		case 2:
			addMap(sx, sy,
				    "                   "
				  + "                   "
				  + "                   "
				  + "                   "
			      + "#.......xxx.......#"
			      + "#.......xxx.......#"
			      + "#...&...xxx...&...#"
			      + "###.............###"
				  + "###################");
			break;
		case 3:
			addMap(sx, sy,
				    "                   "
				  + "                   "
				  + "                   "
				  + "                   "
			      + "#.................#"
			      + "#.................#"
			      + "#.x.x.x.x.x.x.x.x.#"
			      + "#.................#"
				  + "###################");
			break;
		case 4:
			addMap(sx, sy,
				    "                   "
				  + "                   "
				  + "                   "
				  + "                   "
			      + "#.................#"
				  + "#.x.x.x.x.x.x.x.x.#"
				  + "#.x.x.x.x.x.x.x.x.#"
			      + "#.................#"
				  + "###################");
			break;
		}
	}

	private void addMap(int sx, int sy, String data) {
		addMap(sx, sy, data, Tile.STATUE);
	}
	
	private void addMap(int sx, int sy, String data, Tile specificTile) {
		int mx = sx * screenWidth;
		int my = sy * screenHeight;
		Tile floor   = cells[sx][sy].defaultGround;
		Tile wall    = cells[sx][sy].defaultWall;
		Tile local   = Math.random() < 0.66 ? wall : getRandomWall();
		Tile bridge  = Tile.BRIDGE;
		
		if (Math.random() < 0.025) local = Tile.STATUE;
		
		Tile special = Math.random() < 0.10 ? Tile.STATUE : (Math.random() < 0.50 ? floor : wall);
		
		if (wall == Tile.WHITE_WALL){
			local = Tile.STATUE_WHITE;
			special = Tile.STATUE_WHITE;
			bridge = Tile.WHITE_TILE1;
			if (specificTile != Tile.WATER1 && specificTile != Tile.LAVA1 && specificTile != Tile.BRIDGE)
				specificTile = Tile.STATUE_WHITE;
		}
		
		for (int x = 0; x < screenWidth; x++)
		for (int y = 0; y < screenHeight; y++) {
			switch (data.charAt(x + y * screenWidth)){
			case '.': tiles[mx+x][my+y] = floor.variation(mx+x, my+y); break;
			case '#': tiles[mx+x][my+y] = wall.variation(mx+x, my+y); break;
			case 'x': tiles[mx+x][my+y] = local.variation(mx+x, my+y); break;
			case '~': tiles[mx+x][my+y] = Tile.WATER1.variation(mx+x, my+y); break;
			case '&': tiles[mx+x][my+y] = special; break;
			case 't': tiles[mx+x][my+y] = specificTile; break;
			case '=': tiles[mx+x][my+y] = bridge.variation(mx+x, my+y); break;
			case ' ': break;
			}
		}
	}
	
	private Tile getRandomWall(){
		Tile[] tiles = { Tile.BROWN_ROCK, Tile.BROWN_TREE1, 
						 Tile.BROWN_ROCK, Tile.BROWN_TREE4, 
						 Tile.GREEN_ROCK, Tile.GREEN_TREE1, 
						 Tile.GREEN_ROCK, Tile.PINE_TREE1, 
						 Tile.WHITE_ROCK, Tile.WHITE_TREE1 };
		
		return tiles[(int)(Math.random() * tiles.length)];
	}
	
	private void addBorderOpenings(int x, int y){
		if (cells[x][y].nEdge == WorldScreen.CENTER)
			clear(x * screenWidth + screenWidth/2, y * screenHeight, 1, 2, cells[x][y].defaultGround);
		else if (cells[x][y].nEdge == WorldScreen.TOP_LEFT)
			clear(x * screenWidth + screenWidth/3, y * screenHeight, 1, 2, cells[x][y].defaultGround);
		else if (cells[x][y].nEdge == WorldScreen.BOTTOM_RIGHT)
			clear(x * screenWidth + screenWidth/3*2-1, y * screenHeight, 1, 2, cells[x][y].defaultGround);
		else if (cells[x][y].nEdge == WorldScreen.WIDE)
				clear(x * screenWidth + 1, y * screenHeight, screenWidth - 2, 2, cells[x][y].defaultGround);
		
		if (cells[x][y].sEdge == WorldScreen.CENTER)
			clear(x * screenWidth + screenWidth/2, (y+1) * screenHeight - 2, 1, 2, cells[x][y].defaultGround);
		else if (cells[x][y].sEdge == WorldScreen.TOP_LEFT)
			clear(x * screenWidth + screenWidth/3, (y+1) * screenHeight - 2, 1, 2, cells[x][y].defaultGround);
		else if (cells[x][y].sEdge == WorldScreen.BOTTOM_RIGHT)
			clear(x * screenWidth + screenWidth/3*2-1, (y+1) * screenHeight - 2, 1, 2, cells[x][y].defaultGround);
		else if (cells[x][y].sEdge == WorldScreen.WIDE)
			clear(x * screenWidth + 1, (y+1) * screenHeight - 2, screenWidth - 2, 2, cells[x][y].defaultGround);
		
		if (cells[x][y].wEdge == WorldScreen.CENTER) 
			clear(x * screenWidth, y * screenHeight + screenHeight/2, 2, 1, cells[x][y].defaultGround);
		else if (cells[x][y].wEdge == WorldScreen.TOP_LEFT) 
			clear(x * screenWidth, y * screenHeight + screenHeight/3, 2, 1, cells[x][y].defaultGround);
		else if (cells[x][y].wEdge == WorldScreen.BOTTOM_RIGHT) 
			clear(x * screenWidth, y * screenHeight + screenHeight/3*2-1, 2, 1, cells[x][y].defaultGround);
		else if (cells[x][y].wEdge == WorldScreen.WIDE) 
			clear(x * screenWidth, y * screenHeight + 1, 2, screenHeight - 2, cells[x][y].defaultGround);
		
		if (cells[x][y].eEdge == WorldScreen.CENTER)
			clear((x+1) * screenWidth - 2, y * screenHeight + screenHeight/2, 2, 1, cells[x][y].defaultGround);
		else if (cells[x][y].eEdge == WorldScreen.TOP_LEFT) 
			clear((x+1) * screenWidth - 2, y * screenHeight + screenHeight/3, 2, 1, cells[x][y].defaultGround);
		else if (cells[x][y].eEdge == WorldScreen.BOTTOM_RIGHT) 
			clear((x+1) * screenWidth - 2, y * screenHeight + screenHeight/3*2-1, 2, 1, cells[x][y].defaultGround);
		else if (cells[x][y].eEdge == WorldScreen.WIDE)
			clear((x+1) * screenWidth - 2, y * screenHeight + 1, 2, screenHeight - 2, cells[x][y].defaultGround);
	}

	private void clear(int x, int y, int w, int h, Tile tile) {
		for (int x2 = x; x2 < x + w; x2++)
		for (int y2 = y; y2 < y + h; y2++)
			if (x2 >= 0 && y2 >= 0 && x2 < tiles.length && y2 < tiles[0].length)
				tiles[x2][y2] = tile.variation(x2, y2);
	}
	
	public void addLakeToTiles(){
		int w = cells.length;
		int h = cells[0].length;
		int x = (int)(Math.random() * (w - 2) + 1);
		int y = (int)(Math.random() * (h - 2) + 1);
		
		clear(x * screenWidth - screenWidth/2, 
			  y * screenHeight - screenHeight/2, 
			  screenWidth - 2 + 1, 
			  screenHeight - 2 + 1, Tile.WATER1);
		
		cells[x-1][y-1].seWater = true;
		cells[x][y-1].swWater = true;
		cells[x][y].nwWater = true;
		cells[x-1][y].neWater = true;
		
		addRiver(x, y, 2, 2);
	}

	public void addShoreLineToTiles(){
		char dir = 'N';
		int totalWidth = cells.length * screenWidth;
		int totalHeight = cells[0].length * screenHeight;
		int x = screenWidth / 2;
		int y = 0;
		int start = (int)(Math.random() * (cells.length + cells[0].length) * 2);
		int length = 5 + (int)(Math.random() * (cells.length + cells[0].length - 5)) / 2;
		boolean started = false;
		
		while (length > 0){
			if (!started)
				started = --start < 1;
			
			int sx = x / screenWidth;
			int sy = y / screenHeight;
			
			switch (dir){
			case 'N':
				if (started) {
					clear(x, y, screenWidth, 4, Tile.WATER1);
					cells[sx][sy].nWater = true;
					cells[sx][sy].neWater = true;
					if (sx+1 < cells.length)
						cells[sx+1][sy].nwWater = true;
				}
				x += screenWidth;
				if (x > totalWidth){
					if (started) clear(totalWidth-4, 0, 4, screenHeight / 2, Tile.WATER1);
					x = totalWidth - 4;
					y = screenHeight / 2;
					dir = 'E';
				}
				break;
			case 'E':
				if (started) {
					clear(x, y, 4, screenHeight, Tile.WATER1);
					cells[sx][sy].eWater = true;
					cells[sx][sy].seWater = true;
					if (sy+1 < cells[0].length)
						cells[sx][sy+1].neWater = true;
				}
				y += screenHeight;
				if (y > totalHeight){
					if (started) clear(totalWidth-screenWidth/2, totalHeight-4, screenWidth/2, 4, Tile.WATER1);
					x = totalWidth - screenWidth / 2;
					y = totalHeight - 4;
					dir = 'S';
				}
				break;
			case 'S':
				if (started) {
					clear(x-screenWidth, y, screenWidth, 4, Tile.WATER1);
					cells[sx][sy].sWater = true;
					cells[sx][sy].swWater = true;
					if (sx > 0)
						cells[sx-1][sy].seWater = true;
				}
				x -= screenWidth;
				if (x < 0){
					if (started) clear(0, totalHeight-screenHeight/2, 4, screenHeight / 2, Tile.WATER1);
					x = 0;
					y = totalHeight - screenHeight / 2;
					dir = 'W';
				}
				break;
			case 'W':
				if (started) {
					clear(x, y-screenHeight, 4, screenHeight, Tile.WATER1);
					cells[sx][sy].wWater = true;
					cells[sx][sy].nwWater = true;
					if (sy > 0)
						cells[sx][sy-1].swWater = true;
				}
				y -= screenHeight;
				if (y < 0){
					clear(0, 0, screenWidth/2, 4, Tile.WATER1);
					x = screenWidth / 2;
					y = 0;
					dir = 'N';
				}
				break;
			}
			if (started)
				length--;
		}
	}

	public void addRiver(int x, int y, int offsetX, int offsetY){
		char dir = dirs.charAt((int)(Math.random() * 4));
		
		while (x >= 0 && y >= 0 && x < cells.length+1 && y < cells[0].length+1){
			dir = changeDirection(dir);
			
			if (x < 0 || y < 0 || x >= cells.length || y >= cells[0].length)
				break;
			
			switch (dir){
			case 'N':
				clear(x*screenWidth + offsetX, --y*screenHeight + offsetY, 1, screenHeight+1, Tile.WATER1);
				if (cells[x][y+1].wEdge != WorldScreen.WALL || cells[x][y+1].sEdge == WorldScreen.TOP_LEFT)
					addBridge(x, y+1);
				break;
			case 'S': 
				clear(x*screenWidth + offsetX, y++*screenHeight + offsetY, 1, screenHeight, Tile.WATER1);
				if (cells[x][y-1].wEdge != WorldScreen.WALL || cells[x][y-1].sEdge == WorldScreen.TOP_LEFT)
					addBridge(x, y-1);
				break;
			case 'W': 
				clear(--x*screenWidth + offsetX, y*screenHeight + offsetY, screenWidth+1, 1, Tile.WATER1);
				if (cells[x+1][y].nEdge != WorldScreen.WALL || cells[x+1][y].eEdge == WorldScreen.TOP_LEFT)
					addBridge(x+1, y);
				break;
			case 'E': 
				clear(x++*screenWidth + offsetX, y*screenHeight + offsetY, screenWidth, 1, Tile.WATER1);
				if (cells[x-1][y].nEdge != WorldScreen.WALL || cells[x-1][y].eEdge == WorldScreen.TOP_LEFT)
					addBridge(x-1, y);
				break;
			}
		}
	}

	private char changeDirection(char dir) {
		if (Math.random() < 0.66)
			return dir;
		
		char dir2 = dirs.charAt((int)(Math.random() * 4));
		if (dir2 == 'N' && dir != 'S'
			 || dir2 == 'S' && dir != 'N'
			 || dir2 == 'W' && dir != 'E'
			 || dir2 == 'E' && dir != 'W')
				dir = dir2;
		
		return dir;
	}
	
	public void addBridge(int x, int y){
		List<Point> candidates = getBridgeCandidates(x, y);
		
		if (candidates.size() == 0)
			return;
		
		Point p = candidates.get((int)(Math.random() *  candidates.size()));
		tiles[p.x][p.y] = Tile.BRIDGE; 
	}

	private List<Point> getBridgeCandidates(int x, int y) {
		List<Point> candidates = new ArrayList<Point>();
		
		for (int rx = 1; rx < screenWidth-1; rx++)
		for (int ry = 1; ry < screenHeight-1; ry++)
			addLocationIfTileIsBridgeCandidate(x * screenWidth + rx, y * screenHeight + ry, candidates);
		
		return candidates;
	}

	private void addLocationIfTileIsBridgeCandidate(int tx, int ty, List<Point> candidates) {
		if (!tiles[tx][ty].isWater())
			return;
		
		if (tiles[tx][ty-1].isWater() 
				&& tiles[tx][ty+1].isWater() 
				&& tiles[tx-1][ty].isGround() 
				&& tiles[tx+1][ty].isGround())
			candidates.add(new Point(tx, ty));
		else if (tiles[tx][ty-1].isGround() 
				&& tiles[tx][ty+1].isGround() 
				&& tiles[tx-1][ty].isWater() 
				&& tiles[tx+1][ty].isWater())
			candidates.add(new Point(tx, ty));
	}

	private void addExtraQuarterScreenWallsToTiles() {
		for (int x = 0; x < cells.length; x++)
		for (int y = 0; y < cells[0].length; y++)
			addExtraQuarterScreenOfWalls(x,y);
	}

	private void addExtraQuarterScreenOfWalls(int sx, int sy) {
		if (!cells[sx][sy].canAddQuarterSection)
			return;
		
		int hw = screenWidth / 2;
		int hh = screenHeight / 2;
		
		int centerBlockers = 0;
		for (Point p : new Point[]{ new Point(0,0), new Point(0,-1), new Point(0,1), new Point(-1,0), new Point(1,0)}){
			if (tiles[sx * screenWidth + hw + p.x][sy * screenHeight + hh + p.y] != cells[sx][sy].defaultGround)
				centerBlockers++;
		}
		
		boolean allowOnlyOnce = centerBlockers > 2;
		
		if (Math.random() < 0.1 && cells[sx][sy].nEdge != WorldScreen.TOP_LEFT && cells[sx][sy].wEdge != WorldScreen.TOP_LEFT){
			clear(sx*screenWidth, sy*screenHeight, hw, hh, cells[sx][sy].defaultWall);
			if (allowOnlyOnce)
				return;
		}
		
		if (Math.random() < 0.1 && cells[sx][sy].nEdge != WorldScreen.BOTTOM_RIGHT && cells[sx][sy].eEdge != WorldScreen.TOP_LEFT){
			clear(sx*screenWidth+hw+1, sy*screenHeight, hw, hh, cells[sx][sy].defaultWall);
			if (allowOnlyOnce)
				return;
		}
		
		if (Math.random() < 0.1 && cells[sx][sy].sEdge != WorldScreen.BOTTOM_RIGHT && cells[sx][sy].eEdge != WorldScreen.BOTTOM_RIGHT){
			clear(sx*screenWidth+hw+1, sy*screenHeight+hh+1, hw, hh, cells[sx][sy].defaultWall);
			if (allowOnlyOnce)
				return;
		}
		
		if (Math.random() < 0.1 && cells[sx][sy].sEdge != WorldScreen.TOP_LEFT && cells[sx][sy].wEdge != WorldScreen.BOTTOM_RIGHT){
			clear(sx*screenWidth, sy*screenHeight+hh+1, hw, hh, cells[sx][sy].defaultWall);
			if (allowOnlyOnce)
				return;
		}
	}
}
