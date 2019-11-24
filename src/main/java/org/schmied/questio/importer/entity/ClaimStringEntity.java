package org.schmied.questio.importer.entity;

public class ClaimStringEntity extends ClaimEntity {

	public final String value;

	public ClaimStringEntity(final int itemId, final int propertyId, final String value) {
		super(itemId, propertyId);
		this.value = value;
	}
}
