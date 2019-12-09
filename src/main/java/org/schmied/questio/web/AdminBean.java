package org.schmied.questio.web;

import java.io.Serializable;
import java.nio.file.*;
import java.sql.*;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.faces.model.*;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.schmied.questio.importer.Importer;
import org.schmied.questio.importer.db.*;
import org.schmied.questio.importer.parser.*;

@ViewScoped
@Named
public class AdminBean implements Serializable {

	private static final long serialVersionUID = 7856501155722117039L;

	private transient DataModel<String> dmFiles;

	private String directory, selection, dbConnectionUrl;

	private Path dir() {
		Path dir = null;
		if (directory != null && !directory.trim().isEmpty())
			dir = Paths.get(directory).toAbsolutePath();
		if (dir == null || !Files.exists(dir) || !Files.isDirectory(dir))
			dir = Paths.get("/").toAbsolutePath();
		return dir;
	}

	public Object actionSetDirectory() {
		final Path dir = dir();
		if (dir == null)
			return null;
		directory = dir.toString();
		return null;
	}

	public DataModel<String> getDmFiles() {
		if (dmFiles == null)
			dmFiles = new ListDataModel<>();
		final Path dir = dir();
		if (dir == null) {
			dmFiles.setWrappedData(null);
			return null;
		}
		dmFiles.setWrappedData(Arrays.asList(dir.toFile().listFiles()).stream().map(f -> f.toPath().getFileName().toString()).sorted().collect(Collectors.toList()));
		return dmFiles;
	}

	// ---

	public Object actionSelect() {
		selection = null;
		if (dmFiles == null)
			return null;
		final Path dir = dir();
		final Path p = dir.resolve(dmFiles.getRowData());
		final String absolutePath = p.toAbsolutePath().toString();
		System.out.println(">>>> importFile() " + absolutePath);
		if (Files.isDirectory(p)) {
			directory = absolutePath;
			actionSetDirectory();
			return null;
		}
		selection = absolutePath;
		return null;
	}

	public Object actionImport() throws Exception {
		if (selection == null)
			return null;
		final Path p = Paths.get(selection);
		if (!Files.isReadable(p) || Files.isDirectory(p))
			return null;
		final Connection cn = DriverManager.getConnection(dbConnectionUrl);
		final ImportDatabase db = new CopyDatabase(cn);
		final Parser parser = new JsonParser();
		Importer.fullImport(db, parser, p);
		return null;
	}

	// ---

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(final String directory) {
		this.directory = directory;
	}

	public String getSelection() {
		return selection;
	}

	public void setSelection(String selection) {
		this.selection = selection;
	}

	public String getDbConnectionUrl() {
		if (dbConnectionUrl == null)
			dbConnectionUrl = "jdbc:postgresql://localhost/questio?user=postgres&password=postgres";
		return dbConnectionUrl;
	}

	public void setDbConnectionUrl(String dbConnectionUrl) {
		this.dbConnectionUrl = dbConnectionUrl;
	}
}
