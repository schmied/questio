package org.schmied.questio.importer.db;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.schmied.questio.importer.entity.*;

public class InsertDatabase extends ImporterDatabase {

	public static final int CAPACITY = 200;

	private final PreparedStatement psItem, psClaimGeo, psClaimItem, psClaimQuantity, psClaimString, psClaimTime;

	public InsertDatabase(final Connection connection) throws SQLException {
		super(connection, CAPACITY);
		psItem = connection().prepareStatement("INSERT INTO item (item_id, popularity, label_en, label_de) VALUES (?, ?, ?, ?)");
		psClaimGeo = connection().prepareStatement("INSERT INTO claim_geo (item_id, property_id, lat, lng) VALUES (?, ?, ?, ?)");
		psClaimItem = connection().prepareStatement("INSERT INTO claim_item (item_id, property_id, value) VALUES (?, ?, ?)");
		psClaimQuantity = connection().prepareStatement("INSERT INTO claim_quantity (item_id, property_id, value, unit) VALUES (?, ?, ?, ?)");
		psClaimString = connection().prepareStatement("INSERT INTO claim_string (item_id, property_id, value) VALUES (?, ?, ?)");
		psClaimTime = connection().prepareStatement("INSERT INTO claim_time (item_id, property_id, value, precision) VALUES (?, ?, to_date(?, 'YYYY-MM-DD'), ?)");
	}

	@Override
	public void closeImportResources() {
		try {
			psItem.close();
			psClaimGeo.close();
			psClaimItem.close();
			psClaimQuantity.close();
			psClaimString.close();
			psClaimTime.close();
		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void flushItems(final List<ItemEntity> entities) throws Exception {
		for (final ItemEntity e : entities) {
			psItem.setInt(1, e.itemId);
			psItem.setInt(2, e.popularity);
			psItem.setString(3, e.labelEn);
			psItem.setString(4, e.labelDe);
			psItem.addBatch();
		}
		final int length = psItem.executeBatch().length;
		if (length != entities.size())
			throw new Exception("insert count " + length + " does not match entity count " + entities.size());

	}

	@Override
	protected void flushClaimsGeo(final List<ClaimGeoEntity> entities) throws Exception {
		for (final ClaimGeoEntity e : entities) {
			psClaimGeo.setInt(1, e.itemId);
			psClaimGeo.setInt(2, e.propertyId);
			psClaimGeo.setFloat(3, e.lat);
			psClaimGeo.setFloat(4, e.lng);
			psClaimGeo.addBatch();
		}
		final int length = psClaimGeo.executeBatch().length;
		if (length != entities.size())
			throw new Exception("insert count " + length + " does not match entity count " + entities.size());
	}

	@Override
	protected void flushClaimsItem(final List<ClaimItemEntity> entities) throws Exception {
		for (final ClaimItemEntity e : entities) {
			psClaimItem.setInt(1, e.itemId);
			psClaimItem.setInt(2, e.propertyId);
			psClaimItem.setInt(3, e.value);
			psClaimItem.addBatch();
		}
		final int length = psClaimItem.executeBatch().length;
		if (length != entities.size())
			throw new Exception("insert count " + length + " does not match entity count " + entities.size());
	}

	@Override
	protected void flushClaimsQuantity(final List<ClaimQuantityEntity> entities) throws Exception {
		for (final ClaimQuantityEntity e : entities) {
			psClaimQuantity.setInt(1, e.itemId);
			psClaimQuantity.setInt(2, e.propertyId);
			psClaimQuantity.setFloat(3, e.value);
			psClaimQuantity.setInt(4, e.unit);
			psClaimQuantity.addBatch();
		}
		final int length = psClaimQuantity.executeBatch().length;
		if (length != entities.size())
			throw new Exception("insert count " + length + " does not match entity count " + entities.size());
	}

	@Override
	protected void flushClaimsString(final List<ClaimStringEntity> entities) throws Exception {
		for (final ClaimStringEntity e : entities) {
			psClaimString.setInt(1, e.itemId);
			psClaimString.setInt(2, e.propertyId);
			psClaimString.setString(3, e.value);
			psClaimString.addBatch();
		}
		final int length = psClaimString.executeBatch().length;
		if (length != entities.size())
			throw new Exception("insert count " + length + " does not match entity count " + entities.size());
	}

	@Override
	protected void flushClaimsTime(final List<ClaimTimeEntity> entities) throws Exception {
		for (final ClaimTimeEntity e : entities) {
			psClaimTime.setInt(1, e.itemId);
			psClaimTime.setInt(2, e.propertyId);
			psClaimTime.setString(3, e.value.format(DateTimeFormatter.ISO_LOCAL_DATE));
			psClaimTime.setShort(4, e.precision);
			psClaimTime.addBatch();
		}
		final int length = psClaimTime.executeBatch().length;
		if (length != entities.size())
			throw new Exception("insert count " + length + " does not match entity count " + entities.size());
	}
}
