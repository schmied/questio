package org.schmied.questio.importer.entity;

public abstract class ClaimEntity extends Entity {

	public final int itemId, propertyId;

	public ClaimEntity(final int itemId, final int propertyId) {
		this.itemId = itemId;
		this.propertyId = propertyId;
	}
}
