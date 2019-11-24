package org.schmied.questio.importer.entity;

public class ClaimGeoEntity extends ClaimEntity {

	public final float lat, lng;

	public ClaimGeoEntity(final int itemId, final int propertyId, final float lat, final float lng) {
		super(itemId, propertyId);
		this.lat = lat;
		this.lng = lng;
	}
}
