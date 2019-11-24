package org.schmied.questio.importer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalTime;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.schmied.questio.importer.db.ImporterDatabase;
import org.schmied.questio.importer.entity.*;
import org.schmied.questio.importer.parser.Parser;

public class Importer {

	private static final int MAX_ITEMS = 57000000;

	private static final int MAX_STRING_LENGTH = 100;

	private static final int BUFFER_SIZE_READER = 16 * 1024;
	private static final int BUFFER_SIZE_STREAM = 16 * 1024;

	@SuppressWarnings("all")
	public static final String validString(String s) {
		if (s == null)
			return null;
		s = s.trim();
		if (s.isEmpty())
			return null;
		if (s.length() > MAX_STRING_LENGTH) {
			if (s.length() > 2 * MAX_STRING_LENGTH)
				return null;
			s = s.substring(0, MAX_STRING_LENGTH);
		}
		return s;
	}

	private static void importStream(final ImporterDatabase db, final Parser parser, final InputStream is) throws Exception {
		try (final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.US_ASCII); final BufferedReader br = new BufferedReader(isr, BUFFER_SIZE_READER)) {

			final long ticks = System.currentTimeMillis();
			int countImported = 0;

			parser.initialize(br);

			int countRead = 0;
			for (;;) {
				try {
					final ItemEntity item = parser.readItem(br);
					if (item == null || countRead > MAX_ITEMS) {
						final long secondsElapsed = (System.currentTimeMillis() - ticks) / 1000;
						System.out.println(countImported + " / " + countRead + " " + (Math.round(100.0 * countImported / countRead)) + "% in "
								+ LocalTime.ofSecondOfDay(secondsElapsed).toString() + ", " + Math.round((double) countRead / secondsElapsed) + " items/s");
						break;
					}
					if (db.addItem(item))
						countImported++;
					else
						System.out.println("ERROR item " + item.toString());
				} catch (final Exception e) {
					System.out.println(e.getMessage());
					break;
				}
				if (countRead % 1000 == 0 && countRead > 0) {
					final long secondsElapsed = (System.currentTimeMillis() - ticks) / 1000;
					long secondsEta = Math.round((double) secondsElapsed * MAX_ITEMS / countRead - secondsElapsed);
					if (secondsEta < 0)
						secondsEta = 0;
					System.out.println(countImported + " / " + countRead + " " + (Math.round(100.0 * countImported / countRead)) + "% in "
							+ LocalTime.ofSecondOfDay(secondsElapsed).toString() + ", " + Math.round((double) countRead / secondsElapsed) + " items/s, ETA "
							+ (secondsEta > 86000 ? ">1d (" + Math.round(secondsEta / 60.0f / 60.0f) + "h)" : LocalTime.ofSecondOfDay(secondsEta).toString()));
				}
				countRead++;
			}

		} catch (final Exception e) {
			throw e;
		} finally {
			db.closeImport();
		}
	}

	private static void importPlainFile(final ImporterDatabase db, final Parser parser, final Path file) throws Exception {
		try (final InputStream is = Files.newInputStream(file); final BufferedInputStream bis = new BufferedInputStream(is, BUFFER_SIZE_STREAM)) {
			importStream(db, parser, bis);
		} catch (final Exception e) {
			throw e;
		}
	}

	private static void importBzipFile(final ImporterDatabase db, final Parser parser, final Path file) throws Exception {
		try (final InputStream is = Files.newInputStream(file);
				final BufferedInputStream bis = new BufferedInputStream(is, BUFFER_SIZE_STREAM);
				final BZip2CompressorInputStream bzis = new BZip2CompressorInputStream(bis)) {
			importStream(db, parser, bzis);
		} catch (final Exception e) {
			throw e;
		}
	}

	private static void importFile(final ImporterDatabase db, final Parser parser, final Path file) throws Exception {

		//final Path file = Paths.get(DUMP_FILE);
		if (!Files.isRegularFile(file))
			throw new Exception("file does not exist: " + file.toAbsolutePath().toString());

		db.recreateTables();
		db.insertProperties();

		final String lowerFilename = file.getFileName().toString().toLowerCase();
		if (lowerFilename.endsWith(".bz2")) {
			importBzipFile(db, parser, file);
		} else if (lowerFilename.endsWith(".json")) {
			importPlainFile(db, parser, file);
		} else {
			throw new Exception("unrecognized file format: " + file.getFileName().toString());
		}

		db.createIndexes();
		ClaimItemEntity.deleteInvalidReferences(db.connection());
		db.addConstraints();
	}
}
