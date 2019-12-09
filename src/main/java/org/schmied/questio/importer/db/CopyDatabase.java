package org.schmied.questio.importer.db;

import java.io.StringReader;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.schmied.questio.importer.entity.*;
import org.slf4j.*;

public class CopyDatabase extends ImportDatabase {

	private static final Logger LOGGER = LoggerFactory.getLogger(CopyDatabase.class);

	private static final int CAPACITY = 256 * 1024;

	private static final Pattern PATTERN_JUNK = Pattern
			.compile("[^a-zA-Z0-9ßäëïöüáćéíóśúýźàèìòùâêîôûčğřšžãñõåçşąęłøðæœıÄËÏÖÜÁĆÉÍÓŚÚÝŹÀÈÌÒÙÂÊÎÔÛČĞŘŠŽÃÑÕÅÇŞĄĘŁØÐÆŒ" + ".,:;+*/'²³!?§%& -]");
	private static final Pattern PATTERN_WHITESPACES = Pattern.compile("\\s+");
	private static final Pattern PATTERN_DATE = Pattern.compile("^\\d+-\\d\\d?-\\d\\d?$");

	private final CopyManager copyManager;

	public CopyDatabase(final Connection connection) throws SQLException {
		super(connection, CAPACITY);
		copyManager = ((PGConnection) connection()).getCopyAPI();
	}

	@Override
	public void closeImportResources() {
		// nothing to clean up here
	}

	@Override
	protected void flushItems(final List<ItemEntity> entities) throws Exception {
		final long ticks = System.currentTimeMillis();
		final StringBuilder sb = new StringBuilder(4 * 16 * CAPACITY);
		for (final ItemEntity e : entities) {
			sb.append(e.itemId);
			sb.append('\t');
			sb.append(e.popularity);
			sb.append('\t');
			sb.append(PATTERN_WHITESPACES.matcher(PATTERN_JUNK.matcher(e.labelEn).replaceAll(" ")).replaceAll(" ").trim());
			sb.append('\t');
			sb.append(PATTERN_WHITESPACES.matcher(PATTERN_JUNK.matcher(e.labelDe).replaceAll(" ")).replaceAll(" ").trim());
			sb.append('\n');
		}
		try (final StringReader sr = new StringReader(sb.toString())) {
			copyManager.copyIn("COPY item FROM STDIN", sr);
		} catch (final Exception e) {
			throw e;
		}
		LOGGER.info("flush items [" + (System.currentTimeMillis() - ticks) + "ms]");
	}

	@Override
	protected void flushClaimsGeo(final List<ClaimGeoEntity> entities) throws Exception {
		final long ticks = System.currentTimeMillis();
		final StringBuilder sb = new StringBuilder(4 * 16 * CAPACITY);
		for (final ClaimGeoEntity e : entities) {
			sb.append(e.itemId);
			sb.append('\t');
			sb.append(e.propertyId);
			sb.append('\t');
			sb.append(e.lat);
			sb.append('\t');
			sb.append(e.lng);
			sb.append('\n');
		}
		try (final StringReader sr = new StringReader(sb.toString())) {
			copyManager.copyIn("COPY claim_geo FROM STDIN", sr);
		} catch (final Exception e) {
			throw e;
		}
		LOGGER.info("flush claims geo [" + (System.currentTimeMillis() - ticks) + "ms]");
	}

	@Override
	protected void flushClaimsItem(final List<ClaimItemEntity> entities) throws Exception {
		final long ticks = System.currentTimeMillis();
		final StringBuilder sb = new StringBuilder(4 * 16 * CAPACITY);
		for (final ClaimItemEntity e : entities) {
			sb.append(e.itemId);
			sb.append('\t');
			sb.append(e.propertyId);
			sb.append('\t');
			sb.append(e.value);
			sb.append('\n');
		}
		try (final StringReader sr = new StringReader(sb.toString())) {
			copyManager.copyIn("COPY claim_item FROM STDIN", sr);
		} catch (final Exception e) {
			throw e;
		}
		LOGGER.info("flush claims item [" + (System.currentTimeMillis() - ticks) + "ms]");
	}

	@Override
	protected void flushClaimsQuantity(final List<ClaimQuantityEntity> entities) throws Exception {
		final long ticks = System.currentTimeMillis();
		final StringBuilder sb = new StringBuilder(4 * 16 * CAPACITY);
		for (final ClaimQuantityEntity e : entities) {
			sb.append(e.itemId);
			sb.append('\t');
			sb.append(e.propertyId);
			sb.append('\t');
			sb.append(e.value);
			sb.append('\t');
			sb.append(e.unit);
			sb.append('\n');
		}
		try (final StringReader sr = new StringReader(sb.toString())) {
			copyManager.copyIn("COPY claim_quantity FROM STDIN", sr);
		} catch (final Exception e) {
			throw e;
		}
		LOGGER.info("flush claims quantity [" + (System.currentTimeMillis() - ticks) + "ms]");
	}

	@Override
	protected void flushClaimsString(final List<ClaimStringEntity> entities) throws Exception {
		final long ticks = System.currentTimeMillis();
		final StringBuilder sb = new StringBuilder(4 * 16 * CAPACITY);
		for (final ClaimStringEntity e : entities) {
			sb.append(e.itemId);
			sb.append('\t');
			sb.append(e.propertyId);
			sb.append('\t');
			String value = e.value;
			// 18 image, 41 flag image, 94 coat of arms image, 154 logo image
			if (e.propertyId != 18 && e.propertyId != 41 && e.propertyId != 94 && e.propertyId != 154)
				value = PATTERN_WHITESPACES.matcher(PATTERN_JUNK.matcher(value).replaceAll(" ")).replaceAll(" ").trim();
			sb.append(value);
			sb.append('\n');
		}
		try (final StringReader sr = new StringReader(sb.toString())) {
			copyManager.copyIn("COPY claim_string FROM STDIN", sr);
		} catch (final Exception e) {
			throw e;
		}
		LOGGER.info("flush claims string [" + (System.currentTimeMillis() - ticks) + "ms]");
	}

	@Override
	protected void flushClaimsTime(final List<ClaimTimeEntity> entities) throws Exception {
		final long ticks = System.currentTimeMillis();
		final StringBuilder sb = new StringBuilder(4 * 16 * CAPACITY);
		for (final ClaimTimeEntity e : entities) {

			String value = e.value.format(DateTimeFormatter.ISO_LOCAL_DATE);
			final boolean isBc = value.startsWith("-");
			if (isBc)
				value = value.substring(1);
			if (!PATTERN_DATE.matcher(value).matches()) {
				LOGGER.info("value '" + value + "' is not a valid date.");
				continue;
			}
			if (isBc)
				value = value + " BC";

			sb.append(e.itemId);
			sb.append('\t');
			sb.append(e.propertyId);
			sb.append('\t');
			sb.append(value);
			sb.append('\t');
			sb.append(e.precision);
			sb.append('\n');
		}
		try (final StringReader sr = new StringReader(sb.toString())) {
			copyManager.copyIn("COPY claim_time FROM STDIN", sr);
		} catch (final Exception e) {
			throw e;
		}
		LOGGER.info("flush claims time [" + (System.currentTimeMillis() - ticks) + "ms]");
	}
}
