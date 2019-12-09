package org.schmied.questio.importer.db;

import java.sql.*;
import java.util.*;
import java.util.function.BiFunction;

import org.schmied.questio.importer.Importer;
import org.schmied.questio.importer.entity.*;
import org.slf4j.*;

public abstract class ImportDatabase extends Database {

	protected abstract void flushItems(final List<ItemEntity> entities) throws Exception;

	protected abstract void flushClaimsGeo(final List<ClaimGeoEntity> entities) throws Exception;

	protected abstract void flushClaimsItem(final List<ClaimItemEntity> entities) throws Exception;

	protected abstract void flushClaimsQuantity(final List<ClaimQuantityEntity> entities) throws Exception;

	protected abstract void flushClaimsString(final List<ClaimStringEntity> entities) throws Exception;

	protected abstract void flushClaimsTime(final List<ClaimTimeEntity> entities) throws Exception;

	protected abstract void closeImportResources();

	// ---

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportDatabase.class);

	private static final int CLAIM_CAPACITY_BUFFER = 10;

	private final int capacity;

	private final ArrayList<ItemEntity> items;
	private final ArrayList<ClaimGeoEntity> claimsGeo;
	private final ArrayList<ClaimItemEntity> claimsItem;
	private final ArrayList<ClaimQuantityEntity> claimsQuantity;
	private final ArrayList<ClaimStringEntity> claimsString;
	private final ArrayList<ClaimTimeEntity> claimsTime;

	public ImportDatabase(final Connection connection, final int capacity) {
		super(connection);
		this.capacity = capacity;
		items = new ArrayList<>(capacity);
		claimsGeo = new ArrayList<>(capacity + CLAIM_CAPACITY_BUFFER);
		claimsItem = new ArrayList<>(capacity + CLAIM_CAPACITY_BUFFER);
		claimsQuantity = new ArrayList<>(capacity + CLAIM_CAPACITY_BUFFER);
		claimsString = new ArrayList<>(capacity + CLAIM_CAPACITY_BUFFER);
		claimsTime = new ArrayList<>(capacity + CLAIM_CAPACITY_BUFFER);
	}

	private void flush(final int maxCapacity) throws Exception {
		final long ticks = System.currentTimeMillis();
		int cntFlush = 0;
		if (items.size() >= maxCapacity) {
			flushItems(items);
			cntFlush += items.size();
			items.clear();
			if (maxCapacity > 0)
				items.ensureCapacity(maxCapacity);
		}
		if (claimsGeo.size() >= maxCapacity) {
			flushClaimsGeo(claimsGeo);
			cntFlush += claimsGeo.size();
			claimsGeo.clear();
			if (maxCapacity > 0)
				claimsGeo.ensureCapacity(maxCapacity + CLAIM_CAPACITY_BUFFER);
		}
		if (claimsItem.size() >= maxCapacity) {
			flushClaimsItem(claimsItem);
			cntFlush += claimsItem.size();
			claimsItem.clear();
			if (maxCapacity > 0)
				claimsItem.ensureCapacity(maxCapacity + CLAIM_CAPACITY_BUFFER);
		}
		if (claimsQuantity.size() >= maxCapacity) {
			flushClaimsQuantity(claimsQuantity);
			cntFlush += claimsQuantity.size();
			claimsQuantity.clear();
			if (maxCapacity > 0)
				claimsQuantity.ensureCapacity(maxCapacity + CLAIM_CAPACITY_BUFFER);
		}
		if (claimsString.size() >= maxCapacity) {
			flushClaimsString(claimsString);
			cntFlush += claimsQuantity.size();
			claimsString.clear();
			if (maxCapacity > 0)
				claimsString.ensureCapacity(maxCapacity + CLAIM_CAPACITY_BUFFER);
		}
		if (claimsTime.size() >= maxCapacity) {
			flushClaimsTime(claimsTime);
			cntFlush += claimsTime.size();
			claimsTime.clear();
			if (maxCapacity > 0)
				claimsTime.ensureCapacity(maxCapacity + CLAIM_CAPACITY_BUFFER);
		}
		if (cntFlush > 0) {
			final long elapsed = System.currentTimeMillis() - ticks;
			LOGGER.info("flush " + cntFlush + " entries [" + elapsed + "ms " + Math.round(1000.0 * cntFlush / elapsed) + " rows/s]");
		}
	}

	public boolean addItem(final ItemEntity item) throws Exception {

		for (final ClaimEntity c : item.claims) {
			if (c instanceof ClaimItemEntity)
				claimsItem.add((ClaimItemEntity) c);
			else if (c instanceof ClaimGeoEntity)
				claimsGeo.add((ClaimGeoEntity) c);
			else if (c instanceof ClaimQuantityEntity)
				claimsQuantity.add((ClaimQuantityEntity) c);
			else if (c instanceof ClaimStringEntity)
				claimsString.add((ClaimStringEntity) c);
			else if (c instanceof ClaimTimeEntity)
				claimsTime.add((ClaimTimeEntity) c);
			else {
				LOGGER.info("Unknows entity " + c.getClass().getName());
				return false;
			}
		}
		item.claims.clear();

		items.add(item);

		flush(capacity);

		return true;
	}

	public void closeImport() {
		try {
			flush(0);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		closeImportResources();
	}

	// ---

	public void recreateTables() throws Exception {
		final long ticks = System.currentTimeMillis();
		try (final Statement st = connection().createStatement()) {
			// drop
			st.execute("DROP TABLE IF EXISTS claim_geo");
			st.execute("DROP TABLE IF EXISTS claim_item");
			st.execute("DROP TABLE IF EXISTS claim_quantity");
			st.execute("DROP TABLE IF EXISTS claim_string");
			st.execute("DROP TABLE IF EXISTS claim_time");
			st.execute("DROP TABLE IF EXISTS item");
			st.execute("DROP TABLE IF EXISTS property");
			// create
			st.execute("CREATE TABLE IF NOT EXISTS property       (property_id INT4 PRIMARY KEY, label_en character varying(" + Importer.MAX_STRING_LENGTH
					+ ") NOT NULL, label_de character varying(" + Importer.MAX_STRING_LENGTH + ") NOT NULL)");
			st.execute("CREATE TABLE IF NOT EXISTS item           (item_id INT4, popularity SMALLINT, label_en CHARACTER VARYING(" + Importer.MAX_STRING_LENGTH
					+ "), label_de CHARACTER VARYING(" + Importer.MAX_STRING_LENGTH + "))");
			st.execute("CREATE TABLE IF NOT EXISTS claim_geo      (item_id INT4, property_id INT4, lat REAL, lng REAL)");
			st.execute("CREATE TABLE IF NOT EXISTS claim_item     (item_id INT4, property_id INT4, value INT4)");
			st.execute("CREATE TABLE IF NOT EXISTS claim_quantity (item_id INT4, property_id INT4, value REAL, unit INT4)");
			st.execute("CREATE TABLE IF NOT EXISTS claim_string   (item_id INT4, property_id INT4, value CHARACTER VARYING(" + Importer.MAX_STRING_LENGTH + "))");
			st.execute("CREATE TABLE IF NOT EXISTS claim_time     (item_id INT4, property_id INT4, value DATE, precision SMALLINT)");
		} catch (final Exception e) {
			throw e;
		}
		LOGGER.info("recreate tables [" + (System.currentTimeMillis() - ticks) + "ms]");
	}

	public void insertProperties() throws Exception {
		final long ticks = System.currentTimeMillis();
		for (final PropertyEntity property : PropertyEntity.VALID_PROPERTIES) {
			try (final PreparedStatement ps = connection().prepareStatement("INSERT INTO property (property_id, label_en, label_de) VALUES (?, ?, ?)")) {
				ps.setInt(1, property.propertyId);
				ps.setString(2, property.labelEn);
				ps.setString(3, property.labelDe);
				ps.execute();
			} catch (final SQLException e) {
				throw e;
			}
		}
		LOGGER.info("insert properties [" + (System.currentTimeMillis() - ticks) + "ms]");
	}

	public void createIndexes() throws Exception {
		final long ticks = System.currentTimeMillis();
		try (final Statement st = connection().createStatement()) {
			st.execute("ALTER TABLE item ADD CONSTRAINT pk_item_item_id PRIMARY KEY (item_id)");
			st.execute("CREATE INDEX idx_item_label_en              ON item           USING btree (label_en)");
			st.execute("CREATE INDEX idx_item_label_de              ON item           USING btree (label_de)");
			st.execute("CREATE INDEX idx_claim_geo_item_id          ON claim_geo      USING btree (item_id)");
			st.execute("CREATE INDEX idx_claim_geo_property_id      ON claim_geo      USING btree (property_id)");
			st.execute("CREATE INDEX idx_claim_item_item_id         ON claim_item     USING btree (item_id)");
			st.execute("CREATE INDEX idx_claim_item_property_id     ON claim_item     USING btree (property_id)");
			st.execute("CREATE INDEX idx_claim_item_value           ON claim_item     USING btree (value)");
			st.execute("CREATE INDEX idx_claim_quantity_item_id     ON claim_quantity USING btree (item_id)");
			st.execute("CREATE INDEX idx_claim_quantity_property_id ON claim_quantity USING btree (property_id)");
			st.execute("CREATE INDEX idx_claim_string_item_id       ON claim_string   USING btree (item_id)");
			st.execute("CREATE INDEX idx_claim_string_property_id   ON claim_string   USING btree (property_id)");
			st.execute("CREATE INDEX idx_claim_time_item_id         ON claim_time     USING btree (item_id)");
			st.execute("CREATE INDEX idx_claim_time_property_id     ON claim_time     USING btree (property_id)");
			st.execute("ANALYZE");
		} catch (final Exception e) {
			throw e;
		}
		LOGGER.info("create indexes and analzye [" + (System.currentTimeMillis() - ticks) + "ms]");
	}

	public void addConstraints() throws Exception {
		final long ticks = System.currentTimeMillis();
		try (final Statement st = connection().createStatement()) {
			st.execute("ALTER TABLE claim_geo      ADD CONSTRAINT fk_claim_geo_item_id           FOREIGN KEY (item_id)     REFERENCES item     (item_id)");
			st.execute("ALTER TABLE claim_geo      ADD CONSTRAINT fk_claim_geo_property_id       FOREIGN KEY (property_id) REFERENCES property (property_id)");
			st.execute("ALTER TABLE claim_item     ADD CONSTRAINT fk_claim_item_item_id          FOREIGN KEY (item_id)     REFERENCES item     (item_id)");
			st.execute("ALTER TABLE claim_item     ADD CONSTRAINT fk_claim_item_value            FOREIGN KEY (value)       REFERENCES item     (item_id)");
			st.execute("ALTER TABLE claim_item     ADD CONSTRAINT fk_claim_item_property_id      FOREIGN KEY (property_id) REFERENCES property (property_id)");
			st.execute("ALTER TABLE claim_quantity ADD CONSTRAINT fk_claim_quantity_item_id      FOREIGN KEY (item_id)     REFERENCES item     (item_id)");
			st.execute("ALTER TABLE claim_quantity ADD CONSTRAINT fk_claim_quantity_property_id  FOREIGN KEY (property_id) REFERENCES property (property_id)");
			st.execute("ALTER TABLE claim_string   ADD CONSTRAINT fk_claim_string_item_id        FOREIGN KEY (item_id)     REFERENCES item     (item_id)");
			st.execute("ALTER TABLE claim_string   ADD CONSTRAINT fk_claim_string_property_id    FOREIGN KEY (property_id) REFERENCES property (property_id)");
			st.execute("ALTER TABLE claim_time     ADD CONSTRAINT fk_claim_time_item_id          FOREIGN KEY (item_id)     REFERENCES item     (item_id)");
			st.execute("ALTER TABLE claim_time     ADD CONSTRAINT fk_claim_geo_property_id       FOREIGN KEY (property_id) REFERENCES property (property_id)");
		} catch (final Exception e) {
			throw e;
		}
		LOGGER.info("add constraints [" + (System.currentTimeMillis() - ticks) + "ms]");
	}

	// ---

	private static final int IN_CLAUSE_MAX_COUNT = 400;

	private static String inClauseCommaSeparate(final int ids[]) {
		final StringBuilder sb = new StringBuilder();
		sb.append(ids[0]);
		for (int i = 1; i < ids.length; i++)
			sb.append(", " + ids[i]);
		return sb.toString();
	}

	private int delete(final String table, final String column, final int ids[]) throws Exception {
		final String sql = "DELETE FROM " + table + " WHERE " + column + " IN (" + inClauseCommaSeparate(ids) + ")";
		try (final PreparedStatement ps = connection().prepareStatement(sql)) {
			return ps.executeUpdate();
		} catch (final Exception e) {
			throw e;
		}
	}

	public static Boolean deleteItems(final ImportDatabase db, final int[] itemIds) {
		try {
			final long ticks = System.currentTimeMillis();
			final int cntClaimGeo = db.delete("claim_geo", "item_id", itemIds);
			final int cntClaimItem = db.delete("claim_item", "item_id", itemIds);
			//final int cntClaimItemValue = db.delete("claim_item", "value", itemIds);
			final int cntClaimQuantity = db.delete("claim_quantity", "item_id", itemIds);
			final int cntClaimString = db.delete("claim_string", "item_id", itemIds);
			final int cntClaimTime = db.delete("claim_time", "item_id", itemIds);
			final int cntItem = db.delete("item", "item_id", itemIds);
			LOGGER.info("delete items: " + cntClaimGeo + " claim_geo, " + cntClaimItem + " claim_item (id), " + cntClaimQuantity + " claim_quantity, " + cntClaimString
					+ " claim_string, " + cntClaimTime + " claim_time, " + cntItem + " item / " + itemIds.length + " [" + (System.currentTimeMillis() - ticks) + "ms]");
		} catch (final Exception e) {
			LOGGER.warn(e.getMessage());
		}
		return Boolean.TRUE;
	}

	public static Boolean deleteClaimItems(final ImportDatabase db, final int[] claimItemIds) {
		try {
			final long ticks = System.currentTimeMillis();
			final int cntClaimItem = db.delete("claim_item", "item_id", claimItemIds);
			LOGGER.info("delete claim items: " + cntClaimItem + " [" + (System.currentTimeMillis() - ticks) + "ms]");
		} catch (final Exception e) {
			LOGGER.warn(e.getMessage());
		}
		return Boolean.TRUE;
	}

	public void blaaa(final String sql, final int maxSize, final BiFunction<ImportDatabase, int[], Boolean> callback) throws Exception {
		boolean reset = false;
		try (final PreparedStatement pst = connection().prepareStatement(sql); final ResultSet rs = pst.executeQuery()) {
			final int[] arr = new int[maxSize];
			int cnt = 0;
			while (!reset && rs.next()) {
				arr[cnt] = rs.getInt(1);
				cnt++;
				if (cnt >= maxSize) {
					final Boolean b = callback.apply(this, Arrays.copyOf(arr, cnt));
					reset = b != null && b.booleanValue();
					cnt = 0;
				}
			}
			if (!reset && cnt > 0) {
				callback.apply(this, Arrays.copyOf(arr, cnt));
			}
		} catch (final Exception e) {
			throw e;
		} finally {
		}
		if (reset)
			blaaa(sql, maxSize, callback);
	}
}
