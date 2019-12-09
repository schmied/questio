package org.schmied.questio.importer.entity;

public class ClaimItemEntity extends ClaimEntity {

	public final int value;

	public ClaimItemEntity(final int itemId, final int propertyId, final int value) {
		super(itemId, propertyId);
		this.value = value;
	}

	// ---------------------------------------------------------------------------------------------------------------- sql

/*
	public static void deleteInvalidReferences(final Connection cn) throws Exception {
		final long ticks = System.currentTimeMillis();

		final List<Integer> itemIdList = new ArrayList<>(1024 * 1024);
		try (final Statement st = cn.createStatement(); final ResultSet rs = st.executeQuery("SELECT item_id FROM item")) {
			while (rs.next())
				itemIdList.add(Integer.valueOf(rs.getInt(1)));
		} catch (final SQLException e) {
			throw e;
		}

		final List<Integer[]> invalidClaims = new ArrayList<>();
		try (final Statement st = cn.createStatement(); final ResultSet rs = st.executeQuery("SELECT item_id, value FROM claim_item")) {
			final int[] itemIds = Importer.intArray(itemIdList);
			while (rs.next()) {
				final int value = rs.getInt(2);
				if (Arrays.binarySearch(itemIds, value) >= 0)
					continue;
				invalidClaims.add(new Integer[] { Integer.valueOf(rs.getInt(1)), Integer.valueOf(value) });
			}
		} catch (final SQLException e) {
			throw e;
		}

		try (final PreparedStatement psDeleteClaim = cn.prepareStatement("DELETE FROM claim_item WHERE item_id = ? AND value = ?")) {
			int idx = 0;
			for (final Integer[] invalidClaim : invalidClaims) {
				psDeleteClaim.setInt(1, invalidClaim[0].intValue());
				psDeleteClaim.setInt(2, invalidClaim[1].intValue());
				psDeleteClaim.addBatch();
				if (idx % InsertDatabase.CAPACITY == 0 && idx > 0) {
					psDeleteClaim.executeBatch();
					//System.out.println(idx + " / " + invalidClaims.size() + " " + invalidClaim[0].intValue() + " " + invalidClaim[1].intValue());
				}
				idx++;
			}
			psDeleteClaim.executeBatch();
		} catch (final SQLException e) {
			throw e;
		}

		System.out.println("delete invalid references: " + invalidClaims.size() + " [" + (System.currentTimeMillis() - ticks) + "ms]");
	}

	public static boolean deleteRedundant(final Connection cn) {
		final long ticks = System.currentTimeMillis();

		final List<int[]> redundantClaims = new ArrayList<>();
		try {
			//final String sql = "SELECT item_id, property_id, value, count(*) FROM claim_item WHERE property_id IN (31, " + Database.commaSeparate(Graphs.PROPERTIES)
			//		+ ") GROUP BY item_id, property_id, value HAVING count(*) > 1 ORDER BY property_id ASC, count DESC, item_id ASC";
			final String sql = "SELECT item_id, property_id, value, count(*) FROM claim_item GROUP BY item_id, property_id, value HAVING count(*) > 1 ORDER BY property_id ASC, count DESC, item_id ASC";
			//System.out.println(sql);
			final PreparedStatement pst = cn.prepareStatement(sql);
			final ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				final int[] i = new int[4];
				i[0] = rs.getInt(1);
				i[1] = rs.getInt(2);
				i[2] = rs.getInt(3);
				i[3] = rs.getInt(4);
				redundantClaims.add(i);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
		try {
			final PreparedStatement pst = cn.prepareStatement("DELETE FROM claim_item WHERE item_id = ? AND property_id = ? AND value = ?");
			for (final int[] i : redundantClaims) {
				pst.setInt(1, i[0]);
				pst.setInt(2, i[1]);
				pst.setInt(3, i[2]);
				pst.addBatch();
			}
			pst.executeBatch();
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
		try {
			final PreparedStatement pst = cn.prepareStatement("INSERT INTO claim_item (item_id, property_id, value) VALUES (?, ?, ?)");
			for (final int[] i : redundantClaims) {
				pst.setInt(1, i[0]);
				pst.setInt(2, i[1]);
				pst.setInt(3, i[2]);
				pst.addBatch();
			}
			pst.executeBatch();
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("removed redunant claims: " + redundantClaims.size() + " [" + (System.currentTimeMillis() - ticks) + "ms]");
		return true;
	}
*/
}
