package org.schmied.questio.importer.db;

import java.sql.*;
import java.util.*;

public class Database {

	private static final int IN_CLAUSE_MAX_COUNT = 400;

	protected static final int MAX_STRING_LENGTH = 100;

	private final Connection connection;

	public Database(final Connection connection) {
		this.connection = connection;
	}

	public Connection connection() {
		return connection;
	}

	private static final int[] intArray(final Collection<? extends Number> c) {
		if (c == null)
			return null;
		final int[] a = new int[c.size()];
		int idx = 0;
		for (final Number n : c) {
			a[idx] = n.intValue();
			idx++;
		}
		Arrays.sort(a);
		return a;
	}

	private static int[] interval(final int ids[], final int begin) {
		int end = begin + IN_CLAUSE_MAX_COUNT;
		if (end > ids.length)
			end = ids.length;
		return Arrays.copyOfRange(ids, begin, end);
	}

	public static String commaSeparate(final int ids[]) {
		final StringBuilder sb = new StringBuilder();
		sb.append(ids[0]);
		for (int i = 1; i < ids.length; i++)
			sb.append(", " + ids[i]);
		return sb.toString();
	}

	public int[] ids(final String table, final String column, final String where) throws Exception {
		try (final Statement st = connection.createStatement();
				final ResultSet rs = st.executeQuery("SELECT " + column + " FROM " + table + (where == null ? "" : " WHERE " + where))) {
			final List<Number> ids = new ArrayList<>();
			while (rs.next())
				ids.add((Number) rs.getObject(1));
			return intArray(ids);
		} catch (final Exception e) {
			throw e;
		}
	}

	public int[] referenced(final int ids[], final String where) throws Exception {
		return whereIn("claim_item", "value", where, "value", ids);
	}

	public int[] unreferenced(final int ids[], final String where) throws Exception {
		final int[] referenced = referenced(ids, where);
		final TreeSet<Integer> unreferenced = new TreeSet<>();
		for (final int id : ids) {
			if (Arrays.binarySearch(referenced, id) < 0)
				unreferenced.add(Integer.valueOf(id));
		}
		return intArray(unreferenced);
	}

	public int[] whereIn(final String table, final String columnSelect, final String where, final String columnIn, final int ids[]) throws Exception {
		final List<Number> filteredIds = new ArrayList<>();
		for (int intervalBegin = 0; intervalBegin < ids.length; intervalBegin += IN_CLAUSE_MAX_COUNT) {
			final int[] interval = interval(ids, intervalBegin);
			final String w = where == null ? "" : where + " AND ";
			final String sql = "SELECT " + columnSelect + " FROM " + table + " WHERE " + w + columnIn + " IN (" + commaSeparate(interval) + ")";
			//System.out.println("---> " + sql);
			try (final Statement st = connection.createStatement(); final ResultSet rs = st.executeQuery(sql)) {
				while (rs.next())
					filteredIds.add((Number) rs.getObject(1));
			} catch (final Exception e) {
				throw e;
			}
		}
		return intArray(filteredIds);
	}

	public int delete(final String table, final String column, final int ids[]) throws Exception {
		int deleteCount = 0;
		for (int intervalBegin = 0; intervalBegin < ids.length; intervalBegin += IN_CLAUSE_MAX_COUNT) {
			final int[] interval = interval(ids, intervalBegin);
			final String sql = "DELETE FROM " + table + " WHERE " + column + " IN (" + commaSeparate(interval) + ")";
			//System.out.println("---> " + sql);
			try (final PreparedStatement ps = connection.prepareStatement(sql)) {
				deleteCount += ps.executeUpdate();
			} catch (final Exception e) {
				throw e;
			}
		}
		return deleteCount;
	}
}
