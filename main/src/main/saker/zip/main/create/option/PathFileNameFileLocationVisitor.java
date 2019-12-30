package saker.zip.main.create.option;

import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocationVisitor;
import saker.std.api.file.location.LocalFileLocation;

public class PathFileNameFileLocationVisitor implements FileLocationVisitor {
	private String fileName;

	@Override
	public void visit(ExecutionFileLocation loc) {
		fileName = loc.getPath().getFileName();
	}

	@Override
	public void visit(LocalFileLocation loc) {
		fileName = loc.getLocalPath().getFileName();
	}

	public String getFileName() {
		return fileName;
	}
}
