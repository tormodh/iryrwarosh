package iryrwarosh;

public class DiscoveredLostArtifact extends Message {
	public Creature creature;
	public Item item;
	
	public DiscoveredLostArtifact(World world, Creature creature, Item item) {
		super(world, addArticle("The", creature.name()) + " has discovered a lost treasure: " + item.name());
		this.creature = creature;
		this.item = item;
	}

	@Override
	public boolean involves(Creature player) {
		return false;
	}
}
