package org.schmied.questio.importer.entity;

import java.time.LocalDate;

public class ClaimTimeEntity extends ClaimEntity {

	public final LocalDate value;
	public final short precision;

	public ClaimTimeEntity(final int itemId, final int propertyId, final LocalDate value, final short precision) {
		super(itemId, propertyId);
		this.value = value;
		this.precision = precision;
	}
}
