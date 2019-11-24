package org.schmied.questio.importer.entity;

public class ClaimQuantityEntity extends ClaimEntity {

	public final float value;
	public final int unit;

	public ClaimQuantityEntity(final int itemId, final int propertyId, final float value, final int unit) {
		super(itemId, propertyId);
		this.value = value;
		this.unit = unit;
	}
}
