package org.schmied.questio.web;

import java.io.Serializable;
import java.nio.file.*;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.faces.model.*;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

@ViewScoped
@Named
public class AdminBean implements Serializable {

	private static final long serialVersionUID = 7856501155722117039L;

	private transient DataModel<String> dmFiles;

	private String importDirectory;

	private Path dir() {
		Path dir = null;
		if (importDirectory != null && !importDirectory.trim().isEmpty())
			dir = Paths.get(importDirectory).toAbsolutePath();
		if (dir == null || !Files.exists(dir) || !Files.isDirectory(dir))
			dir = Paths.get("/").toAbsolutePath();
		return dir;
	}

	public Object actionSetImportDirectory() {
		final Path dir = dir();
		if (dir == null)
			return null;
		importDirectory = dir.toString();
		return null;
	}

	public Object actionImportFile() {
		if (dmFiles != null)
			System.out.println(">>>> importFile() " + dmFiles.getRowData());
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

	public String getImportDirectory() {
		return importDirectory;
	}

	public void setImportDirectory(final String importDirectory) {
		this.importDirectory = importDirectory;
	}
}
