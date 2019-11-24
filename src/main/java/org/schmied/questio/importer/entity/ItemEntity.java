package org.schmied.questio.importer.entity;

import java.util.List;

public class ItemEntity extends Entity {

	public final int itemId;
	public final short popularity;
	public final String labelEn, labelDe;
	public final List<ClaimEntity> claims;

	public ItemEntity(final int itemId, final List<ClaimEntity> claims, final short popularity, final String labelEn, final String labelDe) {
		this.itemId = itemId;
		this.claims = claims;
		this.popularity = popularity;
		this.labelEn = labelEn;
		this.labelDe = labelDe;
	}

	@Override
	public String toString() {
		return "i" + itemId + ":" + labelEn + ":" + labelDe;
	}

	// ---------------------------------------------------------------------------------------------------------------- sql

/*
	public static boolean reduceItems(final Connection cn) {
		final Database db = new Database(cn);
		int[] unpopularItemIds = null;
		for (;;) {
			unpopularItemIds = db.ids("item", "item_id", "popularity < " + ClaimEntity.MIN_POPULARITY_CNT);
			if (unpopularItemIds == null)
				return false;
			if (unpopularItemIds.length == 0) {
				System.out.println("reduce items: all items reduced");
				return true;
			}

			final Graphs graphs = new Graphs();

			final int[] unpopularLeafIds131 = graphs.unpopularLeafIds(db, 131); // loc adm ter ent
			if (unpopularLeafIds131 != null && unpopularLeafIds131.length > 1000) {
				System.out.println(
						"reduce items: take " + unpopularLeafIds131.length + " locAdmTerEnt leaf nodes instead of " + unpopularItemIds.length + " random items.");
				unpopularItemIds = unpopularLeafIds131;
			} else {
				final int[] unpopularLeafIds171 = graphs.unpopularLeafIds(db, 171); // parent taxon
				if (unpopularLeafIds171 != null && unpopularLeafIds171.length > 1000) {
					System.out.println(
							"reduce items: take " + unpopularLeafIds171.length + " parentTaxon leaf nodes instead of " + unpopularItemIds.length + " random items.");
					unpopularItemIds = unpopularLeafIds171;
				}
			}

			final SortedSet<Integer> validatedItemIds = graphs.reduceValidateDelete(cn, unpopularItemIds);
			System.out.println("reduce items: " + validatedItemIds.size() + " / " + unpopularItemIds.length + " valid");
			graphs.reduceReconnect(cn, validatedItemIds);
			if (!delete(cn, Questionator.intArray(validatedItemIds)))
				return false;
		}
	}
*/
}
