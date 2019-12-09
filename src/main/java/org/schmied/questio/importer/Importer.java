package org.schmied.questio.importer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalTime;
import java.util.*;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.schmied.questio.importer.db.ImportDatabase;
import org.schmied.questio.importer.entity.ItemEntity;
import org.schmied.questio.importer.parser.Parser;
import org.slf4j.*;

public class Importer implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Importer.class);

	final ImportDatabase db;
	final Parser parser;
	final Path file;

	private Importer(final ImportDatabase db, final Parser parser, final Path file) {
		this.db = db;
		this.parser = parser;
		this.file = file;
	}

	@Override
	public void run() {
		if (isImportRunning)
			return;
		isImportRunning = true;
		try {

			parseFile();

			integrate();

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			isImportRunning = false;
		}
	}

	public static void fullImport(final ImportDatabase db, final Parser parser, final Path file) throws Exception {
		if (isImportRunning)
			throw new Exception("Import already running.");
		final Importer importer = new Importer(db, parser, file);
		final Thread thread = new Thread(importer);
		thread.start();
	}

	// --- parse

	private static String parseStreamLog(final long ticks, final int countImported, final int countRead) {
		final long secondsElapsed = (System.currentTimeMillis() - ticks) / 1000;
		long secondsEta = Math.round((double) secondsElapsed * MAX_ITEMS / countRead - secondsElapsed);
		if (secondsEta < 0)
			secondsEta = 0;
		long itemsPerS = Math.round((double) countRead / secondsElapsed);
		if (itemsPerS > 9999999)
			itemsPerS = 9999999;
		final StringBuilder sb = new StringBuilder(256);
		sb.append(countImported + " / " + countRead + " (" + (Math.round(100.0 * countImported / countRead)) + "%) in "
				+ LocalTime.ofSecondOfDay(secondsElapsed).toString());
		if (secondsEta > 0)
			sb.append(", ETA " + (secondsEta > 86000 ? ">1d (" + Math.round(secondsEta / 60.0f / 60.0f) + "h)" : LocalTime.ofSecondOfDay(secondsEta).toString()));
		sb.append(" [" + itemsPerS + " items/s]");
		return sb.toString();
	}

	private void parseStream(final InputStream is) throws Exception {
		try (final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.US_ASCII); final BufferedReader br = new BufferedReader(isr, BUFFER_SIZE_READER)) {

			final long ticks = System.currentTimeMillis();
			int countImported = 0;

			parser.initialize(br);

			int countRead = 0;
			for (;;) {
				if (countRead >= MAX_ITEMS)
					break;
				try {
					final ItemEntity item = parser.readItem(br);
					if (item == null)
						break;
					countRead++;
					if (db.addItem(item))
						countImported++;
					else
						LOGGER.info("ERROR item " + item.toString());
				} catch (final Exception e) {
					countRead++;
					LOGGER.debug(e.getMessage());
				}
				if (countRead % 1000 == 0 && countRead > 0)
					LOGGER.info(parseStreamLog(ticks, countImported, countRead));
			}
			LOGGER.info(parseStreamLog(ticks, countImported, countRead));

		} catch (final Exception e) {
			throw e;
		} finally {
			db.closeImport();
		}
	}

	private void parseFilePlain() throws Exception {
		try (final InputStream is = Files.newInputStream(file); final BufferedInputStream bis = new BufferedInputStream(is, BUFFER_SIZE_STREAM)) {
			parseStream(bis);
		} catch (final Exception e) {
			throw e;
		}
	}

	private void parseFileBzip() throws Exception {
		try (final InputStream is = Files.newInputStream(file);
				final BufferedInputStream bis = new BufferedInputStream(is, BUFFER_SIZE_STREAM);
				final BZip2CompressorInputStream bzis = new BZip2CompressorInputStream(bis)) {
			parseStream(bzis);
		} catch (final Exception e) {
			throw e;
		}
	}

	private void parseFile() throws Exception {

		if (!Files.isRegularFile(file))
			throw new Exception("file does not exist: " + file.toAbsolutePath().toString());

		db.recreateTables();

		final String lowerFilename = file.getFileName().toString().toLowerCase();
		if (lowerFilename.endsWith(".bz2")) {
			parseFileBzip();
		} else if (lowerFilename.endsWith(".json")) {
			parseFilePlain();
		} else {
			throw new Exception("unrecognized file format: " + file.getFileName().toString());
		}
	}

	// --- integrage

	private void integrate() throws Exception {

		db.insertProperties();

		db.createIndexes();

		final String sql = "SELECT DISTINCT(claim_item.item_id) FROM claim_item LEFT OUTER JOIN item ON (claim_item.value = item.item_id) WHERE item.item_id IS NULL";
		db.blaaa(sql, 100000, ImportDatabase::deleteClaimItems);

		db.addConstraints();
	}

	// ---

	//private static final int MAX_ITEMS = 57000000;
	private static final int MAX_ITEMS = 10000;

	private static final int BUFFER_SIZE_READER = 16 * 1024;
	private static final int BUFFER_SIZE_STREAM = 16 * 1024;

	public static final int MAX_STRING_LENGTH = 100;

	public static boolean isImportRunning;

	public static final int[] intArray(final Collection<? extends Number> c) {
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

}
