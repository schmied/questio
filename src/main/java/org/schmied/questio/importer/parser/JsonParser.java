package org.schmied.questio.importer.parser;

import java.io.BufferedReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.json.*;
import org.schmied.questio.importer.entity.*;
import org.slf4j.*;

public class JsonParser extends Parser {

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonParser.class);

	private static final String label(final JSONObject labels, final String lang) {
		final JSONObject o = labels.optJSONObject(lang);
		if (o == null)
			return null;
		return validString(o.optString("value"));
	}

	private static ClaimGeoEntity claimGeo(final int itemId, final int propertyId, final JSONObject json) {
		if (!"globecoordinate".equals(json.opt("type")))
			return null;
		final JSONObject jValue = json.optJSONObject("value");
		if (jValue == null)
			return null;
		final float lat = (float) jValue.optDouble("latitude", 1111.1);
		if (lat > 1000.0f)
			return null;
		final float lng = (float) jValue.optDouble("longitude", 1111.1);
		if (lng > 1000.0f)
			return null;
		return new ClaimGeoEntity(itemId, propertyId, lat, lng);
	}

	private static ClaimItemEntity claimItem(final int itemId, final int propertyId, final JSONObject json) {
		if (!"wikibase-entityid".equals(json.opt("type")))
			return null;
		final JSONObject jValue = json.optJSONObject("value");
		if (jValue == null)
			return null;
		if (!"item".equals(jValue.opt("entity-type")))
			return null;
		final int value = jValue.optInt("numeric-id", -1);
		if (value < 0)
			return null;
		return new ClaimItemEntity(itemId, propertyId, value);
	}

	private static ClaimQuantityEntity claimQuantity(final int itemId, final int propertyId, final JSONObject json) {
		if (!"quantity".equals(json.opt("type")))
			return null;
		final JSONObject jValue = json.optJSONObject("value");
		if (jValue == null)
			return null;
		final float value = (float) jValue.optDouble("amount", 0.0);
		final String unitString = jValue.optString("unit");
		int unit = -1;
		if (unitString != null && !unitString.trim().isEmpty())
			unit = Integer.parseInt(unitString.replaceAll("\\D", ""));
		return new ClaimQuantityEntity(itemId, propertyId, value, unit);
	}

	private static ClaimStringEntity claimString(final int itemId, final int propertyId, final JSONObject json) {
		if (!"string".equals(json.opt("type")))
			return null;
		final String value = validString(json.optString("value"));
		if (value == null)
			return null;
		return new ClaimStringEntity(itemId, propertyId, value);
	}

	private static ClaimTimeEntity claimTime(final int itemId, final int propertyId, final JSONObject json) {
		if (!"time".equals(json.opt("type")))
			return null;
		final JSONObject jValue = json.optJSONObject("value");
		if (jValue == null)
			return null;
		String time = jValue.optString("time");
		if (time == null)
			return null;
		time = time.replaceAll("^\\+", "");
		final short precision = (short) jValue.optInt("precision", 11);
		LocalDateTime value = null;
		try {
			value = LocalDateTime.parse(time, DateTimeFormatter.ISO_ZONED_DATE_TIME);
		} catch (final Exception e1) {
			try {
				final int year = Integer.parseInt(time.split("-\\d\\d-")[0]);
				value = LocalDateTime.of(year, 1, 1, 0, 0);
			} catch (final Exception e2) {
				LOGGER.info("Q" + itemId + " P" + propertyId + ": Cannot parse year from " + time + " (" + e2.getClass().getSimpleName() + ": " + e2.getMessage() + ")");
				return null;
			}
		}
		final int year = value.getYear();
		if (year < -4700 || year > 10000) {
			LOGGER.info("Q" + itemId + " P" + propertyId + ": year " + year + " out of range for database");
			return null;
		}
		if (year == 0) {
			LOGGER.info("Q" + itemId + " P" + propertyId + ": year " + year + " does not exist, using year 1");
			value = value.plusYears(1);
		}
		return new ClaimTimeEntity(itemId, propertyId, value.toLocalDate(), precision);
	}

	private static ClaimEntity claim(final int itemId, final int propertyId, final Object o) {
		if (o == null)
			return null;
		if (!(o instanceof JSONObject))
			return null;
		final JSONObject json = (JSONObject) o;
		if (!"statement".equals(json.optString("type")))
			return null;
		final JSONObject jMainsnak = json.optJSONObject("mainsnak");
		if (jMainsnak == null)
			return null;
		if (!("P" + propertyId).equals(jMainsnak.opt("property")))
			return null;
		final String datatype = jMainsnak.optString("datatype");
		if (datatype == null)
			return null;
		if (!"value".equals(jMainsnak.opt("snaktype")))
			return null;
		final JSONObject jDatavalue = jMainsnak.optJSONObject("datavalue");
		if (jDatavalue == null)
			return null;

		if (datatype.equals("wikibase-item"))
			return claimItem(itemId, propertyId, jDatavalue);
		if (datatype.equals("globe-coordinate"))
			return claimGeo(itemId, propertyId, jDatavalue);
		if (datatype.equals("quantity"))
			return claimQuantity(itemId, propertyId, jDatavalue);
		if (datatype.equals("string"))
			return claimString(itemId, propertyId, jDatavalue);
		if (datatype.equals("commonsMedia"))
			return claimString(itemId, propertyId, jDatavalue);
		if (datatype.equals("time"))
			return claimTime(itemId, propertyId, jDatavalue);

		return null;
	}

	public static List<ClaimEntity> claims(final JSONObject json, final int itemId, final short popularity) {

		final List<ClaimEntity> claims = new ArrayList<>();
		final List<Integer> classes = new ArrayList<>();
		boolean isNode = false;

		for (final String propertyKey : json.keySet()) {
			final int propertyId = PropertyEntity.propertyId(propertyKey);
			if (propertyId < 0)
				continue;

			final JSONArray jPropertyValues = json.getJSONArray(propertyKey);
			for (final Object jPropertyValue : jPropertyValues) {
				final ClaimEntity claim = claim(itemId, propertyId, jPropertyValue);
				if (claim == null)
					continue;

				claims.add(claim);

				// remember class for skip-classes 
				if (propertyId == 31 || propertyId == 279)
					classes.add(Integer.valueOf(((ClaimItemEntity) claim).value));

				if (Arrays.binarySearch(TRANSITIVE_PROPERTY_IDS, propertyId) >= 0)
					isNode = true;
			}
		}
		if (claims.isEmpty())
			return null;

		if (!isNode && popularity < MIN_POPULARITY_CNT)
			return null;

		// do not allow instances or subclasses of SKIP_CLASS_IDS
		for (final Integer c : classes) {
			if (Arrays.binarySearch(SKIP_PROPERTY_IDS, c.intValue()) >= 0)
				return null;
		}

		return claims;
	}

	private static ItemEntity item(final JSONObject json) throws Exception {
		if (json == null)
			throw new Exception("No JSON.");

		// labels
		final JSONObject jLabels = json.optJSONObject("labels");
		if (jLabels == null)
			throw new Exception("No labels for " + json.toString());
		String labelEn = label(jLabels, "en");
		String labelDe = label(jLabels, "de");
		if (labelDe == null)
			labelDe = labelEn;
		if (labelEn == null)
			labelEn = labelDe;
		if (labelEn == null || labelDe == null)
			throw new Exception("No valid labels for " + json.toString());

		// id
		final String idString = json.optString("id");
		if (idString == null)
			throw new Exception("No id for " + json.toString());
		if (idString.charAt(0) != 'Q')
			throw new Exception("Id does not start with 'Q' for " + json.toString());
		final int itemId = Integer.valueOf(idString.substring(1)).intValue();

		// popularity
		final JSONObject jSitelinks = json.optJSONObject("sitelinks");
		final short popularity = jSitelinks == null ? 0 : (short) jSitelinks.length();

		final List<ClaimEntity> claims = claims(json.optJSONObject("claims"), itemId, popularity);
		if (claims == null)
			throw new Exception("No claims for " + idString);

		return new ItemEntity(itemId, claims, popularity, labelEn, labelDe);
	}

	// ---

	@Override
	public void initialize(final BufferedReader br) throws Exception {
		br.readLine(); // first line cannot be importet
	}

	@Override
	public ItemEntity readItem(final BufferedReader br) throws Exception {
		final String line = br.readLine();
		if (line == null)
			return null;
		try {
			final JSONObject json = new JSONObject(line);
			return item(json);
		} catch (final Exception e) {
			throw e;
		}
	}
}
