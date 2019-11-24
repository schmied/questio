package org.schmied.questio.importer.parser;

import java.io.BufferedReader;
import java.util.Arrays;

import org.schmied.questio.importer.entity.ItemEntity;

public abstract class Parser {

	abstract public void initialize(final BufferedReader br) throws Exception;

	abstract public ItemEntity readItem(final BufferedReader br) throws Exception;

	// ---

	public static final int MIN_POPULARITY_CNT = 10;

	public static final int[] SKIP_PROPERTY_IDS = { //

			7187, // gene
			8054, // protein
			11053, // rna
			21199, // natural number
			30612, // clinical trial
			139677, // operon
			201448, // transfer rna
			277338, // pseudogene
			284416, // small nucleolar rna
			417841, // protein family
			420927, // protein complex
			427087, // non-coding rna
			898273, // protein domain
			4167410, // wikimedia disambiguation page
			4167836, // wikimedia category
			5636047, // cell line
			7644128, // supersecondary structure
			11266439, // wikimedia template
			13366104, // even number
			13366129, // odd number
			13406463, // wikimedia list article
			13442814, // scholarly article
			17633526, // wikinews article
			14204246, // wikimedia project page
			15184295, // wikimedia module
			19842659, // wikimedia user language template
			20010800, // wikimedia user language category
			20747295, // protein-coding gene
			24719571, // alcohol dehydrogenase superfamily, zinc-type
			24726117, // sdr
			24726420, // abc transporter, permease
			24771218, // transcription regulator hth
			24774756, // olfactory receptor
			24781630, // amino acid / polyamine transporter
			24781392, // mfs
			24787504, // bordetella uptake gene

	};
	static {
		Arrays.sort(SKIP_PROPERTY_IDS);
	}

	public static final int[] TRANSITIVE_PROPERTY_IDS = { //

			127, // owned by
			131, // located in the administrative territorial entity
			155, // follows
			156, // followed by
			171, // parent taxon
			279, // subclass of
			355, // subsidiary
			361, // part of
			527, // has part
			749, // parent organization
			1365, // replaces
			1366, // replaced by
			1830, // owner of

	};
	static {
		Arrays.sort(TRANSITIVE_PROPERTY_IDS);
	}
}
